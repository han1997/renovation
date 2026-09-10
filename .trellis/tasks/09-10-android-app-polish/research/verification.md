# 验证记录

## 已完成
- 正常构建：`:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` 全部成功。
- JVM/Robolectric/Compose：75 项通过，0 失败，0 跳过；含整装/半包/局改 UI 流程、保存重开、规划、备份回滚、日期刷新、精确金额及溢出、状态恢复。
- Android Lint：0 errors，61 warnings，4 hints；未通过关闭检查掩盖错误。
- API 36 真机（PKJ110）：5 项仪器测试通过。覆盖首次设置、主子路由导航、真实 ContentResolver 写入完成时机、FileProvider 读取、MediaStore 相册保存后读取；临时相册图片在 finally 删除。
- 使用独立包名 `com.renovation.guardian.validation20260910` 及独立构建目录；测试目标包由合并 Manifest 校验。临时应用及测试包均已卸载，原 `com.renovation.guardian.debug` 未覆盖、未清空。
- `git diff --check` 通过；新增/修改文本 UTF-8、JSON/JSONL 和 Markdown 链接检查通过。
- Room v3 schema 与离线 assets 均未改变；data/domain 不反向导入 ui。

## 产物
- 正常安装包：`android/app/build/outputs/apk/debug/app-debug.apk`（20591125 bytes，applicationId=com.renovation.guardian.debug）。
- SHA-256：`9eef3e92aa334cbaedfe08ee25d285ba39b09d02a9035a043774e16400c55947`。
- 仪器测试包：`android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`。
- JVM 截图：`android/app/build/reports/ui-polish/quote.png`、`planner.png`、`partial-quote.png`、`semi-before-next.png`；已检查并据此修复 Snackbar 遮挡固定按钮。
- 真机日志：[device-api36.txt](./device-api36.txt)。隔离构建脚本：[device-validation.init.gradle](./device-validation.init.gradle)。

## 仍待完成（不标记任务完成、不归档）
- API 24/31/34/35 真实设备兼容矩阵（API 34 的 JVM 测试不冒充真机）。
- 系统文件选择器交互、第三方分享接收、大字体/旋转、系统深色及换壁纸动态配色的完整人工验收。
- 用户确认工作提交；本次未 commit / push，未执行 archive / journal 自动提交。

## 后续操作
先审阅正常 APK 与改动。设备验收使用隔离包或先备份测试应用数据，禁止直接清空用户现用 debug。完成剩余验收后进入提交确认与 Trellis 收尾。
