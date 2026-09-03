# Research: skills 目录布局与 opencode 配置

- **Query**: 现有 skills 目录布局（`.opencode/skills/`、`.claude/skills/`、`.agents/skills/`），opencode.json 是否存在及 permission/agent 配置
- **Scope**: internal
- **Date**: 2026-09-03

## Findings

### 1. 三个 skills 目录内容对比

**.claude/skills/**（7 个）：
`trellis-before-dev`、`trellis-brainstorm`、`trellis-break-loop`、`trellis-check`、`trellis-meta`、`trellis-spec-bootstarp`、`trellis-update-spec`

**.opencode/skills/**（7 个，与 .claude 同名同内容）：
`trellis-before-dev`、`trellis-brainstorm`、`trellis-break-loop`、`trellis-check`、`trellis-meta`、`trellis-spec-bootstarp`、`trellis-update-spec`

**.agents/skills/**（10 个，最全）：
`trellis-before-dev`、`trellis-brainstorm`、`trellis-break-loop`、`trellis-check`、`trellis-continue`、`trellis-finish-work`、`trellis-meta`、`trellis-spec-bootstarp`、`trellis-start`、`trellis-update-spec`

> 差异：`.agents/skills/` 独有 `trellis-start` / `trellis-continue` / `trellis-finish-work`（主 session 会话级技能），`.claude` 与 `.opencode` 用 commands（`.claude/commands/trellis/continue.md`、`finish-work.md` 与 `.opencode/commands/trellis/` 相同）代替。

验证：`.claude/skills/trellis-check/SKILL.md`、`.opencode/skills/trellis-check/SKILL.md`、`.agents/skills/trellis-check/SKILL.md` 三个文件 SHA256 一致（`76A1617A...`），可推断同套技能三处同步。

### 2. 子 agent 定义目录

| 平台 | 路径 | 内容 |
|---|---|---|
| Claude Code | `.claude/agents/` | `trellis-check.md`、`trellis-implement.md`、`trellis-research.md` |
| OpenCode | `.opencode/agents/` | `trellis-check.md`、`trellis-implement.md`、`trellis-research.md`（Markdown + `mode: subagent` + `permission` frontmatter） |
| Codex | `.codex/agents/` | `trellis-check.toml`、`trellis-implement.toml`、`trellis-research.toml`（TOML + `sandbox_mode`） |
| `.agents/agents/` | **不存在** | Test-Path = False |

### 3. opencode.json 是否存在

- **项目根目录 `opencode.json` / `opencode.jsonc` / `.opencode/opencode.json`：均不存在**（Test-Path 三个路径全 False）。
- **用户级全局配置存在**：`C:\Users\hanhu\.config\opencode\opencode.json`（353 行）。

全局 `opencode.json` 的内容**只含 `provider` 配置**（模型路由），**没有任何 `permission` / `agent` / `skill` / `plugin` 配置**：

- 配置了多个 provider：`abrdns`、`anyrouter`、`ark`、`elysiver`、`fastaitoken`、`fengwind`、`hyb-default`、`hybgpt`、`jimao`、`lilililwan`、`muyuan`、`nas`（局域网 192.168.10.4:3016 Ollama）、`opencode`、`ririxin`、`sharedchat`、`wong`、`wong-gpt`、`zhipuai`、`zmoon`。
- 全部走 `@ai-sdk/openai-compatible`，含 apiKey/baseURL（模型以 deepseek-v4-flash/pro、glm-5.2、gpt-5.x、kimi、grok、sensenova 为主）。
- 顶层键只有 `$schema` 和 `provider`；**无 `permission`、`agent`、`skill`、`plugin`、`mcp` 段**。

结论：OpenCode 平台的 agent/permission 配置在 `.opencode/agents/*.md` 的 frontmatter 里（每个 agent 自带 `mode: subagent` 和 `permission` 块），而不是 opencode.json。permission 内容示例（`.opencode/agents/trellis-check.md` 第 5-12 行）：

```yaml
mode: subagent
permission:
  read: allow
  write: allow
  edit: allow
  bash: allow
  glob: allow
  grep: allow
  mcp__exa__*: allow
```

`.opencode/agents/trellis-research.md` 额外多一条 `mcp__chrome-devtools__*: allow`。

### 4. OpenCode 平台结构（`.opencode/`）

```
.opencode/
├── agents/        # trellis-check / trellis-implement / trellis-research（.md，subagent + permission）
├── commands/      # trellis/continue.md、trellis/finish-work.md
├── lib/           # session-utils.js、trellis-context.js（TrellisContext 类：JSONL 读取、任务解析）
├── plugins/       # inject-subagent-context.js、inject-workflow-state.js、session-start.js
├── skills/        # 7 个 trellis-* 技能
├── package.json   # dependencies: @opencode-ai/plugin 1.18.11
├── .gitignore
├── package-lock.json
└── node_modules/  # zod 等依赖（由 plugin 引入）
```

插件注册方式：`.opencode/plugins/*.js` 默认导出 `(input) => hooks`（OpenCode 1.2.x 约定），通过 `tool.execute.before`（context 注入）等 hook 生效。

### 5. Claude Code 平台 hooks（对照）

`.claude/settings.json`：
- `SessionStart` → `session-start.py`（matcher: startup/clear/compact）
- `PreToolUse` Task+Agent → `inject-subagent-context.py`
- `UserPromptSubmit` → `inject-workflow-state.py`
- `env.CLAUDE_BASH_MAINTAIN_PROJECT_WORKING_DIR=1`

`.claude/hooks/` 下为同名 Python 脚本；`.opencode/plugins/` 下为同名 JS 插件，二者职责对应（session-start / inject-subagent-context / inject-workflow-state）。

## Related Specs

- `.trellis/spec/guides/cross-layer-thinking-guide.md` 第 88-99 行 — 跨平台命令模板一致性规则
- `.claude/settings.json` — Claude 平台 hook 注册

## Caveats / Not Found

- 项目内无 `opencode.json`；全局配置文件只含 provider 路由，agent 权限全在 `.opencode/agents/*.md` frontmatter。
- `.agents/agents/` 不存在；`.agents/` 下只有 `skills/`。
