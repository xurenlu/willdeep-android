# Mobile Gateway · `workspace.list` 协议需求

> 面向：`willdeep-agent`（Go 移动网关，relay）+ `willdeep-mac`（macOS 桌面端，desktop peer）
>
> 由：Android 客户端 `1.17.0-rc58`（`mobile-gateway.v1`）
>
> 状态：Android 侧接口/UI 已落地（FAB「新建会话」必须先选 workspace），Mac 侧两端实现尚缺，新建会话流程会卡在「正在向 Mac 查询工作区列表…」直到 Mac 侧上线该命令。

## 1. 背景与动机

当前手机端点 home `+` 直接发 `session.create`（无 payload），desktop peer 会创建一个未绑定 workspace 的「空挂」会话，后续 `message.send` 仍要靠用户额外补 `workspace_path`，体验差且容易把 LLM 跑到错的目录上。

新流程：手机端在新建会话前，**先向 Mac 拉取「Mac 上已存在的 workspace 列表」**，让用户从中选一项；选中后再发 `session.create`，同时在 payload 里带上 `workspace_path`。Mac 侧据此把新会话直接锚定到对应 workspace。

## 2. 协议总览

新增 1 个手机→Mac 命令、1 个 Mac→手机事件，共用同一个 type 字符串：

| 方向 | type | 描述 |
| --- | --- | --- |
| 手机 → Mac | `workspace.list` | 请求当前可用的 workspace 列表 |
| Mac → 手机 | `workspace.list` | 返回 workspace 列表（也可主动推送以做增量更新） |

`session.create` 命令同时扩展：payload 必须支持 `workspace_path` 字段（已是 Android 实现的约定，Mac 侧需正式落地）。

## 3. 修改清单（two parties）

### 3.1 `willdeep-agent`（Go 移动网关）

文件：`internal/agent/mobile_gateway.go`

1. **命令白名单**：把 `workspace.list` 加进 `mobileCommandTypes`，否则 relay 会拒收手机端的请求。
   ```go
   var mobileCommandTypes = map[string]struct{}{
       "session.list":   {},
       "session.create": {},
       "session.select": {},
       "message.send":   {},
       "turn.stop":      {},
       "queue.update":   {},
       "tool.decide":    {},
       "patch.decide":   {},
       "file.read":      {},
       "diff.get":       {},
       "job.kill":       {},
       "workspace.list": {},  // ← 新增
   }
   ```
2. **桌面事件白名单**：把 `workspace.list` 加进 `mobileDesktopEventTypes`，否则 desktop peer 推回来的事件会被丢弃。
   ```go
   var mobileDesktopEventTypes = map[string]struct{}{
       "state.snapshot":   {},
       "session.upsert":   {},
       "message.append":   {},
       "message.delta":    {},
       "message.done":     {},
       "tool.pending":     {},
       "tool.updated":     {},
       "patch.upsert":     {},
       "job.updated":      {},
       "worktree.updated": {},
       "workspace.list":   {},  // ← 新增
       "error":            {},
       "ack":              {},
       "command.error":    {},
   }
   ```
3. **Relay 语义保持原样**：网关不解析 payload，原样转发；ack 与 error 走原有路径。
4. **测试覆盖**：在 `mobile_gateway_test.go` 给出一条 round-trip：手机发 `workspace.list` → 网关转给 desktop peer → desktop peer 返回 `workspace.list` 事件 → 网关转回手机。同时验证未注册 desktop peer 时返回 `command.error`。

### 3.2 `willdeep-mac`（desktop peer）

desktop peer 需要新增一个命令处理器和一个事件发射器。

1. **接收 `workspace.list` 命令**：从手机来的 envelope（`type = "workspace.list"`，payload 可能为空 `{}`，也可能包含 `{ "include_inactive": true }` 等可选过滤参数，详见下文「字段说明」）。
2. **枚举 workspace**：从桌面端持有的 workspace 注册表（recent workspaces + 当前打开的 workspace + 用户书签等）收集条目，按下文 schema 序列化。
3. **回应**：
   - 发一个 `ack`（envelope id 用手机原始 id，`payload.type = "workspace.list"`），表示命令已收到；
   - 紧接着发一个 `workspace.list` 事件（独立 envelope），payload 形如 `{ "workspaces": [...] }`。
4. **错误**：若枚举失败（权限、IO 等），不要返回空数组——返回 `command.error`（用原 envelope id）+ 人类可读的 `message`，让手机端区分「真的没有」和「拿不到」。
5. **主动推送**：当 Mac 端 workspace 列表发生变化（用户在桌面端新打开了一个工作区、关闭了一个），desktop peer **可以**主动推一个 `workspace.list` 事件做增量同步。手机端会用最后一次收到的列表覆盖本地缓存。
6. **session.create 兼容**：手机现在会发 `session.create` 带 `payload.workspace_path`：
   ```json
   { "id": "...", "type": "session.create", "payload": { "workspace_path": "/Users/rocky/Sites/Xedit" } }
   ```
   desktop peer 必须把新会话绑定到该 workspace；如果路径无效（不存在 / 非目录 / 越权 / 与现有 workspace 不匹配），返回 `command.error`，**不要静默 fallback 到当前选中的 workspace**。

## 4. Schema 细节

### 4.1 命令 envelope（手机 → Mac）

```json
{
  "id": "uuid-v4",
  "type": "workspace.list",
  "payload": {}
}
```

**可选字段**（desktop peer 可以无视，但若实现请按以下语义）：

| key | 类型 | 描述 |
| --- | --- | --- |
| `include_inactive` | `bool` | 默认 `false`。`true` 时把最近一年内打开过但当前未挂载的 workspace 也算进来。 |
| `limit` | `int` | 截断条数；缺省返回全部。 |
| `query` | `string` | 模糊匹配 `path` / `name` 的子串过滤；缺省不过滤。 |

> Android 当前版本不传上述任何字段。后续如需，可对协议无伤升级。

### 4.2 事件 envelope（Mac → 手机）

```json
{
  "id": "uuid-v4",
  "type": "workspace.list",
  "payload": {
    "workspaces": [
      {
        "path": "/Users/rocky/Sites/Xedit",
        "name": "Xedit",
        "last_used_at": "2026-06-17T03:21:08Z",
        "session_count": 2,
        "is_current": true,
        "is_git_repo": true,
        "repository_root": "/Users/rocky/Sites/Xedit"
      },
      {
        "path": "/Users/rocky/Sites/willdeep-android",
        "name": "willdeep-android",
        "last_used_at": "2026-06-15T22:04:31Z",
        "session_count": 5,
        "is_current": false,
        "is_git_repo": true,
        "repository_root": "/Users/rocky/Sites/willdeep-android"
      }
    ]
  }
}
```

**`workspaces[i]` 字段**：

| key | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `path` | `string` | **是** | Mac 上的绝对路径，需是 `realpath`（解过软链），用作主键。 |
| `name` | `string` | 否 | 展示名；缺省 Android 端会取 `path` 的最后一段。 |
| `last_used_at` | `string` (RFC3339) | 否 | 用于按近期使用排序；desktop peer 应按此降序返回。 |
| `session_count` | `int` | 否 | 当前活跃会话数；用于排序提示。Android 端在卡片右下小字展示。 |
| `is_current` | `bool` | 否 | 是否是 desktop peer 现在选中的 workspace。Android 端会高亮，但目前不会强制。 |
| `is_git_repo` | `bool` | 否 | 仅作展示与未来扩展，Android 暂未使用。 |
| `repository_root` | `string` | 否 | 当 `path` 是子目录而 git 根在更上层时返回；Android 暂未使用。 |

**键的兼容别名**：解析时同时接受以下别名，desktop peer 选其一即可：

- `path` ←→ `workspace_path` ←→ `absolute_path`
- `name` ←→ `workspace_name` ←→ `title`
- `last_used_at` ←→ `updated_at` ←→ `ts`

> 推荐用首列规范名；兼容别名只是为了对接历史代码不致中断。

### 4.3 `session.create` payload

```json
{
  "id": "uuid-v4",
  "type": "session.create",
  "payload": {
    "workspace_path": "/Users/rocky/Sites/Xedit"
  }
}
```

- `workspace_path` 必传、字符串、绝对路径。
- desktop peer 必须用此路径解析并校验：路径存在、是目录、归属可访问的 workspace 集合。
- 校验失败 → `command.error`；不要静默改写或忽略字段。
- 创建成功后通过现有 `session.upsert` / `state.snapshot` 通道把新会话广播出来即可。

## 5. 行为与时序

```
手机                      网关                       desktop peer
 |  workspace.list (cmd) ->  |  workspace.list (cmd) ->  |
 |                            |                            |  枚举本地 workspace
 |                            |  <- ack (type=workspace.list) |
 |  <- ack (type=workspace.list) |                            |
 |                            |  <- workspace.list (event)    |
 |  <- workspace.list (event) |                            |

 用户点选一项 path = X
 |  session.create payload={workspace_path:X} -> |  session.create -> |
 |                            |                            |  绑定到 X，建好会话
 |                            |  <- ack + session.upsert  |
 |  <- ack + session.upsert  |                            |
```

- 手机端打开「workspace 选择器」时立即发 `workspace.list`，等 desktop peer 回事件后渲染列表。
- 若收到 `command.error` 或网关断线，手机端在选择器内回退到「手动粘贴绝对路径」模式（已实现）。
- desktop peer **可以**主动重发 `workspace.list` 事件做增量更新；手机端总是用最新一次事件全量覆盖本地列表。

## 6. 边界情况

| 场景 | desktop peer 行为 |
| --- | --- |
| 当前没有任何 workspace | 返回 `workspace.list` 事件，`workspaces = []`。手机端会显示「Mac 暂未上报工作区」+ 手动路径输入。 |
| 用户在 desktop peer 还没启动 / 还没连上网关 | 网关返回 `command.error`，message 形如 `"desktop peer unavailable"`。 |
| `session.create` 带的 `workspace_path` 在 `workspaces[]` 之外 | 推荐放行（用户可能粘贴了未注册的路径），但要做安全校验（存在、是目录、非系统敏感目录）。校验失败 → `command.error`。 |
| 同一路径同时是 git 仓库子目录 | 以 `realpath` 后的字符串去重；保留最深的可识别 workspace 条目。 |
| 路径含 unicode / 空格 / `~` | desktop peer 自行展开 `~` 与转义；返回给手机的 `path` 必须已 `realpath` 解析。 |

## 7. 安全 / 隐私

- `path`、`name` 可能包含项目代号或用户名，归属用户私有，不要写进网关日志。沿用现有 redaction 规则即可。
- 不允许列出 `/`, `/etc`, `~/Library/Mail/...` 等敏感目录；desktop peer 维护白名单（与现有 workspace 注册表一致）。

## 8. 版本 / 协议号

- 不需要 bump `MOBILE_GATEWAY_PROTOCOL_VERSION`（仍是 `mobile-gateway.v1`）：新增 type 对旧客户端是 forward-compatible（旧客户端会落入 `Raw` 分支，无害）。
- 旧版手机客户端发 `session.create` 不带 `workspace_path` 仍要兼容——desktop peer 可以保留原逻辑（沿用当前选中 workspace），同时把这种用法标记为 deprecated。Android `1.17.0-rc58` 起一律带该字段。

## 9. 测试与验收

Mac 侧需自验证：

1. **网关**：`go test ./internal/agent/...` 新增一条 round-trip 测试覆盖 `workspace.list` 命令 + 事件 + ack 转发。
2. **desktop peer**：
   - 单测：枚举返回的 workspace 严格按 `last_used_at` 降序；`session_count` 与活跃会话数一致；同路径去重。
   - 联调：与 Android `1.17.0-rc58` 真机配对后：
     1. 手机点 `+`，选择器应在 1s 内显示出至少一个工作区；
     2. 选中后会话立刻出现在 home 列表且 `workspace_name` 与选中项一致；
     3. 在该会话里发 `message.send`，desktop peer 必须在选中的 workspace 内执行（验证：让 Agent 在该 workspace 写一个 marker 文件，检查文件落地路径）。

手机侧验收已对应到 `scripts/mobile_gateway_live_acceptance.rb`，待 Mac 侧上线后联调即可。

## 10. 时间线建议

| 阶段 | 责任 | 内容 |
| --- | --- | --- |
| 1 | willdeep-agent | 加白名单 + 单测，先 ship（手机端在 picker 里立刻不再卡死，能拿到 `command.error`）。 |
| 2 | willdeep-mac | 实现枚举与事件 emit。 |
| 3 | willdeep-mac | `session.create` payload `workspace_path` 校验与绑定。 |
| 4 | 全栈 | 与 Android `1.17.0-rc58` 联调，跑通 live acceptance。 |
