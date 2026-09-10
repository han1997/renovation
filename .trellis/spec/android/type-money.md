# Type & Money

## Overview

本项目用 Kotlin，无强类型运行时校验（非 TS）。类型安全靠 Kotlin 编译器 + kotlinx-serialization 反序列化时防御性容错体现。

## 金额约定

- 金额一律 `Long`（分 cents）入库：`money_cents` 等字段。
- 换算工具 `util/MoneyUtil.kt`：

  ```kotlin
  object MoneyUtil {
      fun fromYuan(yuan: Double): Long   // 元 → 分（roundToLong）
      fun parseYuan(text: String): Long? // 精确解析，非法输入返回 null
      fun fromYuan(yuan: String): Long   // 严格入口，非法输入抛异常
      fun input(cents: Long): String    // 无分组、Locale.ROOT 的可回解析文本
      fun toYuan(cents: Long): Double    // 分 → 元
      fun format(cents: Long): String    // "12,345" 整数显示
      fun formatFull(cents: Long): String // "12,345.67" 带小数
  }
  ```

- 浮点累积精度风险：长期记账必须用分，禁止直接用 `Double` 存库、禁止 `Double` 累加求总额后直接展示。

## JSON 反序列化容错

- 知识数据 `Json { ignoreUnknownKeys = true; isLenient = true }`，字段有默认值兜底（见 `KnowledgeCache.kt`）。
- 用户备份必须校验版本、完整结构、金额和引用，不能采用知识数据的宽松兜底；见 [全 App 契约](./app-polish-contracts.md)。
- `@SerialName` 用于与 Web 字段对齐的驼峰 / 下划线映射（如 `schema_version`、`when`）。

## 日期 / id

- 日期统一 `String`（`yyyy-MM-dd` 形态），由 `util/DateUtil.kt` 生成 / 比较；不裸用 `java.util.Date` 落库。
- id 用 `util/IdGen.kt` 生成带前缀短 id（预算分类 `bc`、支出 `ex` 等），保证稳定且可读。
- 持久化 id（任务模板 / 分类 / 风格 / 验收 / 空间预设）必须稳定，不可重命名。

## 不可变数据类

- 实体 / DTO 一律 `data class`，字段用 `val`；需要变更用 `copy()`。
- 聚合行（`CategoryWithSpent`、`ExpenseExportRow`）用 `@ColumnInfo` 显式标注列名，计算属性（如 `pct`、`overCents`）不落库。