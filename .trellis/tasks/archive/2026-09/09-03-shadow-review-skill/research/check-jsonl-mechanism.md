# Research: check.jsonl / implement.jsonl 机制与 spec 目录树

- **Query**: check.jsonl 机制：归档任务的 jsonl 条目格式（file+reason）、spec 注入方式；`.trellis/spec/` 目录树（backend/frontend 有哪些 .md），有无通用 quality/check 规范文件
- **Scope**: internal
- **Date**: 2026-09-03

## Findings

### 1. jsonl 种子机制

`task.py create` 会对每个任务目录播种 `implement.jsonl` 和 `check.jsonl`（`task_store.py` 中第 267-277 行，仅在文件不存在时写入；第 321-323 行提示 AI 后续去 curate）。

种子内容（`task_store.py` 第 154-161 行，`_SEED_EXAMPLE`）即当前任务目录里看到的：

```json
{"_example": "Fill with {\"file\": \"<path>\", \"reason\": \"<why>\"}. Put spec/research files only — no code paths. Run `py -3 .trellis/scripts/get_context.py --mode packages` to list available specs. Delete this line once real entries are added."}
```

- 种子行是**自描述示例**，没有 `"file"` 字段，消费方（Codex TOML、JS 插件、`task.py validate`）会自动跳过。
- 真实条目格式：**每行一个 JSON 对象**，`{"file": "<repo-root 相对路径>", "reason": "<为什么需要>"}`。
- 只放 **spec 文件和 research 文件**，不放代码文件。
- 支持目录条目 `{"file": "path/to/dir/", "type": "directory", "reason": "..."}`（JS 插件 `readJsonlWithFiles` 支持，第 313-348 行）。

### 2. 归档任务中的真实示例

**`.trellis/tasks/archive/2026-08/08-19-improve-stage-decision-support/check.jsonl`**（5 条真实条目）：

```json
{"file": ".trellis/spec/project-conventions.md", "reason": "检查项目全局约定"}
{"file": ".trellis/spec/frontend/index.md", "reason": "检查前端模块边界"}
{"file": ".trellis/spec/frontend/component-guidelines.md", "reason": "检查渲染、事件和路由聚焦行为"}
{"file": ".trellis/spec/frontend/state-management.md", "reason": "检查派生状态与数据兼容"}
{"file": ".trellis/spec/frontend/quality-guidelines.md", "reason": "检查静态验证和移动端质量"}
```

同任务 `implement.jsonl` 用**中文 reason** 列出几乎相同的 spec 集合（project-conventions + frontend/index + component-guidelines + state-management + quality-guidelines），视角是"写代码前要遵守"。

**`.trellis/tasks/archive/2026-08/08-04-renovation-app/check.jsonl`**（3 条，英文 reason）：

```json
{"file": ".trellis/spec/frontend/index.md", "reason": "Frontend spec index for quality verification"}
{"file": ".trellis/spec/guides/code-reuse-thinking-guide.md", "reason": "Check for duplicated data patterns between knowledge.js and prices.js"}
{"file": ".trellis/spec/guides/cross-layer-thinking-guide.md", "reason": "Verify DATA fields consumed by app.js/views are all defined in knowledge.js — no missing references"}
```

**`.trellis/tasks/archive/2026-08/08-04-space-planning/check.jsonl`**（5 条，中文 reason，含 prd.md）：

```json
{"file": ".trellis/spec/frontend/index.md", "reason": "验证中文化约定落地"}
{"file": ".trellis/spec/frontend/directory-structure.md", "reason": "验证 spaces 字段与 spaceNeeds 数据遵循 IIFE 全局模式与 data→view 只读契约"}
{"file": ".trellis/spec/project-conventions.md", "reason": "验证 commit 格式、中文、零依赖约定"}
{"file": ".trellis/spec/guides/cross-layer-thinking-guide.md", "reason": "验证 spaceNeeds 跨层（knowledge→app向导→more→budget→stages）数据契约一致"}
{"file": ".trellis/tasks/08-04-space-planning/prd.md", "reason": "对照 8 项验收标准检查完成度"}
```

**`.trellis/tasks/archive/2026-08/08-04-project-infra/check.jsonl`**（3 条）：含 `.trellis/spec/frontend/index.md`、`.trellis/spec/backend/index.md`、`.trellis/tasks/08-04-project-infra/prd.md`。

> 注意：部分归档 jsonl 里还残留种子 `_example` 行（如 08-19-improve-stage-decision-support 第 1 行），消费方按规则跳过。另外可见实际做法中**也会把 `prd.md` 列入 jsonl**（虽然 workflow.md 说 prd.md 由插件单独注入，手工加入也无害）。

### 3. spec 注入方式（三平台）

**OpenCode**（`.opencode/plugins/inject-subagent-context.js`，用 `tool.execute.before` hook）：
- 派发 `Task` 工具时识别 `subagent_type`（去掉 `trellis-` 前缀后为 implement/check/research）。
- 任务目录解析优先级：session runtime context → dispatch prompt 里的 `Active task:` hint → 单 session fallback。
- `getCheckContext`（第 60-77 行）：读 `<taskDir>/check.jsonl` → `ctx.readJsonlWithFiles(jsonlPath)` 把每个 `file` 的内容读出来 → `ctx.buildContextFromEntries(entries)` 拼成 `=== <path> ===\n<content>` 块 → 再附加 `<taskDir>/prd.md` 内容 → `buildPrompt` 包一层 `<!-- trellis-hook-injected -->` 模板注入到 dispatch prompt。
- `getImplementContext`（第 33-55 行）：同样读 `implement.jsonl`，额外附加 `info.md`。
- `getFinishContext`（第 82-85 行）：finish 阶段复用 check 逻辑（`[finish]` 标记由 dispatch prompt 中 `[finish]` 子串触发）。
- 底层读取在 `.opencode/lib/trellis-context.js`：`readJsonlWithFiles`（第 319-348 行，支持 file 与 directory 两种 entry）与 `buildContextFromEntries`（第 350-352 行）。

**Claude Code**：`.claude/hooks/inject-subagent-context.py` 挂到 `PreToolUse`（Task + Agent matcher，`.claude/settings.json` 第 38-59 行）。

**Codex**：无 hook，靠 TOML 里"手动加载上下文协议"让 agent 自己读 `check.jsonl` / `implement.jsonl`。

### 4. `.trellis/spec/` 目录树

```
.trellis/spec/
├── project-conventions.md            # 项目级全局约定（中文、commit 格式、README、忽略规则）★
├── backend/
│   ├── index.md                      # 入口：明确本项目无业务后端，仅 server.js 静态服务
│   ├── directory-structure.md        # 静态服务文件边界
│   ├── database-guidelines.md        # 明确无数据库层
│   ├── error-handling.md             # 静态服务错误 + 浏览器端错误处理约定 ★
│   ├── quality-guidelines.md         # 后端质量检查清单 ★
│   └── logging-guidelines.md         # server.js 控制台输出约定
├── frontend/
│   ├── index.md                      # 入口：零构建 vanilla JS 单页应用，Before Coding 清单 ★
│   ├── directory-structure.md        # 文件边界、加载顺序、window.* 全局模块契约
│   ├── component-guidelines.md       # 字符串渲染、事件委托、样式复用
│   ├── hook-guidelines.md            # 明确无 React/hooks，记录可复用逻辑位置
│   ├── state-management.md           # Store.state、localStorage、派生任务同步
│   ├── quality-guidelines.md         # 无构建项目的人工/命令验证清单 ★
│   └── type-safety.md                # JS 运行时形状约束与防御性校验 ★
└── guides/
    ├── index.md                      # 思考指南入口（何时触发跨层/复用检查）★
    ├── code-reuse-thinking-guide.md  # 复用思考：先搜索、重复模式、3+ 次才抽象 ★
    └── cross-layer-thinking-guide.md # 跨层思考：数据流、边界契约、错误处理 ★
```

- **没有**名为 `quality.md` / `check.md` 的独立通用规范文件；质量检查规范按层分散在 `backend/quality-guidelines.md` 与 `frontend/quality-guidelines.md`。
- 与质量/审查/错误处理/约定最相关的文件标注 ★：`project-conventions.md`、`backend/error-handling.md`、`backend/quality-guidelines.md`、`frontend/quality-guidelines.md`、`frontend/type-safety.md`、`frontend/state-management.md`、`guides/index.md` 及两个 thinking guide。
- `get_context.py --mode packages` 实际输出：`Single-repo project (no packages configured)` + `Spec layers: backend, frontend`。

### 5. spec 关键内容摘录

**frontend/quality-guidelines.md**（check agent 的核心依据）：
- 项目无 npm/构建/lint/自动测试 → 质量靠源码审查 + 浏览器手测。
- Required Manual Checks：直接打开 `index.html` 不报错；手机访问运行 `node server.js` + `http://localhost:8787`；底部 5 tab 可切换（home/stages/budget/guide/more）；表单保存/关闭/重渲染/刷新保留；JSON 导入导出、CSV 导出含支出明细；新控件有 `data-action`/`data-change` 处理。
- Source Review Checklist：脚本顺序、`UI.esc()`、`Store.save()`、`App.rerender()`/`App.go()`、持久字段写 `defaults()`、新 id 全文搜索、CSS 复用 tokens。
- Encoding Rule：不要格式化/重写 `README.md`/`index.html`/`js/data/*.js`/`js/views/*.js` 中的大段中文文案（历史编码错位）。
- Forbidden Patterns：不加构建工具/依赖、不用未转义 innerHTML、不用 DOM dataset 当唯一状态源、不复制 helper、不重复派生计算。

**backend/quality-guidelines.md**：只覆盖 `server.js`/启动脚本；`node server.js` 验证（打印 localhost + IPv4、首页可加载、CSS/JS 正常返回、无路径穿越）；保持 CommonJS/零依赖/固定端口 8787。

**backend/error-handling.md**：越界 `403`、读失败 `404`、MIME 兜底 `application/octet-stream`；浏览器端 `js/storage.js` 捕获 localStorage 异常、`UI.toast()`/`UI.formModal()` 处理可恢复错误；不要引入统一 JSON 错误格式。

**frontend/type-safety.md**：无 TS/JSDoc/Zod，靠运行时防御（`defaults()` 合并、`Array.isArray()`、`typeof === 'object'`、导入校验 `ver`）；数据契约 id 引用（`DATA.stages[].id`、`DATA.checklists[].id`、`DATA.styles[].id`、`PRICES.rates[].id`）改动前必须全文搜索；`UI.formModal()` 返回字符串/数字；HTML 必须 `UI.esc()`。

**project-conventions.md**：所有产出物用中文（文档/注释/commit/AI 沟通）；commit 格式 `<类型>: <中文描述>`（新增/修复/文档/重构/配置/性能/测试）；不维护 CHANGELOG.md，以 git log 为更新记录；分支约定；README 维护；忽略规则。

## Related Specs

- `.trellis/workflow.md` 第 54-57、371-414 行 — jsonl 用途、格式、curation（Phase 1.3）规则
- `.trellis/scripts/common/task_store.py` — 种子写入逻辑
- `.trellis/scripts/common/task_context.py` — `add-context` / `validate` / `list-context` 命令

## Caveats / Not Found

- `get_context.py --mode packages` 只输出 layer 名（backend/frontend），不输出具体 .md 列表；具体文件列表需按目录自行枚举。
- 未见单独的「通用 check 规范」文件；quality 规范按 backend/frontend 层拆分。
