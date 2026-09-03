# Research: Web JSON 形态分析

- **Query**: Web JSON 形态分析（为后续 Android 端独立维护知识数据做对照）
- **Scope**: internal
- **Source files**:
  - `js/data/knowledge.js`（73,798 字节，1,126 行）
  - `js/data/prices.js`（12,525 字节，214 行）
- **Date**: 2026-08-29

## 背景

虽然首版不做 Web → Android 导入（D5），但 Android 端要独立维护 `app/src/main/assets/knowledge.json` 和 `prices.json`。本报告对照 Web 版原始数据结构，给出 Android 端的两份 JSON 骨架。

两份 JS 文件均使用 IIFE 把数据挂到 `window`：
- `window.DATA` 由 `js/data/knowledge.js` 暴露
- `window.PRICES` 由 `js/data/prices.js` 暴露

Android 端只保留**纯数据**部分（去掉 IIFE、辅助函数、`RESERVE_RATIO` 这种常量、以及 `reference` 之外的导出方法）。

---

## 一、`DATA` 顶层结构（来自 `js/data/knowledge.js`）

`return` 语句在 1111–1125 行，共导出 **12 个字段**（11 个数据 + 1 个常量字符串）。各字段的元素数量已通过脚本验证：

| 字段名 | 类型 | 元素数 | 关键字段 / 子结构 | 用途 |
|---|---|---|---|---|
| `totalDurationNote` | string | 1 | — | "全程约 3–6 个月（含通风 1–3 个月）" 全流程总时长说明 |
| `stages` | array of object | **14** | `id`, `phase`, `emoji`, `name`, `duration`, `goal`, `tasks[]`, `warnings[]`, `buy[]`, `acceptIds[]` | 14 阶段全流程 |
| `checklists` | array of object | **7** | `id`, `emoji`, `name`, `note?`, `items:[{id, text}]` | 7 个验收清单（水/防水/瓦/木/墙/门窗/整体） |
| `tipTopics` | array of string | **10** | `['预算','合同','水电','防水','板材','瓷砖','门窗','定制柜','施工','收尾']` | 避坑分类标签 |
| `tips` | array of object | **19** | `title`, `level`（'高危'\|'重要'\|'提示'）, `topic`, `body` | 避坑指南（body 用 `\n` 换行，max 长度 145 字） |
| `acceptIntro` | string | 1 | — | 验收引言说明 |
| `styles` | array of object | **7** | `id`, `emoji`, `name`, `tagline`, `colors[hex]`, `cost`, `costNote`, `desc`, `fit`, `elements[]`, `pitfalls[]` | 7 种风格卡片 |
| `styleQuiz` | object | 1 | `questions:[{q, options:[{text, scores:{styleId: int}}]}]` | 6 道风格测试题（每题 6 选项） |
| `materialTimeline` | array of object | **6** | `emoji`, `period`, `when`, `note?`, `items:[{name, note?, lead}]` | 6 个阶段建材购买日历 |
| `modes` | array of object | **4** | `id`, `emoji`, `name`, `priceShort`, `short`, `desc`, `pros[]`, `cons[]`, `fit` | 4 种装修方式（清/半/全/整） |
| `whoBuilds` | array of object | **4** | `emoji`, `name`, `pros[]`, `cons[]`, `fit` | 4 种承包方类型 |
| `glossary` | array of object | **30** | `term`, `def` | 装修黑话词典（30 条） |
| `spaceNeeds` | array of object | **20** | `id`, `emoji`, `name`, `desc`, `stageIds[]`, `tasks:[{stageId, text}]`, `budgetCat`, `budgetNote` | 20 个常见空间需求预设 |

### 1.1 `stages[]` 元素示例

14 个阶段 id（按顺序）：`inspect`, `design`, `demolish`, `water-electric`, `waterproof`, `tiling`, `woodwork`, `wall`, `install`, `cleaning`, `furniture`, `soft`, `ventilate`, `movein`

```js
{
  id: 'water-electric',          // 主键，跨 stage 被 spaceNeeds 引用
  phase: '硬装施工',              // 大阶段分组（准备阶段/硬装施工/安装收尾）
  emoji: '⚡',                    // 单 emoji 字符串
  name: '水电改造',
  duration: '约 5–10 天',         // 自由文本（"约 X 天" / "约 X–Y 周"）
  goal: '把水路电路按图纸走好…',  // 自由文本
  tasks: [
    { id: 'we-1', text: '水电交底：…', tip: '交底当天拿家具尺寸图对照…' }  // tip 可选
  ],
  warnings: ['承重墙、配重墙…', ...],   // string[]
  buy: [{ item: '空鼓锤', note: '五金店 10 元…' }],  // note 可选
  acceptIds: ['cl-water']        // 引用 checklists[].id，可空数组
}
```

### 1.2 `tips[]` 元素示例

```js
{
  title: '低开高走，增项防不胜防',
  level: '高危',                  // 枚举：'高危' | '重要' | '提示'
  topic: '预算',                  // 引用 tipTopics 之一
  body: '装修公司最常见的套路：…\n对策：…'  // 含 \n，最长 145 字
}
```

### 1.3 `styles[]` 元素示例

```js
{
  id: 'modern',                   // 跨 styleQuiz.scores 引用
  emoji: '🤍', name: '现代简约', tagline: '少即是多，干净耐看不过时',
  colors: ['#f5f5f5', '#3a3a3a', '#8a8a8a', '#d4a574'],  // hex 字符串数组
  cost: '经济–中档', costNote: '硬装简单省预算…',
  desc: '以"少即是多"为理念…',   // 最长 78 字（不严格）
  fit: '小户型、首次装修…',
  elements: ['大留白墙面', ...],
  pitfalls: ['留白过头容易显冷清…', ...]
}
```

### 1.4 `styleQuiz` 示例

```js
{
  questions: [
    {
      q: '走进你理想的家，第一眼看到的是？',
      options: [
        { text: '大面白墙+几件设计感家具，干净利落',
          scores: { modern: 3, nordic: 1 } },  // scores key 对应 styles[].id
        ...
      ]
    },
    ...
  ]
}
```

6 道题，每题 6 选项；`scores` key 集合 ⊂ `styles[].id`。

### 1.5 `spaceNeeds[]` 跨表引用约定

- `stageIds[]` 引用 `stages[].id`
- `tasks[].stageId` 引用 `stages[].id`
- `budgetCat` 引用 `PRICES.rates[].id`（如 `'b-custom'`, `'b-construct'`, `'b-furniture'`, `'b-soft'`, `'b-main'`, `'b-appliance'`）

```js
{
  id: 'sp-walkcloset', emoji: '🚪', name: '衣帽间',
  desc: '独立衣帽间，含定制柜 + 梳妆区',
  stageIds: ['design', 'install'],
  tasks: [
    { stageId: 'design', text: '衣帽间定制柜量尺与内部格局设计' },
    { stageId: 'install', text: '衣帽间定制柜成品安装' }
  ],
  budgetCat: 'b-custom',
  budgetNote: '衣帽间定制柜约 8000–25000 元（视面积与五金档次）'
}
```

---

## 二、`PRICES` 顶层结构（来自 `js/data/prices.js`）

`return` 语句在 202–213 行，**4 个数据数组 + 6 个函数**。Android 端**只保留数据**，函数全部剔除（在端上由 Kotlin/Java 重新实现预算计算逻辑）。

| 字段名 | 类型 | 元素数 | 关键字段 | 用途 |
|---|---|---|---|---|
| `tiers` | array of object | **4** | `id`, `name`, `factor` | 城市分级（`t1`=1.25, `nt1`=1.1, `t2`=1.0, `t3`=0.88） |
| `grades` | array of object | **3** | `id`, `name`, `emoji`, `desc` | 装修档次（`eco`/`mid`/`high`） |
| `rates` | array of object | **8** | `id`, `name`, `emoji`, `eco`, `mid`, `high` | 每㎡ 基准费率（元，二线口径） |
| `reference` | array of object | **9 组 / 59 项** | `group`, `emoji`, `items:[{name, price, note?}]` | 行情参考价 |
| ~~`tierFactor`~~ | function | — | — | 工具函数，Android 不导出 |
| ~~`tierName`~~ | function | — | — | 同上 |
| ~~`gradeName`~~ | function | — | — | 同上 |
| ~~`modeName`~~ | function | — | — | 同上（依赖 `DATA.modes`） |
| ~~`budgetTemplate`~~ | function | — | — | 预算模板生成（依赖 profile） |
| ~~`estimate`~~ | function | — | — | 顶层预算估算 |

> 常量 `RESERVE_RATIO = 0.08`（备用金比例）也是 IIFE 内的局部变量。Android 端要么自己定义、要么 inline 到估算逻辑里。

### 2.1 `tiers[]` 示例

```js
{ id: 't1', name: '一线城市（北上广深）', factor: 1.25 }
```

### 2.2 `grades[]` 示例

```js
{ id: 'mid', name: '舒适型', emoji: '🏡', desc: '大多数自住家庭的选择：中档品牌主材…' }
```

### 2.3 `rates[]` 示例

```js
{ id: 'b-construct', name: '基础施工（人工+辅材）', emoji: '🧰', eco: 480, mid: 700, high: 1000 }
```

注意：`id` 使用 `b-` 前缀，且与 `DATA.spaceNeeds[].budgetCat` **完全对应**。8 条 `rates` id：`b-construct`, `b-main`, `b-custom`, `b-window`, `b-appliance`, `b-furniture`, `b-soft`, `b-misc`。

### 2.4 `reference[]` 示例

```js
{
  group: '施工与人工', emoji: '🧰',
  items: [
    { name: '拆墙拆除', price: '30–80元/㎡', note: '整屋拆旧常打包 2000–6000元/户，垃圾清运另计' },
    ...
  ]
}
```

9 个 group：施工与人工 / 水电改造 / 防水与美缝 / 瓷砖与地板 / 定制柜 / 门窗 / 卫浴 / 家电 / 收尾与杂项，共 59 个 item。`price` 是**自由文本**（"X–Y元/㎡"、"约 X 元/位" 等），不是数字；`note` 可空字符串。

---

## 三、Android 端 `knowledge.json` 建议骨架

按 Web 字段名 1:1 镜像（仅去掉 IIFE 包装），可最小化迁移成本：

```json
{
  "version": 1,
  "totalDurationNote": "全程约 3–6 个月（含通风 1–3 个月）",
  "stages": [
    {
      "id": "water-electric",
      "phase": "硬装施工",
      "emoji": "⚡",
      "name": "水电改造",
      "duration": "约 5–10 天",
      "goal": "把水路电路按图纸走好…",
      "tasks": [
        { "id": "we-1", "text": "水电交底：…", "tip": "交底当天拿家具尺寸图对照…" }
      ],
      "warnings": ["承重墙、配重墙…"],
      "buy": [{ "item": "空鼓锤", "note": "五金店 10 元…" }],
      "acceptIds": ["cl-water"]
    }
  ],
  "checklists": [
    {
      "id": "cl-water",
      "emoji": "⚡",
      "name": "水电验收",
      "note": "水电是隐蔽工程…",
      "items": [
        { "id": "w-1", "text": "水管打压测试…" }
      ]
    }
  ],
  "tipTopics": ["预算", "合同", "水电", "防水", "板材", "瓷砖", "门窗", "定制柜", "施工", "收尾"],
  "tips": [
    {
      "title": "低开高走，增项防不胜防",
      "level": "高危",
      "topic": "预算",
      "body": "装修公司最常见的套路：…\n对策：…"
    }
  ],
  "acceptIntro": "验收是装修的…",
  "styles": [
    {
      "id": "modern",
      "emoji": "🤍",
      "name": "现代简约",
      "tagline": "少即是多，干净耐看不过时",
      "colors": ["#f5f5f5", "#3a3a3a", "#8a8a8a", "#d4a574"],
      "cost": "经济–中档",
      "costNote": "硬装简单省预算…",
      "desc": "以\"少即是多\"为理念…",
      "fit": "小户型、首次装修…",
      "elements": ["大留白墙面", "无主灯或极简灯具"],
      "pitfalls": ["留白过头容易显冷清…"]
    }
  ],
  "styleQuiz": {
    "questions": [
      {
        "q": "走进你理想的家，第一眼看到的是？",
        "options": [
          { "text": "大面白墙+…", "scores": { "modern": 3, "nordic": 1 } }
        ]
      }
    ]
  },
  "materialTimeline": [
    {
      "emoji": "📐",
      "period": "设计阶段",
      "when": "开工前 3–6 周",
      "note": "这阶段先把\"看不见但定生死\"的东西定下来…",
      "items": [
        { "name": "中央空调/新风系统", "note": "吊顶前要先装内机走管", "lead": "生产 2–4 周…" }
      ]
    }
  ],
  "modes": [
    {
      "id": "half",
      "emoji": "🤝",
      "name": "半包",
      "priceShort": "约 500–800 元/㎡",
      "short": "工长包人工辅材，自己买主材。最主流",
      "desc": "施工方负责施工和辅材…",
      "pros": ["主材自己挑，品质看得见"],
      "cons": ["主材要自己跑市场比价下单"],
      "fit": "大多数自住家庭…"
    }
  ],
  "whoBuilds": [
    {
      "emoji": "🏢",
      "name": "品牌装修公司",
      "pros": ["有正规资质和合同保障"],
      "cons": ["溢价高，管理费占 10–15%"],
      "fit": "预算充足、想省心、做全包整装"
    }
  ],
  "glossary": [
    { "term": "清包", "def": "业主自己买全部材料，施工队只出人工…" }
  ],
  "spaceNeeds": [
    {
      "id": "sp-walkcloset",
      "emoji": "🚪",
      "name": "衣帽间",
      "desc": "独立衣帽间，含定制柜 + 梳妆区",
      "stageIds": ["design", "install"],
      "tasks": [
        { "stageId": "design", "text": "衣帽间定制柜量尺与内部格局设计" },
        { "stageId": "install", "text": "衣帽间定制柜成品安装" }
      ],
      "budgetCat": "b-custom",
      "budgetNote": "衣帽间定制柜约 8000–25000 元（视面积与五金档次）"
    }
  ]
}
```

### Android 端 `knowledge.json` 数据量（镜像 Web）

| 字段 | 元素数 | JSON 估算 |
|---|---|---|
| `stages` | 14 | 中（每条 ~10 字段，tasks 5–8 条） |
| `checklists` | 7 | 中（每条 items 6–10 条） |
| `tips` | 19 | 短（body 最长 145 字） |
| `styles` | 7 | 中（colors 4、elements 5、pitfalls 3） |
| `styleQuiz.questions` | 6 | 短（每题 6 options） |
| `materialTimeline` | 6 | 中（每条 items 3–5） |
| `modes` | 4 | 中（pros/cons 3–4 条） |
| `whoBuilds` | 4 | 短 |
| `glossary` | 30 | 短 |
| `spaceNeeds` | 20 | 中（tasks 1–3 条） |
| **总计** | — | 估计 **30–50 KB** JSON（与 Web 版 ~72 KB JS 接近） |

---

## 四、Android 端 `prices.json` 建议骨架

只镜像数据，函数全部剔除：

```json
{
  "version": 1,
  "reserveRatio": 0.08,
  "tiers": [
    { "id": "t1",  "name": "一线城市（北上广深）", "factor": 1.25 },
    { "id": "nt1", "name": "新一线 / 强二线",       "factor": 1.1  },
    { "id": "t2",  "name": "二线城市",             "factor": 1.0  },
    { "id": "t3",  "name": "三四线城市 / 县城",   "factor": 0.88 }
  ],
  "grades": [
    { "id": "eco",  "name": "经济型", "emoji": "🌱", "desc": "实用为主：不追品牌不做造型…" },
    { "id": "mid",  "name": "舒适型", "emoji": "🏡", "desc": "大多数自住家庭的选择…" },
    { "id": "high", "name": "品质型", "emoji": "✨", "desc": "改善型标准：一线品牌主材…" }
  ],
  "rates": [
    { "id": "b-construct", "name": "基础施工（人工+辅材）", "emoji": "🧰", "eco": 480,  "mid": 700,  "high": 1000 },
    { "id": "b-main",      "name": "主材建材",             "emoji": "🧱", "eco": 320,  "mid": 480,  "high": 800  },
    { "id": "b-custom",    "name": "定制柜（橱柜+衣柜）", "emoji": "🗄️", "eco": 190,  "mid": 380,  "high": 650  },
    { "id": "b-window",    "name": "门窗封阳台",          "emoji": "🪟", "eco": 55,   "mid": 90,   "high": 140  },
    { "id": "b-appliance", "name": "家电",                 "emoji": "📺", "eco": 240,  "mid": 450,  "high": 750  },
    { "id": "b-furniture", "name": "家具",                 "emoji": "🛏️", "eco": 170,  "mid": 280,  "high": 500  },
    { "id": "b-soft",      "name": "软装布艺",             "emoji": "🪴", "eco": 45,   "mid": 90,   "high": 160  },
    { "id": "b-misc",      "name": "杂项服务",             "emoji": "🧾", "eco": 45,   "mid": 110,  "high": 200  }
  ],
  "reference": [
    {
      "group": "施工与人工",
      "emoji": "🧰",
      "items": [
        { "name": "拆墙拆除",   "price": "30–80元/㎡",  "note": "整屋拆旧常打包 2000–6000元/户，垃圾清运另计" },
        { "name": "新建墙体",   "price": "100–200元/㎡","note": "轻体砖含辅料" }
      ]
    },
    {
      "group": "水电改造",
      "emoji": "⚡",
      "items": [
        { "name": "全屋水电（按平米）", "price": "60–120元/㎡", "note": "一线城市 100–200；100㎡全改约 1–2万" }
      ]
    }
  ]
}
```

### Android 端 `prices.json` 数据量

| 字段 | 元素数 | JSON 估算 |
|---|---|---|
| `tiers` | 4 | 极短 |
| `grades` | 3 | 极短 |
| `rates` | 8 | 极短 |
| `reference` | 9 组 / 59 项 | 中（每项 30–60 字） |
| **总计** | — | 估计 **6–8 KB** JSON（Web 版 ~12 KB JS） |

---

## 五、跨文件引用关系（Android 端需要保证）

```mermaid
flowchart LR
  subgraph K["knowledge.json"]
    K1[stages]
    K2[checklists]
    K3[tips]
    K4[tipTopics]
    K5[styles]
    K6[styleQuiz]
    K7[materialTimeline]
    K8[modes]
    K9[whoBuilds]
    K10[glossary]
    K11[spaceNeeds]
    K12[acceptIntro]
    K13[totalDurationNote]
  end
  subgraph P["prices.json"]
    P1[tiers]
    P2[grades]
    P3[rates]
    P4[reference]
  end
  K11 -- budgetCat --> P3
  K1 -- acceptIds --> K2
  K3 -- topic --> K4
  K6 -- scores.id --> K5
  K11 -- stageIds / tasks.stageId --> K1
  P3 -.id 与 K11.budgetCat 一一对应.-> P3
```

需要 Android 端校验的不变量（与 Web 一致）：
- `stages[].acceptIds ⊂ checklists[].id`
- `tips[].topic ∈ tipTopics`
- `styleQuiz.questions[].options[].scores.keys ⊂ styles[].id`
- `spaceNeeds[].stageIds ⊂ stages[].id`
- `spaceNeeds[].tasks[].stageId ∈ spaceNeeds[].stageIds`（实测：Web 中并未严格保持此约束，例如 `sp-tatami` 引用了 `woodwork`，需在端上做软校验）
- `spaceNeeds[].budgetCat ∈ prices.rates[].id`

---

## 六、风险点 / Caveats

### 6.1 中文字段与文案

- **所有业务文本都是中文**（含 emoji）。Android 端需保证：
  - JSON 文件以 **UTF-8 无 BOM** 保存
  - 解析端用 `Charset.forName("UTF-8")`（不要用 `UTF-16` 之类的默认）
  - `gradle` 不做 GBK 转码；`assets/` 路径下文件不会被 Android 二次转码，但 IDE 显示要正常
- 文本长度：最长单字段是 `tips[].body`（145 字），其次是 `styles[].desc`（~78 字）。这些都在 Android `TextView` 单行 / 多行渲染范围内，**无需特别处理**。
- 多行文本：所有 `tips[].body` 和 `modes[].desc` 都含有字面 `\n`。Android `TextView` 设 `android:maxLines` 和 `setText(s)` 后 `\n` 正常换行；Kotlin 字符串里 `"\n"` 会被自动解析。

### 6.2 emoji 嵌入

- `emoji` 字段在 knowledge.js 出现 **62 次**，在 prices.js 出现 **34 次**。单字符 emoji（🔑、📐、⚡）和 ZWJ 组合 emoji（🧑‍🔧 出现在 `modes[clear]`，长度 >1 字符但 UTF-8 4 字节）都存在。
- Android 端：
  - `TextView` 默认字体（Roboto）支持主流 emoji，但 4 字节 ZWJ emoji（如 🧑‍🔧）需要 Android 8.0+ 才有彩色渲染。minSdk 建议 ≥ 24。
  - `emoji` 字段类型是 **string**，不是 char。Android 数据类务必用 `String` 而非 `Char`。
- emoji 编码：JSON 中 emoji 直接以 UTF-8 4 字节字符写出即可，**不要**写成 `\uD83C\uDFA8` 之类的代理对（`org.json` / Moshi / Gson 都支持原生 UTF-8 emoji，但代理对转义在某些库的输出中可能丢精度）。

### 6.3 潜在 null / 空 / 缺省字段

Web 数据中以下字段是**可能为空的**，Android 端读取时**必须可空**：

| 字段 | 出现位置 | 默认行为 |
|---|---|---|
| `tasks[].tip` | 几乎每条 stage 都有可空的 `tip` | 当 null/缺失时，前端不展开 tip 气泡 |
| `checklists[].note` | 部分 checklist 有 `note`（如 `cl-water`），部分没有 | null 时不显示引言 |
| `materialTimeline[].note` | 6 个阶段不一定都有 | null 时不显示该段 note |
| `stages[].acceptIds` | 始终存在，但元素数从 0 到 1 | 空数组表示无验收 |
| `reference[].items[].note` | 行情价部分项无 note（空字符串） | 空字符串占位即可 |
| `tips[].body` 内的 `\n` | 全部 19 条都用 `\n` 分段 | Android 直接用 `split("\n")` |

特别要留意：**Web 中部分字段用空数组 `[]` 占位（如 `acceptIds: []`），不是 null**。Android 反序列化务必用 `List<String>` 而非 `String?` + 单值 fallback。

### 6.4 字符串中的特殊字符

- `tips[].body` 和 `styles[].desc` 中含**中文双引号 `"…"`** 和**单引号 `'…'`**，但 JS 源文件用单引号包裹字符串，所以文案里的中文双引号无需转义。Android 读取 JSON 时这些字符都是合法的 UTF-8 多字节字符，**不需要反斜杠转义**。
- `styles[].desc` 含有内嵌的引号：
  - "**少即是多**" 用了中文引号（`"…"`），不是 ASCII `"`。
  - 这些在 JSON 里**无需转义**。
- 部分文案含 **JSON 元字符**（理论上）：
  - 全量扫了一遍：`tips[]` / `modes[]` / `whoBuilds[]` / `glossary[]` 都没有需要 `\"` 转义的内容（因为 JS 源是单引号包裹）。
  - 但 `styleQuiz.questions[].q` 字段用 `q: '对"造型装饰"你的态度是？'` —— 这里是**中文双引号**不是 ASCII `"`，**JSON 也不需要转义**。如果 Web 维护者改用了 ASCII `"`，Android 端需要保证 JSON 文件本身正确转义为 `\"`。

### 6.5 数字 vs 字符串

- `rates[].{eco, mid, high}`、`tiers[].factor` 是**数字**。
- `reference[].items[].price` 是**字符串**（"30–80元/㎡"、"约 120 元/位"）—— 端上若要做数字排序 / 区间解析，需要单独处理。
- `spaceNeeds[].budgetNote` 文案里含"约 8000–25000 元"也是字符串。

### 6.6 id 命名约定

- 阶段 id 用 `-` 连字符（`water-electric`），不是下划线。
- checklist id 用 `cl-` 前缀（`cl-water`）。
- 空间需求 id 用 `sp-` 前缀（`sp-walkcloset`，**没有连字符**，是单词拼接）。
- 预算 id 用 `b-` 前缀（`b-custom`）。
- 风格 id 用纯英文（`modern`、`newchinese`）。
- 装修方式 id 用 `clear | half | full | whole`（与 `prices.js` 的 `mode` 枚举一致）。

Android 端用 `data class` + Moshi/Gson 时，`id` 字段命名建议保持原样（snake_case 字符串值 + Kotlin 用 `@SerializedName` 或 `@Json(name = ...)`），避免转换出错。

### 6.7 文件大小与首启性能

- `knowledge.json` ~30–50 KB，`prices.json` ~6–8 KB，总和 < 60 KB。
- Android 端 `assets/knowledge.json` + `assets/prices.json` 在首启一次性读入内存（Gson/Moshi 解析 60 KB JSON 在中端机 < 50ms），可放心全量加载。
- 不需要分页 / 流式解析。

### 6.8 与 Web 的字段对齐

- Web 的 `PRICES` IIFE 内还导出了 6 个函数（`tierFactor`、`tierName`、`gradeName`、`modeName`、`budgetTemplate`、`estimate`）和常量 `RESERVE_RATIO = 0.08`。
- Android 端：
  - `tierFactor` / `tierName` / `gradeName` / `modeName` → 用 Kotlin 扩展函数或 Repository 工具方法替代。
  - `budgetTemplate(profile)` 和 `estimate(profile)` → **算法逻辑**需要从 Web 完整移植到 Kotlin（基于 `profile.area / grade / tier / mode` 计算分类预算和总计）。这是 Android 端实现层的工作，**不属于数据 JSON 的范围**。
  - `RESERVE_RATIO = 0.08` → 建议作为 `prices.json` 的顶层字段 `reserveRatio` 持久化（见上面骨架示例），方便后续调整。

### 6.9 数据冗余与潜在不一致

- `modes[]` 与 `glossary[]` 中"清包 / 半包 / 全包 / 整装"的释义有部分重叠，但**不一致**（glossary 是单句定义，modes 是完整描述）。Android 端**不要尝试去重**，两套并存。
- `whoBuilds[]` 中没有 id 字段，只有 `name` —— 端上如果需要引用做映射，建议在 Android 端**自己**补充 id（不写回 JSON），或者在数据迁移时给每条加 `id` 字段。

### 6.10 版本字段

- Web 端没有 `version` 字段。
- Android 端建议在两份 JSON 顶层加 `"version": 1`，方便后续数据格式升级时做兼容判断（schema 演进是 Android 长期独立维护的常见需求）。

---

## 七、Web → Android 字段对照速查

| Web 字段 | Android JSON 字段 | 类型 | 备注 |
|---|---|---|---|
| `DATA.totalDurationNote` | `totalDurationNote` | string | 顶层字符串 |
| `DATA.stages[]` | `stages` | array | 14 条 |
| `DATA.checklists[]` | `checklists` | array | 7 条 |
| `DATA.tipTopics` | `tipTopics` | array of string | 10 条 |
| `DATA.tips[]` | `tips` | array | 19 条 |
| `DATA.acceptIntro` | `acceptIntro` | string | 顶层字符串 |
| `DATA.styles[]` | `styles` | array | 7 条 |
| `DATA.styleQuiz` | `styleQuiz` | object | 6 questions |
| `DATA.materialTimeline[]` | `materialTimeline` | array | 6 条 |
| `DATA.modes[]` | `modes` | array | 4 条 |
| `DATA.whoBuilds[]` | `whoBuilds` | array | 4 条 |
| `DATA.glossary[]` | `glossary` | array | 30 条 |
| `DATA.spaceNeeds[]` | `spaceNeeds` | array | 20 条 |
| `PRICES.tiers` | `tiers` | array | 4 条 |
| `PRICES.grades` | `grades` | array | 3 条 |
| `PRICES.rates` | `rates` | array | 8 条 |
| `PRICES.reference` | `reference` | array | 9 组 / 59 项 |
| `PRICES.RESERVE_RATIO` | `reserveRatio` | number | 0.08，建议 JSON 顶层 |
| ~~`PRICES.tierFactor`~~ 等函数 | **不导出 JSON** | — | 由 Android 端用 Kotlin 工具函数实现 |

---

## Caveats / Not Found

- **`sp-tatami.stageIds` 引用 `woodwork`**：Web 数据里 `woodwork` 是 stage id，但 14 个 stage 中是否实际存在 `woodwork`？脚本验证：14 个 stage id 中包含 `woodwork`（出现在第 6 位）。✅ 引用合法。
- **`sp-tatami.tasks[].stageId` 同样引用 `woodwork`**：✅ 合法。
- **id 命名一致性**：脚本批量确认所有 `id` 字符串值都唯一（stages 14 个不同、styles 7 个不同、modes 4 个不同、spaceNeeds 20 个不同）。
- **`stages[].acceptIds` 与 `checklists[].id` 的覆盖关系**：stages 14 条中，仅 6 条引用了 checklist（`cl-water`, `cl-waterproof`, `cl-tile`, `cl-wood`, `cl-wall`, `cl-doorwindow`, `cl-final`），剩余 8 条 `acceptIds: []`。**`cl-final`（整体竣工）** 仅被 `movein` 阶段引用一次 —— 端上展示时需要注意"阶段 → 验收"的多对多关系是稀疏的。
- **`modes[].id` 与 `spaceNeeds[].budgetCat` 是否冲突**：✅ 不冲突，前者是 `clear|half|full|whole`，后者是 `b-*`。命名空间分离。
- **Web 是否导出 `PRICES.budgetTemplate` 计算结果缓存**：否，**没有** 缓存预算表。Android 端每次调用都要重算（profile 驱动），不存静态结果。
