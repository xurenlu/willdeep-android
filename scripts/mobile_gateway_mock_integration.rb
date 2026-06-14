#!/usr/bin/env ruby
# frozen_string_literal: true

require "base64"
require "digest/sha1"
require "fileutils"
require "json"
require "net/http"
require "securerandom"
require "socket"
require "time"
require "uri"

ROOT_DIR = File.expand_path("..", __dir__)
OUT_DIR = File.join(ROOT_DIR, "build", "mobile_gateway_mock_integration")
REPORT_JSON = File.join(OUT_DIR, "report.json")
REPORT_MD = File.join(OUT_DIR, "report.md")
PROTOCOL_VERSION = "mobile-gateway.v1"
APP_VERSION = File.read(File.join(ROOT_DIR, "app", "build.gradle.kts"))[/versionName\s*=\s*"([^"]+)"/, 1]
raise "versionName not found" if APP_VERSION.nil? || APP_VERSION.empty?

class StepRecorder
  attr_reader :steps

  def initialize
    @steps = []
  end

  def record(name)
    started = Time.now
    result = yield
    @steps << {
      name: name,
      status: "passed",
      duration_ms: ((Time.now - started) * 1000).round,
    }
    result
  rescue StandardError => e
    @steps << {
      name: name,
      status: "failed",
      duration_ms: ((Time.now - started) * 1000).round,
      error: "#{e.class}: #{e.message}",
    }
    raise
  end
end

class MinimalWebSocket
  GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

  def initialize(url, headers = {})
    @uri = URI(url)
    @socket = TCPSocket.new(@uri.host, @uri.port)
    key = Base64.strict_encode64(SecureRandom.random_bytes(16))
    path = @uri.query ? "#{@uri.path}?#{@uri.query}" : @uri.path
    path = "/" if path.empty?
    request_headers = {
      "Host" => "#{@uri.host}:#{@uri.port}",
      "Upgrade" => "websocket",
      "Connection" => "Upgrade",
      "Sec-WebSocket-Key" => key,
      "Sec-WebSocket-Version" => "13",
    }.merge(headers)
    request = +"GET #{path} HTTP/1.1\r\n"
    request_headers.each { |name, value| request << "#{name}: #{value}\r\n" }
    request << "\r\n"
    @socket.write(request)
    response = read_http_response
    unless response.start_with?("HTTP/1.1 101")
      raise "websocket upgrade failed: #{response.lines.first&.strip}"
    end
    accept = response[/^Sec-WebSocket-Accept:\s*(.+)$/i, 1]&.strip
    expected = Base64.strict_encode64(Digest::SHA1.digest(key + GUID))
    raise "websocket accept mismatch" if accept != expected
  end

  def write_json(value)
    write_frame(1, JSON.generate(value), masked: true)
  end

  def read_json(timeout: 5)
    deadline = Time.now + timeout
    loop do
      remaining = deadline - Time.now
      raise "timed out waiting for websocket frame" if remaining <= 0
      ready = IO.select([@socket], nil, nil, remaining)
      raise "timed out waiting for websocket frame" unless ready
      opcode, payload = read_frame
      return JSON.parse(payload) if opcode == 1
      raise "websocket closed" if opcode == 8
      write_frame(10, payload, masked: true) if opcode == 9
    end
  end

  def close
    write_frame(8, "", masked: true) rescue nil
    @socket.close unless @socket.closed?
  end

  private

  def read_http_response
    response = +""
    response << @socket.readpartial(1) until response.include?("\r\n\r\n")
    response
  end

  def read_frame
    first = read_exact(2).bytes
    opcode = first[0] & 0x0f
    masked = (first[1] & 0x80) != 0
    length = first[1] & 0x7f
    length = read_exact(2).unpack1("n") if length == 126
    length = read_exact(8).unpack1("Q>") if length == 127
    mask = masked ? read_exact(4).bytes : nil
    payload = read_exact(length).bytes
    payload = payload.each_with_index.map { |byte, index| byte ^ mask[index % 4] } if masked
    [opcode, payload.pack("C*")]
  end

  def read_exact(length)
    data = +""
    data << @socket.readpartial(length - data.bytesize) while data.bytesize < length
    data
  end

  def write_frame(opcode, payload, masked:)
    bytes = payload.bytes
    header = [0x80 | opcode]
    if bytes.length < 126
      header << (masked ? 0x80 | bytes.length : bytes.length)
    elsif bytes.length <= 65_535
      header << (masked ? 0x80 | 126 : 126)
      header.concat([bytes.length].pack("n").bytes)
    else
      header << (masked ? 0x80 | 127 : 127)
      header.concat([bytes.length].pack("Q>").bytes)
    end
    if masked
      mask = SecureRandom.random_bytes(4).bytes
      body = bytes.each_with_index.map { |byte, index| byte ^ mask[index % 4] }
      @socket.write((header + mask + body).pack("C*"))
    else
      @socket.write(header.pack("C*"))
      @socket.write(payload)
    end
  end
end

class ServerWebSocket
  def initialize(socket)
    @socket = socket
  end

  def write_json(value)
    write_frame(1, JSON.generate(value))
  end

  def read_json(timeout: 5)
    deadline = Time.now + timeout
    loop do
      remaining = deadline - Time.now
      raise "timed out waiting for client command" if remaining <= 0
      ready = IO.select([@socket], nil, nil, remaining)
      raise "timed out waiting for client command" unless ready
      opcode, payload = read_frame
      return JSON.parse(payload) if opcode == 1
      return nil if opcode == 8
      write_frame(10, payload) if opcode == 9
    end
  end

  def close
    write_frame(8, "") rescue nil
    @socket.close unless @socket.closed?
  end

  private

  def read_frame
    first = read_exact(2).bytes
    opcode = first[0] & 0x0f
    masked = (first[1] & 0x80) != 0
    length = first[1] & 0x7f
    length = read_exact(2).unpack1("n") if length == 126
    length = read_exact(8).unpack1("Q>") if length == 127
    mask = masked ? read_exact(4).bytes : nil
    payload = read_exact(length).bytes
    payload = payload.each_with_index.map { |byte, index| byte ^ mask[index % 4] } if masked
    [opcode, payload.pack("C*")]
  end

  def read_exact(length)
    data = +""
    data << @socket.readpartial(length - data.bytesize) while data.bytesize < length
    data
  end

  def write_frame(opcode, payload)
    bytes = payload.bytes
    header = [0x80 | opcode]
    if bytes.length < 126
      header << bytes.length
    elsif bytes.length <= 65_535
      header << 126
      header.concat([bytes.length].pack("n").bytes)
    else
      header << 127
      header.concat([bytes.length].pack("Q>").bytes)
    end
    @socket.write(header.pack("C*"))
    @socket.write(payload)
  end
end

class MockGateway
  attr_reader :base_url, :commands, :observed_headers

  def initialize
    @server = TCPServer.new("127.0.0.1", 0)
    @base_url = "http://127.0.0.1:#{@server.addr[1]}"
    @pairing_token = "pair_#{SecureRandom.hex(8)}"
    @device_token = "device_#{SecureRandom.hex(12)}"
    @claimed = false
    @running = true
    @threads = []
    @commands = []
    @observed_headers = []
  end

  def pairing_payload
    {
      "base_url" => base_url,
      "pairing_token" => @pairing_token,
      "protocol_version" => PROTOCOL_VERSION,
      "desktop_name" => "Mock Mac",
      "expires_at" => (Time.now.utc + 120).iso8601,
    }
  end

  def start
    @thread = Thread.new do
      while @running
        socket = @server.accept
        @threads << Thread.new { handle(socket) }
      end
    rescue IOError
      nil
    end
  end

  def stop
    @running = false
    @server.close rescue nil
    @thread&.join(1)
    @threads.each { |thread| thread.join(1) }
  end

  private

  def handle(socket)
    request = read_http_request(socket)
    @observed_headers << request[:headers]
    case [request[:method], request[:path]]
    when ["GET", "/mobile/health"]
      write_json_response(socket, 200, {
        "ok" => true,
        "data" => {
          "status" => "ok",
          "version" => APP_VERSION,
          "protocol_version" => PROTOCOL_VERSION,
          "pairing_allowed" => true,
        },
      })
    when ["POST", "/mobile/pair/claim"]
      handle_pair_claim(socket, request)
    when ["GET", "/mobile/ws"]
      handle_websocket(socket, request)
    else
      write_json_response(socket, 404, { "ok" => false, "error" => "not found" })
    end
  rescue StandardError => e
    warn "mock gateway request failed: #{e.class}: #{e.message}"
    socket.close rescue nil
  end

  def handle_pair_claim(socket, request)
    body = JSON.parse(request[:body].to_s)
    unless request[:headers]["x-app-version"] == APP_VERSION
      return write_json_response(socket, 400, { "ok" => false, "error" => "missing app version" })
    end
    if body["pairing_token"] != @pairing_token
      return write_json_response(socket, 401, { "ok" => false, "error" => "invalid pairing token" })
    end
    if @claimed
      return write_json_response(socket, 409, { "ok" => false, "error" => "pairing token already claimed" })
    end
    @claimed = true
    write_json_response(socket, 200, {
      "ok" => true,
      "data" => {
        "device" => {
          "id" => "device_mock",
          "name" => body["device_name"],
          "created_at" => Time.now.utc.iso8601,
        },
        "device_token" => @device_token,
        "protocol_version" => PROTOCOL_VERSION,
      },
    })
  end

  def handle_websocket(socket, request)
    unless request[:headers]["authorization"] == "Bearer #{@device_token}"
      return write_json_response(socket, 401, { "ok" => false, "error" => "unauthorized" })
    end
    unless request[:headers]["x-app-version"] == APP_VERSION
      return write_json_response(socket, 400, { "ok" => false, "error" => "missing app version" })
    end
    accept = Base64.strict_encode64(Digest::SHA1.digest(request[:headers].fetch("sec-websocket-key") + MinimalWebSocket::GUID))
    write_raw_response(socket, "101 Switching Protocols", {
      "Upgrade" => "websocket",
      "Connection" => "Upgrade",
      "Sec-WebSocket-Accept" => accept,
    })
    peer = ServerWebSocket.new(socket)
    peer.write_json(snapshot_event)
    loop do
      command = peer.read_json
      break if command.nil?
      @commands << command
      handle_command(peer, command)
    end
  end

  def handle_command(peer, command)
    case command["type"]
    when "session.list"
      peer.write_json(ack(command, "session.list"))
    when "message.send"
      peer.write_json(ack(command, "message.send"))
      peer.write_json(envelope("message.append", session_id: "s1", payload: {
        "id" => "m_user",
        "role" => "user",
        "content" => command.dig("payload", "text").to_s,
        "created_at" => Time.now.utc.iso8601,
      }))
      peer.write_json(envelope("message.delta", session_id: "s1", payload: {
        "message_id" => "m_assistant",
        "delta" => "Applied the requested Android change.",
      }))
      peer.write_json(envelope("message.done", session_id: "s1", payload: {
        "message_id" => "m_assistant",
      }))
    when "file.read"
      path = command.dig("payload", "path").to_s
      peer.write_json(ack(command, "file.read", {
        "path" => path,
        "content" => "content for #{path}",
        "truncated" => false,
        "byte_count" => "content for #{path}".bytesize,
      }))
    else
      peer.write_json(envelope("error", id: command["id"], session_id: command["session_id"], payload: {
        "message" => "unknown command: #{command["type"]}",
        "type" => command["type"],
      }))
    end
  end

  def snapshot_event
    envelope("state.snapshot", payload: {
      "active_session_id" => "s1",
      "sessions" => [
        {
          "id" => "s1",
          "title" => "Mock coding session",
          "workspace_name" => "Xedit",
          "message_count" => 2,
          "is_active" => true,
          "is_responding" => false,
        },
      ],
      "messages" => [
        {
          "id" => "m0",
          "role" => "assistant",
          "content" => "Ready from Mac.",
          "created_at" => Time.now.utc.iso8601,
          "session_id" => "s1",
        },
      ],
      "pending_tools" => [
        {
          "approval_id" => "tool_mock",
          "tool_name" => "shell",
          "summary" => "Run Android tests on the Mac",
          "command" => "./gradlew :app:testDebugUnitTest",
          "session_id" => "s1",
        },
      ],
      "patch_proposals" => [
        {
          "patch_id" => "patch_mock",
          "title" => "Mock Android patch",
          "summary" => "Review generated mobile gateway changes",
          "path" => "app/src/main/java/com/willdeep/android/ui/WillDeepApp.kt",
          "diffstat" => "+8 -1",
          "session_id" => "s1",
        },
      ],
      "worktree_changes" => [
        {
          "repository_root" => "/Users/rocky/Sites/Xedit",
          "file_count" => 1,
          "total_added_lines" => 8,
          "total_deleted_lines" => 1,
          "session_id" => "s1",
          "files" => [
            {
              "path" => "app/src/main/java/com/willdeep/android/ui/WillDeepApp.kt",
              "kind" => "M",
              "added_lines" => 8,
              "deleted_lines" => 1,
            },
          ],
        },
      ],
    })
  end

  def ack(command, type, payload = {})
    envelope("ack", id: command["id"], session_id: command["session_id"], payload: payload.merge("type" => type))
  end

  def envelope(type, id: SecureRandom.uuid, session_id: nil, payload: {})
    json = {
      "id" => id,
      "type" => type,
      "payload" => payload,
      "ts" => Time.now.utc.iso8601,
    }
    json["session_id"] = session_id unless session_id.nil? || session_id.empty?
    json
  end

  def read_http_request(socket)
    header = +""
    header << socket.readpartial(1) until header.include?("\r\n\r\n")
    lines = header.split("\r\n")
    method, path = lines.shift.split(" ", 3)
    headers = {}
    lines.each do |line|
      name, value = line.split(":", 2)
      headers[name.downcase] = value.to_s.strip if name && value
    end
    length = headers.fetch("content-length", "0").to_i
    body = length.positive? ? socket.read(length) : ""
    { method: method, path: path, headers: headers, body: body }
  end

  def write_json_response(socket, code, body)
    write_raw_response(socket, "#{code} #{status_text(code)}", {
      "Content-Type" => "application/json",
      "X-App-Version" => APP_VERSION,
      "X-Server-Version" => APP_VERSION,
    }, JSON.generate(body))
  end

  def write_raw_response(socket, status, headers, body = "")
    socket.write("HTTP/1.1 #{status}\r\n")
    headers = {
      "Date" => Time.now.httpdate,
      "Content-Length" => body.bytesize.to_s,
    }.merge(headers)
    headers.each { |name, value| socket.write("#{name}: #{value}\r\n") }
    socket.write("\r\n")
    socket.write(body)
  end

  def status_text(code)
    {
      200 => "OK",
      400 => "Bad Request",
      401 => "Unauthorized",
      404 => "Not Found",
      409 => "Conflict",
    }.fetch(code, "HTTP")
  end
end

def http_json(method, url, headers: {}, body: nil)
  uri = URI(url)
  request = method == :post ? Net::HTTP::Post.new(uri) : Net::HTTP::Get.new(uri)
  headers.each { |name, value| request[name] = value }
  if body
    request["Content-Type"] = "application/json"
    request.body = JSON.generate(body)
  end
  response = Net::HTTP.start(uri.host, uri.port, open_timeout: 5, read_timeout: 5) do |http|
    http.request(request)
  end
  parsed = response.body.to_s.empty? ? {} : JSON.parse(response.body)
  [response, parsed]
end

def expect(condition, message)
  raise message unless condition
end

def envelope(type, session_id: "s1", payload: {})
  {
    "id" => SecureRandom.uuid,
    "type" => type,
    "session_id" => session_id,
    "payload" => payload,
    "ts" => Time.now.utc.iso8601,
  }
end

def write_reports(result)
  FileUtils.mkdir_p(OUT_DIR)
  File.write(REPORT_JSON, JSON.pretty_generate(result))
  lines = [
    "# WillDeep Android Mobile Gateway Mock Integration",
    "",
    "- Status: #{result[:status]}",
    "- Android version: #{result[:version]}",
    "- Protocol: #{result[:protocol_version]}",
    "- Base URL: `#{result[:base_url]}`",
    "- Generated at: #{result[:generated_at]}",
    "",
    "| Step | Status | Duration |",
    "| --- | --- | --- |",
  ]
  result[:steps].each do |step|
    lines << "| #{step[:name]} | #{step[:status]} | #{step[:duration_ms]} ms |"
  end
  failures = result[:steps].select { |step| step[:status] != "passed" }
  unless failures.empty?
    lines << ""
    lines << "## Failures"
    failures.each { |step| lines << "- #{step[:name]}: #{step[:error]}" }
  end
  File.write(REPORT_MD, lines.join("\n") + "\n")
end

def main
  FileUtils.mkdir_p(OUT_DIR)
  recorder = StepRecorder.new
  gateway = MockGateway.new
  gateway.start
  ws = nil
  result = {
    status: "failed",
    generated_at: Time.now.utc.iso8601,
    base_url: gateway.base_url,
    version: APP_VERSION,
    protocol_version: PROTOCOL_VERSION,
    steps: recorder.steps,
  }

  begin
    recorder.record("health endpoint exposes version headers") do
      response, body = http_json(:get, "#{gateway.base_url}/mobile/health")
      expect(response.code.to_i == 200, "health returned HTTP #{response.code}")
      expect(response["X-App-Version"] == APP_VERSION, "X-App-Version mismatch")
      expect(response["X-Server-Version"] == APP_VERSION, "X-Server-Version mismatch")
      expect(body.dig("data", "protocol_version") == PROTOCOL_VERSION, "protocol mismatch")
    end

    claim = recorder.record("pair claim exchanges qr token") do
      response, body = http_json(
        :post,
        "#{gateway.base_url}/mobile/pair/claim",
        headers: { "X-App-Version" => APP_VERSION },
        body: {
          "pairing_token" => gateway.pairing_payload.fetch("pairing_token"),
          "device_name" => "Pixel Mock",
        },
      )
      expect(response.code.to_i == 200, "pair claim returned HTTP #{response.code}")
      expect(body.dig("data", "protocol_version") == PROTOCOL_VERSION, "claim protocol mismatch")
      body.fetch("data")
    end

    recorder.record("pair claim is single use") do
      response, = http_json(
        :post,
        "#{gateway.base_url}/mobile/pair/claim",
        headers: { "X-App-Version" => APP_VERSION },
        body: {
          "pairing_token" => gateway.pairing_payload.fetch("pairing_token"),
          "device_name" => "Pixel Mock",
        },
      )
      expect(response.code.to_i == 409, "duplicate pair claim returned HTTP #{response.code}")
    end

    recorder.record("websocket rejects invalid device token") do
      begin
        MinimalWebSocket.new(
          gateway.base_url.sub("http://", "ws://") + "/mobile/ws",
          "Authorization" => "Bearer invalid",
          "X-App-Version" => APP_VERSION,
        )
      rescue RuntimeError => e
        expect(e.message.include?("401"), "expected 401 rejection, got #{e.message}")
      else
        raise "invalid token unexpectedly connected"
      end
    end

    ws = recorder.record("websocket accepts device token and sends snapshot") do
      socket = MinimalWebSocket.new(
        gateway.base_url.sub("http://", "ws://") + "/mobile/ws",
        "Authorization" => "Bearer #{claim.fetch("device_token")}",
        "X-App-Version" => APP_VERSION,
      )
      snapshot = socket.read_json
      expect(snapshot["type"] == "state.snapshot", "expected state.snapshot")
      expect(snapshot.dig("payload", "sessions").first["id"] == "s1", "missing session")
      expect(snapshot.dig("payload", "pending_tools").first["approval_id"] == "tool_mock", "missing pending tool")
      expect(snapshot.dig("payload", "patch_proposals").first["patch_id"] == "patch_mock", "missing patch proposal")
      expect(snapshot.dig("payload", "worktree_changes").first["files"].first["path"].end_with?("WillDeepApp.kt"), "missing changed file")
      socket
    end

    recorder.record("session list command receives ack") do
      command = envelope("session.list")
      ws.write_json(command)
      ack = ws.read_json
      expect(ack["type"] == "ack", "expected ack")
      expect(ack["id"] == command.fetch("id"), "session.list ack id mismatch")
      expect(ack.dig("payload", "type") == "session.list", "expected session.list ack")
    end

    recorder.record("message send receives ack and streaming events") do
      command = envelope("message.send", payload: { "text" => "请在 Mac 上改 Android UI" })
      ws.write_json(command)
      ack = ws.read_json
      append = ws.read_json
      delta = ws.read_json
      done = ws.read_json
      expect(ack["id"] == command.fetch("id"), "message.send ack id mismatch")
      expect(ack.dig("payload", "type") == "message.send", "missing message.send ack")
      expect(append["type"] == "message.append", "missing message.append")
      expect(delta["type"] == "message.delta", "missing message.delta")
      expect(done["type"] == "message.done", "missing message.done")
    end

    recorder.record("changed file path can request file.read") do
      path = "app/src/main/java/com/willdeep/android/ui/WillDeepApp.kt"
      command = envelope("file.read", payload: { "path" => path, "max_bytes" => 65_536 })
      ws.write_json(command)
      ack = ws.read_json
      expect(ack["type"] == "ack", "expected file.read ack")
      expect(ack["id"] == command.fetch("id"), "file.read ack id mismatch")
      expect(ack.dig("payload", "type") == "file.read", "expected file.read payload")
      expect(ack.dig("payload", "path") == path, "file path mismatch")
      expect(ack.dig("payload", "content").include?(path), "file content missing path marker")
    end

    recorder.record("unknown command returns correlated error") do
      command = envelope("unknown.command")
      ws.write_json(command)
      error = ws.read_json
      expect(error["type"] == "error", "expected error")
      expect(error["id"] == command.fetch("id"), "error id mismatch")
      expect(error.dig("payload", "message").include?("unknown.command"), "missing command name")
    end

    result[:status] = "passed"
  rescue StandardError
    result[:status] = "failed"
    raise
  ensure
    ws&.close
    gateway.stop
    write_reports(result)
  end

  puts "Wrote #{REPORT_JSON}"
  puts "Wrote #{REPORT_MD}"
end

main if $PROGRAM_NAME == __FILE__
