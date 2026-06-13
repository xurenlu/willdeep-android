# Product Overview

> Last updated: 2026-06-14 | Current version: v1.6.0-rc1

## Project Summary

WillDeep Android is the native mobile companion for the WillDeep Mac desktop app. The phone connects to the Mac over the local network, displays Agent session state, and sends control commands. The Mac remains responsible for all Agent execution and safety decisions.

## Core Features

- Pair with the Mac Mobile Gateway by scanning the QR payload or pasting the short-lived JSON payload.
- Store the long-lived device token securely on Android.
- Connect to the Mac gateway over WebSocket.
- Display gateway status, paired desktop name, protocol version, sessions, selected session, and recent event log.
- Send `session.list`, `session.create`, `session.select`, `message.send`, and `turn.stop` commands.
- Review pending tool and patch approvals, then send `tool.decide` or `patch.decide` back to the Mac.
- Answer Mac-side `ask_user` prompts from the Android approval panel.
- Request and view pending patch diffs before approving or rejecting them.
- Display Mac background jobs and kill running jobs through the gateway.
- Read text files from the selected Mac session workspace through desktop-mediated `file.read`.

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
| WS | `tool.decide` | Approve, reject, or answer Mac-side tool approval requests. |
| WS | `patch.decide` | Approve or reject Mac-side patch proposals. |
| WS | `diff.get` | Request the unified diff for a pending patch proposal. |
| WS | `job.kill` | Ask the Mac to stop a running background job in the selected session. |
| WS | `file.read` | Request a text file from the selected Mac session workspace. |

## Run

```bash
./gradlew :app:assembleDebug
```

Install the debug APK on an Android device on the same LAN as the Mac running WillDeep Mobile Gateway.

## Known Gaps

- Reconnect and revoked-device UX should be improved after full gateway integration testing.
- Additional locales are pending.
