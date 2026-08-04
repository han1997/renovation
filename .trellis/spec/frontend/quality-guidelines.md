# Quality Guidelines

## Project Constraints

本项目没有 npm、构建、lint 或自动测试脚本。质量检查以源码审查和浏览器手测为主。

## Required Manual Checks

前端改动后至少验证：

- 直接打开 `index.html` 不报错。
- 如涉及手机访问，运行 `node server.js` 并访问 `http://localhost:8787`。
- 底部 5 个 tab 可切换：home、stages、budget、guide、more。
- 修改的表单能保存、关闭、重新渲染并刷新后保留。
- JSON 导出/导入仍可用，CSV 导出仍包含支出明细。
- 新增按钮或控件有对应 `data-action` / `data-change` 处理。

## Source Review Checklist

- `index.html` 脚本顺序满足依赖。
- 新增用户输入进入 HTML 前使用 `UI.esc()`。
- 修改 `Store.state` 后调用 `Store.save()`。
- 保存后调用 `App.rerender()` 或 `App.go()`。
- 新增持久字段写入 `defaults()` 并兼容旧数据。
- 新增 `DATA` / `PRICES` id 后全文搜索引用一致性。
- CSS 复用现有 tokens 和组件类，避免重复定义相近样式。

## Encoding Rule

当前应用文案文件显示存在编码错位历史。除非任务就是修复编码，不要格式化或重写整份 `README.md`、`index.html`、`js/data/*.js`、`js/views/*.js` 中的大段中文文案。

## Forbidden Patterns

- 不要新增构建工具或依赖包来解决小范围问题。
- 不要使用 `innerHTML` 插入未转义用户输入。
- 不要把业务状态放进 DOM dataset 后当作唯一来源。
- 不要复制已有金额、日期、modal、toast helper。
- 不要在多个视图重复实现同一派生计算。

## Useful Commands

```bash
rg "data-action=\"new-action\"" js index.html
node server.js
```
