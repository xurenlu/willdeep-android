#!/usr/bin/env ruby
# frozen_string_literal: true

require "digest"
require "fileutils"
require "json"
require "open3"
require "securerandom"

ROOT = File.expand_path("..", __dir__)
Dir.chdir(ROOT)
File.umask(0o077)
signing_dir = File.join(ROOT, ".signing")
key = File.join(signing_dir, "willdeep-release.jks")
password_file = File.join(signing_dir, "password")

def run!(*command, env: {})
  output, status = Open3.capture2e(env, *command)
  raise "#{File.basename(command.first)} failed:\n#{output.lines.last(70).join}" unless status.success?
  output
end

if ARGV.include?("--initialize-key")
  raise "Signing files already exist; refusing to replace them" if File.exist?(key) || File.exist?(password_file)
  FileUtils.mkdir_p(signing_dir, mode: 0o700)
  File.write(password_file, SecureRandom.hex(32), mode: "w", perm: 0o600)
  password = File.read(password_file).strip
  run!("keytool", "-genkeypair", "-keystore", key, "-storetype", "JKS",
       "-alias", "willdeep", "-keyalg", "RSA", "-keysize", "4096",
       "-validity", "10000", "-dname", "CN=WillDeep Android, O=WillDeep",
       "-storepass:env", "WILLDEEP_SIGNING_PASSWORD", "-keypass:env", "WILLDEEP_SIGNING_PASSWORD",
       env: { "WILLDEEP_SIGNING_PASSWORD" => password })
  puts "Created dedicated private signing files; back up .signing securely."
end
raise "Missing release key; initialize once with --initialize-key" unless File.file?(key) && File.file?(password_file)

gradle = File.read("app/build.gradle.kts")
version = gradle[/versionName = "([^"]+)"/, 1]
code = gradle[/versionCode = (\d+)/, 1].to_i
raise "Invalid stable version" unless version&.match?(/\A\d+\.\d+\.\d+\z/) && code.positive?
sdk = ENV["ANDROID_HOME"] || File.read("local.properties")[/^sdk.dir=(.+)$/, 1]
raise "Android SDK not configured" unless sdk && File.directory?(sdk)
build_tools = Dir.glob(File.join(sdk, "build-tools", "*")).select { |path| File.file?(File.join(path, "apksigner")) }
tools_dir = build_tools.max_by { |path| File.basename(path).split(".").map(&:to_i) }
raise "Android build-tools not installed" unless tools_dir
output_dir = File.join(ROOT, "build", "public-release", version)
FileUtils.mkdir_p(output_dir)

puts "Testing, linting and building #{version}..."
build_command = ["./gradlew", "testReleaseUnitTest", "lintRelease", "assembleRelease", "--console=plain"]
build_command << "--offline" if ARGV.include?("--offline")
build_output = run!(*build_command)
File.write(File.join(output_dir, "build.log"), build_output)
apk_name = "WillDeep-#{version}.apk"
apk = File.join(output_dir, apk_name)
run!(File.join(tools_dir, "zipalign"), "-f", "-P", "16", "4", "app/build/outputs/apk/release/app-release-unsigned.apk", apk)
run!(File.join(tools_dir, "apksigner"), "sign", "--ks", key, "--ks-key-alias", "willdeep",
     "--ks-pass", "env:WILLDEEP_SIGNING_PASSWORD", "--key-pass", "env:WILLDEEP_SIGNING_PASSWORD", apk,
     env: { "WILLDEEP_SIGNING_PASSWORD" => File.read(password_file).strip })
signature = run!(File.join(tools_dir, "apksigner"), "verify", "--verbose", "--print-certs", apk)
run!(File.join(tools_dir, "zipalign"), "-c", "-P", "16", "4", apk)
metadata = run!(File.join(tools_dir, "aapt"), "dump", "badging", apk)
raise "Unexpected APK metadata" unless metadata.include?("package: name='com.willdeep.android' versionCode='#{code}' versionName='#{version}'")
raise "Release APK is debuggable" if metadata.include?("application-debuggable")
raise "Unexpected minimum SDK" unless metadata.include?("sdkVersion:'33'")
tests = Dir.glob("app/build/test-results/testReleaseUnitTest/TEST-*.xml").map { |path| File.read(path) }
test_count = tests.sum { |xml| xml[/<testsuite\b[^>]*\btests="(\d+)"/, 1].to_i }
raise "No unit tests executed" unless test_count.positive?
sha = Digest::SHA256.file(apk).hexdigest
report = { version: version, version_code: code, application_id: "com.willdeep.android",
           minimum_android: 13, unit_tests: test_count, lint: "passed", signature_verified: true,
           certificate_sha256: signature[/Signer #1 certificate SHA-256 digest: (\h+)/, 1],
           apk: apk_name, sha256: sha, bytes: File.size(apk) }
lint_report = File.read("app/build/reports/lint-results-release.xml")
report[:lint_warnings] = lint_report.scan(/severity="Warning"/).size
report[:lint_errors] = lint_report.scan(/severity="(?:Error|Fatal)"/).size
raise "Lint errors remain" unless report[:lint_errors].zero?
File.write(File.join(output_dir, "SHA256SUMS"), "#{sha}  #{apk_name}\n")
File.write(File.join(output_dir, "report.json"), JSON.pretty_generate(report) + "\n")
File.write(File.join(output_dir, "report.md"), "# WillDeep Android #{version}\n\n" +
           report.map { |name, value| "- #{name}: `#{value}`" }.join("\n") + "\n")
puts JSON.pretty_generate(report)
