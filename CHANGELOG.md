# Changelog

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
