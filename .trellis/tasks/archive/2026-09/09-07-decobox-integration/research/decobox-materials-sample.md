# decobox 材料库样本（提取自 bundle，价格完整）

本文档是 assets/decobox_catalog.json 的生成依据样本。完整数据由提取脚本从 bundle 生成，此处记录结构与代表性条目。

## 目录总结构

```jsonc
{
  "version": "2026-09-07",
  "source": "decobox.online v1.4.0 (2025-2026 行情)",
  "craft": { "gypsumLevel": 15, "grout": 20 },
  "areas": { "wallFactor": 4, "partialCeilingFactor": 0.6, "partialCeilingMin": 3 },
  "managementFeeRate": 8,
  "defaultHouse": { "totalArea": 90, "budget": 120000, "ceilingHeight": 2.4 },
  "roomPresets": [ { "maxArea": 55, "rooms": [ {"name":"客餐厅","ratio":0.34}, ... ] }, ... ],
  "wall": [ /* MaterialCategory */ ],
  "ceiling": [ /* MaterialCategory */ ],
  "floor": [ /* MaterialCategory */ ],
  "spaceExtras": [ /* ExtraWork */ ],
  "otherMains": [ /* MainMaterial */ ],
  "houseWorks": { "plumbing": 150, "hauling": 1500, "cleaning": 500, "protection": 500 },
  "partialItems": [ /* PartialItem */ ],
  "partialFees": { "demoRatePerSqm": 25, "protectRatePerSqm": 8, "protectMin": 300, "hauling": 800 },
  "doors": [ /* DoorBrand */ ],
  "heaterMatrix": { "opple": {"three":400,...}, ... },
  "toiletMatrix": { "arrow": {"normal":900,...}, ... },
  "heaterBrands": [ {"id":"opple","label":"欧普"}, ... ],
  "toiletBrands": [ {"id":"arrow","label":"箭牌"}, ... ],
  "heaterTiers": [ {"id":"three","label":"三合一"}, ... ],
  "toiletKinds": [ {"id":"normal","label":"普通"}, ... ],
  "windowsillDefaults": { "opening": 1.5, "earLeft": 3, "earRight": 3 }
}
```

## MaterialCategory（墙面/顶面/地面条目）

```jsonc
{
  "id": "wall-paint",
  "label": "乳胶漆",
  "variants": [
    { "id": "nippon-bamboo", "brand": "立邦", "series": "竹炭金装净味",
      "specs": [ { "id": "nippon-bamboo-5l", "label": "5L 桶", "unitLabel": "桶",
                   "unitPrice": 237, "coveragePerUnit": 35 } ] }
  ],
  "craftSteps": [
    { "id": "wall-scrape", "label": "原墙大白铲除", "unitPrice": 8 },
    { "id": "wall-primer", "label": "墙锢涂膜", "unitPrice": 4 },
    { "id": "wall-gypsum", "label": "石膏找平", "unitPrice": 15 },
    { "id": "wall-putty", "label": "批刮腻子", "unitPrice": 12 },
    { "id": "wall-sand", "label": "砂纸打磨", "unitPrice": 4 },
    { "id": "wall-corner", "label": "阴阳角找直", "unitPrice": 6 },
    { "id": "wall-paintwork", "label": "刷乳胶漆", "unitPrice": 10 }
  ],
  "fixedCrafts": false
}
```

网站完整变体（提取脚本会生成全量）：
- wall: `wall-paint`（乳胶漆，含工艺 7 步）、`wall-tile`（瓷砖，工艺 2 步）
- ceiling: `ceiling-gypsum`（泰山平顶 130 / 造型顶 190，fixedCrafts，工艺：龙骨 45 + 板 20）、`ceiling-aluminum`（奥普普通 90 / 加厚 130，fixedCrafts，工艺：龙骨 15 + 安装 15）
- floor: `floor-tile`（同 wall-tile 规格）、`floor-wood`（德尔强化 74 元/㎡，无工艺，includesSkirting）

## MainMaterial（其他主材条目）

```jsonc
{
  "id": "skirting", "label": "踢脚线", "defaultQty": 20,
  "suggestQtyByPerimeter": true,
  "types": [
    { "id": "skirting-alu", "label": "铝合金",
      "specs": [ {"id":"skirting-alu-3","label":"宽 3cm","unit":"米","unitPrice":15},
                 {"id":"skirting-alu-5","label":"宽 5cm","unit":"米","unitPrice":20} ] }
    // pvc / wood / tile 同构
  ]
}
```

multiPicks 类（可勾多个类型，如灯具/卫浴五金）：

```jsonc
{ "id": "lighting", "label": "灯具", "defaultQty": 1,
  "types": [
    {"id":"light-main","label":"主灯","specs":[{"id":"light-main-std","label":"标准","unit":"套","unitPrice":1500}]},
    {"id":"light-down","label":"筒灯","specs":[{"id":"light-down-std","label":"标准","unit":"个","unitPrice":45}]},
    {"id":"light-linear","label":"线性灯","specs":[{"id":"light-linear-std","label":"标准","unit":"米","unitPrice":60}]},
    {"id":"light-spot","label":"射灯","specs":[{"id":"light-spot-std","label":"标准","unit":"个","unitPrice":55}]}
  ] }
// 默认数量映射 $f: light-main:1, light-down:4, light-linear:5, light-spot:2,
//   bh-towel:1, bh-paper:1, bh-basin-faucet:1, bh-shower-set:1, bh-drain:3, bh-angle-valve:6, kh-sink:1, kh-faucet:1
```

## ExtraWork（空间附加项）

```jsonc
[
  { "id": "waterproof", "label": "防水工程", "basis": "area", "unit": "㎡", "unitPrice": 65,
    "desc": "阳台/卫生间墙地面防水" },
  { "id": "pipeWrap", "label": "包管+隔音", "basis": "count", "unit": "根", "unitPrice": 380 },
  { "id": "wallDemolition", "label": "墙体拆改", "basis": "area", "unit": "㎡", "unitPrice": 35 }
]
```

## DoorBrand（木门）

```jsonc
[
  { "id": "tata", "label": "TATA 木门",
    "standardOpening": { "height": 2300, "width": 900, "wallThickness": 240 },
    "series": [
      { "id": "tz", "label": "免漆门 · TZ 系列", "unitPrice": 1150, "framePrices": {"single":105,"double":128} },
      { "id": "t",  "label": "免漆门 · T 系列",   "unitPrice": 1010, "framePrices": {"single":105,"double":128} }
    ] },
  { "id": "guanzun", "label": "重庆冠尊",
    "standardOpening": { "height": 2100, "width": 900, "wallThickness": 240 },
    "customOpening": true,
    "surcharge": { "tallFrom": 2100, "tallTo": 2300, "wideFrom": 900, "wideTo": 1000,
                   "perLeaf": 60, "tubeFrom": 2300, "tubePerLeaf": 60 },
    "series": [
      { "id": "baked",   "label": "烤漆门", "unitPrice": 655, "framePrices": {"single":53,"double":64} },
      { "id": "nopaint", "label": "无漆门", "unitPrice": 590, "framePrices": {"single":53,"double":64} }
    ] }
]
```

金属移门玻璃：standard 0 / gray 25 / oil-sand 30 / ultra-changhong 45（地轨）；吊轨同款玻璃价。

## PartialItem（局改）

```jsonc
[
  { "id": "wall-refresh", "label": "墙面刷新", "unit": "㎡", "integerQty": false, "needsArea": false,
    "tiers": [
      { "id": "light", "label": "墙面结实，只是旧了", "unitPrice": 45,
        "note": "不用铲：修补 → 打磨 → 重刷乳胶漆，人工一口价，乳胶漆按所选品牌另计" },
      { "id": "full", "label": "有掉皮、发霉、开裂", "unitPrice": 85,
        "note": "铲到底重做：铲除 → 墙固 → 找平 → 腻子 → 刷漆，人工一口价，乳胶漆按所选品牌另计" }
    ] },
  { "id": "floor-replace", "label": "地面更换", "unit": "㎡", "integerQty": false, "needsArea": false,
    "tiers": [
      { "id": "tile", "label": "瓷砖", "catalogCategoryId": "floor-tile" },
      { "id": "wood", "label": "木地板", "catalogCategoryId": "floor-wood" }
    ] }
]
```

## 计价规则备忘

- ㎡ 计价：subtotal = qty × unitPrice（qty 保留 1 位小数）
- 按桶/片等覆盖率计价：qty = ceil(面积 / coveragePerUnit)，subtotal = qty × unitPrice
- 木门：总价 = round(series.unitPrice + framePrices[frameStyle] + surcharge)
- 金属移门：unitPrice = spec.unitPrice + glass.unitPrice；qty 默认 = 门洞面积（㎡）
- 窗台石 qty = opening + (earLeft + earRight)/100（延米）
- 防水 = (地面面积 + 墙面面积) × 65（遍数仅记录）
- 墙面刷新 full 档材料费 = 所选乳胶漆按涂布率另计（light 档只有人工）
