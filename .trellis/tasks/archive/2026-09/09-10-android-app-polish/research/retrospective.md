# Bug Analysis：完整流程与跨层状态一致性

## 1. Root Cause Category
- Architecture / Missing contract：数据对象可编辑却被当只读种子；保存没有建立 ID；文件 UI 发出请求就当完成；需求选取与分配引用分开维护。
- Integration：计算引擎存在并不代表表单、清单、备份、导出已经闭环。

## 2. Why Fixes Failed
- 先前只执行编译/纯计算测试，没有走 UI 保存再打开、备份重建与引用删除流程。
- 本轮 UI 测试最初直接轮询 VM 状态，Robolectric 主循环未同步导致伪超时；语义树证明页面已经进入编辑，改为等待 UI 条件后通过。
- 本机中文脚本经 PowerShell 管道丢编码，临时脚本显式 UTF-8 加内存 compile 后修复，并检查生成文件。

## 3. Prevention Mechanisms
| Priority | Mechanism | Action | Status |
|---|---|---|---|
| P0 | 完整快照与事务 | 备份覆盖可变模板/完成/报价/规划，插入失败回滚 | DONE |
| P0 | 稳定身份与前置校验 | 方案 ID 回写、mode 一致、预算预览、引用归一化 | DONE |
| P0 | 流程回归 | 真实 Room/目录的 VM 与 Compose 保存/重开测试 | DONE |
| P1 | 工具规范 | UTF-8 写入、Compose idle 同步、许可证合并 | DONE |

## 4. Systematic Expansion
同步核查日期刷新、总上限与分类计划、自购预算口径、整行复选、长图内存和行高、空状态/初始化错误、未配置空间的报价范围。

## 5. Knowledge Capture
已更新 android/app-polish-contracts.md、data-layer.md、type-money.md、ui-theme.md、quality-guidelines.md 和 Android README。设备矩阵验收仍单独记录，不用 JVM 截图冒充真机验证。

## 实测交互回归：Snackbar 遮挡固定操作区
保存后立即切模式，按钮在语义树中启用但触摸不生效。320×470 JVM 截图证实 Snackbar 位于按钮之上。根因是操作区放在 Scaffold 内容区而非 bottomBar，宿主无法避让。修复采用实测底栏高度偏移提示条，兼容大字体换行；保留快速连续操作测试，不通过延时掩盖。
