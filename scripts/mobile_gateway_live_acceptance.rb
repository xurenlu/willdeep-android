#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "digest"
require "json"
require "open3"
require "time"

ROOT_DIR = File.expand_path("..", __dir__)
SMOKE_SCRIPT = File.join(ROOT_DIR, "scripts", "android_connected_smoke_test.rb")
SMOKE_REPORT_JSON = File.join(ROOT_DIR, "build", "android_connected_smoke", "report.json")
SMOKE_REPORT_MD = File.join(ROOT_DIR, "build", "android_connected_smoke", "report.md")
OUT_DIR = File.join(ROOT_DIR, "build", "mobile_gateway_live_acceptance")
REPORT_JSON = File.join(OUT_DIR, "report.json")
REPORT_MD = File.join(OUT_DIR, "report.md")
DEFAULT_LIVE_MESSAGE = "Create a short TODO note in the current workspace from Android live acceptance."

def sha256(path)
  return nil unless File.file?(path)

  Digest::SHA256.file(path).hexdigest
end

def run_smoke
  env = {
    "REQUIRE_MOBILE_GATEWAY_LIVE_ACCEPTANCE" => "1",
    "MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY" => "1",
    "MOBILE_GATEWAY_LIVE_MESSAGE" => ENV.fetch("MOBILE_GATEWAY_LIVE_MESSAGE", DEFAULT_LIVE_MESSAGE),
  }
  Open3.capture3(env, "ruby", SMOKE_SCRIPT, chdir: ROOT_DIR)
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

def build_report(stdout, stderr, exit_status, smoke)
  {
    generated_at: Time.now.utc.iso8601,
    status: smoke.fetch("status", exit_status.success? ? "passed" : "failed"),
    app_version: smoke["app_version"],
    smoke_exit_code: exit_status.exitstatus,
    smoke_report_json: SMOKE_REPORT_JSON,
    smoke_report_md: SMOKE_REPORT_MD,
    smoke_report_json_sha256: sha256(SMOKE_REPORT_JSON),
    smoke_report_md_sha256: sha256(SMOKE_REPORT_MD),
    live_payload_source: smoke["live_payload_source"],
    live_message_provided: true,
    strict_live_acceptance_required: true,
    agent_activity_required: true,
    attached_device_count: smoke.fetch("devices", []).size,
    mobile_message_send_ack: smoke["mobile_message_send_ack"],
    mac_agent_activity_signal: smoke["mac_agent_activity_signal"],
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
    "- Attached devices: `#{report[:attached_device_count]}`",
    "- Message send ack: `#{report[:mobile_message_send_ack] || "missing"}`",
    "- Smoke JSON: `#{report[:smoke_report_json]}`",
    "- Smoke JSON SHA256: `#{report[:smoke_report_json_sha256] || "missing"}`",
    "- Smoke Markdown: `#{report[:smoke_report_md]}`",
    "- Smoke Markdown SHA256: `#{report[:smoke_report_md_sha256] || "missing"}`",
    "- Agent activity signal: `#{report[:mac_agent_activity_signal] || "missing"}`",
    "",
  ]
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

stdout, stderr, exit_status = run_smoke
smoke = read_smoke_result
report = build_report(stdout, stderr, exit_status, smoke)
write_reports(report)

puts "Wrote #{REPORT_JSON}"
puts "Wrote #{REPORT_MD}"
exit(report[:status] == "passed" ? 0 : 1)
