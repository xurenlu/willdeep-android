# Changelog

## [1.25.0-rc1] - 2026-07-18

### Added

- Added a dedicated attention center for tool approvals, typed questions, patch reviews, and failed mobile commands, while retaining inline session approvals on the workspace-grouped home screen.
- Added a connection diagnostics screen with multi-Mac switching, truthful Mac App response health, measured gateway latency, LAN/Tailscale route visibility, the five-second poll and twenty-second stale policy, and redacted diagnostic sharing.
- Added Conversation, Actions, and Changes tabs to session details so live messages, approvals/jobs/queue, and Mac-reported file changes have clear dedicated surfaces.

### Changed

- Tightened home session cards and replaced repeated transport-oriented “Connected” labels with task-oriented Working, Waiting for input, and Completed states.
- Bumped Android client version to `1.25.0-rc1` (`versionCode = 111`).

## [1.24.0-rc3] - 2026-07-18

### Fixed

- Added the required Compose experimental layout opt-in so the production Release variant compiles successfully under the stricter release compiler settings.
- Bumped Android client version to `1.24.0-rc3` (`versionCode = 110`).

## [1.24.0-rc2] - 2026-07-18

### Fixed

- Restored workspace-grouped home sessions, showing only the three newest gateway-ordered sessions per workspace until that group is expanded.
- Restored the compact workspace switcher and removed the oversized home “Code” heading so remote Mac status starts the content hierarchy.
- Bumped Android client version to `1.24.0-rc2` (`versionCode = 109`).

## [1.24.0-rc1] - 2026-07-18

### Added

- Added encrypted multi-Mac credential storage with legacy single-Mac migration, remote computer selection, remembered last-response timestamps, and per-device reconnect switching.
- Added the approved editorial Android home design with a top-level remote Mac selector, truthful device status, session filters, and tool/patch confirmations embedded directly in their owning session cards.

### Changed

- Split WebSocket transport connectivity from Mac App responsiveness: Android now shows online only after a real Mac-originated snapshot, acknowledgement, session, message, approval, workspace, capability, job, or worktree event.
- Changed Mac polling to five seconds and the stale-response threshold to twenty seconds; the session list remains visible while the server channel waits for the Mac App.
- Standardized the app on the single light ivory/orange/green WillDeep visual theme selected in the approved UI direction.
- Bumped Android client version to `1.24.0-rc1` (`versionCode = 108`).

### Fixed

- Recovered safely from stale encrypted pairing preferences whose Android Keystore authentication no longer matches the current installation, resetting only recoverable pairing data instead of crashing at startup.

## [1.23.0-rc1] - 2026-07-08

### Added

- Added Android AI message Markdown rendering for fenced code blocks, standalone Markdown images, and simple pipe tables.
- Bumped Android client version to `1.23.0-rc1` (`versionCode = 107`).

## [1.22.0-rc10] - 2026-07-08

### Fixed

- Aligned the Android composer placeholder and typed text to the top of the input area instead of vertically centering them.
- Bumped Android client version to `1.22.0-rc10` (`versionCode = 106`).

## [1.22.0-rc9] - 2026-07-08

### Fixed

- Reworked the Android composer action button to match the Mac composer: idle shows a cleaner upward send arrow, while responding turns the same button into a red stop control.
- Removed the separate inline Stop turn text action from the composer responding state.
- Bumped Android client version to `1.22.0-rc9` (`versionCode = 105`).

## [1.22.0-rc8] - 2026-07-08

### Fixed

- Removed the Changed Files review card from the Android session composer so worktree and diff details stay on the desktop.
- Reworked Android tool approval previews to show human-readable fields for JSON payloads instead of raw JSON.
- Made Approve the larger primary action and Reject the smaller secondary action in mobile review cards.
- Bumped Android client version to `1.22.0-rc8` (`versionCode = 104`).

## [1.22.0-rc7] - 2026-07-08

### Fixed

- Hid empty Android conversation rows when a gateway message has no visible text, images, or raw message body preview.
- Bumped Android client version to `1.22.0-rc7` (`versionCode = 103`).

## [1.22.0-rc6] - 2026-07-08

### Fixed

- Preserved a redacted raw message body preview for assistant messages with no visible text so Android can show the underlying payload for verification instead of only the empty-content fallback.
- Added parser coverage for reasoning/tool-only raw previews and sensitive-field redaction.
- Bumped Android client version to `1.22.0-rc6` (`versionCode = 102`).

## [1.22.0-rc5] - 2026-07-08

### Fixed

- Made Android session details scroll to the actual bottom of the chat when opening a session and while new assistant content streams in.
- Removed inline diff viewing from the Android patch review card so diffs stay on the desktop, and tightened review card spacing for denser mobile use.
- Bumped Android client version to `1.22.0-rc5` (`versionCode = 101`).

## [1.22.0-rc4] - 2026-07-08

### Fixed

- Replaced the vague empty conversation fallback with explicit assistant thinking, tool activity, or waiting-for-visible-output states so session details no longer show "No visible text" for hidden reasoning or tool-status messages.
- Added message parsing coverage for reasoning-only and tool-only assistant content arrays.
- Bumped Android client version to `1.22.0-rc4` (`versionCode = 100`).

## [1.22.0-rc3] - 2026-07-07

### Fixed

- Added compact H5 QR parsing for `r` / `t` relay URLs and `b` / `k` local gateway URLs so dense QR codes are no longer required for browser/App scanning.
- Kept compatibility with legacy raw JSON and `p` / `pair` / `payload` base64url QR payloads.
- Bumped Android client version to `1.22.0-rc3` (`versionCode = 99`).

## [1.22.0-rc2] - 2026-07-07

### Added

- Added H5 QR URL parsing so the Android scanner accepts `https://.../?p=<base64url-payload>` as well as the legacy raw JSON payload.
- Added `willdeep://mobile/pair?p=...` deep-link handling so the H5 page can wake the Android app and hand off the same Mobile Gateway credentials.

### Changed

- Bumped Android client version to `1.22.0-rc2` (`versionCode = 98`).

## [1.22.0-rc1] - 2026-07-06

### Added

- Integrated Umeng U-Push SDK through Maven (`com.umeng.umsdk:common`, `asms`, and `push`) for real remote push delivery.
- Added `UMENG_PUSH_ENABLED`, `UMENG_APPKEY`, `UMENG_MESSAGE_SECRET`, and `UMENG_CHANNEL` build configuration, sourced from Gradle properties, environment variables, or ignored `local.properties`, so push can be enabled without committing credentials.
- Added `WillDeepApplication`, Umeng pre-initialization/registration, device-token storage, and optional `push.register` WebSocket registration for Mac gateway support.
- Added Umeng custom-payload handling that converts remote attention payloads into the existing WillDeep approval/input notification flow.
- Added `push` / `push_small` notification icon resources and resource keep rules for push notification display.
- Added `docs/UMENG_PUSH_SETUP.md` with local configuration, server-secret boundaries, payload contract, and privacy-compliance notes.
- Added JVM tests for remote push attention payload parsing.

### Changed

- Bumped Android client version to `1.22.0-rc1` (`versionCode = 97`).

## [1.21.0-rc2] - 2026-07-06

### Fixed

- Collapsed the paired Mac status actions when opening or choosing from the workspace switcher, so Disconnect / Re-pair / Unpair controls no longer remain visible while viewing workspace tabs such as All.
- Bumped Android client version to `1.21.0-rc2` (`versionCode = 96`).

## [1.21.0-rc1] - 2026-07-06

### Added

- Added Android attention notifications for Mac-side `tool.pending` and `patch.upsert` events so the phone alerts the user when WillDeep needs approval, patch review, typed confirmation, or an `ask_user` answer.
- Safe approval and patch notifications now include direct `Approve` / `Reject` actions; requests requiring an answer or typed `confirm` open the matching session instead of approving from the notification.
- Notification taps route back into the relevant session and queue direct notification decisions until the WebSocket is connected and the matching pending item is available.
- Added Android 13+ `POST_NOTIFICATIONS` permission handling plus localized notification channel/title/body strings.
- Added JVM coverage for the notification action intent contract.

### Changed

- Bumped Android client version to `1.21.0-rc1` (`versionCode = 95`).

## [1.20.0-rc9] - 2026-07-06

### Changed

- Locked the Android app to portrait orientation so the mobile control surface no longer rotates into landscape.
- Tightened the home hero by moving the `Code` title up, reducing its size, and removing the subtitle plus session-count badge.
- Bumped Android client version to `1.20.0-rc9` (`versionCode = 94`).

## [1.20.0-rc8] - 2026-07-06

### Fixed

- Tightened Android desktop liveness detection so a relay/WebSocket-only connection is no longer treated as Mac online.
- The Android heartbeat watchdog now probes once per second and marks the Mac offline after 5 seconds without a Mac-originated snapshot/session/workspace/message event.
- Bumped Android client version to `1.20.0-rc8` (`versionCode = 93`).

## [1.20.0-rc7] - 2026-07-06

### Changed

- Combined the paired Mac status and workspace switcher into one compact home row with concise dot/icon + text affordances.
- Replaced the composer image icon with the generic plus attachment icon.
- Added WebSocket ping/pong plus an Android-side desktop heartbeat watchdog: the client probes `session.list` every 15 seconds and treats 45 seconds without any Mac event as offline, then enters automatic reconnect.
- Bumped Android client version to `1.20.0-rc7` (`versionCode = 92`).

## [1.20.0-rc6] - 2026-07-06

### Changed

- Replaced the centered capability picker dialog and uneven chips with a bottom sheet picker using full-width rows, stable typography, and radio/checkbox selection controls.
- Renamed the approval picker title from `Run controls` / `运行控制` to `Approval mode` / `审批模式`.
- Bumped Android client version to `1.20.0-rc6` (`versionCode = 91`).

## [1.20.0-rc5] - 2026-07-06

### Fixed

- Removed inactive gray status dots from the session composer icon toolbar so only active controls show a blue indicator or selected-count badge.
- Bumped Android client version to `1.20.0-rc5` (`versionCode = 90`).

## [1.20.0-rc4] - 2026-07-06

### Changed

- Reworked the session composer run controls into a compact icon-only toolbar so approval mode, provider, model, skills, experts, and plugins no longer consume a full title/value row above the input.
- Added dedicated vector icons for approval, provider, model, skills, expert mode, and plugins, with small active indicators and count badges for selected multi-choice controls.
- Bumped Android client version to `1.20.0-rc4` (`versionCode = 89`).

## [1.20.0-rc3] - 2026-07-06

### Fixed

- Treated old Mac gateway `Unsupported mobile command: capabilities.get.` responses as a graceful capability downgrade instead of a connection-level Android error.
- Bumped Android client version to `1.20.0-rc3` (`versionCode = 88`).

## [1.20.0-rc2] - 2026-07-06

### Added

- Added `capabilities.updated` parsing and `capabilities.get` requests so Android now renders Mac-reported providers, models, skills, experts, and plugins instead of local placeholder lists.
- Added compact picker dialogs for approval mode, provider, model, skills, experts, and plugins in the session composer.

### Changed

- Collapsed the home connection state into a compact status row that expands only when the user needs connect/disconnect/unpair actions.
- Collapsed the workspace selector into a compact summary row with an expandable horizontal picker, and limited each workspace group to the first five sessions until expanded.
- Resized the session composer so run controls stay in a single horizontal strip while the message text area defaults to a larger three-to-five-line writing surface.
- `message.send` now uses Mac capability IDs with `provider_id`, `model`, `skills`, `experts`, and `plugins` payload fields.
- Bumped Android client version to `1.20.0-rc2` (`versionCode = 87`).

## [1.20.0-rc1] - 2026-07-06

### Added

- Added a horizontally scrollable workspace rail above the home session list. It includes an all-workspaces tab plus Mac-reported or session-derived workspaces, and selecting a workspace filters the visible sessions.
- Added mobile run controls to the session composer: approval mode, model selection, skill selection, expert mode, and plugin activation.
- `message.send` now includes mobile-selected `approval_mode`, `model`, `skills`, `expert_mode`, and `plugins_enabled` fields so the Mac side can honor the chosen run context without introducing a new WebSocket command type.

### Changed

- Redesigned the paired home screen with more top whitespace, a clearer hero area, and a workspace-first session browsing flow.
- Redesigned the session bottom composer as a rounded mobile control console. Pending user asks, tool approvals, patch proposals, queued requests, and worktree changes now render directly below the latest input instead of above the conversation list.
- Bumped Android client version to `1.20.0-rc1` (`versionCode = 86`).

## [1.19.0-rc1] - 2026-07-01

### Added

- Added public relay pairing support. QR payloads that include `relay_base_url`, `relay_room`, and `relay_token` now skip the LAN `pair/claim` step and connect directly to `cdnproxy` `/ws/broadcast/<room>` with the relay bearer token.
- Stored gateway credentials now remember the optional relay room, while legacy LAN credentials continue to use the existing `/mobile/ws` endpoint.
- Bumped Android client version to `1.19.0-rc1` (`versionCode = 85`).

## [1.18.0-rc2] - 2026-06-20

### Changed

- Reworked the session composer end-to-end. The old in-list `ComposerCard` (ElevatedCard + section title + OutlinedTextField + image/send/stop row) is gone. New `MessageInputBar` is pinned to `Scaffold.bottomBar`, follows `imePadding()` so the keyboard pushes it up cleanly, and renders as a single chat-pill row: `[+ image] [rounded BasicTextField with placeholder] [send circle]`.
- Composer now exposes the "queue while responding" path that already existed in `MobileGatewayViewModel.sendMessage`. While the agent is responding, the send button stays visible (recolored secondary, content description `composer_queue_button`) so tapping it issues `queue.update {action:add}`. A subtle "Agent is working · sending will queue" hint with an inline red `Stop turn` text button shows above the input.
- Conversation list now uses `LazyListState` and auto-scrolls to the bottom whenever `conversationMessages.size` or `queuedMessages.size` changes, so streamed assistant tokens stay in view without manual scrolling.
- Placeholder text adapts to state: connected → `composer_placeholder` ("Message WillDeep…" / "给 WillDeep 发消息…"); agent responding → `composer_placeholder_queue`; disconnected → `composer_placeholder_offline`.
- Attachment thumbnails moved into the bottom bar above the pill (shrunk from 84dp to 64dp) so they no longer steal scroll real estate.
- Bumped Android client version to `1.18.0-rc2` (`versionCode = 84`).

### Removed

- Deleted `ComposerCard` from `WillDeepApp.kt` and its companion strings `section_composer` (unsurfaced; resource kept for now). `message_placeholder` is also unused by the new bar but retained until the next string sweep.

## [1.18.0-rc1] - 2026-06-18

### Added

- Added Mobile Gateway Tailscale fallback support. Pairing payloads can now carry `fallback_base_urls`, and Android also accepts `base_url#tailscale=100.x.x.x` style QR hints for compatibility with Mac payload experiments.
- `PairingPayload` now normalizes `base_url` by stripping URL fragments, de-duplicates fallback endpoints, and keeps the LAN URL as the primary endpoint.
- Stored gateway credentials now persist fallback endpoints alongside the device token, desktop name, and protocol version.
- Connection and health flows now try the LAN endpoint first, then fallback endpoints such as Tailscale before entering bounded reconnect backoff.
- Added JVM tests for fallback payload parsing, URL-fragment parsing, endpoint de-duplication, and health-target fallback retention.

### Changed

- Bumped Android client version to `1.18.0-rc1` (`versionCode = 83`).

## [1.17.0-rc59] - 2026-06-17

### Changed

- Stripped the noisy pairing card from the not-paired home screen. The flow is now: tap the scan card → camera opens → scan the QR from Mac → app auto-pairs and connects. No more exposed `payload` textarea, `device_name` field, `Gateway URL`, `protocol_version`, `gateway_server_version`, `pairing_allowed` chip, or the `Check Gateway` / `Pair` / `Disconnect` / `Forget token` button grid.
- New `MobileGatewayViewModel.scanAndPair(payload)` chains the existing `loadPairingPayloadFromQr` and `pair` so the scanner can hand off in one call. `WillDeepApp` wires the scanner to it.
- Not-paired body now shows only: the big "Scan to pair" card + (optional) a small spinner card while pairing/connecting + (optional) a red error card with one "Scan to pair" button.
- Paired-state `ConnectionSummaryRow` no longer leaks `state.baseUrl` (the `http://192.168.x.x:port` subtitle) or the verbose reconnect counter. Subtitle is now just `Connected` / `Reconnecting…` / `Disconnected` / error.
- Renamed `forget_button`: EN `"Forget token"` → `"Unpair"`; zh-CN `"忘记 token"` → `"解除配对"`.
- Reworded `error_device_revoked` to drop the word "token": EN `"Mac no longer recognizes this phone. Scan the QR code again to re-pair."`; zh-CN `"Mac 已不再识别这台手机，请重新扫码配对。"`.
- Added string `status_reconnecting_short` (EN `"Reconnecting…"` / zh-CN `"正在重连…"`).
- Bumped Android client version to `1.17.0-rc59` (`versionCode = 82`).

### Removed

- Deleted `PairingCard` composable from `WillDeepApp.kt`. It is no longer referenced; the not-paired home now uses `ScanPromptCard` + small status cards only. Strings `section_pairing`, `not_paired`, `gateway_url`, `gateway_server_version`, `gateway_pairing_allowed/blocked`, `pairing_payload_label/placeholder`, `device_name_label/placeholder`, `check_gateway_button`, `checking_gateway_button`, `pair_button`, `pairing_help` are no longer surfaced in the UI but kept in resources for now to avoid churn (they remain referenced from `StatusLine` and indirect call sites; can be pruned in a follow-up sweep).

## [1.17.0-rc58] - 2026-06-17

### Added

- New session flow now requires picking a Mac workspace first. Tapping the home `+` FAB opens a workspace picker dialog instead of immediately creating an empty session on the phone.
- New gateway protocol command `workspace.list` (Android → Mac) plus a matching `workspace.list` event payload (`payload.workspaces`) the Mac desktop peer is expected to return. Each entry carries `path`, optional `name`, `last_used_at`, and `session_count`. TODO: Mac side (`willdeep-agent` command whitelist + desktop peer responder) still owes its half of the contract — picker degrades gracefully (loading → empty + manual path entry) until that lands.
- `session.create` envelope now ships a `payload.workspace_path` field so the Mac binds the new session to the chosen workspace.
- `MobileGatewayUiState` carries `workspaces`, `isLoadingWorkspaces`, `workspacePickerVisible`, `workspacePickerError`, and `recentWorkspacePaths`. The picker remembers the last 8 paths chosen on this device for one-tap reuse.
- Strings (EN / zh-CN): `workspace_picker_title`, `workspace_picker_hint`, `workspace_picker_loading`, `workspace_picker_empty`, `workspace_picker_disconnected`, `workspace_picker_recent_title`, `workspace_picker_manual_title`, `workspace_picker_manual_placeholder`, `workspace_picker_create_button`, `workspace_picker_refresh_button`, `workspace_picker_session_count`, `cancel_button`, `error_workspace_required`.

### Changed

- `MobileGatewayViewModel.createSession` now takes a `workspacePath: String` and returns `Boolean`. Callers must route through the workspace picker first; the legacy zero-arg invocation no longer exists.
- Replaced the launcher icon with the Xedit / Mac WillDeep logo (sourced from `~/Sites/Xedit/appicons/icon_1024x1024.png`). Generated `ic_launcher.png` and `ic_launcher_round.png` at 48 / 72 / 96 / 144 / 192 px for the `mdpi` / `hdpi` / `xhdpi` / `xxhdpi` / `xxxhdpi` mipmap buckets via `sips`.
- Removed the adaptive-icon scaffolding (`mipmap-anydpi/ic_launcher.xml`, `ic_launcher_round.xml`, `drawable/ic_launcher_background.xml`, `drawable/ic_launcher_foreground.xml`) and the original `.webp` mipmaps so launchers display the full rounded-square Mac artwork instead of system-masking it.
- Bumped Android client version to `1.17.0-rc58` (`versionCode = 81`).

## [1.17.0-rc57] - 2026-06-17

### Changed

- `SessionDetailScreen` no longer pins the composer to the bottom. The composer is now the last item inside the conversation `LazyColumn`, so the input box scrolls with the conversation (Codex-style).
- Rewrote `ComposerCard`:
  - Removed the explicit "Queue" button. Implicit queueing: if the active session is `isResponding`, sending automatically issues a `queue.update` (`action: add`) envelope instead of `message.send`.
  - Stop control is now a red filled square button (only visible while the session is responding); send is a circular FAB-style button (visible otherwise).
  - Added an image-attachment button: opens `PickMultipleVisualMedia` (up to 6 images). Selected images appear as 84dp thumbnails above the text field with an `×` remove button; they ride along with the next send.
- `MobileGatewayUiState` now carries `attachments: List<ImageAttachment>`. New `addAttachment` / `removeAttachment` view-model actions.
- `sendMessage` now base64-encodes attached images into a `data:` URL and packs them into `payload.images` as OpenAI-style `{type:"image_url", image_url:{url:...}}` entries. Mac-side support for this field is needed in `willdeep-mac` for the images to be visible there.
- Home session list is now grouped by `workspace_name`. Each group has a header (workspace name + count) followed by its session cards. Sessions with no workspace name fall under an "Other" / "其他" group.

### Added

- Vector drawables: `ic_send`, `ic_stop_square`, `ic_image`, `ic_close`.
- Strings: `composer_add_image`, `attachment_remove`, `workspace_unknown_group` (EN / zh-CN).

### Changed

- Bumped Android client version to `1.17.0-rc57` (`versionCode = 80`).

## [1.17.0-rc56] - 2026-06-17

### Fixed

- `SessionDetailScreen` was leaking pending tool approvals, patch proposals, queued messages, conversation messages, and worktree updates across sessions: every session showed the same approval card. Now the detail view scopes those lists by `selectedSessionId` (entries with a different `sessionId` are filtered out) before passing the state down to `ApprovalCard` / `ConversationCard` / `QueueCard` / `WorktreeCard`.

### Changed

- Bumped Android client version to `1.17.0-rc56` (`versionCode = 79`).

## [1.17.0-rc55] - 2026-06-17

### Fixed

- Conversation messages no longer collapse to "No visible text" when the Mac sends OpenAI-style multi-part `content` arrays. `extractMessageContent` now walks the JSON tree, picks up every `{type: "text"}` / `output_text` / `input_text` segment, and collects image URLs from `image_url` / `image` parts into `GatewayMessage.imageUrls`.

### Added

- Assistant replies are now rendered with a minimal Markdown layer: fenced code blocks (```lang … ```), inline `code`, **bold**, *italic*, `# ## ###` headings, `-/*/+` bullet lists, `>` block quotes, and `[text](url)` links.
- Conversation rows now render image attachments (`imageUrls`) as Coil-loaded thumbnails (up to 6 per message). `coil-compose` 2.7.0 was added as a dependency.
- WebSocket frames are logged to logcat under the `GatewayWs` tag (first 2000 chars) to make protocol debugging tractable.

### Notes

- The "Mac side does not show the conversation" report is a Mac-side issue (the Android side already sends `message.send` envelopes correctly per protocol). It needs to be addressed in the `willdeep-mac` repository — the Mac gateway must also append mobile-originated messages to its local conversation log.

### Changed

- Bumped Android client version to `1.17.0-rc55` (`versionCode = 78`).

## [1.17.0-rc54] - 2026-06-16

### Changed

- Home screen now switches between two states based on `state.isPaired`:
  - **Not paired**: a prominent "Scan to pair" CTA card + the full `PairingCard` (payload paste, device name, check / pair / connect / disconnect / forget). No filter chips, no session list.
  - **Paired**: a compact one-row connection summary (status dot + paired desktop name + base URL + Connect/Disconnect + Forget), filter chips (counts), and the session card list.
- Tapping a session card or the "+ New session" FAB now opens a dedicated `SessionDetailScreen` instead of the legacy workspace dump. The detail screen has: back arrow + session title, pending tool/patch approvals on top, conversation stream, queued requests, changed-files panel, and a bottom `ComposerCard` (send / queue / stop).
- Removed the intermediate `WorkspaceScreen` route; routes are now `home` ↔ `scanner` ↔ `session`.
- Scanner now navigates back to home (not workspace) after a successful payload scan, so the user lands on the new paired-state home and can pick a session.

### Refactored

- Promoted `PairingCard`, `ConversationCard`, `ApprovalCard`, `ComposerCard`, `QueueCard`, `WorktreeCard`, `StatusLine` to `internal` so the new `HomeScreen` and `SessionDetailScreen` can compose them across files.

### i18n

- Added `session_detail_title` (EN / zh-CN).

### Changed

- Bumped Android client version to `1.17.0-rc54` (`versionCode = 77`).

## [1.17.0-rc53] - 2026-06-16

### Changed

- Redesigned the home screen in a Codex-style: large "Code" title, filter chips (All / Working / Needs input / Completed), tappable session cards with a connection-state dot, an extended "+ New session" floating action button, and a discreet version badge at the bottom.
- Moved the QR pairing scanner out of the home screen and into a dedicated full-screen `ScannerScreen`, reachable via a small QR icon in the home top bar (and from the workspace top bar). The legacy embedded `PairingScannerCard` was removed.
- Introduced state-based routing (`home` ↔ `scanner` ↔ `workspace`) in `WillDeepApp` so the busy multi-section legacy UI now lives behind the `WorkspaceScreen` route, opened by tapping a session card or creating a new one.
- Added new vector drawables for scan / back / add icons to avoid pulling in `material-icons-extended`.

### i18n

- Added English and Simplified Chinese strings for the new home, scanner, and workspace surfaces (`home_title`, filter labels, `home_session_connected`/`disconnected`, `scanner_screen_title`, etc.).

### Changed

- Bumped Android client version to `1.17.0-rc53` (`versionCode = 76`).

## [1.17.0-rc52] - 2026-06-15

### Fixed

- Strict live acceptance now runs only the real Mac gateway instrumentation test when a live pairing payload is available, preventing unrelated mock instrumentation tests from failing the final Android-to-Mac gate.
- Live acceptance evidence now uses collected ack/activity markers before the overall Gradle task status, preserving successful command flow evidence when a later target-file assertion times out.
- Target-file final acceptance can record host-side target file changes under the requested Mac workspace as `host_target_file` evidence when Android receives code activity but misses the specific target-file event.

### Changed

- Bumped Android client version to `1.17.0-rc52`.
- The default final live request now includes a unique run marker so repeated acceptance runs create an observable workspace update.

### Tests

- Re-ran Android JVM tests after marker and target-file evidence hardening.
- Re-ran the full Mac wrapper end-to-end acceptance; Android live pairing, WebSocket, message ack, Agent activity, code activity, target-file activity, and workspace-path evidence all passed.

## [1.17.0-rc51] - 2026-06-15

### Fixed

- Live Android acceptance now approves pending Mac tools that require neither an answer nor typed confirmation, allowing the requested workspace file edit to proceed while preserving danger-confirm and ask-user gates.
- Connected smoke now collects live logcat markers even when the instrumented test fails, so partial ack/activity evidence remains visible in JSON and Markdown reports.

### Changed

- Bumped Android client version to `1.17.0-rc51`.

### Tests

- Re-ran Android JVM tests after adding live non-interactive tool approval.

## [1.17.0-rc50] - 2026-06-15

### Fixed

- `message.send` omits the currently selected session id when a live workspace path is supplied, allowing the Mac desktop peer to bind the request to the intended workspace instead of reusing an old selected session.

### Changed

- Bumped Android client version to `1.17.0-rc50`.

### Tests

- Re-ran Android JVM tests after the workspace-bound send fix.

## [1.17.0-rc49] - 2026-06-15

### Added

- Android live acceptance now passes `MOBILE_GATEWAY_LIVE_WORKSPACE_PATH` into instrumentation as `mobileGatewayWorkspacePath`, so live `message.send` can bind to the intended Mac workspace.
- Final connected smoke evidence now includes a required `mac_workspace_path` gate for strict live acceptance.

### Changed

- Bumped Android client version to `1.17.0-rc49`.
- Updated live acceptance reports to include the Mac workspace path used for phone-originated coding requests.

### Tests

- Ruby smoke/final acceptance scripts now report and require workspace binding for final Android-to-Mac coding acceptance.

## [1.17.0-rc48] - 2026-06-15

### Fixed

- Aligned Android live acceptance and connected-device smoke defaults with the Mac Mobile Gateway default port `8877`.

### Changed

- Bumped Android client version to `1.17.0-rc48`.
- Updated Mobile Gateway docs, examples, and pairing payload tests from the old `8876` default to `8877`.

### Tests

- Verified Ruby smoke scripts and JVM tests still use the updated default Mac Gateway port.

## [1.17.0-rc47] - 2026-06-14

### Fixed

- Target-file live acceptance now treats changed kind or diff stats for an already-listed target file as valid Mac target-file activity, so create-or-update requests do not require a newly observed path.

### Changed

- Bumped Android client version to `1.17.0-rc47`.

### Tests

- Added JVM coverage for target-file activity when an existing worktree file is updated.

## [1.17.0-rc46] - 2026-06-14

### Added

- Added a target-file live acceptance gate: final Android live smoke can now require the requested Mac workspace file to appear in patch or worktree activity after `message.send`.
- The final wrapper now passes `MOBILE_GATEWAY_EXPECTED_TARGET_FILE=WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md` and reports `mac_target_file_signal`.

### Changed

- Bumped Android client version to `1.17.0-rc46`.

### Tests

- Added JVM coverage for target-file activity matching and verified final wrapper reports the pending target-file gate while Mac Gateway is not running.

## [1.17.0-rc45] - 2026-06-14

### Changed

- Strengthened the default live acceptance request so the Android final wrapper asks the Mac Agent to create or update `WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md` instead of sending a generic note request.
- The live acceptance wrapper report now records the non-secret request profile and target file used by the default workspace-edit task.
- Bumped Android client version to `1.17.0-rc45`.

### Tests

- Verified the final live wrapper still writes JSON/Markdown reports and exposes the default workspace-edit request profile while Mac Gateway is not running.

## [1.17.0-rc44] - 2026-06-14

### Added

- Added a strict Mac code-activity live acceptance gate: Android instrumentation can now wait for a post-`message.send` tool, patch, background job, or worktree signal and report `mac_code_activity_signal`.

### Changed

- Bumped Android client version to `1.17.0-rc44`.
- The final live acceptance wrapper now enables `MOBILE_GATEWAY_EXPECT_CODE_ACTIVITY=1` by default, so the one-command gate distinguishes a normal assistant response from a real code/workspace activity signal.

### Tests

- Added JVM coverage proving assistant text alone does not satisfy the code-activity signal.

## [1.17.0-rc43] - 2026-06-14

### Added

- Added Mac Gateway preflight diagnostics to `scripts/mobile_gateway_live_acceptance.rb`, including default token-file presence and public `/mobile/health` reachability/version/pairing metadata.

### Changed

- Bumped Android client version to `1.17.0-rc43`.

### Tests

- Verified the live acceptance wrapper still fails intentionally without a running Mac Gateway while writing the new preflight diagnostics.

## [1.17.0-rc42] - 2026-06-14

### Added

- Added Android support for Mac danger-tier tool approvals: `requires_confirmation` now shows a typed `confirm` field and sends `typed_confirmation` with `tool.decide`.

### Changed

- Bumped Android client version to `1.17.0-rc42`.
- Connected smoke now skips device instrumentation when no live pairing payload is available instead of running a guaranteed-failing connected test on attached devices.

### Tests

- Updated model/state tests, the connected smoke runner, and the Compose instrumentation smoke path to cover danger-tier typed confirmation before tool approval.

## [1.17.0-rc41] - 2026-06-14

### Added

- Added audit metadata to `scripts/mobile_gateway_live_acceptance.rb` reports, including Android version, payload source, attached device count, `message.send` ack marker, Mac Agent activity signal, and SHA256 hashes for the underlying connected-smoke JSON/Markdown reports.

### Changed

- Bumped Android client version to `1.17.0-rc41`.

### Tests

- Verified the live acceptance wrapper still fails intentionally without a connected Android device/live Mac payload while writing the expanded audit summary.

## [1.17.0-rc40] - 2026-06-14

### Added

- Added `scripts/mobile_gateway_live_acceptance.rb`, a one-command final acceptance wrapper that always enables strict live acceptance, requires Mac Agent activity, runs the connected smoke test, and writes JSON/Markdown summary reports.

### Changed

- Bumped Android client version to `1.17.0-rc40`.

### Tests

- Verified the live acceptance wrapper fails intentionally without a connected Android device/live Mac payload and records actionable final acceptance failures.

## [1.17.0-rc39] - 2026-06-14

### Added

- Added `REQUIRE_MOBILE_GATEWAY_LIVE_ACCEPTANCE=1` as a strict connected-smoke gate that fails unless live payload, Mac health, Android device, device reachability, live instrumentation, `message.send` acknowledgement, and Mac Agent activity evidence all pass.
- Added final live acceptance failure details to connected smoke JSON/Markdown reports.

### Changed

- Bumped Android client version to `1.17.0-rc39`.

### Tests

- Verified the no-device connected smoke path still reports pending evidence by default and fails intentionally when strict final live acceptance is required.

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
