# decobox.online 数据模型与计算公式（从 JS bundle 提取）

- 来源：`C:\Users\hanhu\.local\share\opencode\tool-output\tool_07a7dfc56001Xi9j3Jc51jlZ7f`（~783KB minified React bundle，中文文案在反引号字符串中）
- 提取日期：2026-09-07
- 网站版本：v1.4.0（2026-09-06）

## 0. 顶层模式

```js
em = { full: '整装全包', semi: '半包', partial: '局改' }
```

- 三模式共用一个 planner state 结构（localStorage key `reno-planner-state:v1`，按模式 scope 分存）
- 整装/半包共用材料目录；局改用独立项目表

## 1. 房屋信息（houseInfo）

```js
Zm = { totalArea: 90, budget: 120000, ceilingHeight: 2.4 }  // ㎡ / 元 / 米
```

## 2. 划分空间

### 面积推荐（Km，按总建面分档，返回房间名+面积比例）

| 建面 | 房间组合（比例） |
|---|---|
| <55㎡ | 客餐厅 .34 / 卧室 .27 / 厨房 .12 / 卫生间 .10 / 阳台 .09 / 玄关过道 .08 |
| <95㎡ | 客厅 .26 / 餐厅 .11 / 主卧 .16 / 次卧 .13 / 厨房 .08 / 卫生间 .06 / 阳台 .09 / 玄关过道 .11 |
| <130㎡ | 客厅 .24 / 餐厅 .10 / 主卧 .15 / 次卧 .12 / 书房 .09 / 厨房 .07 / 卫生间 .06 / 主卫 .05 / 阳台 .07 / 玄关过道 .05 |
| ≥130㎡ | 客厅 .22 / 餐厅 .10 / 主卧 .14 / 次卧 .11 / 儿童房 .10 / 书房 .08 / 厨房 .06 / 卫生间 .05 / 主卫 .04 / 阳台 .06 / 玄关过道 .04 |

- 尾差补偿：第一个房间面积 += (总建面 − Σ各房面积)
- 房间名重复校验（重名不能进下一步）
- 空间上限：自定义空间最多（文案提及，具体上限值未提取到，取 15 保守值即可）

## 3. 面积估算公式

```js
// Nm(surface, floorArea, ceilingHeight)
wall    => Z(4 * sqrt(floorArea) * ceilingHeight)   // 墙面 = 4×√地面×层高
ceiling/floor => Z(floorArea)
// Z(x) = round(x*10)/10  （保留 1 位小数）
// surfaceOverrides 可手动覆盖任意部位面积
// Im(area)：局部吊顶默认面积 = min(max(area,0), max(3, 4*sqrt(area)*0.6))
```

## 4. 部位（Qp）

```js
Qp = ['wall', 'ceiling', 'floor']
$p = { wall:'墙面', ceiling:'顶面', floor:'地面' }
tm = { none:'不吊顶', partial:'局部吊顶', full:'全部吊顶' }
```

顶面方案独立于部位选材：`ceiling.plan ∈ {null, none, partial, full}`；`none` 时顶面走"顶面基层"（同墙面乳胶漆工艺）；局部吊顶面积 = min(partialArea, 顶面面积)，剩余顶面走基层。

## 5. 材料目录（ap）

### 5.1 墙面 wall

**乳胶漆 wall-paint**：
- 品牌/系列/规格：立邦 竹炭金装净味 5L 桶 237 元，coveragePerUnit 35（㎡/桶）
- 基层工艺 craftSteps（元/㎡，按墙面面积计）：

| id | 名称 | 单价 |
|---|---|---|
| wall-scrape | 原墙大白铲除 | 8 |
| wall-primer | 墙锢涂膜 | 4 |
| wall-gypsum | 石膏找平 | 15（yf.level） |
| wall-putty | 批刮腻子 | 12 |
| wall-sand | 砂纸打磨 | 4 |
| wall-corner | 阴阳角找直 | 6 |
| wall-paintwork | 刷乳胶漆 | 10 |

- 乳胶漆材料费 = ceil(面积/coveragePerUnit) × spec.unitPrice（`lm()`：㎡ 计价的直接用面积，其他向上取整桶数）

**瓷砖 wall-tile**：东鹏通体砖 800×800 单片 39 元（0.64㎡/片）+ 工艺：铺贴辅材 30 / 铺贴人工 35 元/㎡

### 5.2 顶面 ceiling

**石膏板吊顶 ceiling-gypsum**（fixedCrafts:!0，工艺强制全选）：泰山平顶 130 元/㎡ / 造型顶 190 元/㎡；工艺：轻钢龙骨基层 45 + 石膏板安装固定 20 元/㎡
**铝扣板 ceiling-aluminum**（fixedCrafts）：奥普 300×300 普通 90 / 加厚抗菌 130 元/㎡；工艺：龙骨系统 15 + 安装 15 元/㎡

### 5.3 地面 floor

**瓷砖 floor-tile**：同墙砖（辅材 30 + 人工 35）
**木地板 floor-wood**：德尔强化 1200×195×12mm 74 元/㎡，includesSkirting:!0（含踢脚线，选它则踢脚线不必另购）

### 5.4 石膏板附加参数

```js
yf = { level: 15, grout: 20 }   // 石膏找平 15、美缝 20（grout 用于瓷砖？未完全确认，保守记为美缝单价）
```

## 6. 空间附加项（bf，每空间可勾选）

| id | 名称 | 计价 | 单价 | 说明 |
|---|---|---|---|---|
| waterproof | 防水工程 | ㎡ | 65 | 输入：防水地面面积 + 防水墙面面积 + 遍数（默认 2 遍，遍数仅记录不影响价格）；默认地面=空间面积、墙面=估算墙面积 |
| pipeWrap | 包管+隔音 | 根 | 380 | 默认 1 根 |
| wallDemolition | 墙体拆改 | ㎡ | 35 | 默认 0 |

## 7. 其他主材（Cf）

每项 defaultQty 起始数量，types→specs，可 priceOverride/qtyOverride。

| id | 名称 | 类型 | 规格 | 单价 |
|---|---|---|---|---|
| door | 门 | door-wood 木门 | 门套单包口 1200 / 双包口 1600 元/樘（配合品牌系列价，见下） | — |
| | | door-metal-slide 金属移门 | 地轨推拉门 265 / 吊轨门 405 元/㎡（玻璃：标配 0 / 灰玻 25 / 超白油砂 30 / 超白长虹 45） | |
| skirting | 踢脚线（defaultQty 20，suggestQtyByPerimeter） | 铝合金 3cm 15 / 5cm 20；PVC 8 / 12；实木 30 / 40；瓷砖 15 / 20 | 元/米 | |
| windowsill | 窗台石（defaultQty 2） | 大理石 ≤22cm 150 / 22–28cm 190 / 28–35cm 240；石英石 220/270/330；人造石 120/…；花岗岩 180/220/270 | 元/延米 | |
| lighting | 灯具 | 主灯 1500/套、筒灯 45/个、线性灯 60/米、射灯 55/个 | 多选（$f 默认数量：主灯1 筒灯4 线性5 射灯2） | |
| sanitary | 洁具 | 风暖一体机（品牌×档位矩阵）、坐便（品牌×类型矩阵） | 见下 | |
| kitchen-hardware | 厨房五金 | 大单槽 800/套、抽拉龙头 300/个 | | |
| bath-hardware | 卫浴五金 | 毛巾架 120、卷纸架 40、面盆龙头 260、淋浴花洒 900、地漏 60、角阀 25（默认数量 1/1/1/1/3/6） | | |
| switch-panel | 开关面板 | 86 型 15 元/个（defaultQty 5） | | |

### 7.1 木门品牌（Af）

- TATA 木门：标准门洞 2300×900×240；系列：免漆门 TZ 1150 元、免漆门 T 1010 元；门套 framePrices {single:105, double:128}
- 重庆冠尊：标准门洞 2100×900×240，支持自定义门洞；系列：烤漆门 655 / 无漆门 590；framePrices {single:53, double:64}；超规加价：高 2100–2300 加 60/扇、宽 900–1000 加 60/扇、高>2300 加方管 60/扇
- 木门总价 = round(系列价 + 门套价 + 超规加价)；下单面积 = 门洞高(m)×宽(m) 保留 2 位（仅展示）
- 金属移门价 = spec.unitPrice + 玻璃价；数量可覆盖（默认按门洞面积）

### 7.2 洁具矩阵

```js
// 风暖一体机：品牌 Vf {opple 欧普, nvc 雷士, panasonic 松下, aupu 奥普} × 档位 Hf {three 三合一, four 四合一, five 五合一}
Uf = { opple:{three:400,four:600,five:900}, nvc:{three:450,four:650,five:950},
       panasonic:{three:600,four:900,five:1300}, aupu:{three:500,four:800,five:1200} }
// 坐便：品牌 Wf {arrow 箭牌, hengjie 恒洁, jomoo 九牧} × 类型 Kf {normal 普通, smart 智能}
qf = { arrow:{normal:900,smart:2600}, hengjie:{normal:1000,smart:2800}, jomoo:{normal:950,smart:2700} }
```

### 7.3 窗台石数量（zp）

```js
Xf = { opening: 1.5, earLeft: 3, earRight: 3 }  // 米 / cm / cm
数量 = opening + (earLeft + earRight) / 100      // 延米
```

## 8. 全屋工程（tp='全屋工程'，整装/半包可开关，价格可改）

```js
op = { plumbing: 150, hauling: 1500, cleaning: 500, protection: 500 }
```

| key | 名称 | 计价 |
|---|---|---|
| plumbing | 水电改造 | 150 元/㎡ × 总建面 |
| hauling | 垃圾清运 | 1500 元/项 |
| cleaning | 开荒保洁 | 500 元/项 |
| protection | 成品保护 | 500 元/项 |

默认全部关闭（houseWorksEnabled 全 false）。

## 9. 管理费

```js
sp = { management: 8 }   // 8%
整装专属：管理费 = 8% × (主材 Bn+Un + 人工辅材 Jn)   // nr = (qn+Jn)*u/100
半包无管理费、无全屋工程附加费用
```

## 10. 采购方式（sourcing）

- 每项主材 + 乳胶漆可设 `sourcing ∈ { self 自购, included 施工方代购 }`（默认 self）
- **自购材料不计入总价**，清单单独列出（`jm(line) = line.sourcing==='self' ? 0 : line.subtotal`）
- 整装总价 = 代购主材 + 人工辅材 + 全屋工程 + 管理费
- 半包总价 = 人工辅材（含工艺）+ 代购主材（自购单列）

## 11. 局改（partial）

### 11.1 事项（up）

| id | 名称 | 单位 | 档位 | 单价 |
|---|---|---|---|---|
| wall-refresh | 墙面刷新 | ㎡ | light 墙面结实只是旧了 | 45 元/㎡（修补→打磨→重刷乳胶漆人工一口价，乳胶漆按所选品牌另计） |
| | | | full 有掉皮发霉开裂 | 85 元/㎡（铲除→墙固→找平→腻子→刷漆，乳胶漆另计） |
| floor-replace | 地面更换 | ㎡ | tile 瓷砖 / wood 木地板 | 套用地面目录（floor-tile / floor-wood），按空间逐个填面积 |

- 施工量 qtyRows：可多行（房间名+面积），自动加总；墙面刷新 full 档的乳胶漆材料费另计（km()）
- 局改无管理费

### 11.2 局改费用（Sm = demo/protect/hauling，可开关可改价）

| key | 名称 | 默认计算 |
|---|---|---|
| demo | 拆旧费 | 25 元/㎡ × 相关事项施工量 |
| protect | 成品保护费 | max(8 元/㎡ × 施工量总和, 300) |
| hauling | 垃圾清运 | 800 元固定 |

## 12. 汇总与预算对比

```
total = Σ(非自购行) + 全屋工程 + 管理费(整装)
预算剩余 = budget − total；超出时警示"超出预算：¥xxx"
Top3：非自购行按 subtotal 降序前 3
```

## 13. 文本清单格式（对齐网站导出）

```
装修宝典（整装全包）
总建面 90㎡ · 预算 ¥120,000 · 完成面层高 2.4m

【客厅】 30㎡
  墙面 · 乳胶漆 …
  （自购标注：xxx（自购））
  …

【全屋工程】
  水电改造 …

预计总价：¥xxx
超出预算：¥xxx / 预算剩余：¥xxx
```

局改：
```
装修宝典（局改）
预算 ¥xxx

【事项】
  墙面刷新：xx㎡ × ¥45/㎡ = ¥xxx
  事项小计：¥xxx

【局改费用】
  拆旧费：¥xxx

预计总价：¥xxx
超出预算/预算剩余：¥xxx
```

## 14. 需求规划（另一独立模块）

- localStorage keys：`reno-demand:demands`（选中的需求）、`reno-demand:spaces`（空间列表）、`reno-demand:distributed`（分配+重要度）、`reno-demand:step1Done..step4Done`
- 四步：选择需求 → 添加空间 → 分配需求 → 生成清单
- 重要度 4 档：`['普通','必备','重要','非常重要']`（白/绿/蓝/红）
- 空间预设 10 个：电梯前厅/玄关/客厅/餐厅/厨房/卧室/卫生间/阳台/全屋/功能房（可自定义，重名校验）
- 需求类型前缀映射：dtqt=电梯前厅, xg=玄关, kt=客厅, ct=餐厅, cf=厨房, ws=卧室, wsj=卫生间, yt=阳台, qw=全屋, df=功能房
- 类型数(实测,2026-09-07 直接从 bundle 解析)：电梯前厅 10 / 玄关 12 / 客厅 25 / 餐厅 19 / 厨房 19 / 卧室 15 / 卫生间 15 / 阳台 14 / 全屋 6 / 功能房 15 = **150 类型**；叶子需求项 **819 个(816 唯一)**。早期人工估算的「218 类型 / 751 项」作废。
- 需求清单长图：标题"装修需求清单"，按空间分组，含重要度标注
- 同一需求可分配给多个空间；"按面积推荐分配"按钮可把已选需求按空间面积比例分配

## 15. 局改默认价格覆盖点

- 全屋工程四项价格、局改三项费用、管理费率均可手动覆盖（amountOverride / priceOverride）
- 单项工艺价格可覆盖（craftPriceOverrides）、材料价可覆盖（materialPriceOverride）
