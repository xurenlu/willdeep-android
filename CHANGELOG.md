# Changelog

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
