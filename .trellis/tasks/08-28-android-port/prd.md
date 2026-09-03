# Android 版装修管家移植

## Goal

把现有装修管家（零依赖 vanilla JS Web SPA）以 **原生 Kotlin + Jetpack Compose** 的形态重写到 Android 平台，提供一个可独立分发、原生体验、可调用 Android 平台能力（系统分享、文件保存、通知等）的"装修管家"App，作为 Web 版长期演进之外的原生客户端。

## What I already know

- 项目当前形态：纯静态 Web 应用（`index.html` + `css/style.css` + `js/**/*.js`），无后端、无构建、无 npm。
- 数据全部落在浏览器 `localStorage`，由 `window.Store` 管理；导入导出走 JSON / CSV。
- 知识库全部为只读静态数据：`window.DATA`（流程、指南、风格、百科、验收清单）、`window.PRICES`（价格档位、预算模板）。
- 模块划分（来自 `README.md`）：
  - `js/data/knowledge.js` `js/data/prices.js` 只读知识库
  - `js/storage.js` 本地存储与导入导出
  - `js/ui.js` 通用界面工具（HTML 转义、modal、toast、日期/金额格式化）
  - `js/views/{home,stages,budget,guide,more}.js` 五个 Tab 页面
  - `js/app.js` 路由 / 进度计算 / 首次设置向导
- 14 个装修阶段：验房收房 → 量房设计 → 主体拆改 → 水电改造 → 防水工程 → 瓦工贴砖 → 木工工程 → 墙面油漆 → 成品安装 → 开荒保洁 → 家具家电进场 → 软装布置 → 通风除醛 → 入住验收。
- 当前 `phone-server.bat` + `server.js` 仅用于局域网 Web 访问，不属于 App 业务。
- 已存在的 `.trellis/spec/` 只有 backend（实质只是 `server.js` 静态服务器）和 frontend（Web SPA）两层；**没有 Android / Kotlin / Compose 相关 spec**，需要新建。

## Assumptions (temporary)

- 本任务的"Android"指现代 Android（minSdk ≥ 24，覆盖 Android 7.0+），跟随 Jetpack Compose 当前稳定版。
- App 仅面向中国大陆用户，不强求海外分发；不引入 Google Play 依赖（不强制 GMS）。
- 数据本地化优先；不引入登录、账号、云同步；保留 Web 版同样的"换设备靠 JSON 备份恢复"语义。
- 与 Web 版长期共存：两份代码各走各的迭代路线，不要求编译期共享（因为 Web 版本身零构建，Kotlin 无法直接 import）。但**知识数据**（流程、避坑、风格、价格、验收清单、百科）需要可复用——参见 Technical Approach。
- 范围先到"功能等价 Web 版"为 MVP；通知 / Widget / 桌面快捷方式等原生扩展进 Out of Scope。

## Decisions (ADR-lite)

### D1 · 技术路线：原生 Kotlin + Jetpack Compose 重写

**Context**：现有装修管家是零依赖 vanilla JS Web SPA。"Android 版"有多种实现路径：WebView 包装、Capacitor / Cordova 桥接、原生 Kotlin 重写。
**Decision**：完全用 Kotlin + Jetpack Compose 重写，与 Web 版长期双线维护。
**Consequences**：
- (+) 原生体验、Material You 取色、Android 12+ 动态主题。
- (+) 可调用 SAF / `Intent.ACTION_SEND` 等 Android 平台能力。
- (-) 需重写所有 View 层与状态层，工程量大。
- (-) 与 Web 版无法编译期共享代码；功能等价靠"功能对齐"而非"代码复用"保证。

### D2 · 知识数据事实源：Android 端独立维护一份

**Context**：知识数据（14 阶段、避坑要点、风格、价格、验收、百科）需要在 Android 端可读。
**Decision**：Android 端在 `app/src/main/assets/knowledge.json` 和 `prices.json` 维护一份独立 JSON，启动时一次性读入内存缓存。**不**与 Web 版 `js/data/*.js` 自动同步。
**Consequences**：
- (+) 两端彻底解耦，互不阻塞。
- (+) Android 端可针对移动端体验调整文案与字段。
- (-) 需要长期双份维护；资料可能漂移。
- (-) 后续若发现漂移，需要"对齐快照"流程（暂不在本任务范围）。

### D3 · MVP 范围：完全功能等价 Web 版

**Context**：是否分阶段交付。
**Decision**：首版覆盖 5 Tab（首页 / 流程 / 预算 / 指南 / 我的）+ 首次设置向导 + JSON 备份 / 恢复 + CSV 导出。
**Consequences**：
- (+) 一次性交付完整的换机替代品。
- (-) 工作量大、迭代周期长。
- (-) 验收 / 测试矩阵更广。

### D4 · 新增原生能力：仅 Material You 动态取色

**Context**：是否叠加系统分享、推送、Widget 等原生能力。
**Decision**：MVP 仅引入 Material You 动态取色（Android 12+ 走 `dynamicColorScheme`，低版本回退静态色板）。其它原生能力（系统分享、通知、Widget 等）暂不做。
**Consequences**：
- (+) 取色风险最低，不引入运行时权限。
- (+) 与 D1 的"原生体验"卖点形成最小呼应。
- (-) 缺少通知提醒等高频感知功能，需后续迭代补齐。

### D5 · 数据迁移：首版不做 Web → Android 导入

**Context**：用户已有 Web 版数据，如何迁移到 Android。
**Decision**：首版不实现 Web JSON 备份导入到 Android；用户需在 Android 端重新完成首次设置向导。
**Consequences**：
- (+) 简化首版范围，避免 Web JSON 字段映射的兼容性陷阱。
- (-) 换机 / 双端并用用户需要重新录入。
- (-) 后续若用户反馈强烈，需要补一个"导入 Web 备份"的迭代版本。

## Open Questions (resolved)

- ~~知识数据的单一事实源~~ → 见 D2
- ~~MVP 范围~~ → 见 D3
- ~~是否新增原生能力~~ → 见 D4
- ~~Web → Android 数据迁移~~ → 见 D5

## Remaining Open Questions

（暂无；进入研究阶段。）

## Research Topics (to dispatch as `trellis-research` sub-agents)

1. **Android 工程脚手架** — Gradle / Kotlin / Compose / minSdk / targetSdk / 编译版本号约定 / 签名 / 输出 APK 路径；为后续仓库结构奠基。
2. **Room schema 规划** — 从 `js/storage.js` 读出所有 `Store.state` 字段与派生任务同步逻辑，反推 Android Room 表 / DAO / 迁移策略。
3. **Web JSON 形态分析**（即使首版不做导入，仍要为后续预留解析路径） — 记录 `js/data/knowledge.js` `js/data/prices.js` 与 `js/storage.js` 的 JSON 形态，供 Android 端做独立的"对照表"使用。
4. **Material You 取色方案** — `dynamicLightColorScheme` / `dynamicDarkColorScheme` / 静态回退色板 / 与 Web 版米色 (#f6f3ee) 品牌的衔接策略。

## Requirements (evolving)

### MVP（待用户确认范围）

- 五 Tab：首页 / 流程 / 预算 / 指南 / 我的。
- 首次设置向导：与 Web 版同结构（面积 / 城市 / 开工日期 → 装修方式 → 档次与总预算）。
- 流程页：14 阶段时间轴、阶段详情（任务勾选 / 避坑 / 需购材料 / 验收清单）、首页行动中心（逾期 / 今天 / 未来 7 天）。
- 预算页：按分类管理预算与实际支出、超支提醒、CSV 导出（用 Android `Intent.ACTION_SEND` / `Storage Access Framework`）。
- 指南页：避坑 / 验收清单 / 风格 / 建材日历 / 百科。
- 我的页：房屋信息、联系人、笔记、JSON 备份导入导出、数据重置。
- 主题与视觉风格延续 Web 版的米色 + 圆角 + 单栏移动布局（落到 Compose `MaterialTheme`）。

### 数据持久化

- 用 Room（SQLite）保存任务、阶段进度、预算分类、支出条目、联系人、笔记、设置项。
- 知识数据（流程、避坑、风格、价格、验收、百科）作为只读资源以 JSON 形式打进 APK assets，启动时一次性读入内存缓存。
- Web 版 JSON 备份可导入到 App，作为换机迁移手段；导入时做字段映射（Web `localStorage` 形态 → Room 表）。

## Acceptance Criteria (evolving)

- [ ] App 可在 Android 7.0+ 真机 / 模拟器上安装并启动到首页。
- [ ] 首次安装后呈现与 Web 版语义一致的首次设置向导（3 步）。
- [ ] 完成设置后能进入首页，看到按"逾期 / 今天 / 未来 7 天"排列的行动列表，并可直接勾选完成任务。
- [ ] 流程页 14 阶段可展开，每阶段任务勾选状态、避坑要点、需购材料、验收清单均能展示并勾选完成。
- [ ] 预算页可新增分类、设置预算、记录支出；超额时给出提醒。
- [ ] 预算页可导出 CSV（通过系统分享 / 保存到下载目录）。
- [ ] 指南页 5 个子区全部可访问，内容与 Web 版一致。
- [ ] 我的页可导出 JSON 备份、可从 Web 版 JSON 备份恢复、可清除全部数据。
- [ ] lint / typecheck / 单元测试 / instrumented 测试通过；APK 可成功构建并签名。

## Definition of Done (team quality bar)

- 知识数据（`assets/knowledge.json` `assets/prices.json`）按 `web-json-shapes.md` 骨架整理完毕，覆盖原 Web 版的 12+4 字段、约 60KB。
- Room schema 按 `room-schema.md` 设计落 `v1`，DAO 单测覆盖 4 个关键聚合（首页三段分组 / 阶段完成度 / 分类超支 / CSV 扁平视图）。
- 工程按 `android-scaffold.md` 脚手架初始化：单 module / AGP 9.3.2 / Kotlin 2.4.10 / Compose BOM 2026.08.00 / Room 2.8.4 / minSdk 24 / targetSdk 35 / compileSdk 36。
- Material You 按 `material-you.md` 接入：Android 12+ 走 `dynamicLight/DarkColorScheme`、Android 7-11 走静态色板（陶土橙 + 米色）。
- ViewModel / Compose UI 测试覆盖：首次设置、首页三段分组、阶段勾选、预算录入、CSV 导出、JSON 导入/导出。
- `README.md` 与 `.trellis/spec/android/` 文档已建立（包含模块契约、构建命令、调试命令、签名约定）。
- APK 能在 Android 7.0 / 12 / 14 三档真机或模拟器上跑通主流程。
- 手测清单：首次设置 → 首页打卡 → 流程勾选 → 预算录入 → CSV 导出 → JSON 导入/导出 → 数据重置。

## Out of Scope (explicit)

- iOS / 鸿蒙 / 其它平台。
- 账号系统、云同步、多端协同。
- 后端服务、推送、远程配置、A/B 实验。
- 桌面小部件、Tile Service、Android Auto、桌面快捷方式、磁贴、Widget。
- Wear OS / Tablet 适配（不做横屏大屏专门适配，但保持基本可用）。
- 应用市场上架所需的合规材料（隐私政策、签名合规说明、ICP 等）——仅准备可填模板。
- 国际化（先保证简体中文）。

## Technical Approach

- **新工程位置**：在仓库根新建子目录 `android/`，与现有 `index.html` / `js/` / `css/` / `server.js` 平级；保持 Web 工程不受 Android 工程影响。
- **技术栈**：Kotlin 2.4.10 + Jetpack Compose（BOM 2026.08.00）+ Material 3 + Room 2.8.4 + Navigation Compose 2.10.0 + kotlinx-serialization-json 1.11.0 + kotlinx-datetime 0.8.0。
- **数据层**：Room 单 module（v1）；启动时一次性把 `assets/knowledge.json` 和 `assets/prices.json` 加载到内存缓存（`@Singleton`）。
- **导航**：Navigation Compose，5 Tab（home / stages / budget / guide / more）+ 首次设置向导作为顶层 "OnboardingGraph"。
- **主题**：按 `material-you.md` 接入 Material You；`AppTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = true)`；低版本回退静态色板。
- **平台集成**：CSV 导出用 SAF（`ActivityResultContracts.CreateDocument`）落到下载目录；JSON 导出/导入用 SAF（`OpenDocument` / `CreateDocument`）；不引入 Google Play 依赖。
- **签名**：debug keystore 走 Android SDK 默认；release keystore 走本地 `~/.gradle/keystore/renovation-release.jks` + `gradle.properties` 环境变量注入密码，不进仓库。
- **测试**：JUnit 5 + MockK + Turbine 覆盖 Room DAO 与 ViewModel；Compose UI 测试覆盖关键交互；instrumented 测试按需。

## Subtask Decomposition

无子任务。整个 MVP 在单一 task 下推进；如 implement 阶段发现需要拆分（如"知识数据整理"作为前置 PR），再补 child task。

## Research References

- [`research/android-scaffold.md`](research/android-scaffold.md) — AGP 9.3.2 / Gradle 9.5.0 / Kotlin 2.4.10 / Compose BOM 2026.08.00 / Room 2.8.4 / minSdk 24 / targetSdk 35 / compileSdk 36；单 module 工程结构、签名、构建/调试命令、Material You 接入点。
- [`research/room-schema.md`](research/room-schema.md) — Web `Store.state` 12 字段反推为 12 张 Room 表（金额统一 `INTEGER cents`）；9 组 DAO 方法；4 个关键聚合 SQL 草案（首页三段分组、阶段完成度、分类超支、CSV 扁平视图）。
- [`research/web-json-shapes.md`](research/web-json-shapes.md) — `DATA` 12 字段 / `PRICES` 4 字段结构；Android 端 `assets/knowledge.json` `prices.json` 骨架建议；emoji / 价格自由文本 / id 命名等 10 类风险点。
- [`research/material-you.md`](research/material-you.md) — `dynamicLight/DarkColorScheme` 接入；Android 7-11 静态回退色板（陶土橙 + 米色，与 Web 品牌色阶一致）；不引入 Accompanist / 不做 App 内暗色切换。

## Research Notes

### 关键发现

- **AGP 9.x 已稳定**：9.0.0 (2025-07-31) → 9.3.2 (2026-06)，4+ 月窗口期，工程上可直接采用；如团队其它插件仍锁 AGP 8.x，再降级为 AGP 8.13.x + Gradle 8.13+。
- **KSP 版本是占位符**：研究子代理只能确认 Kotlin 2.4.10，KSP 需要在 implement 阶段去 [KSP releases](https://github.com/google/ksp/releases) 查与 Kotlin 2.4.10 配对的最新稳定号。
- **targetSdk 35 强制 edge-to-edge**：`enableEdgeToEdge()` 不再是可选调用，所有 Screen 都要按 WindowInsets 处理。
- **金额必须用 `INTEGER cents`**：Web 浮点累计误差在长期记账下不可忽略。
- **`house_profile` 用 `PRIMARY KEY CHECK(id = 1)` 强制单行**：多套房屋留 v2+。
- **`task_completion` 与 `task` 三选一**：v1 推荐内置任务勾选走 `task_completion`（只存 id + 勾选时间）、用户自建/派生任务走 `task`（独立条目）——避免把 14 阶段所有任务在 `task` 表里复制一份。
- **派生任务（来自 `spaceNeeds`）保留语义**：已勾选的派生任务应清掉 `space_id` 转普通任务；这必须在单个 `withTransaction` 内完成。
- **知识数据打包后 ~60KB**（`knowledge.json` 30-50KB + `prices.json` 6-8KB），启动时一次性读入内存。
- **`reference.items[].price` 是自由文本**（如 "30–80元/㎡"），不是数字，Android 端要做区间解析或仅做展示。
- **emoji 渲染要求 minSdk ≥ 24**：4 字节 ZWJ emoji（如 🧑‍🔧）彩色渲染需要 Android 7.0+。
- **国产 ROM 对 `dynamicColorScheme` 的支持度未实测**：实现完成后需在 MIUI/HyperOS、ColorOS、OriginOS 真机上验"换壁纸 → App 颜色变化"。

### 关键未决（实现期澄清）

- Room schema §6.2 列了 8 项"待澄清"，主要是 `task_completion` 拆分方案最终选型、`budget_category.is_from_template` 在用户改名后的策略、`quiz.at` 的精度。
- 静态色板 hex（来自 Material Theme Builder 跑 brand seed `cf6b45`）需视觉确认。
- KSP 版本号需在 implement 阶段查最新稳定号。
