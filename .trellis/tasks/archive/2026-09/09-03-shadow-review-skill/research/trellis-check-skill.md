# Research: trellis-check 技能与子 agent 工作机制

- **Query**: trellis-check 技能如何工作：读取 SKILL.md 与子 agent 定义，职责说明、加载哪些 spec、有没有步骤清单
- **Scope**: internal
- **Date**: 2026-09-03

## Findings

### 1. SKILL.md 位置与一致性

三个平台的 `trellis-check/SKILL.md` **内容完全一致**（SHA256 均以 `76A1617A...` 开头）：

| 路径 | 说明 |
|---|---|
| `.claude/skills/trellis-check/SKILL.md` | Claude Code 平台 |
| `.opencode/skills/trellis-check/SKILL.md` | OpenCode 平台 |
| `.agents/skills/trellis-check/SKILL.md` | 跨平台通用副本 |

### 2. SKILL.md 内容（`frontmatter` + 6 步清单）

`frontmatter.description` 原文：

> "Comprehensive quality verification: spec compliance, lint, type-check, tests, cross-layer data flow, code reuse, and consistency checks. Use when code is written and needs quality verification, before committing changes, or to catch context drift during long sessions."

步骤清单（`.claude/skills/trellis-check/SKILL.md`）：

| Step | 内容 |
|---|---|
| Step 1 | `git diff --name-only HEAD` + `git status` 找出改动 |
| Step 2 | 运行 `py -3 ./.trellis/scripts/get_context.py --mode packages` 列出 spec layer；读取 `cat .trellis/spec/<package>/<layer>/index.md`，再读 index 指向的具体 guideline 文件（index 只是指针） |
| Step 3 | 运行项目 lint / type-check / test，先修失败项 |
| Step 4 | 按清单审查：Code Quality（linter/typecheck/tests/无 debug 日志/无类型绕过）、Test Coverage（新函数→单测、bug fix→回归测试、行为变更→更新现有测试）、Spec Sync（是否要更新 `.trellis/spec/`） |
| Step 5 | 跨层维度（仅当改动跨 3+ 层）：A 数据流（Storage→Service→API→UI 读写方向）、B 代码复用（先 `grep -r "pattern" src/`）、C 导入/依赖（相对/绝对路径、无循环依赖）、D 同层一致性 |
| Step 6 | 报告并直接修复违规项，重新运行项目检查 |

### 3. 子 agent 定义

三个平台都有 `trellis-check` 子 agent 定义，职责是"审查代码变更 + 自我修复"（不只是报告）：

| 平台 | 路径 | 格式 | 与 SKILL.md 差异 |
|---|---|---|---|
| Claude Code | `.claude/agents/trellis-check.md` | Markdown frontmatter（`name` / `description` / `tools`） | 无 `mode` / `permission` 字段 |
| OpenCode | `.opencode/agents/trellis-check.md` | Markdown frontmatter（`description` / `mode: subagent` / `permission` 块） | 显式 permission：read/write/edit/bash/glob/grep/mcp__exa__* 全 allow |
| Codex | `.codex/agents/trellis-check.toml` | TOML（`sandbox_mode = "workspace-write"`，禁用 multi_agent） | 最长，含手动上下文加载协议 |

`.claude/agents/trellis-check.md` 与 `.opencode/agents/trellis-check.md` 内容不同（hash 不同：`D1359521...` vs `76E221C8...`），主要差异是 OpenCode 版多了 `mode`/`permission` frontmatter，且 Context Loading Protocol 补充了 `task.py current --source` 兜底命令。

### 4. 子 agent 核心内容摘录

- **Recursion Guard**：明确自己是已派发的 `trellis-check` 子 agent，**禁止**再 spawn `trellis-check` / `trellis-implement`；只有主 session 才能派发。
- **Trellis Context Loading Protocol**：
  - 若 dispatch prompt 上方有 `<!-- trellis-hook-injected -->` 标记 → prd/spec/research 已自动注入，直接干活。
  - 若标记缺失（Windows+Claude Code、`--continue`、hook 失败等）→ 读 dispatch prompt 首行 `Active task: <path>`，然后自行 Read `<task-path>/prd.md` 和 `<task-path>/check.jsonl` 里列出的 spec 文件。
- **Context**：读 `.trellis/spec/` 与预提交检查清单。
- **Core Responsibilities**：1) `git diff` 取未提交改动；2) 按 spec 核对；3) 自我修复（不单是报告）；4) 运行 typecheck/lint 验证。
- **Report Format**：`## Self-Check Complete` → Files Checked / Issues Found and Fixed / Issues Not Fixed / Verification Results / Summary。

### 5. Codex 版 TOML 独有内容

`.codex/agents/trellis-check.toml`：
- `sandbox_mode = "workspace-write"`。
- 显式「Required: Load Trellis Context First」协议（该平台无 hook 自动注入）：先在 dispatch prompt 找 `Active task:` 行 → `task.py current --source` → 都没有则问用户。随后读 `prd.md`/`info.md`、读 `check.jsonl`，**跳过没有 `"file"` 字段的行**（如 `{"_example": ...}` 种子行）。
- `check.jsonl` 无真实条目时的 fallback：读 `prd.md`，用 `get_context.py --mode packages` 自己挑选 spec。
- 无 `prd.md` 时禁止继续，须问用户。
- `[features] multi_agent = false` + `multi_agent_v2.enabled = false`：彻底移除 spawn/wait 工具，防止父进程继承 transcript 导致的 wait_agent 自死锁。

### 6. 流程中的调用位置

`.trellis/workflow.md`（第 197-204、255-258、510-525 行）：
- `trellis-check` **同时是 skill 和 agent**；"prefer the Agent form when verifying after code changes"。
- Phase 2.2 Quality check：sub-agent 平台 spawn `trellis-check` 子 agent（Claude/Cursor/OpenCode/codex-sub-agent/Kiro/Gemini/Qoder/CodeBuddy/Copilot/Droid/Pi）；inline 平台（codex-inline/Kilo/Antigravity/Windsurf）则主 session 加载 `trellis-check` skill。
- Phase 3.1 Quality verification：主 session 加载 `trellis-check` skill 做最终验证。
- 派发协议：dispatch prompt 必须以 `Active task: <task path>` 开头（全平台、全部子 agent）。

## Related Specs

- `.trellis/workflow.md` — Phase 2.2 / 3.1 使用位置、派发协议
- `.trellis/spec/guides/cross-layer-thinking-guide.md` — SKILL Step 5 跨层数据流对应

## Caveats / Not Found

- `.agents/agents/` 目录不存在（`Test-Path` = False）；`.agents/` 下只有 `skills/`。通用 agent 定义实际放在 `.claude/agents/`、`.opencode/agents/`、`.codex/agents/`。
