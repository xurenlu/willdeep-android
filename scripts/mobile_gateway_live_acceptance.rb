#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "digest"
require "json"
require "net/http"
require "open3"
require "time"
require "uri"

ROOT_DIR = File.expand_path("..", __dir__)
SMOKE_SCRIPT = File.join(ROOT_DIR, "scripts", "android_connected_smoke_test.rb")
SMOKE_REPORT_JSON = File.join(ROOT_DIR, "build", "android_connected_smoke", "report.json")
SMOKE_REPORT_MD = File.join(ROOT_DIR, "build", "android_connected_smoke", "report.md")
OUT_DIR = File.join(ROOT_DIR, "build", "mobile_gateway_live_acceptance")
REPORT_JSON = File.join(OUT_DIR, "report.json")
REPORT_MD = File.join(OUT_DIR, "report.md")
LIVE_ACCEPTANCE_TARGET_FILE = "WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md"
DEFAULT_LIVE_MESSAGE = [
  "Use the Mac WillDeep workspace tools to create or update `#{LIVE_ACCEPTANCE_TARGET_FILE}`.",
  "Add one short section titled `Android Live Acceptance` with today's date and one bullet explaining that the request came from Android.",
  "Do not only describe the change; make the workspace file edit so the live smoke can observe code activity.",
].join("\n")
MAC_BUNDLE_ID = ENV.fetch("MOBILE_GATEWAY_MAC_BUNDLE_ID", "com.willdeep.app")
DEFAULT_DESKTOP_PAIRING_PORT = ENV.fetch("MOBILE_GATEWAY_DESKTOP_PORT", "8877")
DEFAULT_DESKTOP_PAIRING_BASE_URL = "http://127.0.0.1:#{DEFAULT_DESKTOP_PAIRING_PORT}"
DEFAULT_DESKTOP_PAIRING_TOKEN_FILE = File.join(
  Dir.home,
  "Library",
  "Application Support",
  MAC_BUNDLE_ID,
  "MobileGateway",
  "desktop-token",
)
MAC_GATEWAY_HEALTH_TIMEOUT_SECONDS = ENV.fetch("MOBILE_GATEWAY_HEALTH_TIMEOUT_SECONDS", "2").to_f

def sha256(path)
  return nil unless File.file?(path)

  Digest::SHA256.file(path).hexdigest
end

def run_smoke
  env = {
    "REQUIRE_MOBILE_GATEWAY_LIVE_ACCEPTANCE" => "1",
    "MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY" => "1",
    "MOBILE_GATEWAY_EXPECT_CODE_ACTIVITY" => "1",
    "MOBILE_GATEWAY_EXPECTED_TARGET_FILE" => LIVE_ACCEPTANCE_TARGET_FILE,
    "MOBILE_GATEWAY_LIVE_MESSAGE" => ENV.fetch("MOBILE_GATEWAY_LIVE_MESSAGE", DEFAULT_LIVE_MESSAGE),
  }
  Open3.capture3(env, "ruby", SMOKE_SCRIPT, chdir: ROOT_DIR)
end

def live_request_profile
  ENV.key?("MOBILE_GATEWAY_LIVE_MESSAGE") ? "custom" : "default_workspace_file_edit"
end

def mac_gateway_preflight
  health = {
    "base_url" => DEFAULT_DESKTOP_PAIRING_BASE_URL,
    "reachable" => false,
    "status_code" => nil,
    "error" => nil,
  }
  begin
    uri = URI("#{DEFAULT_DESKTOP_PAIRING_BASE_URL}/mobile/health")
    http = Net::HTTP.new(uri.host, uri.port, nil)
    http.use_ssl = uri.scheme == "https"
    http.open_timeout = MAC_GATEWAY_HEALTH_TIMEOUT_SECONDS
    http.read_timeout = MAC_GATEWAY_HEALTH_TIMEOUT_SECONDS
    response = http.start do |http|
      http.request(Net::HTTP::Get.new(uri))
    end
    body = JSON.parse(response.body)
    data = body.fetch("data", {})
    health.merge!(
      "reachable" => response.is_a?(Net::HTTPSuccess),
      "status_code" => response.code.to_i,
      "app_version" => data["app_version"],
      "server_version" => data["server_version"],
      "protocol_version" => data["protocol_version"],
      "pairing_allowed" => data["pairing_allowed"],
      "desktop_name" => data["desktop_name"],
    )
  rescue StandardError => e
    health["error"] = "#{e.class}: #{e.message}"
  end

  {
    "default_base_url" => DEFAULT_DESKTOP_PAIRING_BASE_URL,
    "default_token_file_present" => File.file?(DEFAULT_DESKTOP_PAIRING_TOKEN_FILE),
    "default_token_file_path" => DEFAULT_DESKTOP_PAIRING_TOKEN_FILE,
    "health" => health,
  }
end

def read_smoke_result
  JSON.parse(File.read(SMOKE_REPORT_JSON))
rescue Errno::ENOENT
  {
    "status" => "failed",
    "error" => "smoke report was not generated",
    "acceptance_evidence" => [],
    "final_live_acceptance_failures" => ["smoke report was not generated"],
  }
end

def build_report(stdout, stderr, exit_status, smoke, preflight)
  {
    generated_at: Time.now.utc.iso8601,
    status: smoke.fetch("status", exit_status.success? ? "passed" : "failed"),
    app_version: smoke["app_version"],
    smoke_exit_code: exit_status.exitstatus,
    smoke_report_json: SMOKE_REPORT_JSON,
    smoke_report_md: SMOKE_REPORT_MD,
    smoke_report_json_sha256: sha256(SMOKE_REPORT_JSON),
    smoke_report_md_sha256: sha256(SMOKE_REPORT_MD),
    mac_gateway_preflight: preflight,
    live_payload_source: smoke["live_payload_source"],
    live_request_profile: live_request_profile,
    live_acceptance_target_file: LIVE_ACCEPTANCE_TARGET_FILE,
    target_file_required: true,
    live_message_provided: true,
    strict_live_acceptance_required: true,
    agent_activity_required: true,
    code_activity_required: true,
    attached_device_count: smoke.fetch("devices", []).size,
    mobile_message_send_ack: smoke["mobile_message_send_ack"],
    mac_agent_activity_signal: smoke["mac_agent_activity_signal"],
    mac_code_activity_signal: smoke["mac_code_activity_signal"],
    mac_target_file_signal: smoke["mac_target_file_signal"],
    acceptance_evidence: smoke.fetch("acceptance_evidence", []),
    final_live_acceptance_failures: smoke.fetch("final_live_acceptance_failures", []),
    next_actions: smoke.fetch("next_actions", []),
    stdout: stdout.strip,
    stderr: stderr.strip,
  }
end

def write_reports(report)
  FileUtils.mkdir_p(OUT_DIR)
  File.write(REPORT_JSON, JSON.pretty_generate(report) + "\n")
  lines = [
    "# Mobile Gateway Live Acceptance",
    "",
    "- Generated at: `#{report[:generated_at]}`",
    "- Android version: `#{report[:app_version] || "unknown"}`",
    "- Status: `#{report[:status]}`",
    "- Smoke exit code: `#{report[:smoke_exit_code]}`",
    "- Live payload source: `#{report[:live_payload_source] || "unknown"}`",
    "- Live request profile: `#{report[:live_request_profile] || "unknown"}`",
    "- Live acceptance target file: `#{report[:live_acceptance_target_file] || "unknown"}`",
    "- Attached devices: `#{report[:attached_device_count]}`",
    "- Message send ack: `#{report[:mobile_message_send_ack] || "missing"}`",
    "- Smoke JSON: `#{report[:smoke_report_json]}`",
    "- Smoke JSON SHA256: `#{report[:smoke_report_json_sha256] || "missing"}`",
    "- Smoke Markdown: `#{report[:smoke_report_md]}`",
    "- Smoke Markdown SHA256: `#{report[:smoke_report_md_sha256] || "missing"}`",
    "- Agent activity signal: `#{report[:mac_agent_activity_signal] || "missing"}`",
    "- Code activity signal: `#{report[:mac_code_activity_signal] || "missing"}`",
    "- Target file signal: `#{report[:mac_target_file_signal] || "missing"}`",
    "",
  ]
  lines << "## Mac Gateway Preflight"
  lines << ""
  preflight = report[:mac_gateway_preflight] || {}
  health = preflight.fetch("health", {})
  lines << "- Default base URL: `#{preflight["default_base_url"] || "unknown"}`"
  lines << "- Default desktop token file: `#{preflight["default_token_file_present"] ? "present" : "missing"}`"
  lines << "- Health reachable: `#{health["reachable"] ? "yes" : "no"}`"
  lines << "- Health status code: `#{health["status_code"] || "missing"}`"
  lines << "- Server version: `#{health["server_version"] || health["app_version"] || "unknown"}`"
  lines << "- Protocol version: `#{health["protocol_version"] || "unknown"}`"
  lines << "- Pairing allowed: `#{health.key?("pairing_allowed") ? health["pairing_allowed"] : "unknown"}`"
  lines << "- Desktop name: `#{health["desktop_name"] || "unknown"}`"
  lines << "- Health error: `#{health["error"] || "none"}`"
  lines << ""
  unless report[:final_live_acceptance_failures].empty?
    lines << "## Final Live Acceptance Failures"
    lines << ""
    report[:final_live_acceptance_failures].each { |failure| lines << "- #{failure}" }
    lines << ""
  end
  unless report[:next_actions].empty?
    lines << "## Next Actions"
    lines << ""
    report[:next_actions].each { |action| lines << "- #{action}" }
    lines << ""
  end
  lines << "## Acceptance Evidence"
  lines << ""
  lines << "| Requirement | Status | Evidence |"
  lines << "| --- | --- | --- |"
  report[:acceptance_evidence].each do |item|
    lines << "| `#{item.fetch("key")}` | `#{item.fetch("status")}` | #{item.fetch("evidence").gsub("|", "\\|")} |"
  end
  lines << ""
  File.write(REPORT_MD, lines.join("\n"))
end

preflight = mac_gateway_preflight
stdout, stderr, exit_status = run_smoke
smoke = read_smoke_result
report = build_report(stdout, stderr, exit_status, smoke, preflight)
write_reports(report)

puts "Wrote #{REPORT_JSON}"
puts "Wrote #{REPORT_MD}"
exit(report[:status] == "passed" ? 0 : 1)
