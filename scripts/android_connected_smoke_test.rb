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
LIVE_PAIRING_PAYLOAD_ENV = ENV.fetch("MOBILE_GATEWAY_PAIRING_PAYLOAD", "")
DESKTOP_PAIRING_BASE_URL_ENV = ENV.fetch("MOBILE_GATEWAY_DESKTOP_BASE_URL", "")
DESKTOP_PAIRING_TOKEN = ENV.fetch("MOBILE_GATEWAY_DESKTOP_TOKEN", "")
DESKTOP_PAIRING_TOKEN_FILE_ENV = ENV.fetch("MOBILE_GATEWAY_DESKTOP_TOKEN_FILE", "")
MAC_BUNDLE_ID = ENV.fetch("MOBILE_GATEWAY_MAC_BUNDLE_ID", "com.willdeep.app")
DEFAULT_DESKTOP_PAIRING_PORT = ENV.fetch("MOBILE_GATEWAY_DESKTOP_PORT", "8876")
DEFAULT_DESKTOP_PAIRING_BASE_URL = "http://127.0.0.1:#{DEFAULT_DESKTOP_PAIRING_PORT}"
DEFAULT_DESKTOP_PAIRING_TOKEN_FILE = File.join(
  Dir.home,
  "Library",
  "Application Support",
  MAC_BUNDLE_ID,
  "MobileGateway",
  "desktop-token",
)
AUTO_DESKTOP_PAIRING_TOKEN_FILE = if LIVE_PAIRING_PAYLOAD_ENV.empty? &&
                                     DESKTOP_PAIRING_BASE_URL_ENV.empty? &&
                                     DESKTOP_PAIRING_TOKEN.empty? &&
                                     DESKTOP_PAIRING_TOKEN_FILE_ENV.empty? &&
                                     File.file?(DEFAULT_DESKTOP_PAIRING_TOKEN_FILE)
                                    DEFAULT_DESKTOP_PAIRING_TOKEN_FILE
                                  else
                                    ""
                                  end
DESKTOP_PAIRING_BASE_URL = DESKTOP_PAIRING_BASE_URL_ENV.empty? && !AUTO_DESKTOP_PAIRING_TOKEN_FILE.empty? ? DEFAULT_DESKTOP_PAIRING_BASE_URL : DESKTOP_PAIRING_BASE_URL_ENV
DESKTOP_PAIRING_TOKEN_FILE = DESKTOP_PAIRING_TOKEN_FILE_ENV.empty? ? AUTO_DESKTOP_PAIRING_TOKEN_FILE : DESKTOP_PAIRING_TOKEN_FILE_ENV
DESKTOP_PAIRING_AUTO_DISCOVERED = !AUTO_DESKTOP_PAIRING_TOKEN_FILE.empty?
DESKTOP_PAIRING_TIMEOUT_SECONDS = ENV.fetch("MOBILE_GATEWAY_DESKTOP_PAIRING_TIMEOUT_SECONDS", "5").to_f
LIVE_DEVICE_NAME = ENV.fetch("MOBILE_GATEWAY_DEVICE_NAME", "Android Live Smoke")
LIVE_MESSAGE = ENV.fetch("MOBILE_GATEWAY_LIVE_MESSAGE", "")
EXPECT_AGENT_ACTIVITY = ENV["MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY"] == "1"
AGENT_ACTIVITY_TIMEOUT_MS = ENV.fetch("MOBILE_GATEWAY_AGENT_ACTIVITY_TIMEOUT_MS", "60000")
SKIP_HEALTH_PREFLIGHT = ENV["MOBILE_GATEWAY_SKIP_HEALTH_PREFLIGHT"] == "1"
HEALTH_PREFLIGHT_TIMEOUT_SECONDS = ENV.fetch("MOBILE_GATEWAY_HEALTH_TIMEOUT_SECONDS", "5").to_f
SKIP_DEVICE_REACHABILITY = ENV["MOBILE_GATEWAY_SKIP_DEVICE_REACHABILITY"] == "1"
DEVICE_REACHABILITY_TIMEOUT_SECONDS = ENV.fetch("MOBILE_GATEWAY_DEVICE_REACHABILITY_TIMEOUT_SECONDS", "5").to_i
SENSITIVE_REPORT_VALUES = [LIVE_PAIRING_PAYLOAD_ENV, LIVE_MESSAGE, DESKTOP_PAIRING_TOKEN].reject(&:empty?)
LIVE_SMOKE_LOG_TAG = "WillDeepLiveSmoke"

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

def parse_gateway_base_url(value)
  uri = URI.parse(value.to_s)
  raise "unsupported base_url scheme: #{uri.scheme}" unless %w[http https].include?(uri.scheme)
  raise "missing base_url host" if uri.host.to_s.strip.empty?

  uri
rescue URI::InvalidURIError => e
  raise "invalid base_url: #{e.message}"
end

def parse_pairing_expiry(value)
  Time.iso8601(value.to_s)
rescue ArgumentError => e
  raise "invalid expires_at: #{e.message}"
end

def perform_direct_http_request(uri, request, timeout_seconds)
  http = Net::HTTP.new(uri.host, uri.port, nil)
  http.use_ssl = uri.scheme == "https"
  http.open_timeout = timeout_seconds
  http.read_timeout = timeout_seconds
  http.start { |connection| connection.request(request) }
end

def validate_live_pairing_payload(payload)
  return "no live payload provided" if payload.empty?

  data = parse_live_pairing_payload(payload)
  required_fields = %w[base_url pairing_token protocol_version desktop_name expires_at]
  missing_fields = required_fields.select { |field| data[field].to_s.strip.empty? }
  raise "missing pairing payload fields: #{missing_fields.join(", ")}" unless missing_fields.empty?
  raise "unsupported protocol_version: #{data["protocol_version"]}" unless data["protocol_version"] == "mobile-gateway.v1"
  parse_gateway_base_url(data["base_url"])

  expires_at = parse_pairing_expiry(data["expires_at"])
  seconds_remaining = (expires_at - Time.now.utc).round
  raise "pairing payload expired at #{expires_at.utc.iso8601}" unless seconds_remaining.positive?

  "valid payload; expires in #{seconds_remaining}s"
end

def desktop_pairing_fetch_requested?
  [
    DESKTOP_PAIRING_BASE_URL,
    DESKTOP_PAIRING_TOKEN,
    DESKTOP_PAIRING_TOKEN_FILE,
  ].any? { |value| !value.to_s.strip.empty? }
end

def desktop_pairing_token
  token = DESKTOP_PAIRING_TOKEN.to_s.strip
  return token unless token.empty?
  raise "MOBILE_GATEWAY_DESKTOP_TOKEN_FILE is empty" if DESKTOP_PAIRING_TOKEN_FILE.to_s.strip.empty?

  File.read(DESKTOP_PAIRING_TOKEN_FILE).strip.tap do |file_token|
    raise "desktop token file is empty" if file_token.empty?

    SENSITIVE_REPORT_VALUES << file_token
  end
rescue Errno::ENOENT
  raise "desktop token file not found: #{DESKTOP_PAIRING_TOKEN_FILE}"
rescue Errno::EACCES
  raise "desktop token file is not readable: #{DESKTOP_PAIRING_TOKEN_FILE}"
end

def desktop_pairing_request(method, path, token)
  base_url = DESKTOP_PAIRING_BASE_URL.to_s.sub(%r{/+\z}, "")
  parse_gateway_base_url(base_url)
  uri = URI.parse("#{base_url}#{path}")
  request = case method
            when :get then Net::HTTP::Get.new(uri)
            when :post then Net::HTTP::Post.new(uri)
            else raise "unsupported desktop pairing method: #{method}"
            end
  request["Authorization"] = "Bearer #{token}"
  request["X-App-Version"] = APP_VERSION
  if method == :post
    request["Content-Type"] = "application/json"
    request.body = "{}"
  end
  perform_direct_http_request(uri, request, DESKTOP_PAIRING_TIMEOUT_SECONDS)
end

def pairing_payload_from_desktop_response(response, action)
  raise "desktop pairing #{action} status #{response.code}" unless response.is_a?(Net::HTTPSuccess)

  body = JSON.parse(response.body)
  payload = body.dig("data", "pairing_payload")
  raise "desktop pairing response missing pairing_payload" unless payload.is_a?(Hash)

  payload_json = JSON.generate(payload)
  SENSITIVE_REPORT_VALUES << payload_json
  SENSITIVE_REPORT_VALUES << payload["pairing_token"].to_s if payload["pairing_token"]
  payload_json
rescue JSON::ParserError => e
  raise "invalid desktop pairing JSON: #{e.message}"
end

def fetch_desktop_pairing_payload
  raise "MOBILE_GATEWAY_DESKTOP_BASE_URL is required to fetch pairing payload" if DESKTOP_PAIRING_BASE_URL.to_s.strip.empty?
  if DESKTOP_PAIRING_TOKEN.to_s.strip.empty? && DESKTOP_PAIRING_TOKEN_FILE.to_s.strip.empty?
    raise "MOBILE_GATEWAY_DESKTOP_TOKEN or MOBILE_GATEWAY_DESKTOP_TOKEN_FILE is required to fetch pairing payload"
  end

  token = desktop_pairing_token
  rotate_response = desktop_pairing_request(:post, "/mobile/pairing/rotate", token)
  if rotate_response.is_a?(Net::HTTPSuccess)
    return [
      pairing_payload_from_desktop_response(rotate_response, "rotate"),
      "desktop-rotate",
    ]
  end

  unless %w[404 405].include?(rotate_response.code)
    raise "desktop pairing rotate status #{rotate_response.code}"
  end

  get_response = desktop_pairing_request(:get, "/mobile/pairing", token)
  [
    pairing_payload_from_desktop_response(get_response, "fetch"),
    "desktop-get",
  ]
end

def resolve_live_pairing_payload(current_payload)
  return [current_payload, "env"] unless current_payload.empty?
  return ["", "none"] unless desktop_pairing_fetch_requested?

  fetch_desktop_pairing_payload
end

def check_live_gateway_health(payload)
  return "no live payload provided" if payload.empty?
  return "skipped by MOBILE_GATEWAY_SKIP_HEALTH_PREFLIGHT=1" if SKIP_HEALTH_PREFLIGHT

  pairing = parse_live_pairing_payload(payload)
  base_url = pairing.fetch("base_url").to_s.sub(%r{/+\z}, "")
  uri = URI.parse("#{base_url}/mobile/health")
  request = Net::HTTP::Get.new(uri)
  request["X-App-Version"] = APP_VERSION
  response = perform_direct_http_request(uri, request, HEALTH_PREFLIGHT_TIMEOUT_SECONDS)
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

def collect_device_network_diagnostics(runner, devices)
  return false if devices.empty?

  shell_command = [
    "ip route 2>/dev/null",
    "ip -o addr show scope global 2>/dev/null",
  ].join("; ")
  redacted_shell_command = "( #{shell_command} ) | sed -E 's/[0-9]+(\\.[0-9]+){3}/<ipv4>/g'"
  devices.each do |device|
    serial = device.fetch(:serial)
    runner.run(
      "collect device network diagnostics",
      "adb",
      "-s",
      serial,
      "shell",
      "sh",
      "-c",
      redacted_shell_command,
      allow_failure: true,
      redacted_command: "adb -s #{serial} shell network diagnostics",
    )
  end
  true
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

def clear_live_smoke_logcat(runner, devices)
  devices.each do |device|
    serial = device.fetch(:serial)
    runner.run(
      "clear live smoke logcat",
      "adb",
      "-s",
      serial,
      "logcat",
      "-c",
      allow_failure: true,
      redacted_command: "adb -s #{serial} logcat -c",
    )
  end
end

def collect_live_smoke_logcat(runner, devices)
  devices.each do |device|
    serial = device.fetch(:serial)
    runner.run(
      "collect live smoke logcat",
      "adb",
      "-s",
      serial,
      "logcat",
      "-d",
      "-s",
      "#{LIVE_SMOKE_LOG_TAG}:I",
      "*:S",
      allow_failure: true,
      redacted_command: "adb -s #{serial} logcat -d -s #{LIVE_SMOKE_LOG_TAG}:I '*:S'",
    )
  end
end

def live_smoke_markers(steps)
  log_output = steps.select { |step| step[:name] == "collect live smoke logcat" }
                    .map { |step| step[:stdout].to_s }
                    .join("\n")
  {
    mobile_message_send_ack: log_output[/mobile_message_send_ack=([a-z_]+)/, 1],
    mac_agent_activity_signal: log_output[/mac_agent_activity_signal=([a-z_]+)/, 1],
  }
end

def write_reports(result)
  FileUtils.mkdir_p(OUT_DIR)
  File.write(REPORT_JSON, JSON.pretty_generate(result) + "\n")
  File.write(REPORT_MD, markdown_report(result))
end

def build_next_actions(result)
  actions = []
  unless result[:live_payload_provided]
    actions << "Start WillDeep on the Mac, enable Settings > Mobile Gateway, and allow new device pairing; the smoke runner will auto-discover the default desktop-token file when the gateway is running."
    actions << "Alternatively set MOBILE_GATEWAY_PAIRING_PAYLOAD, or set MOBILE_GATEWAY_DESKTOP_BASE_URL with MOBILE_GATEWAY_DESKTOP_TOKEN or MOBILE_GATEWAY_DESKTOP_TOKEN_FILE."
  end
  if result[:devices].empty?
    actions << "Connect an Android device with USB debugging enabled, then verify it appears in `adb devices` with state `device`."
  end
  if result[:live_payload_provided] && result[:devices].empty?
    actions << "After a device is attached, rerun the smoke command before the pairing payload expires."
  end
  if result[:live_payload_provided] && !result[:health_preflight_enabled]
    actions << "Run without MOBILE_GATEWAY_SKIP_HEALTH_PREFLIGHT=1 before final acceptance so Mac gateway reachability and pairing availability are verified."
  end
  if result[:devices].any? && !result[:live_payload_provided]
    actions << "Provide or auto-discover a live Mac pairing payload before connected instrumentation can pair with the gateway."
  end
  if result[:live_payload_provided] && result[:devices].any? && !result[:live_message_provided]
    actions << "Set MOBILE_GATEWAY_LIVE_MESSAGE to send a real Android-originated request into the Mac Agent runtime."
  end
  if result[:live_message_provided] && !result[:expect_agent_activity]
    actions << "Set MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY=1 for final acceptance so the live test waits for Mac-side Agent activity after message.send."
  end
  actions.uniq
end

def step_status(result, name)
  result[:steps].find { |step| step[:name] == name }&.fetch(:status, nil)
end

def evidence_status(result, step_name, pending:, skipped: nil)
  status = step_status(result, step_name)
  return ["passed", nil] if status == "passed"
  return ["failed", nil] if status == "failed"
  return ["skipped", skipped] if skipped

  ["pending", pending]
end

def build_acceptance_evidence(result)
  payload_status, payload_detail = if result[:live_payload_provided]
                                     evidence_status(
                                       result,
                                       "validate live pairing payload",
                                       pending: "Validate a live Mac pairing payload before instrumentation."
                                     )
                                   else
                                     ["pending", "Provide or auto-discover a live Mac pairing payload."]
                                   end

  health_status, health_detail = if !result[:live_payload_provided]
                                   ["pending", "Health preflight needs a live pairing payload."]
                                 elsif !result[:health_preflight_enabled]
                                   ["skipped", "MOBILE_GATEWAY_SKIP_HEALTH_PREFLIGHT=1 was set."]
                                 else
                                   evidence_status(
                                     result,
                                     "check live gateway health",
                                     pending: "Run the Mac gateway health preflight."
                                   )
                                 end

  device_status = result[:devices].any? ? "passed" : (result[:require_device] ? "failed" : "pending")
  device_detail = result[:devices].any? ? nil : "Attach an Android device with adb state `device`."

  reachability_status, reachability_detail = if !result[:live_payload_provided] || result[:devices].empty?
                                               ["pending", "Needs both a live payload and an attached Android device."]
                                             elsif !result[:device_reachability_check_enabled]
                                               ["skipped", "MOBILE_GATEWAY_SKIP_DEVICE_REACHABILITY=1 was set or the check did not run."]
                                             else
                                               evidence_status(
                                                 result,
                                                 "check device gateway reachability",
                                                 pending: "Run device-side gateway reachability checks."
                                               )
                                             end

  instrumentation_status, instrumentation_detail = evidence_status(
    result,
    "run connected instrumented smoke",
    pending: "Run connectedDebugAndroidTest against an attached device."
  )

  message_status, message_detail = if !result[:live_message_provided]
                                     ["pending", "Set MOBILE_GATEWAY_LIVE_MESSAGE to verify Android-originated message.send."]
                                   elsif instrumentation_status == "passed"
                                     detail = if result[:mobile_message_send_ack]
                                                "Instrumentation marker reported message.send ack=#{result[:mobile_message_send_ack]}."
                                              else
                                                "Instrumentation asserted message.send was acknowledged by the Mac gateway."
                                              end
                                     ["passed", detail]
                                   else
                                     [instrumentation_status, instrumentation_detail]
                                   end

  activity_status, activity_detail = if !result[:expect_agent_activity]
                                      ["pending", "Set MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY=1 for final acceptance."]
                                    elsif !result[:agent_activity_check_enabled]
                                      ["pending", "Agent activity check needs both a live payload and MOBILE_GATEWAY_LIVE_MESSAGE."]
                                    elsif instrumentation_status == "passed"
                                      detail = if result[:mac_agent_activity_signal]
                                                 "Instrumentation observed #{result[:mac_agent_activity_signal]} after message.send."
                                               else
                                                 "Instrumentation observed post-send Mac Agent activity."
                                               end
                                      ["passed", detail]
                                    else
                                      [instrumentation_status, instrumentation_detail]
                                    end

  [
    {
      key: "live_pairing_payload",
      status: payload_status,
      evidence: payload_detail || "Live pairing payload parsed, protocol-checked, and not expired.",
    },
    {
      key: "mac_gateway_health",
      status: health_status,
      evidence: health_detail || "Host-side /mobile/health preflight passed with pairing_allowed=true.",
    },
    {
      key: "android_device",
      status: device_status,
      evidence: device_detail || "#{result[:devices].size} attached Android device(s) are ready.",
    },
    {
      key: "device_gateway_reachability",
      status: reachability_status,
      evidence: reachability_detail || "Attached Android device can reach the Mac gateway host/port.",
    },
    {
      key: "instrumented_pair_and_ws",
      status: instrumentation_status,
      evidence: instrumentation_detail || "Connected instrumentation paired with the live gateway and reached WebSocket connected state.",
    },
    {
      key: "mobile_message_send_ack",
      status: message_status,
      evidence: message_detail || "Android sent message.send and observed an accepted command status.",
    },
    {
      key: "mac_agent_activity_after_send",
      status: activity_status,
      evidence: activity_detail || "Android observed Mac Agent activity after the mobile-originated message.",
    },
  ]
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
  lines << "- Live payload source: `#{result[:live_payload_source]}`"
  lines << "- Desktop gateway auto-discovery: `#{result[:desktop_pairing_auto_discovered] ? "enabled" : "disabled"}`"
  lines << "- Live message: `#{result[:live_message_provided] ? "provided" : "not provided"}`"
  lines << "- Agent activity check: `#{result[:agent_activity_check_enabled] ? "enabled" : "disabled"}`"
  if result[:mac_agent_activity_signal]
    lines << "- Agent activity signal: `#{result[:mac_agent_activity_signal]}`"
  end
  lines << "- Health preflight: `#{result[:health_preflight_enabled] ? "enabled" : "disabled"}`"
  lines << "- Device reachability check: `#{result[:device_reachability_check_enabled] ? "enabled" : "disabled"}`"
  unless result[:next_actions].empty?
    lines << ""
    lines << "## Next Actions"
    lines << ""
    result[:next_actions].each { |action| lines << "- #{action}" }
  end
  unless result[:acceptance_evidence].empty?
    lines << ""
    lines << "## Acceptance Evidence"
    lines << ""
    lines << "| Requirement | Status | Evidence |"
    lines << "| --- | --- | --- |"
    result[:acceptance_evidence].each do |item|
      lines << "| `#{item[:key]}` | `#{item[:status]}` | #{item[:evidence].gsub("|", "\\|")} |"
    end
  end
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
live_pairing_payload = LIVE_PAIRING_PAYLOAD_ENV
live_payload_source = live_pairing_payload.empty? ? "none" : "env"
run_agent_activity_check = false

begin
  runner.check("resolve live pairing payload") do
    live_pairing_payload, live_payload_source = resolve_live_pairing_payload(live_pairing_payload)
    case live_payload_source
    when "env"
      "using MOBILE_GATEWAY_PAIRING_PAYLOAD"
    when "desktop-rotate"
      "rotated and fetched from desktop pairing endpoint"
    when "desktop-get"
      "fetched from legacy desktop pairing endpoint"
    else
      "no live payload configured"
    end
  end
  run_agent_activity_check = EXPECT_AGENT_ACTIVITY && !live_pairing_payload.empty? && !LIVE_MESSAGE.empty?
  runner.check("validate live pairing payload") { validate_live_pairing_payload(live_pairing_payload) }
  runner.check("check live gateway health") { check_live_gateway_health(live_pairing_payload) }
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
      collect_device_network_diagnostics(runner, devices)
      device_reachability_check_enabled = check_device_gateway_reachability(runner, devices, live_pairing_payload)
      clear_live_smoke_logcat(runner, devices)
      runner.run("build instrumented test APK", "./gradlew", ":app:assembleDebugAndroidTest")
      connected_command = ["./gradlew", ":app:connectedDebugAndroidTest"]
      redacted_connected_command = connected_command.dup
      unless live_pairing_payload.empty?
        connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayPairingPayload=#{live_pairing_payload}"
        connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayDeviceName=#{LIVE_DEVICE_NAME}"
        redacted_connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayPairingPayload=<redacted>"
        redacted_connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayDeviceName=#{LIVE_DEVICE_NAME}"
        unless LIVE_MESSAGE.empty?
          connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayLiveMessage=#{LIVE_MESSAGE}"
          redacted_connected_command << "-Pandroid.testInstrumentationRunnerArguments.mobileGatewayLiveMessage=<redacted>"
          if run_agent_activity_check
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
      collect_live_smoke_logcat(runner, devices)
    end
  end
rescue StandardError => e
  status = "failed"
  error = "#{e.class}: #{e.message}"
end

markers = live_smoke_markers(runner.steps)
result = {
  generated_at: Time.now.utc.iso8601,
  app_version: APP_VERSION,
  status: status,
  error: error,
  devices: devices || [],
  require_device: REQUIRE_DEVICE,
  live_payload_provided: !live_pairing_payload.empty?,
  live_payload_source: live_payload_source,
  desktop_pairing_fetch_requested: desktop_pairing_fetch_requested?,
  desktop_pairing_auto_discovered: DESKTOP_PAIRING_AUTO_DISCOVERED,
  live_message_provided: !LIVE_MESSAGE.empty?,
  expect_agent_activity: EXPECT_AGENT_ACTIVITY,
  agent_activity_check_enabled: run_agent_activity_check,
  agent_activity_timeout_ms: AGENT_ACTIVITY_TIMEOUT_MS,
  health_preflight_enabled: !live_pairing_payload.empty? && !SKIP_HEALTH_PREFLIGHT,
  health_preflight_timeout_seconds: HEALTH_PREFLIGHT_TIMEOUT_SECONDS,
  device_reachability_check_enabled: device_reachability_check_enabled,
  device_reachability_timeout_seconds: DEVICE_REACHABILITY_TIMEOUT_SECONDS,
  mobile_message_send_ack: markers[:mobile_message_send_ack],
  mac_agent_activity_signal: markers[:mac_agent_activity_signal],
  steps: runner.steps,
}
result[:acceptance_evidence] = build_acceptance_evidence(result)
result[:next_actions] = build_next_actions(result)

write_reports(result)

puts "Wrote #{REPORT_JSON}"
puts "Wrote #{REPORT_MD}"
exit(status == "failed" ? 1 : 0)
