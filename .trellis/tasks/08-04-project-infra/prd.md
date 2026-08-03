# 建立项目基础设施：Git仓库与中文化开发规范

## Goal

为装修管家项目建立版本控制与文档基础设施：初始化 Git 仓库，确立中文为主的开发与沟通规范，建立更新日志机制，补全 README，并将这些约定固化到开发规范中。让项目从"能跑的单机脚本"升级为"可维护、可协作的工程化项目"。

## What I already know（仓库现状调研）

- 项目形态：零依赖、零构建的纯静态 Web 应用（vanilla JS / HTML / CSS），双击 `index.html` 即用
- **非 Git 仓库**（根目录无 `.git`）
- **缺失文件**：无 `.gitignore`、`README.md`、`CHANGELOG.md`、`LICENSE`、`CONTRIBUTING.md`、`.editorconfig`
- 代码与 UI 均为中文（`js/`、`css/`、`index.html`、PRD、知识库）
- `AGENTS.md` 存在（含 Trellis 管理区块 + 自定义内容）
- `.trellis/spec/` 现状：
  - `backend/*`、`frontend/*` 多为 "To fill" 模板
  - `frontend/index.md` 与 `backend/index.md` 明确写 **"All documentation should be written in English"** ← 与"中文化"要求冲突，需改
  - `guides/*` 已填充（思考指南，英文）
  - `frontend/directory-structure.md` 上一会话已填（中文）
- `.trellis/workspace/han1997/journal-1.md` 含个人会话日志
- 无 `package.json`，无 npm/lint/typecheck 工具链

## Assumptions（待验证）

- "沟通中文化"指：AI 与用户交互用中文、commit message 用中文、代码注释中文、文档中文
- "开发规范"落到 `.trellis/spec/` 下（项目已有此约定，sub-agent 按此加载）
- 更新日志以 `CHANGELOG.md` 文件形式维护
- README 用中文写，含项目介绍、使用方法、技术说明

## Decision (ADR-lite) — 更新日志方式

**Context**: 用户要求"修改需要写更新日志"，项目坚持零依赖（无 npm 工具链）。
**Decision**: 选方案 3 —— 不维护单独 CHANGELOG.md 文件，用 `git log` 作为更新记录。commit message 即更新日志，必须用中文、写得清晰可读（说明改了什么、为什么）。
**Consequences**: commit message 质量即日志质量 → 需在规范中明确 commit 写作要求；失去版本分组叙述（可接受，项目规模小）；省去 CHANGELOG 维护负担。

## Open Questions

- 是否需要 LICENSE（默认不建，除非用户要）

## Requirements（evolving）

1. 初始化 Git 仓库（`git init` + 合理 `.gitignore` + 初始提交）
2. 中文化约定：文档、注释、commit message、AI 沟通均用中文
3. 更新日志机制：以 `git log` 为更新记录，不建 CHANGELOG.md；规范中明确 commit 写作要求
4. commit message 格式：**中文类型前缀 + 中文描述**，例 `新增: 首次设置向导` / `修复: 预算超支计算` / `文档: 补充README` / `重构: ...` / `配置: ...`
5. 创建 `README.md`（中文，含必要内容）
6. 将上述约定写入 `.trellis/spec/` 开发规范
7. `.gitignore` 采用 Trellis 自带的 `.trellis/.gitignore` 规则排除运行时文件；`.trellis/workspace/` 个人日志**提交**（Trellis 默认）

## Acceptance Criteria（evolving）

- [ ] `git init` 完成，工作树干净，有初始提交
- [ ] `.gitignore` 合理（排除系统/临时文件，保留 `.trellis/` 项目配置）
- [ ] `README.md` 存在且为中文，含项目介绍、使用方法、技术说明
- [ ] `CHANGELOG.md` 存在且有初始结构与本次条目
- [ ] 开发规范文档记录了"中文/更新日志/commit 约定"
- [ ] spec index.md 的"English"约定改为中文

## Definition of Done

- Git 仓库可用，初始提交完成
- 文档齐全且语言一致（中文）
- 规范可被 sub-agent 加载（implement.jsonl 引用）

## Out of Scope（explicit）

- 引入 npm / 构建工具 / lint 工具链（保持零依赖）
- CI/CD 配置
- 开源协议选择（除非用户明确要 LICENSE）
- 远程仓库托管（仅本地 git init）

## Technical Notes

- 零依赖原则：不引入 changelog 生成工具，保持纯静态项目无 npm
- `.trellis/` 是项目配置，应提交（但 `.trellis/workspace/<dev>/journal-*.md` 个人日志是否提交需确认）
- 现有 spec "English" 约定需统一改为中文
