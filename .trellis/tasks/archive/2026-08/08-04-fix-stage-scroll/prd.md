# 修复流程查看和滚动位置

## Goal

修复流程页交互：未开始的流程阶段也必须能查看详情；页面内点击不应无条件把页面滚回 0px 顶部，避免用户正在查看列表时被打断。

## What I already know

- 用户反馈：“未开始的流程无法查看，并且页面点击会自动回到 0px 回到顶部”。
- 应用是零构建 vanilla JS 单页应用，路由和滚动逻辑在 `js/app.js`。
- 流程页展开逻辑在 `js/views/stages.js`，通过 `expandedId` 控制阶段展开。
- `App.go(tab, param)` 当前每次调用都会先 `window.scrollTo(0, 0)`，包括同 tab 参数导航。
- `stages.js` 在带 `param` 渲染后会 `scrollIntoView()` 到目标阶段。

## Requirements

- 未开始、进行中、部分完成、已完成的阶段都能点击阶段头展开/收起查看详情。
- 同一 tab 内导航或参数跳转不应先强制回到顶部。
- 切换到底部其它 tab 时可以继续回到顶部，保持现有“新页面从顶部开始”的体验。
- 从首页/指南等入口跳到指定阶段时，应直接打开并滚动到指定阶段，而不是明显先回顶。
- 流程页内部点击展开、设置日期、勾选任务等 rerender 应尽量保持当前滚动位置。

## Acceptance Criteria

- [x] 在流程页点击未开始阶段的阶段头，阶段详情展开。
- [x] 点击流程页某个阶段头，不会跳到页面顶部。
- [x] 从首页“继续打卡”进入流程页时，打开目标阶段并滚到该阶段附近。
- [x] 底部 tab 切换仍从目标 tab 顶部开始。
- [x] 不引入新依赖或构建步骤。

## Definition of Done

- 修改保持现有 IIFE 全局模块模式。
- 通过静态检查或浏览器运行验证没有明显 JS 语法错误。
- 更新 task 记录并提交代码。

## Technical Approach

- 调整 `App.go()`，区分 tab 切换和同 tab/带参数导航。
- 支持导航选项，允许调用方决定是否保留滚动或跳到顶部。
- 保持 `App.rerender()` 的当前滚动恢复逻辑。
- 如有必要，让全局 `nav` action 传入“参数导航不先回顶”的行为。

## Out of Scope

- 不修复现有中文文案编码错位。
- 不重构整个路由系统。
- 不新增自动化测试框架。

## Technical Notes

- 相关源码：`js/app.js`、`js/views/stages.js`。
- 相关规范：`.trellis/spec/frontend/index.md`、`.trellis/spec/frontend/component-guidelines.md`、`.trellis/spec/frontend/state-management.md`。
