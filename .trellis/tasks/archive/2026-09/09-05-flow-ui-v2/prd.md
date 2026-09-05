# 流程页任务清单可编辑

## Goal

Android 流程页任务清单目前只能勾选/删除/新增，无法编辑。Web 版支持：设置计划日期（📅）、查看小贴士（💡）、删除自定义任务。本任务补齐编辑能力。

## Requirements

1. 点击任务行（模板任务与自定义任务均适用）弹出编辑对话框：
   - 模板任务：显示任务文案（只读，种子数据）+ 小贴士（如有）+ 计划日期选择（DatePickerField）
   - 自定义任务：文案可编辑 + 计划日期选择
2. 任务行显示日期 chip（已设置日期时），格式用 `DateUtil.formatCN`
3. ViewModel/Repository 补齐：模板任务设日期（repo 已有 `setTemplateDueDate`，接 UI）、自定义任务改文案/设日期（repo 新增）
4. 行为兼容：勾选、删除、新增逻辑不变

## Acceptance Criteria

- [ ] 点击任意任务行弹出编辑对话框
- [ ] 模板任务可设/清计划日期；自定义任务可改文案、设/清日期
- [ ] 设日期后任务行显示日期 chip
- [ ] 编译 + lint 通过

## Out of Scope

- 模板任务文案编辑（种子数据只读）
- 日期逾期标红（后续可加）

## Technical Notes

- `TaskDao.setTemplateDueDate` / `TaskRepository.setTemplateDueDate` 已存在，仅 VM + UI 未接
- 自定义任务日期存 `task.due_date`（TaskEntity 已有字段），改文案/日期用 `getById + upsert(copy)` 模式
