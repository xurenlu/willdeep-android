#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "json"
require "net/http"
require "open3"
require "shellwords"
require "time"
require "uri"

ROOT_DIR = File.expand_path("..", __dir__)
OUT_DIR = File.join(ROOT_DIR, "build", "android_connected_smoke")
REPORT_JSON = File.join(OUT_DIR, "report.json")
REPORT_MD = File.join(OUT_DIR, "report.md")
APP_VERSION = File.read(File.join(ROOT_DIR, "app", "build.gradle.kts"))[/versionName\s*=\s*"([^"]+)"/, 1]
REQUIRE_DEVICE = ENV["REQUIRE_ANDROID_DEVICE"] == "1"
LIVE_PAIRING_PAYLOAD = ENV.fetch("MOBILE_GATEWAY_PAIRING_PAYLOAD", "")
LIVE_DEVICE_NAME = ENV.fetch("MOBILE_GATEWAY_DEVICE_NAME", "Android Live Smoke")
LIVE_MESSAGE = ENV.fetch("MOBILE_GATEWAY_LIVE_MESSAGE", "")
EXPECT_AGENT_ACTIVITY = ENV["MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY"] == "1"
AGENT_ACTIVITY_TIMEOUT_MS = ENV.fetch("MOBILE_GATEWAY_AGENT_ACTIVITY_TIMEOUT_MS", "60000")
RUN_AGENT_ACTIVITY_CHECK = EXPECT_AGENT_ACTIVITY && !LIVE_PAIRING_PAYLOAD.empty? && !LIVE_MESSAGE.empty?
SKIP_HEALTH_PREFLIGHT = ENV["MOBILE_GATEWAY_SKIP_HEALTH_PREFLIGHT"] == "1"
HEALTH_PREFLIGHT_TIMEOUT_SECONDS = ENV.fetch("MOBILE_GATEWAY_HEALTH_TIMEOUT_SECONDS", "5").to_f
SKIP_DEVICE_REACHABILITY = ENV["MOBILE_GATEWAY_SKIP_DEVICE_REACHABILITY"] == "1"
DEVICE_REACHABILITY_TIMEOUT_SECONDS = ENV.fetch("MOBILE_GATEWAY_DEVICE_REACHABILITY_TIMEOUT_SECONDS", "5").to_i
SENSITIVE_REPORT_VALUES = [LIVE_PAIRING_PAYLOAD, LIVE_MESSAGE].reject(&:empty?)

raise "versionName not found" if APP_VERSION.nil? || APP_VERSION.empty?

class CommandRunner
  attr_reader :steps

  def initialize(root_dir)
    @root_dir = root_dir
    @steps = []
  end

  def run(name, *command, allow_failure: false, redacted_command: nil)
    started = Time.now
    stdout, stderr, status = Open3.capture3(*command, chdir: @root_dir)
    step = {
      name: name,
      command: redacted_command || command.join(" "),
      status: status.success? ? "passed" : "failed",
      exit_code: status.exitstatus,
      duration_ms: ((Time.now - started) * 1000).round,
      stdout: redact(stdout.strip),
      stderr: redact(stderr.strip),
    }
    @steps << step
    raise "#{name} failed with exit #{status.exitstatus}" if !status.success? && !allow_failure

    step
  end

  def skip(name, reason)
    @steps << {
      name: name,
      status: "skipped",
      duration_ms: 0,
      reason: reason,
    }
  end

  def check(name)
    started = Time.now
    detail = yield
    @steps << {
      name: name,
      status: "passed",
      duration_ms: ((Time.now - started) * 1000).round,
      detail: detail,
    }
  rescue StandardError => e
    @steps << {
      name: name,
      status: "failed",
      duration_ms: ((Time.now - started) * 1000).round,
      detail: redact(e.message),
    }
    raise
  end

  private

  def redact(text)
    return text if SENSITIVE_REPORT_VALUES.empty?

    SENSITIVE_REPORT_VALUES.reduce(text) do |current, value|
      current.gsub(value, "<redacted>")
    end
  end
end

def parse_adb_devices(output)
  output.lines.filter_map do |line|
    next if line.strip.empty?
    next if line.start_with?("List of devices")

    serial, state = line.split(/\s+/, 2)
    next if serial.nil? || state.nil?

    { serial: serial, state: state.strip }
  end
end

def parse_live_pairing_payload(payload)
  JSON.parse(payload)
rescue JSON::ParserError => e
  raise "invalid pairing payload JSON: #{e.message}"
end

def validate_live_pairing_payload(payload)
  return "no live payload provided" if payload.empty?

  data = parse_live_pairing_payload(payload)
  required_fields = %w[base_url pairing_token protocol_version desktop_name expires_at]
  missing_fields = required_fields.select { |field| data[field].to_s.strip.empty? }
  raise "missing pairing payload fields: #{missing_fields.join(", ")}" unless missing_fields.empty?
  raise "unsupported protocol_version: #{data["protocol_version"]}" unless data["protocol_version"] == "mobile-gateway.v1"
  base_url = URI.parse(data["base_url"])
  raise "unsupported base_url scheme: #{base_url.scheme}" unless %w[http https].include?(base_url.scheme)

  expires_at = Time.parse(data["expires_at"])
  seconds_remaining = (expires_at - Time.now.utc).round
  raise "pairing payload expired at #{expires_at.utc.iso8601}" unless seconds_remaining.positive?

  "valid payload; expires in #{seconds_remaining}s"
end

def check_live_gateway_health(payload)
  return "no live payload provided" if payload.empty?
  return "skipped by MOBILE_GATEWAY_SKIP_HEALTH_PREFLIGHT=1" if SKIP_HEALTH_PREFLIGHT

  pairing = parse_live_pairing_payload(payload)
  base_url = pairing.fetch("base_url").to_s.sub(%r{/+\z}, "")
  uri = URI.parse("#{base_url}/mobile/health")
  request = Net::HTTP::Get.new(uri)
  request["X-App-Version"] = APP_VERSION
  response = Net::HTTP.start(
    uri.host,
    uri.port,
    use_ssl: uri.scheme == "https",
    open_timeout: HEALTH_PREFLIGHT_TIMEOUT_SECONDS,
    read_timeout: HEALTH_PREFLIGHT_TIMEOUT_SECONDS,
  ) do |http|
    http.request(request)
  end
  raise "health status #{response.code}" unless response.is_a?(Net::HTTPSuccess)

  body = JSON.parse(response.body)
  data = body.fetch("data")
  protocol_version = data.fetch("protocol_version")
  raise "health protocol mismatch: #{protocol_version}" unless protocol_version == "mobile-gateway.v1"

  pairing_allowed = data["pairing_allowed"]
  raise "gateway health reports pairing_allowed=false" unless pairing_allowed == true

  server_version = response["X-Server-Version"] || data.dig("version", "version") || "unknown"
  "health ok; server=#{server_version}; pairing_allowed=true"
rescue JSON::ParserError => e
  raise "invalid health JSON: #{e.message}"
rescue KeyError => e
  raise "health response missing #{e.key}"
end

def gateway_uri_from_payload(payload)
  pairing = parse_live_pairing_payload(payload)
  URI.parse(pairing.fetch("base_url").to_s.sub(%r{/+\z}, ""))
end

def check_device_gateway_reachability(runner, devices, payload)
  return false if payload.empty? || SKIP_DEVICE_REACHABILITY || devices.empty?

  uri = gateway_uri_from_payload(payload)
  host = uri.host
  port = uri.port
  timeout = DEVICE_REACHABILITY_TIMEOUT_SECONDS.clamp(1, 30)
  shell_command = [
    "toybox nc -z -w #{timeout} #{Shellwords.escape(host)} #{port} >/dev/null 2>&1",
    "nc -z -w #{timeout} #{Shellwords.escape(host)} #{port} >/dev/null 2>&1",
    "ping -c 1 -W #{timeout} #{Shellwords.escape(host)} >/dev/null 2>&1",
  ].join(" || ")
  devices.each do |device|
    serial = device.fetch(:serial)
    runner.run(
      "check device gateway reachability",
      "adb",
      "-s",
      serial,
      "shell",
      "sh",
      "-c",
      shell_command,
      redacted_command: "adb -s #{serial} shell gateway reachability check",
    )
  end
  true
end

def write_reports(result)
  FileUtils.mkdir_p(OUT_DIR)
  File.write(REPORT_JSON, JSON.pretty_generate(result) + "\n")
  File.write(REPORT_MD, markdown_report(result))
end

def markdown_report(result)
  lines = []
  lines << "# Android Connected Smoke Test"
  lines << ""
  lines << "- Generated at: `#{result[:generated_at]}`"
  lines << "- Android version: `#{result[:app_version]}`"
  lines << "- Status: `#{result[:status]}`"
  lines << "- Devices: #{result[:devices].empty? ? "_none_" : result[:devices].map { |device| "`#{device[:serial]}` (#{device[:state]})" }.join(", ")}"
  lines << "- Live Mac payload: `#{result[:live_payload_provided] ? "provided" : "not provided"}`"
  lines << "- Live message: `#{result[:live_message_provided] ? "provided" : "not provided"}`"
  lines << "- Agent activity check: `#{result[:agent_activity_check_enabled] ? "enabled" : "disabled"}`"
  lines << "- Health preflight: `#{result[:health_preflight_enabled] ? "enabled" : "disabled"}`"
  lines << "- Device reachability check: `#{result[:device_reachability_check_enabled] ? "enabled" : "disabled"}`"
  lines << ""
  lines << "## Steps"
  lines << ""
  lines << "| Step | Status | Duration | Detail |"
  lines << "| --- | --- | ---: | --- |"
  result[:steps].each do |step|
    detail = step[:reason] || step[:detail] || step[:command] || ""
    lines << "| #{step[:name]} | `#{step[:status]}` | #{step[:duration_ms]} ms | #{detail.gsub("|", "\\|")} |"
  end
  lines << ""
  lines.join("\n")
end

runner = CommandRunner.new(ROOT_DIR)
status = "passed"
error = nil
device_reachability_check_enabled = false

begin
  runner.check("validate live pairing payload") { validate_live_pairing_payload(LIVE_PAIRING_PAYLOAD) }
  runner.check("check live gateway health") { check_live_gateway_health(LIVE_PAIRING_PAYLOAD) }
  adb = runner.run("detect connected Android devices", "adb", "devices", allow_failure: !REQUIRE_DEVICE)
  if adb[:status] == "failed"
    status = "failed"
    error = "adb devices failed"
    devices = []
  else
    devices = parse_adb_devices(adb[:stdout]).select { |device| device[:state] == "device" }
    if devices.empty?
      status = REQUIRE_DEVICE ? "failed" : "skipped"
      runner.skip("run connectedDebugAndroidTest", "no connected Android device")
      error = "no connected Android device" if REQUIRE_DEVICE
    else
      device_reachability_check_enabled = check_device_gateway_reachability(runner, devices, LIVE_PAIRING_PAYLOAD)
      runner.run("build instrumented test APK", "./gradlew", ":app:assembleDebugAndroidTest")
      connected_command = ["./gradlew", ":app:connectedDebugAndroidTest"]
      redacted_connected_command = connected_command.dup
      unless LIVE_PAIRING_PAYLOAD.empty?
        connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayPairingPayload=#{LIVE_PAIRING_PAYLOAD}"
        connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayDeviceName=#{LIVE_DEVICE_NAME}"
        redacted_connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayPairingPayload=<redacted>"
        redacted_connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayDeviceName=#{LIVE_DEVICE_NAME}"
        unless LIVE_MESSAGE.empty?
          connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayLiveMessage=#{LIVE_MESSAGE}"
          redacted_connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayLiveMessage=<redacted>"
          if RUN_AGENT_ACTIVITY_CHECK
            connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayExpectAgentActivity=1"
            connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayAgentActivityTimeoutMillis=#{AGENT_ACTIVITY_TIMEOUT_MS}"
            redacted_connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayExpectAgentActivity=1"
            redacted_connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayAgentActivityTimeoutMillis=#{AGENT_ACTIVITY_TIMEOUT_MS}"
          end
        end
      end
      runner.run(
        "run connected instrumented smoke",
        *connected_command,
        redacted_command: redacted_connected_command.join(" "),
      )
    end
  end
rescue StandardError => e
  status = "failed"
  error = "#{e.class}: #{e.message}"
end

result = {
  generated_at: Time.now.utc.iso8601,
  app_version: APP_VERSION,
  status: status,
  error: error,
  devices: devices || [],
  require_device: REQUIRE_DEVICE,
  live_payload_provided: !LIVE_PAIRING_PAYLOAD.empty?,
  live_message_provided: !LIVE_MESSAGE.empty?,
  expect_agent_activity: EXPECT_AGENT_ACTIVITY,
  agent_activity_check_enabled: RUN_AGENT_ACTIVITY_CHECK,
  agent_activity_timeout_ms: AGENT_ACTIVITY_TIMEOUT_MS,
  health_preflight_enabled: !LIVE_PAIRING_PAYLOAD.empty? && !SKIP_HEALTH_PREFLIGHT,
  health_preflight_timeout_seconds: HEALTH_PREFLIGHT_TIMEOUT_SECONDS,
  device_reachability_check_enabled: device_reachability_check_enabled,
  device_reachability_timeout_seconds: DEVICE_REACHABILITY_TIMEOUT_SECONDS,
  steps: runner.steps,
}

write_reports(result)

puts "Wrote #{REPORT_JSON}"
puts "Wrote #{REPORT_MD}"
exit(status == "failed" ? 1 : 0)
