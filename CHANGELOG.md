# Changelog

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
