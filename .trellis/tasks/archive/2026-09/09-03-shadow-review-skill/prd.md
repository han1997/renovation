# 整合 shadow-review 审查技能到 trellis 检查流程

## Goal

把 pi-shadow-mind 项目的审查思想（并行 Shadow Mind：架构审查 / 项目落地核对 / 文档同步 / 完成度审查）作为技能融合进本仓库的 trellis 代码写作流程，让每次代码检查都带上独立视角的复核，而不只是主 agent 自查。

## What I already know

- pi-shadow-mind 是 Pi（Cline 系）扩展，用 markdown 定义 Shadow Minds（`activation_probability` / `active_for_models` / `tools` / prompt body），靠 `turn_end` 心跳 + `report_to_main` 并行审查。
- 它不能直接跑在 opencode 上；移植的是内容与思想，机制用 opencode skills + trellis-check 承载。
- 本仓库是 Trellis 管理的单 repo，spec 按 backend / frontend 分层（无通用 quality/check 通用文件）。
- `trellis-check` 三平台 SKILL.md 内容一致；子 agent 定义在 `.opencode/agents/trellis-check.md`（mode: subagent），职责"审查 + 自我修复"。
- check 注入机制：`{task}/check.jsonl`（`{"file", "reason"}` 行）→ OpenCode 插件 `inject-subagent-context.js` 注入 spec/research/prd 上下文。
- 任务目录当前只有种子 jsonl（`_example` 行），无 prd.md；session active-task 指针未指向本任务。
- 用户已选定"档 1：转成 Skill"，并要求接线到 trellis。

## Assumptions (temporary)

- 接线目标 = trellis-check 检查流程（每次代码检查时自动带上 shadow-review 维度），而非实现 Pi 式回合末心跳并行。
- 需要跨 opencode / claude / codex 三平台一致（仓库现有 skills 已多平台镜像）。

## Open Questions

- (已答) 接线机制：独立技能 + 注入 check.jsonl。
- (已答) 审查维度：审查三件套（架构 + 项目落地核对 + 完成度），不含文档维护。
- (已答) 生效方式：插件自动附加 + 技能文件双保险。
- (已答) 存放范围：OpenCode 项目级（.opencode/skills + .opencode/plugins）。
- (已答) 多平台（Claude Code / Codex）：本次不做，技能文件日后可复制镜像。
- (已答) read-only：只读审查，只报告不修改。

## Requirements (evolving)

- (已确认) 创建 shadow-review 审查技能，封装三个维度：架构审查、项目落地核对、完成度审查。
- (已确认) 接线机制：独立技能 SKILL.md + 注入 trellis-check 检查流程。
- (已确认) 生效方式：OpenCode 插件 `getCheckContext` 自动附加技能内容 + 保留独立技能文件。
- (已确认) 存放范围：OpenCode 项目级（`.opencode/skills/shadow-review/SKILL.md` + 改 `.opencode/plugins/inject-subagent-context.js`）。
- (已确认) read-only：技能要求只读审查，只报告不修改代码。

## Requirements

1. 创建 `shadow-review` 技能（只读审查立场，只报告不修改代码），封装三个维度：
   - 架构审查：god component / 职责边界 / 模块边界 / 扩展点。
   - 项目落地核对：对照仓库实况，抓虚构的 API / 文件 / 约束 / 实现细节。
   - 完成度审查：独立验证结果是否真正满足任务，而非主 agent 自报"完成"。
2. 技能文件：`.opencode/skills/shadow-review/SKILL.md`（YAML frontmatter：name=shadow-review + description 用于自动发现）。
3. 插件自动附加：改 `.opencode/plugins/inject-subagent-context.js` 的 `getCheckContext`，对每次 `trellis-check` 派发（含 finish 场景）自动注入技能内容。
4. 保留独立技能文件：任何会话可按需手动加载。

## Acceptance Criteria

- [ ] `.opencode/skills/shadow-review/SKILL.md` 存在，frontmatter 合法，description 描述三个审查维度。
- [ ] `getCheckContext` 的返回内容包含 shadow-review 技能正文（不依赖 check.jsonl 里有该路径）。
- [ ] 派发一个 `trellis-check` 子 agent，确认注入的 prompt 里含 "=== .opencode/skills/shadow-review/SKILL.md ===" 块。
- [ ] 现有 `implement` / `research` 注入行为不受影响（未加技能）。
- [ ] 不破坏任何现有文件（纯新增 + 一处 getCheckContext 修改）。

## Definition of Done (team quality bar)

- 技能与插件语法正确（js 文件无语法错误、SKILL.md frontmatter 合法）。
- 通过真实派发一次 `trellis-check` 验证注入生效。
- (本次为配置/技能类改动，无业务代码 lint/typecheck 负担；若验证发现插件损坏需回滚)

## Decision (ADR-lite)

**Context**: pi-shadow-mind 的并行 Shadow Mind 不能直接跑在 opencode 上，需要把内容与思想移植到 trellis 检查流程。
**Decision**: 方案 1（独立技能 + 注入 check.jsonl）+ 插件自动附加双保险，只做 OpenCode 项目级，read-only 只报告不修改。
**Consequences**: 每次 trellis-check 自动带上第二视角；将来扩平台时复制技能文件 + 同步 hook 即可（本次不做）。

## Out of Scope (explicit)

- 不做 Pi 式回合末心跳自动并行唤醒（Plugin 档 2）。
- 不改 pi-shadow-mind 本体。
- 不做 Claude Code / Codex 平台同步。
- 不封装文档维护维度（trellis-update-spec 已覆盖）。

## Technical Notes

- 研究产出：`.trellis/tasks/09-03-shadow-review-skill/research/`（trellis-check 机制、check.jsonl、spec 清单、skills 布局、任务目录状态）。
- 参考实现：`C:\Users\hanhu\AppData\Local\Temp\opencode\pi-shadow-mind\README.md` 与 `src/types.ts`。