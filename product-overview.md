# Product Overview

> Last updated: 2026-06-14 | Current version: v1.17.0-rc5

## Project Summary

WillDeep Android is the native mobile companion for the WillDeep Mac desktop app. The phone connects to the Mac over the local network, displays Agent session state, and sends control commands. The Mac remains responsible for all Agent execution and safety decisions.

## Core Features

- Pair with the Mac Mobile Gateway by scanning the QR payload or pasting the short-lived JSON payload.
- Check Mac gateway health from either a pairing payload or the saved paired Mac, then display server version plus pairing availability without blocking paired-device diagnostics when new pairing is disabled.
- Store the long-lived device token securely on Android.
- Connect to the Mac gateway over WebSocket.
- Automatically resume the Mac gateway connection on app start and foreground return for paired devices.
- Reconnect temporary WebSocket disconnects with bounded backoff and require pairing again when the Mac rejects a revoked token.
- Preserve unsent task text and pending action cards when the WebSocket is unavailable.
- Show recent mobile command status so the user can see whether the Mac gateway accepted or rejected a request.
- Use localized English and Simplified Chinese UI resources.
- Display gateway status, paired desktop name, protocol version, sessions, selected session, and recent event log.
- Display recent Mac-side conversation messages and follow streaming assistant deltas.
- Show when the Mac is still streaming an assistant response and clear the indicator on `message.done`.
- Display Mac-side changed files, repository root, and added/deleted line totals.
- Read a changed file directly from the Changed Files panel through desktop-mediated `file.read`.
- Send `session.list`, `session.create`, `session.select`, `message.send`, and `turn.stop` commands.
- Restore pending tool and patch approvals from the initial Mac snapshot, then send `tool.decide` or `patch.decide` back to the Mac.
- Answer Mac-side `ask_user` prompts from the Android approval panel.
- Request and view pending patch diffs before approving or rejecting them.
- Display Mac background jobs and kill running jobs through the gateway.
- Read text files from the selected Mac session workspace through desktop-mediated `file.read`.
- Display queued Mac requests and control the selected session queue through `queue.update`.

## Technology Stack

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Concurrency: Kotlin coroutines and Flow
- Networking: OkHttp HTTP and WebSocket
- QR scanning: CameraX preview + ML Kit barcode scanning
- Storage: AndroidX Security encrypted shared preferences
- Build: Gradle Kotlin DSL, Android Gradle Plugin, Kotlin Compose compiler plugin
- Integration verification: JVM local mock gateway tests, Android instrumented Compose pairing/WebSocket/message streaming/approval smoke test, plus Ruby stdlib mock gateway script with JSON and Markdown reports

## Gateway API Overview

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/mobile/health` | Check Mac gateway status, version headers, protocol version, and pairing availability. |
| `POST` | `/mobile/pair/claim` | Claim a short-lived pairing token and receive a device token. |
| `GET` | `/mobile/ws` | Open the authenticated WebSocket command and event channel. |
| WS | `tool.decide` | Approve, reject, or answer Mac-side tool approval requests. |
| WS | `patch.decide` | Approve or reject Mac-side patch proposals. |
| WS | `diff.get` | Request the unified diff for a pending patch proposal. |
| WS | `job.kill` | Ask the Mac to stop a running background job in the selected session. |
| WS | `file.read` | Request a text file from the selected Mac session workspace. |
| WS | `queue.update` | Add, remove, clear, or immediately send queued requests on the Mac. |
| WS | `message.append/delta/done` | Display Mac-side conversation updates. |
| WS | `worktree.updated` | Display Mac-side changed-file summaries and provide one-tap file-read entry points. |

## Run

```bash
./gradlew :app:assembleDebug
```

Install the debug APK on an Android device on the same LAN as the Mac running WillDeep Mobile Gateway.

## Verification

```bash
ruby scripts/mobile_gateway_mock_integration.rb
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest
```

The JVM client integration test covers the real `MobileGatewayClient` against a local mock gateway. The instrumented Compose smoke test drives pairing, WebSocket connection, initial snapshot display, `message.send`, streamed assistant text, `tool.pending`, and `tool.decide` against an in-process gateway mock. The Ruby integration script writes `build/mobile_gateway_mock_integration/report.json` and `build/mobile_gateway_mock_integration/report.md`, verifies that `ack` and `error` envelope IDs correlate with the originating mobile command IDs, and checks that snapshots include pending tools plus patch proposals.

## Known Gaps

- Full device integration testing against a live Mac gateway is still pending.
- Additional locales beyond English and Simplified Chinese are pending.
