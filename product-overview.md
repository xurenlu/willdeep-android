# Product Overview

> Last updated: 2026-06-14 | Current version: v1.1.0-rc1

## Project Summary

WillDeep Android is the native mobile companion for the WillDeep Mac desktop app. The phone connects to the Mac over the local network, displays Agent session state, and sends control commands. The Mac remains responsible for all Agent execution and safety decisions.

## Core Features

- Pair with the Mac Mobile Gateway by scanning the QR payload or pasting the short-lived JSON payload.
- Store the long-lived device token securely on Android.
- Connect to the Mac gateway over WebSocket.
- Display gateway status, paired desktop name, protocol version, sessions, selected session, and recent event log.
- Send `session.list`, `session.create`, `session.select`, `message.send`, and `turn.stop` commands.

## Technology Stack

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Concurrency: Kotlin coroutines and Flow
- Networking: OkHttp HTTP and WebSocket
- QR scanning: CameraX preview + ML Kit barcode scanning
- Storage: AndroidX Security encrypted shared preferences
- Build: Gradle Kotlin DSL, Android Gradle Plugin, Kotlin Compose compiler plugin

## Gateway API Overview

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/mobile/pair/claim` | Claim a short-lived pairing token and receive a device token. |
| `GET` | `/mobile/ws` | Open the authenticated WebSocket command and event channel. |

## Run

```bash
./gradlew :app:assembleDebug
```

Install the debug APK on an Android device on the same LAN as the Mac running WillDeep Mobile Gateway.

## Known Gaps

- Tool approval, patch approval, and job controls need dedicated Compose panels.
- Reconnect and revoked-device UX should be improved after full gateway integration testing.
- Additional locales are pending.
