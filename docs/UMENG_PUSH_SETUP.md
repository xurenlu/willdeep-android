# Umeng Push Setup

> Last updated: 2026-07-06 | Android version: v1.22.0-rc1

## What Is Integrated

- Umeng Android SDK dependencies:
  - `com.umeng.umsdk:common`
  - `com.umeng.umsdk:asms`
  - `com.umeng.umsdk:push`
- `UMENG_APPKEY`, `UMENG_MESSAGE_SECRET`, and `UMENG_CHANNEL` build configuration.
- `WillDeepApplication` startup integration with Umeng pre-init and registration.
- Local storage for Umeng device token.
- Optional `push.register` WebSocket command to register the Android device token with the Mac gateway.
- Handling for Umeng custom payloads that describe WillDeep attention events.

## Local Configuration

Do not commit push credentials or server secrets.

For local debug builds, put these values in `local.properties` or export them in the shell:

```properties
UMENG_PUSH_ENABLED=true
UMENG_APPKEY=your_umeng_appkey
UMENG_MESSAGE_SECRET=your_umeng_message_secret
UMENG_CHANNEL=willdeep
```

Then build normally:

```bash
./gradlew :app:assembleDebug
```

If `UMENG_PUSH_ENABLED` is not `true`, or AppKey / MessageSecret is blank, the app compiles and runs but does not initialize Umeng push.

## Server-Side Secrets

Umeng server REST credentials, app master secret, webhook secrets, relay signing keys, and any service-side access tokens belong on the server side only, for example in a non-committed `.env` file.

The Android app should only receive client-side push configuration needed for SDK registration.

## Custom Payload Contract

The mobile client accepts a small JSON payload for attention notifications:

```json
{
  "target_type": "tool",
  "target_id": "approval-123",
  "session_id": "session-abc",
  "title": "WillDeep needs approval",
  "summary": "Review the pending action on your Mac session.",
  "requires_answer": false,
  "requires_confirmation": false
}
```

For patch review:

```json
{
  "target_type": "patch",
  "target_id": "patch-123",
  "session_id": "session-abc",
  "title": "WillDeep has a patch to review",
  "summary": "Tap to open the Android session.",
  "path": "app/src/main/java/Example.kt"
}
```

Keep payloads short and avoid sensitive data. Send only routing metadata and display-safe summaries. Full diffs, command arguments, and private workspace content should stay behind the authenticated gateway.

## Gateway Follow-Up

The Android client sends this optional WebSocket command once an Umeng device token is available:

```json
{
  "type": "push.register",
  "payload": {
    "provider": "umeng",
    "client_id": "...",
    "app_id": "...",
    "platform": "android",
    "app_version": "1.22.0-rc1"
  }
}
```

Older Mac gateways may reject `push.register`; Android treats that as an optional-capability downgrade.

## Compliance Note

Umeng initialization should happen only after the user has accepted the app privacy policy. The current integration is gated behind `UMENG_PUSH_ENABLED`, so production builds should only enable it after privacy-policy wording and user consent flow are ready.
