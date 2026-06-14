# Changelog

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
