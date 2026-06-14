#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "json"
require "open3"
require "time"

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
  lines << ""
  lines << "## Steps"
  lines << ""
  lines << "| Step | Status | Duration | Detail |"
  lines << "| --- | --- | ---: | --- |"
  result[:steps].each do |step|
    detail = step[:reason] || step[:command] || ""
    lines << "| #{step[:name]} | `#{step[:status]}` | #{step[:duration_ms]} ms | #{detail.gsub("|", "\\|")} |"
  end
  lines << ""
  lines.join("\n")
end

runner = CommandRunner.new(ROOT_DIR)
status = "passed"
error = nil

begin
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
  steps: runner.steps,
}

write_reports(result)

puts "Wrote #{REPORT_JSON}"
puts "Wrote #{REPORT_MD}"
exit(status == "failed" ? 1 : 0)
