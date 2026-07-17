# Product Overview

> Last updated: 2026-07-18 | Current version: v1.24.0-rc3

## Project Summary

WillDeep Android is the native mobile companion for the WillDeep Mac desktop app. The phone connects to the Mac over the local network, displays Agent session state, and sends control commands. The Mac remains responsible for all Agent execution and safety decisions.

## Core Features

- Pair with the Mac Mobile Gateway by scanning a short H5 QR URL such as `?r=<room>&t=<token>`, scanning the legacy JSON payload, or receiving a matching `willdeep://mobile/pair?...` deep link from the H5 page.
- Preserve LAN-first pairing while accepting Tailscale fallback endpoints from `fallback_base_urls` or `base_url#tailscale=100.x.x.x` QR hints.
- Show localized invalid-payload errors for malformed QR content or missing required pairing fields.
- Require pairing payload desktop name and expiry fields before sending health or claim requests.
- Reject malformed or unsupported gateway base URLs locally before sending health or claim requests.
- Reject pairing payloads missing an explicit protocol version locally before sending health or claim requests.
- Reject unsupported Mac pairing payload protocol versions locally before sending health or claim requests.
- Reject expired Mac pairing payloads locally before sending health or claim requests.
- Reject malformed Mac pairing payload expiry timestamps locally before sending health or claim requests.
- Check Mac gateway health from either a pairing payload or the saved paired Mac, then display server version plus pairing availability without blocking paired-device diagnostics when new pairing is disabled.
- Store long-lived device tokens securely on Android, migrate the legacy single-Mac credential automatically, recover safely from stale Keystore-encrypted pairing data, and switch between multiple paired remote Macs without rescanning.
- Connect to the Mac gateway over WebSocket.
- Retry Mac gateway health checks, pairing claims, and WebSocket connections against saved fallback endpoints when the LAN endpoint is unreachable.
- Put the selected remote Mac at the top of the home hierarchy, with a bottom-sheet computer picker, per-device last-response information, and scan/remove controls.
- Browse sessions through All, Working, Needs Confirmation, and Completed filters, grouped by workspace with the newest three sessions shown by default and per-workspace expansion for older sessions.
- Switch between all sessions and a single workspace from the compact workspace control above the filters.
- Keep the mobile app portrait-only and lead the home hierarchy with remote Mac status instead of a decorative page title.
- Surface the Ask WillDeep composer and queued requests directly after pairing so sending Mac Agent tasks is the primary mobile flow.
- Use the redesigned mobile composer icon toolbar, plus-style attachment entry, top-aligned multi-line input, Mac-aligned send/stop action button, and bottom-sheet pickers to choose approval mode, Mac-reported provider, model, skills, experts, and plugins before sending a request without crowding the text input.
- Render pending tool and patch confirmations directly inside their owning home-session card as well as in the session composer, while leaving changed-file details on the desktop.
- Accept shared plain text from other Android apps, combining shared subject/title and body/URL content before loading it into Ask WillDeep for forwarding to the Mac.
- Import highlighted Android text actions directly into Ask WillDeep for forwarding selected requirements or code snippets to the Mac.
- Automatically resume the Mac gateway connection on app start and foreground return for paired devices.
- Reconnect temporary WebSocket disconnects with bounded backoff and require pairing again when the Mac rejects a revoked token.
- Preserve unsent task text and pending action cards when the WebSocket is unavailable.
- Treat WebSocket/relay connectivity as transport only, mark a Mac online only after a real Mac App event, poll every five seconds, and wait twenty seconds before showing the Mac reconnecting state.
- Notify the Android user when the Mac needs attention for tool approval, patch review, typed confirmation, or `ask_user` input.
- Let safe approval and patch notifications send direct approve/reject decisions, while requests that need typed input open the matching session for continued conversation.
- Request Android 13+ notification permission and use a high-priority WillDeep attention notification channel.
- Integrate Umeng U-Push as the first real remote-push provider, gated by `UMENG_PUSH_ENABLED`, `UMENG_APPKEY`, and `UMENG_MESSAGE_SECRET` so builds stay safe before credentials and privacy consent are ready.
- Store the Umeng device token locally and optionally register it with the Mac gateway through `push.register` for later server-side attention push delivery.
- Convert Umeng custom attention payloads into the existing Android review/input notification flow.
- Show recent mobile command status so the user can see whether the Mac gateway accepted or rejected a request.
- Send phone-originated coding requests with an optional Mac workspace path in live acceptance so WillDeep edits the intended desktop repository, omitting any stale selected session id when the workspace path is supplied.
- Use localized English and Simplified Chinese UI resources.
- Display gateway status, paired desktop name, protocol version, sessions, selected session, and recent event log.
- Display recent Mac-side conversation messages with lightweight Markdown support for fenced code blocks, images, and simple tables, follow streaming assistant deltas, and auto-scroll session details to the bottom of the chat.
- Label hidden assistant thinking, tool activity, and waiting-for-visible-output messages clearly when a raw payload exists, and hide fully empty message rows.
- Show when the Mac is still streaming an assistant response and clear the indicator on `message.done`.
- Track Mac-side changed-file activity for acceptance evidence without rendering changed-file review cards on Android.
- Send `session.list`, `session.create`, `session.select`, `message.send`, and `turn.stop` commands.
- Attach selected mobile run context (`approval_mode`, `provider_id`, `model`, `skills`, `experts`, and `plugins`) to `message.send` payloads for Mac-side handling.
- Restore pending tool and patch approvals from the initial Mac snapshot, then send `tool.decide` or `patch.decide` back to the Mac.
- Require typed `confirm` input before approving danger-tier Mac tool calls that include `requires_confirmation`.
- During strict live acceptance, automatically approve pending Mac tools only when they require neither an answer nor typed confirmation, preserving the existing safety gates.
- During strict live acceptance, run only the live Mac gateway instrumentation test when a live payload is present, preserve partial ack/activity markers, and record host-side target-file changes as fallback evidence when Android receives code activity but misses the exact target-file event.
- Remove completed Mac tool approvals from the approval panel when `tool.updated` reports a non-pending state.
- Remove completed Mac patch proposals from the approval panel when `patch.upsert` reports a non-pending state.
- Answer Mac-side `ask_user` prompts from the Android approval panel.
- Approve or reject pending patch proposals from Android while leaving detailed diff review on the desktop.
- Display Mac background jobs and kill running jobs through the gateway.
- Read text files from the selected Mac session workspace through desktop-mediated `file.read`.
- Display queued Mac requests and control the selected session queue through `queue.update`.

## Technology Stack

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Concurrency: Kotlin coroutines and Flow
- Networking: OkHttp HTTP and WebSocket
- QR scanning: CameraX preview + ML Kit barcode scanning
- Notifications: Android notification channels with direct approval/rejection actions
- Remote push: Umeng Android SDK (`common` + `asms` + `push`) with custom payload handling
- Storage: AndroidX Security encrypted shared preferences
- Build: Gradle Kotlin DSL, Android Gradle Plugin, Kotlin Compose compiler plugin
- Integration verification: JVM local mock gateway tests, Android instrumented Compose pairing/WebSocket/message streaming/tool and patch approval smoke test, imported-task composer smoke test, optional live Mac gateway connected/message-dispatch/Agent-activity/code-activity/target-file smoke path, connected-device smoke runner with desktop-authenticated fresh pairing payload fetch, plus Ruby stdlib mock gateway script with JSON and Markdown reports

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
| WS | `push.register` | Optionally register the Android Umeng device token with the Mac gateway for remote attention push delivery. |
| WS | `message.append/delta/done` | Display Mac-side conversation updates. |
| WS | `worktree.updated` | Track Mac-side changed-file activity for acceptance and diagnostics without rendering a mobile review panel. |

## Run

```bash
./gradlew :app:assembleDebug
```

Install the debug APK on an Android device on the same LAN as the Mac running WillDeep Mobile Gateway.

## Verification

```bash
ruby scripts/mobile_gateway_mock_integration.rb
ruby scripts/android_connected_smoke_test.rb
MOBILE_GATEWAY_PAIRING_PAYLOAD='{"base_url":"http://192.168.1.20:8877","pairing_token":"...","protocol_version":"mobile-gateway.v1","desktop_name":"WillDeep Mac","expires_at":"2026-06-14T12:02:00Z"}' MOBILE_GATEWAY_LIVE_MESSAGE='Create or update WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md in the current workspace.' MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY=1 MOBILE_GATEWAY_EXPECT_CODE_ACTIVITY=1 MOBILE_GATEWAY_EXPECTED_TARGET_FILE=WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md ruby scripts/android_connected_smoke_test.rb
REQUIRE_MOBILE_GATEWAY_LIVE_ACCEPTANCE=1 MOBILE_GATEWAY_LIVE_MESSAGE='Create or update WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md in the current workspace.' MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY=1 MOBILE_GATEWAY_EXPECT_CODE_ACTIVITY=1 MOBILE_GATEWAY_EXPECTED_TARGET_FILE=WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md ruby scripts/android_connected_smoke_test.rb
ruby scripts/mobile_gateway_live_acceptance.rb
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest
```

The JVM client integration test covers the real `MobileGatewayClient` against a local mock gateway. The instrumented Compose smoke test drives pairing, WebSocket connection, initial snapshot display, `message.send`, streamed assistant text, danger-tier `tool.pending` typed confirmation, `tool.decide`, `patch.upsert`, `diff.get`, and `patch.decide` against an in-process gateway mock. The connected-device smoke runner writes `build/android_connected_smoke/report.json` and `build/android_connected_smoke/report.md`, records a skipped report when no Android device is attached, can fetch a fresh desktop-authenticated pairing payload by preferring `POST /mobile/pairing/rotate` and falling back to legacy `GET /mobile/pairing` only for older Mac gateways, validates live pairing payload JSON, required fields, protocol version, an HTTP/HTTPS `base_url` with a host, and a strict ISO8601 expiry timestamp, then checks the Mac gateway `/mobile/health` endpoint for host-side reachability and pairing availability before passing the payload to instrumentation. When no live payload or desktop gateway variables are provided, it can auto-discover WillDeep's default runtime desktop-token file under macOS Application Support and use `http://127.0.0.1:8877` for desktop-authenticated pairing rotation; if a device is attached but no live payload is available, normal smoke records skipped connected-instrumentation steps instead of running a guaranteed-failing device test. Mac gateway HTTP preflights use direct connections so local proxy environment variables cannot divert LAN requests. When devices are attached and a live payload is available, it captures IPv4-redacted Android network diagnostics, checks Android device-side gateway reachability through `adb shell`, clears live smoke logcat markers, filters connected instrumentation to the live Mac gateway test, and collects `WillDeepLiveSmoke` markers after instrumentation. It can pass a fresh Mac pairing payload to a live-gateway instrumentation test through either `MOBILE_GATEWAY_PAIRING_PAYLOAD`, explicit `MOBILE_GATEWAY_DESKTOP_BASE_URL` plus `MOBILE_GATEWAY_DESKTOP_TOKEN`/`MOBILE_GATEWAY_DESKTOP_TOKEN_FILE`, or the default runtime token-file auto-discovery path; reports identify whether the payload came from `env`, `desktop-rotate`, `desktop-get`, or `none`, whether auto-discovery was used, next actions for missing payloads/devices/live messages/activity checks, and `acceptance_evidence` rows for the live payload, Mac health preflight, Android device, device reachability, live instrumentation, `message.send` acknowledgement, post-send Mac Agent activity, post-send Mac code activity, and the requested target file. When `MOBILE_GATEWAY_LIVE_MESSAGE` is also set, the live instrumentation sends a real `message.send` command, waits for Mac acknowledgement, and emits an acknowledgement marker. When `MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY=1` is also set, it waits for Mac-side Agent activity after the send and does not count a mobile user-message echo as activity; accepted evidence is responding state, assistant output, tool/patch/job updates, or worktree changes. When `MOBILE_GATEWAY_EXPECT_CODE_ACTIVITY=1` is also set, it waits for a stricter code/workspace signal: pending tool, patch proposal, live job, or worktree file change. When `MOBILE_GATEWAY_EXPECTED_TARGET_FILE` is set, it also waits until that path appears in Mac patch or worktree activity, and the host runner records a `host_target_file` fallback when the requested Mac workspace file changes after the Android-originated command. `REQUIRE_MOBILE_GATEWAY_LIVE_ACCEPTANCE=1` turns the runner into the final acceptance gate: it exits non-zero unless live payload, health, Android device, device reachability, live instrumentation, message ack, accepted ack marker, enabled Agent activity check with a concrete Mac activity signal, enabled code activity check with a concrete Mac code signal, configured target file, and target-file signal or host target-file change all pass. `scripts/mobile_gateway_live_acceptance.rb` wraps that final gate into one command, supplies a default workspace-edit request targeting `WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md` with a unique run marker, writes `build/mobile_gateway_live_acceptance/report.json` and `report.md`, mirrors the strict smoke failures for handoff, records Android version, payload source, non-secret request profile, target filename, attached device count, ack/activity/code-activity/target-file markers, host target-file marker, SHA256 hashes for the underlying smoke reports, and adds a Mac Gateway preflight section for default token-file presence plus public health reachability/version/protocol/pairing metadata. Pairing payloads, bearer tokens, and live messages are redacted from reports. The Ruby gateway integration script writes `build/mobile_gateway_mock_integration/report.json` and `build/mobile_gateway_mock_integration/report.md`, verifies that `ack` and `error` envelope IDs correlate with the originating mobile command IDs, and checks that snapshots include pending tools plus patch proposals.

## Known Gaps

- Live Mac gateway testing still requires an attached Android device plus a fresh two-minute Mac pairing payload at run time.
- Additional locales beyond English and Simplified Chinese are pending.
