# Research: `.trellis/spec/` 规范文件完整清单

- **Query**: 列出所有 spec .md 文件的路径，特别是与代码质量、审查、错误处理、约定相关的
- **Scope**: internal
- **Date**: 2026-09-03

## Findings

### 完整文件清单（17 个 .md + 无 README）

| 路径 | 一句话说明 | 主题 |
|---|---|---|
| `.trellis/spec/project-conventions.md` | 项目级全局约定（中文、commit 格式、README、忽略规则），优先级高于分层规范 | 约定 ★ |
| `.trellis/spec/backend/index.md` | 后端入口：本项目无业务后端，只有 server.js 静态服务 | 入口 |
| `.trellis/spec/backend/directory-structure.md` | 静态服务文件边界与目录约定 | 结构 |
| `.trellis/spec/backend/database-guidelines.md` | 明确本项目没有数据库层 | 边界 |
| `.trellis/spec/backend/error-handling.md` | 静态服务错误（403/404/MIME）与浏览器端错误处理约定 | 错误处理 ★ |
| `.trellis/spec/backend/quality-guidelines.md` | 后端质量检查（node server.js 验证、零依赖、防路径穿越） | 质量 ★ |
| `.trellis/spec/backend/logging-guidelines.md` | server.js 控制台输出约定 | 日志 |
| `.trellis/spec/frontend/index.md` | 前端入口：零构建 vanilla JS SPA，Before Coding 清单 | 入口 ★ |
| `.trellis/spec/frontend/directory-structure.md` | 文件边界、加载顺序、window.* 全局模块契约 | 结构 |
| `.trellis/spec/frontend/component-guidelines.md` | 字符串渲染组件、事件委托、样式复用 | 组件 |
| `.trellis/spec/frontend/hook-guidelines.md` | 明确无 React/hooks，记录可复用逻辑位置 | 约定 |
| `.trellis/spec/frontend/state-management.md` | Store.state、localStorage、派生任务同步 | 状态 ★ |
| `.trellis/spec/frontend/quality-guidelines.md` | 无构建项目的人工/命令验证清单（check agent 核心依据） | 质量 ★ |
| `.trellis/spec/frontend/type-safety.md` | JS 运行时形状约束与防御性校验 | 类型/错误 ★ |
| `.trellis/spec/guides/index.md` | 思考指南入口：何时触发跨层/复用检查 | 引导 ★ |
| `.trellis/spec/guides/code-reuse-thinking-guide.md` | 复用思考：先搜索、重复模式、3+ 次才抽象 | 复用 ★ |
| `.trellis/spec/guides/cross-layer-thinking-guide.md` | 跨层思考：数据流、边界契约、错误处理 | 跨层 ★ |

> `get_context.py --mode packages` 输出仅 `Single-repo project (no packages configured)` + `Spec layers: backend, frontend`，不会列出单个 .md，需按目录枚举。

### 与代码质量 / 审查 / 错误处理 / 约定直接相关的文件（★）

1. **质量检查**：`frontend/quality-guidelines.md`（源码审查 + 浏览器手测清单、Forbidden Patterns）、`backend/quality-guidelines.md`（server.js 检查）
2. **错误处理**：`backend/error-handling.md`（403/404/MIME + toast/formModal 模式）、`frontend/type-safety.md`（运行时防御 + UI.esc()）
3. **审查视角的跨层/复用规范**：`guides/cross-layer-thinking-guide.md`、`guides/code-reuse-thinking-guide.md`
4. **项目约定**：`project-conventions.md`（中文、commit 格式 `<类型>: <中文描述>`、README、忽略规则）

### 没有的规范

- 不存在名为 `quality.md` / `check.md` / `conventions.md` 的**通用跨层**质量文件；质量规范按 backend/frontend 层拆分。
- 目录树中 `guides/` 只有 index + 两个 thinking guide，无其他通用指导文件。
- `.trellis/big-question/` 目录不存在（Test-Path = False）。

## Caveats / Not Found

- `project-conventions.md` 声明「优先级高于各分层规范（frontend / backend）」，check agent 应优先读它。
- `frontend/index.md` 提到 Encoding Caution（中文文案历史乱码），check/implement 时避免全量重写中文文案。
