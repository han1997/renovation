# 优化流程页点击后的展示位置

## Goal

优化流程页的点击手感：用户点击阶段（例如“量房设计”）后，页面应自动移动到更适合阅读展开内容的位置，而不是只保留原滚动或把阶段卡片贴到顶部，减少用户二次手动调整。

## What I already know

- 用户反馈：“总感觉现在的前端用起来手感不好，比如我点击量房设计他不会自动移动到最舒服的展示区域”。
- 应用是零构建 vanilla JS 单页应用，路由和滚动逻辑在 `js/app.js`，流程页渲染在 `js/views/stages.js`。
- “量房设计”来自 `js/data/knowledge.js`，阶段 id 是 `design`。
- `Views.stages.render(el, param)` 目前仅在带 `param` 导航时 `scrollIntoView({ block: 'start', behavior: 'smooth' })`。
- 手动点击阶段头 `toggle-stage` 后只设置 `expandedId` 并调用 `App.rerender()`，`rerender()` 会恢复原 `window.scrollY`，不会主动聚焦展开阶段。
- 刚完成的上一任务已把路由 `param` 改为一次性渲染信号，避免后续 rerender 重放旧锚点。

## Assumptions (temporary)

- “最舒服的展示区域”更接近让展开后的阶段卡片位于视口上半区，并露出阶段标题和部分任务内容，而不是紧贴顶部导航栏。
- 该优化应优先覆盖流程页阶段头点击，后续可复用到首页/指南跳转到流程阶段。

## Open Questions

- 最终确认后进入实现。

## Requirements (evolving)

- 点击流程阶段头展开阶段时，页面应自动滚动到更适合阅读该阶段详情的位置。
- 展开后的阶段标题应落在视口上方约 20%-25% 的位置，让标题和第一批任务内容同时可见。
- 从首页、指南等入口跳转到指定流程阶段时，也应使用同一套舒适定位，不再让阶段标题贴近顶部。
- 收起阶段、勾选任务、设置日期、添加/删除任务等阶段内部操作应保持当前位置，不主动重新聚焦。
- 展开/收起仍保持现有阶段详情逻辑，不改变任务打卡、日期设置、自定义任务等状态行为。
- 不新增依赖或构建步骤。

## Acceptance Criteria (evolving)

- [ ] 在流程页点击“量房设计”阶段头后，该阶段展开，阶段标题移动到视口上方约 20%-25% 处。
- [ ] 点击其它未开始、进行中、部分完成、已完成阶段也有一致的聚焦体验。
- [ ] 从首页/指南跳转到指定阶段时，目标阶段展开并使用同样的舒适定位。
- [ ] 勾选任务、设置日期等阶段内部操作不发生突兀跳动。
- [ ] 底部 tab 切换和已有参数跳转行为不回退。
- [ ] 不引入新依赖或构建步骤。

## Definition of Done

- 修改保持现有 IIFE 全局模块模式。
- 通过可用静态检查，例如 `node --check`。
- 如产生新的前端交互约束，更新 `.trellis/spec/frontend/`。

## Out of Scope

- 不重做整体视觉设计或信息架构。
- 不引入动画库、滚动库或前端框架。
- 不修复现有中文文案编码错位。

## Technical Notes

- 相关源码：`js/views/stages.js`、`js/app.js`、`js/data/knowledge.js`。
- 相关规范：`.trellis/spec/frontend/index.md`、`.trellis/spec/frontend/component-guidelines.md`、`.trellis/spec/frontend/state-management.md`、`.trellis/spec/frontend/quality-guidelines.md`。

## Decision Log

- 2026-08-04: 选择“展开后阶段标题停在视口上方约 20%-25% 处”作为舒适展示区域，而不是贴顶或完全居中。
- 2026-08-04: 手动点击阶段头和首页/指南等外部入口跳转都纳入 MVP；收起和阶段内部操作不主动滚动。
