# Frontend Development Guidelines

> 本项目是零构建、零依赖的 vanilla JS / HTML / CSS 单页应用。前端规范必须匹配当前源码事实。

## Overview

应用入口是 `index.html`。所有脚本通过 `<script>` 顺序加载，每个 JS 文件使用 IIFE 挂到 `window.*`：

- `window.DATA`：装修流程、指南、风格、百科、空间需求等静态知识。
- `window.PRICES`：价格档位、预算模板、参考价格。
- `window.Store`：`localStorage` 状态、导入导出、派生任务同步。
- `window.UI`：HTML 转义、金额/日期格式化、modal、toast、表单等 UI helper。
- `window.Views.*`：各 tab 的字符串渲染和事件处理。
- `window.App`：路由、进度计算、首次设置向导、全局事件委托。

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | 文件边界、加载顺序、全局模块契约 | Filled |
| [Component Guidelines](./component-guidelines.md) | 字符串渲染组件、事件委托、样式复用 | Filled |
| [Hook Guidelines](./hook-guidelines.md) | 明确本项目没有 React/hooks，记录可复用逻辑位置 | Filled |
| [State Management](./state-management.md) | `Store.state`、localStorage、派生任务同步 | Filled |
| [Quality Guidelines](./quality-guidelines.md) | 无构建项目的人工/命令验证清单 | Filled |
| [Type Safety](./type-safety.md) | JavaScript 运行时形状约束和防御性校验 | Filled |

## Encoding Caution

当前仓库中的中文文案在终端输出里呈现为乱码形态。编辑时只改必要位置，避免对大段中文文案、README、知识库数据做无关全量重写。新增规范文档使用 UTF-8 中文。

## Before Coding

前端改动前至少阅读：

- 本目录 `index.md`。
- 与改动有关的专题 spec。
- 相关源码：`index.html`、对应 `js/views/*.js`、`js/storage.js` 或 `js/data/*.js`。
