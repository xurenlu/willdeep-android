# Changelog

## [1.17.0-rc38] - 2026-06-14

### Added

- Added live instrumentation markers for `message.send` acknowledgement and the specific Mac Agent activity signal observed after a mobile-originated request.
- Added connected smoke logcat collection so JSON/Markdown reports can show whether the final activity evidence came from responding state, assistant output, tools, patches, jobs, or worktree changes.

### Changed

- Bumped Android client version to `1.17.0-rc38`.

### Tests

- Extended Agent activity JVM coverage to assert the exact activity signal selected for each accepted evidence type.

## [1.17.0-rc37] - 2026-06-14

### Fixed

- Tightened live Mac Agent activity evidence so Android no longer treats a mobile user-message echo as proof that the Mac Agent started work.

### Changed

- Bumped Android client version to `1.17.0-rc37`.

### Tests

- Added JVM coverage for Agent activity evidence, including user echo rejection and assistant/session/tool/patch/job/worktree activity signals.

## [1.17.0-rc36] - 2026-06-14

### Added

- Added structured `acceptance_evidence` to connected smoke JSON/Markdown reports, covering live payload validation, Mac health preflight, attached device detection, device-to-gateway reachability, live instrumentation, `message.send` acknowledgement, and post-send Mac Agent activity.
- Added connected smoke next actions for missing `MOBILE_GATEWAY_LIVE_MESSAGE` and missing `MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY=1` so final live acceptance is easier to drive.

### Changed

- Bumped Android client version to `1.17.0-rc36`.

### Tests

- Verified the no-device connected smoke report includes pending acceptance evidence and actionable final-acceptance follow-ups.

## [1.17.0-rc35] - 2026-06-14

### Added

- Added `next_actions` to connected smoke JSON/Markdown reports so missing live payloads, missing Android devices, skipped health preflight, and stale pairing windows produce actionable follow-up steps.

### Changed

- Bumped Android client version to `1.17.0-rc35`.

### Tests

- Verified the no-device connected smoke report includes next actions for enabling the Mac gateway and attaching an Android device.

## [1.17.0-rc34] - 2026-06-14

### Added

- Added connected smoke auto-discovery for the default WillDeep Mac runtime desktop-token file, allowing live payload rotation without manually exporting desktop gateway variables when the Mac gateway is already running.

### Changed

- Connected smoke reports now record whether desktop gateway auto-discovery was used.
- Bumped Android client version to `1.17.0-rc34`.

### Tests

- Verified auto-discovery against a mock gateway using a temporary WillDeep Application Support token file.

## [1.17.0-rc33] - 2026-06-14

### Changed

- Made connected smoke Mac gateway HTTP preflights use direct `Net::HTTP` connections so local proxy environment variables cannot divert LAN `/mobile/pairing/rotate` or `/mobile/health` requests.
- Bumped Android client version to `1.17.0-rc33`.

### Tests

- Verified desktop pairing rotate fetch still succeeds against a local mock gateway while proxy environment variables point at an unreachable proxy.

## [1.17.0-rc32] - 2026-06-14

### Changed

- Updated the connected smoke desktop payload path to prefer `POST /mobile/pairing/rotate`, falling back to legacy `GET /mobile/pairing` only when the Mac gateway does not support rotation.
- Connected smoke reports now distinguish `desktop-rotate` from legacy `desktop-get` payload sources.
- Bumped Android client version to `1.17.0-rc32`.

### Tests

- Verified the connected smoke runner fetches a rotated pairing payload from a mock desktop endpoint and still redacts bearer and pairing tokens.

## [1.17.0-rc31] - 2026-06-14

### Added

- Added desktop-authenticated live pairing payload fetch support to `scripts/android_connected_smoke_test.rb` through `MOBILE_GATEWAY_DESKTOP_BASE_URL` plus `MOBILE_GATEWAY_DESKTOP_TOKEN` or `MOBILE_GATEWAY_DESKTOP_TOKEN_FILE`.

### Changed

- Connected smoke reports now record whether the live payload came from the environment, the desktop pairing endpoint, or no live source.
- Bumped Android client version to `1.17.0-rc31`.

### Tests

- Verified the connected smoke runner can fetch a fresh pairing payload from a mock desktop endpoint without leaking the desktop bearer token or pairing token in reports.

## [1.17.0-rc30] - 2026-06-14

### Changed

- Tightened connected smoke live pairing payload preflight to require a gateway host and strict ISO8601 `expires_at` values.
- Bumped Android client version to `1.17.0-rc30`.

### Tests

- Verified connected smoke rejects malformed gateway hosts and non-ISO expiry timestamps before instrumentation.

## [1.17.0-rc29] - 2026-06-14

### Changed

- Required `desktop_name` and `expires_at` in Android pairing payload parsing to match the connected smoke preflight contract.
- Bumped Android client version to `1.17.0-rc29`.

### Tests

- Updated model coverage so missing desktop names and expiry timestamps are rejected locally.

## [1.17.0-rc28] - 2026-06-14

### Changed

- Rejected pairing payloads that omit `protocol_version` instead of defaulting them to the current protocol.
- Bumped Android client version to `1.17.0-rc28`.

### Tests

- Added model coverage for missing pairing payload protocol versions.

## [1.17.0-rc27] - 2026-06-14

### Changed

- Rejected pairing payloads with malformed `expires_at` values locally before gateway health or claim requests.
- Bumped Android client version to `1.17.0-rc27`.

### Tests

- Added model coverage for malformed pairing payload expiry timestamps.

## [1.17.0-rc26] - 2026-06-14

### Changed

- Improved Android share import to use `EXTRA_TITLE` as a fallback title when `EXTRA_SUBJECT` is missing.
- Bumped Android client version to `1.17.0-rc26`.

### Tests

- Added parser coverage for title fallback and subject-over-title precedence.

## [1.17.0-rc25] - 2026-06-14

### Changed

- Bumped Android client version to `1.17.0-rc25`.

### Tests

- Added Compose instrumentation coverage that verifies imported shared or selected text appears in Ask WillDeep and later imports append without overwriting draft text.

## [1.17.0-rc24] - 2026-06-14

### Added

- Added Android selected-text import through `ACTION_PROCESS_TEXT` so highlighted text can be loaded into Ask WillDeep without opening the share sheet first.

### Changed

- Bumped Android client version to `1.17.0-rc24`.

### Tests

- Added parser coverage for selected text imports and non-text selected-text intents.

## [1.17.0-rc23] - 2026-06-14

### Changed

- Improved Android share-sheet import so shared text titles and body/URL content are combined before loading Ask WillDeep.
- Bumped Android client version to `1.17.0-rc23`.

### Tests

- Added parser coverage for shared subject/body combinations, subject-only shares, and duplicate subject/body de-duplication.

## [1.17.0-rc22] - 2026-06-14

### Added

- Added an Android share-sheet text entry point so text shared from other apps can be loaded into Ask WillDeep and sent to the Mac gateway.

### Changed

- Bumped Android client version to `1.17.0-rc22`.

### Tests

- Added parser coverage for accepted text shares and rejected non-text/non-send intents.

## [1.17.0-rc21] - 2026-06-14

### Changed

- Moved the Ask WillDeep composer and queued requests panel directly below pairing so connected users can send Mac Agent tasks without scrolling past diagnostic panels.
- Bumped Android client version to `1.17.0-rc21`.

### Tests

- Verified the Compose instrumentation APK and debug build still compile after the task-entry layout change.

## [1.17.0-rc20] - 2026-06-14

### Added

- Added local pairing payload `base_url` validation so malformed or unsupported gateway URLs are rejected before network requests.

### Changed

- Bumped Android client version to `1.17.0-rc20`.

### Tests

- Added model coverage for unsupported URL schemes and malformed gateway `base_url` values.

## [1.17.0-rc19] - 2026-06-14

### Added

- Added a stable invalid pairing payload error path for malformed QR content and missing required pairing fields.

### Changed

- Localized invalid pairing payload feedback in English and Simplified Chinese instead of surfacing low-level JSON parsing errors.
- Bumped Android client version to `1.17.0-rc19`.

### Tests

- Added model coverage for malformed pairing payload JSON and missing `base_url` or `pairing_token`.

## [1.17.0-rc18] - 2026-06-14

### Added

- Added Android-side pairing payload protocol compatibility checks so unsupported QR payload versions fail locally before network requests are sent.

### Changed

- Reused the localized gateway protocol mismatch error for incompatible pairing payloads.
- Bumped Android client version to `1.17.0-rc18`.

### Tests

- Added model coverage for compatible and incompatible pairing payload protocol versions.

## [1.17.0-rc17] - 2026-06-14

### Added

- Added Android-side pairing payload expiry detection so expired QR payloads fail locally before `/mobile/health` or `/mobile/pair/claim` requests are sent.

### Changed

- Added localized English and Simplified Chinese expired-pairing-payload errors.
- Bumped Android client version to `1.17.0-rc17`.

### Tests

- Added model coverage for expired, valid, and missing-expiry pairing payloads.

## [1.17.0-rc16] - 2026-06-14

### Added

- Added Android device network diagnostics collection before live gateway reachability checks, capturing route/address summaries through `adb shell` with IPv4 addresses redacted in reports.

### Changed

- Connected smoke reports now include a device network diagnostics step when Android devices are attached.
- Bumped Android client version to `1.17.0-rc16`.

### Tests

- Verified the connected smoke runner still reports skipped cleanly when no Android device is attached while live gateway preflights are configured.

## [1.17.0-rc15] - 2026-06-14

### Added

- Added Android device-side gateway reachability preflight to the connected smoke runner, checking the live gateway host and port through `adb shell` before instrumentation runs.

### Changed

- Connected smoke reports now show whether device reachability preflight was enabled and redact the gateway host from the reported adb command.
- Bumped Android client version to `1.17.0-rc15`.

### Tests

- Verified the connected smoke runner still reports skipped cleanly when no Android device is attached while live gateway preflights are configured.

## [1.17.0-rc14] - 2026-06-14

### Added

- Added live Mac gateway health preflight to the connected smoke runner, checking `/mobile/health` for reachability, protocol compatibility, server version, and pairing availability before Android instrumentation runs.

### Changed

- Connected smoke reports now show whether the live gateway health preflight is enabled and include a non-sensitive health result.
- Bumped Android client version to `1.17.0-rc14`.

### Tests

- Verified the connected smoke runner can validate a fresh-looking payload with the health preflight explicitly skipped and still produce a skipped no-device report without leaking token contents.

## [1.17.0-rc13] - 2026-06-14

### Added

- Added live pairing payload preflight validation to the connected smoke runner, checking JSON shape, required fields, protocol version, and expiry before any Android device test consumes time.

### Changed

- Connected smoke reports now include a non-sensitive payload validation step when `MOBILE_GATEWAY_PAIRING_PAYLOAD` is provided.
- Ignored local Kotlin daemon diagnostic output under `.kotlin/`.
- Bumped Android client version to `1.17.0-rc13`.

### Tests

- Verified the connected smoke runner can validate a fresh-looking payload and still produce a skipped no-device report without leaking token contents.

## [1.17.0-rc12] - 2026-06-14

### Added

- Added optional live Mac gateway Agent activity verification through `MOBILE_GATEWAY_EXPECT_AGENT_ACTIVITY=1`, allowing connected smoke tests to wait for Mac-side responding state, assistant output, tools, patches, jobs, or worktree changes after Android sends a live message.

### Changed

- Extended connected smoke reports with the Agent activity check state and timeout metadata.
- Bumped Android client version to `1.17.0-rc12`.

### Tests

- Extended the live gateway instrumentation path to compare post-send Agent activity against a pre-send baseline when the stronger live verification flag is enabled.

## [1.17.0-rc11] - 2026-06-14

### Added

- Added optional live Mac gateway message dispatch verification through `MOBILE_GATEWAY_LIVE_MESSAGE`, allowing connected smoke tests to prove Android can send a real `message.send` request into WillDeep after pairing.

### Changed

- Redact both live pairing payloads and live smoke messages from connected smoke JSON/Markdown reports.
- Bumped Android client version to `1.17.0-rc11`.

### Tests

- Extended the live gateway instrumentation path to wait for a non-pending `message.send` command result and require an accepted Mac acknowledgement when a live message is provided.

## [1.17.0-rc10] - 2026-06-14

### Added

- Added an optional live Mac gateway instrumentation path that consumes a fresh pairing payload through `mobileGatewayPairingPayload` and verifies the Android UI reaches the connected state against the real LAN gateway.

### Changed

- Extended `scripts/android_connected_smoke_test.rb` to pass `MOBILE_GATEWAY_PAIRING_PAYLOAD` and `MOBILE_GATEWAY_DEVICE_NAME` into connected Android tests while redacting the payload from JSON and Markdown reports.
- Bumped Android client version to `1.17.0-rc10`.

### Tests

- Added device-test coverage entry points for live Mac gateway pairing without requiring a live payload in default connected test runs.

## [1.17.0-rc9] - 2026-06-14

### Fixed

- Treated completed `patch.upsert` gateway events as patch removals so applied or rejected patches do not linger in the Android approval panel.

### Changed

- Bumped Android client version to `1.17.0-rc9`.

### Tests

- Added model and UI-state coverage for pending and completed `patch.upsert` events.
- Verified targeted coverage with `./gradlew :app:testDebugUnitTest --tests com.willdeep.android.mobile.MobileGatewayModelsTest --tests com.willdeep.android.ui.ConversationStreamStateTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.17.0-rc8] - 2026-06-14

### Fixed

- Treated completed `tool.updated` gateway events as approval removals so approved or rejected tools do not linger in the Android approval panel.

### Changed

- Bumped Android client version to `1.17.0-rc8`.

### Tests

- Added model and UI-state coverage for pending and completed `tool.updated` events.
- Verified targeted coverage with `./gradlew :app:testDebugUnitTest --tests com.willdeep.android.mobile.MobileGatewayModelsTest --tests com.willdeep.android.ui.ConversationStreamStateTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.17.0-rc7] - 2026-06-14

### Added

- Added `scripts/android_connected_smoke_test.rb` to run Android connected smoke verification, detect attached devices, and write JSON/Markdown reports.

### Changed

- Documented connected smoke reporting and the no-device skipped state for live Android validation.
- Bumped Android client version to `1.17.0-rc7`.

### Tests

- Verified the connected smoke script with `ruby scripts/android_connected_smoke_test.rb`.
- Verified the instrumented test APK builds with `./gradlew :app:assembleDebugAndroidTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.17.0-rc6] - 2026-06-14

### Changed

- Extended the Android instrumented Compose smoke test to render `patch.upsert`, request `diff.get`, display the returned diff, and verify Android sends `patch.decide` after approving the patch.
- Bumped Android client version to `1.17.0-rc6`.

### Tests

- Verified the instrumented test APK builds with `./gradlew :app:assembleDebugAndroidTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.17.0-rc5] - 2026-06-14

### Changed

- Extended the Android instrumented Compose smoke test to render a Mac-side `tool.pending` approval and verify Android sends `tool.decide` after tapping Approve.
- Bumped Android client version to `1.17.0-rc5`.

### Tests

- Verified the instrumented test APK builds with `./gradlew :app:assembleDebugAndroidTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.17.0-rc4] - 2026-06-14

### Changed

- Extended the Android instrumented Compose smoke test to send `message.send` over WebSocket and render the streamed assistant response from the mock Mac gateway.
- Bumped Android client version to `1.17.0-rc4`.

### Tests

- Verified the instrumented test APK builds with `./gradlew :app:assembleDebugAndroidTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.17.0-rc3] - 2026-06-14

### Changed

- Extended the Android instrumented Compose smoke test to complete the WebSocket handshake, receive an initial `state.snapshot`, and verify the session appears in the UI.
- Bumped Android client version to `1.17.0-rc3`.

### Tests

- Verified the instrumented test APK builds with `./gradlew :app:assembleDebugAndroidTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.17.0-rc2] - 2026-06-14

### Added

- Added an Android instrumented Compose smoke test that drives the real pairing UI against an in-process mock gateway for `/mobile/health` and `/mobile/pair/claim`.

### Changed

- Replaced the default generated instrumented test with Mobile Gateway pairing coverage.
- Bumped Android client version to `1.17.0-rc2`.

### Tests

- Verified the instrumented test APK builds with `./gradlew :app:assembleDebugAndroidTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.17.0-rc1] - 2026-06-14

### Added

- Added `state.snapshot` parsing for Mac-side `pending_tools` and `patch_proposals` so Android restores blocked approvals immediately after connecting.
- Added snapshot coverage for pending tool approvals and patch proposals in the Kotlin model tests and Ruby mock gateway integration script.

### Changed

- Preserve in-progress `ask_user` answer drafts only for still-current answer-required approvals when a fresh snapshot arrives.
- Bumped Android client version to `1.17.0-rc1`.

### Tests

- Verified targeted snapshot coverage with `./gradlew :app:testDebugUnitTest --tests com.willdeep.android.mobile.MobileGatewayModelsTest --tests com.willdeep.android.ui.ConversationStreamStateTest`.
- Verified the mock integration script with `ruby scripts/mobile_gateway_mock_integration.rb`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.16.0-rc2] - 2026-06-14

### Fixed

- Aligned the Ruby mock gateway integration script with the Mac Go gateway by returning correlated `error` envelopes for unknown commands.
- Added integration assertions that command `ack` and `error` envelope IDs match the mobile command IDs.

### Changed

- Bumped Android client version to `1.16.0-rc2`.

### Tests

- Verified the mock integration script with `ruby scripts/mobile_gateway_mock_integration.rb`.
- Verified targeted command protocol coverage with `./gradlew :app:testDebugUnitTest --tests com.willdeep.android.ui.MobileCommandStatusTest --tests com.willdeep.android.mobile.MobileGatewayModelsTest --tests com.willdeep.android.mobile.MobileGatewayClientIntegrationTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.16.0-rc1] - 2026-06-14

### Added

- Added a Recent Commands panel that shows pending, accepted, and failed mobile commands after they are sent to the Mac gateway.
- Added command tracking for gateway `ack`, `command.error`, `diff.get`, and `file.read` responses using envelope IDs when available.
- Added command status reducer coverage for pending, accepted, failed, and fallback matching paths.

### Changed

- Bumped Android client version to `1.16.0-rc1`.

### Tests

- Verified targeted command coverage with `./gradlew :app:testDebugUnitTest --tests com.willdeep.android.ui.MobileCommandStatusTest --tests com.willdeep.android.mobile.MobileGatewayModelsTest --tests com.willdeep.android.mobile.MobileGatewayClientIntegrationTest`.
- Verified the mock integration script with `ruby scripts/mobile_gateway_mock_integration.rb`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.15.0-rc3] - 2026-06-14

### Fixed

- Treated `pairing_allowed=false` as a pairing-only block so already paired devices can still use Check Gateway for diagnostics.

### Changed

- Bumped Android client version to `1.15.0-rc3`.

### Tests

- Extended target resolution coverage to distinguish pairing payload checks from saved paired gateway diagnostics.
- Verified targeted UI coverage with `./gradlew :app:testDebugUnitTest --tests com.willdeep.android.ui.GatewayHealthTargetTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.15.0-rc2] - 2026-06-14

### Fixed

- Allowed Check Gateway to use the saved paired Mac gateway when the pairing payload field is empty.

### Changed

- Bumped Android client version to `1.15.0-rc2`.

### Tests

- Added target resolution coverage for pairing payload and saved paired gateway health checks.
- Verified targeted UI coverage with `./gradlew :app:testDebugUnitTest --tests com.willdeep.android.ui.GatewayHealthTargetTest`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.15.0-rc1] - 2026-06-14

### Added

- Added Android `/mobile/health` probing before pairing and through a manual Check Gateway action.
- Added pairing UI display for Mac gateway server version and whether pairing is currently allowed.
- Added client coverage for health request headers and version/protocol/pairing status parsing.

### Changed

- Bumped Android client version to `1.15.0-rc1`.

### Tests

- Verified targeted client coverage with `./gradlew :app:testDebugUnitTest --tests com.willdeep.android.mobile.MobileGatewayClientIntegrationTest`.
- Verified the mock integration script with `ruby scripts/mobile_gateway_mock_integration.rb`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.14.0-rc3] - 2026-06-14

### Added

- Added JVM integration coverage for `MobileGatewayClient` against a local mock gateway, verifying pairing request headers, device token parsing, WebSocket authorization, automatic `session.list`, and snapshot parsing.

### Changed

- Bumped Android client version to `1.14.0-rc3`.

### Tests

- Verified targeted client coverage with `./gradlew :app:testDebugUnitTest --tests com.willdeep.android.mobile.MobileGatewayClientIntegrationTest`.
- Verified the mock integration script with `ruby scripts/mobile_gateway_mock_integration.rb`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.14.0-rc2] - 2026-06-14

### Added

- Added `scripts/mobile_gateway_mock_integration.rb`, a Ruby stdlib integration harness that starts a local Mac Gateway mock and verifies pairing, WebSocket authentication, state snapshots, streaming message events, changed-file `file.read`, and unknown command errors.
- Added JSON and Markdown integration reports under `build/mobile_gateway_mock_integration/`.

### Changed

- Bumped Android client version to `1.14.0-rc2`.

### Tests

- Verified the mock integration script with `ruby scripts/mobile_gateway_mock_integration.rb`.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.14.0-rc1] - 2026-06-14

### Added

- Added one-tap `file.read` requests from the Changed Files panel so a Mac-side changed path can be opened without manually copying it into the Files panel.

### Changed

- Bumped Android client version to `1.14.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.13.0-rc1] - 2026-06-14

### Added

- Added Android conversation streaming state so `message.delta` shows an in-progress Mac output indicator and `message.done` clears it.
- Added unit coverage for creating and completing streaming assistant messages from gateway events.

### Changed

- Bumped Android client version to `1.13.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.12.0-rc1] - 2026-06-14

### Added

- Added Simplified Chinese Android string resources for the Mobile Gateway pairing, connection, session, conversation, changed-files, file-read, approval, job, composer, queue, and event-log UI.

### Changed

- Bumped Android client version to `1.12.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.11.0-rc2] - 2026-06-14

### Fixed

- Preserved the Android composer draft when `message.send` or `queue.update` cannot be written to the WebSocket.
- Kept pending tool approvals, patch proposals, queued messages, and job cards visible when their mobile command fails to send.

### Changed

- Bumped Android client version to `1.11.0-rc2`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.11.0-rc1] - 2026-06-14

### Added

- Added automatic gateway connection resume for paired devices when the app starts or returns to the foreground.
- Added connection policy coverage so manual Disconnect does not get overridden by lifecycle auto-resume.

### Changed

- Bumped Android client version to `1.11.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.10.0-rc1] - 2026-06-14

### Added

- Added Android WebSocket reconnect handling with bounded backoff for temporary Mac gateway disconnects.
- Added revoked-token handling that stops reconnecting, clears the stored device token, and asks the user to pair again.
- Added localized reconnect and WebSocket lifecycle status text.

### Changed

- Bumped Android client version to `1.10.0-rc1`.

### Tests

- Added reconnect policy coverage for bounded backoff and authentication rejection detection.
- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.9.0-rc1] - 2026-06-14

### Added

- Added Android parsing for Mobile Gateway `worktree_changes` snapshots and `worktree.updated` events.
- Added a Compose Changed Files panel showing repository root, changed-file count, and added/deleted line totals.
- Added model coverage for worktree changed-file snapshot and event parsing.

### Changed

- Bumped Android client version to `1.9.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.8.0-rc1] - 2026-06-14

### Added

- Added Android parsing for recent conversation messages in `state.snapshot`.
- Added parsing coverage for `message.append`, `message.delta`, and `message.done` gateway events.
- Added a Compose Conversation panel that displays recent Mac-side user, assistant, and system messages.

### Changed

- Bumped Android client version to `1.8.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.7.0-rc1] - 2026-06-14

### Added

- Added Android parsing for queued Mac Agent messages in `state.snapshot`.
- Added a Compose Queued Requests panel with send-now, remove, and clear controls.
- Added `queue.update` command encoding and model coverage.

### Changed

- Bumped Android client version to `1.7.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.6.0-rc1] - 2026-06-14

### Added

- Added Android `file.read` requests for selected Mac session workspaces.
- Added a Compose Files panel for reading and displaying text files returned by the Mac desktop peer.
- Added model coverage for `file.read` ack parsing and command encoding.

### Changed

- Bumped Android client version to `1.6.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.5.0-rc1] - 2026-06-14

### Added

- Added Android parsing for Mobile Gateway background job snapshots and `job.updated` events.
- Added a Compose Background Jobs panel with job status, command metadata, and kill controls for running jobs.
- Added `job.kill` command encoding and model coverage.

### Changed

- Bumped Android client version to `1.5.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.4.0-rc1] - 2026-06-14

### Added

- Added Android `diff.get` requests from patch approval cards.
- Added parsing and display for patch diff payloads returned in gateway `ack` responses.
- Added unit coverage for `diff.get` ack parsing.

### Changed

- Bumped Android client version to `1.4.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.3.0-rc1] - 2026-06-14

### Added

- Added Android answer prompts for `ask_user` / answer-required tool approvals.
- Added `answer` payload support when approving `tool.decide` requests that require user input.
- Added unit coverage for answer-required approval parsing and command encoding.

### Changed

- Bumped Android client version to `1.3.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.2.0-rc1] - 2026-06-14

### Added

- Added Android parsing and Compose panels for `tool.pending` and `patch.upsert` gateway events.
- Added approve/reject controls that send `tool.decide` and `patch.decide` commands back to the Mac gateway.

### Changed

- Bumped Android client version to `1.2.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.1.0-rc1] - 2026-06-14

### Added

- Added CameraX and ML Kit QR scanning for Mac Mobile Gateway pairing payloads.
- Added runtime camera permission handling in the Compose pairing flow.

### Changed

- Bumped Android client version to `1.1.0-rc1`.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## [1.0.0-rc1] - 2026-06-13

### Added

- Added the initial Jetpack Compose Android client for WillDeep Mobile Gateway.
- Added local-network pairing claim support for Mac-generated gateway payloads.
- Added encrypted device token storage on Android.
- Added OkHttp WebSocket support for session events, message deltas, command acknowledgements, and gateway errors.
- Added session list, create, select, message send, and turn stop controls.
- Added Android Mobile Gateway requirements documentation and product overview.

### Tests

- Verified unit tests and debug build with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
