# 流程页视觉优化——缓解拥挤感（Android）

## Goal

Android 版「装修流程」页（`StagesScreen.kt`）展开阶段后的详情区信息全部平铺在同一张卡片里，段落间距仅 8dp，条目为 `· xxx` 紧凑纯文本行，视觉拥挤。本任务重新排版该页，缓解拥挤感，同时微调阶段卡头部密度。

## What I already know

- 「流程」页 = 底部 Tab 的 `stages`，对应 `android/app/src/main/java/com/renovation/guardian/ui/stages/StagesScreen.kt` + `StagesViewModel.kt`。
- 当前结构：`Scaffold` → `LazyColumn`（横向 16dp，卡间距 12dp）→ 每阶段一张 `StageCard`（`SectionCard` 容器，内 padding 16dp）；展开后在同一 `items` 块追加 `StageDetailContent`（普通 Column，无卡片容器，直接排目标/避坑/需购/任务/验收）。
- 详情区现状问题：
  - 目标 / 避坑要点 / 需购材料 / 任务清单 / 添加自定义任务 / 验收清单全部连排，段落间仅 `SpacerH()`（8dp）。
  - 避坑要点、需购材料条目是 `· 文本` 纯 Text 行，无行距、无次级色区分。
  - 任务/清单行直接叠 Checkbox + Text，无分隔、行间无间距。
  - 展开详情区连背景卡片都没有，直接裸排在 LazyColumn 里（与上方 StageCard 卡片之间仅 12dp，视觉上和卡片黏连）。
- 阶段卡头部现状：emoji(titleLarge) + 名称/phase 两行 + 百分比 label，进度条 top 8dp，duration top 4dp——整体偏密。
- Web 版（`js/views/stages.js`）详情区是分区组织（目标一句话 + 任务清单 + 可折叠避坑/需购分区 + 验收入口），可作排版参照；但本任务不做折叠（用户已选「分卡 + 加间距」）。
- 主题事实：`SectionCard`（`ui/components/Components.kt`）是全 App 统一卡片容器（16dp 圆角、surface 底、1dp elevation、内 padding 16dp）；品牌色见 `ui/theme/Color.kt`（陶土橙 primary、`Line #ECE7DE` 分隔线色、error/amber 等）。
- spec 约定（`.trellis/spec/android/ui-theme.md`）：同类条目合并一张卡、行间用 `HorizontalDivider(surfaceVariant)`；Screen 只渲染不碰数据层。

## Requirements

1. **详情区分卡**：展开后的详情拆成独立区块卡（复用 `SectionCard`），与上方 `StageCard` 视觉分离：
   - 卡 1「目标」：目标一句话。
   - 卡 2「⚠️ 避坑要点」（有数据时）。
   - 卡 3「🛒 需购材料」（有数据时）。
   - 卡 4「✅ 任务清单」：模板任务 + 自定义任务 + 添加自定义任务输入框。
   - 卡 5+「验收清单」：每组清单一张卡（保持平铺条目，不折叠）。
2. **条目行呼吸感**：
   - 避坑/需购条目行间距 ≥ 8dp；需购材料行 item 加粗、note 用 `onSurfaceVariant` 次级色。
   - 任务/清单行之间加 ≥ 4dp 间距或用 `HorizontalDivider(surfaceVariant)` 分隔（沿用 spec 列表密度约定）。
3. **区块标题统一**：每张区块卡有标题行（emoji + 文案，`titleSmall` SemiBold），与内容间距 ≥ 8dp。
4. **LazyColumn 卡间距**：`verticalArrangement.spacedBy(12.dp)` 保持或提升至可呼吸值；展开状态的整体观感为「阶段卡头 + 一叠区块卡」。
5. **阶段卡头部微调**：emoji 与文字列间距、进度条/duration 与上方间距适度拉开（+2~4dp），百分比文案与进度条对齐关系保持。
6. **行为零变更**：勾选/添加/删除任务、验收勾选、删除确认弹窗、空态加载占位逻辑全部不动；不新增/修改数据层与 ViewModel API。
7. **暗色模式兼容**：只用 MaterialTheme 语义色（`surfaceVariant`/`onSurfaceVariant`/`error` 等），不硬编码浅色值。

## Acceptance Criteria

- [ ] 展开任一阶段后，目标/避坑/需购/任务清单各在独立 `SectionCard` 中，卡片间有清晰间隔，不再是连排一坨。
- [ ] 避坑要点、需购材料条目行有明显行距；需购 note 为次级色。
- [ ] 任务行、验收清单行之间有分隔或间距，长列表可读性提升。
- [ ] 阶段卡头部间距微调，与详情区块卡视觉连贯。
- [ ] 勾选任务/清单、添加自定义任务、删除自定义任务（含确认弹窗）功能回归正常。
- [ ] 暗色模式下无硬编码颜色违和。
- [ ] 编译通过（`assembleDebug` 或 lint），无新增警告级问题。

## Definition of Done

- Lint / 编译通过
- 手动验证展开-收起-勾选-增删任务链路
- 不改 `assets/knowledge.json`

## Out of Scope

- 避坑/需购折叠收纳（用户明确选分卡方案）
- 验收清单摘要跳转/折叠
- Web 版同步美化
- 阶段卡头部信息增减（只调间距密度）
- 数据层 / ViewModel 结构调整

## Technical Notes

- 主要改动文件：`android/app/src/main/java/com/renovation/guardian/ui/stages/StagesScreen.kt`（`StageDetailContent`、`StageCard`、`StagesScreen` LazyColumn 部分）。
- 复用组件：`SectionCard`（`ui/components/Components.kt`）。
- 展开详情的区块卡在 LazyColumn 的 `items` 内追加，注意 `key` 仍以 stage.id 为准（详情与阶段卡同 item，现状即如此，不改结构）。
- 参考：Web 版 `js/views/stages.js` 的 `stageBody` 分区顺序（目标 → 任务 → 避坑 → 需购 → 验收）。
- 编码注意：仓库中文在终端显示乱码，编辑时只改必要位置。
