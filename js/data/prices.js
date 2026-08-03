/* ============ 价格数据：预算模板生成 + 2025–2026 行情参考价 ============
 * 数据来源：2025Q4–2026 土巴兔成交数据、知乎/网易/新浪家居等公开行情调研，
 * 以二线城市为基准，一线上浮约 25%，三四线下浮约 12%。仅供参考。
 */
window.PRICES = (function () {

  var tiers = [
    { id: 't1', name: '一线城市（北上广深）', factor: 1.25 },
    { id: 'nt1', name: '新一线 / 强二线', factor: 1.1 },
    { id: 't2', name: '二线城市', factor: 1.0 },
    { id: 't3', name: '三四线城市 / 县城', factor: 0.88 }
  ];

  var grades = [
    { id: 'eco', name: '经济型', emoji: '🌱', desc: '实用为主：不追品牌不做造型，把钱花在住得舒服上（预算紧 / 过渡房）' },
    { id: 'mid', name: '舒适型', emoji: '🏡', desc: '大多数自住家庭的选择：中档品牌主材、环保板定制柜、主流家电配置' },
    { id: 'high', name: '品质型', emoji: '✨', desc: '改善型标准：一线品牌主材、多层实木定制、中央空调等舒适系统' }
  ];

  /* 每㎡ 基准费率（元，二线城市口径；全屋含家电家具的完整预算） */
  var rates = [
    { id: 'b-construct', name: '基础施工（人工+辅材）', emoji: '🧰', eco: 480, mid: 700, high: 1000 },
    { id: 'b-main', name: '主材建材', emoji: '🧱', eco: 320, mid: 480, high: 800 },
    { id: 'b-custom', name: '定制柜（橱柜+衣柜）', emoji: '🗄️', eco: 190, mid: 380, high: 650 },
    { id: 'b-window', name: '门窗封阳台', emoji: '🪟', eco: 55, mid: 90, high: 140 },
    { id: 'b-appliance', name: '家电', emoji: '📺', eco: 240, mid: 450, high: 750 },
    { id: 'b-furniture', name: '家具', emoji: '🛏️', eco: 170, mid: 280, high: 500 },
    { id: 'b-soft', name: '软装布艺', emoji: '🪴', eco: 45, mid: 90, high: 160 },
    { id: 'b-misc', name: '杂项服务', emoji: '🧾', eco: 45, mid: 110, high: 200 }
  ];
  var RESERVE_RATIO = 0.08; // 备用金比例

  function tierFactor(tierId) {
    var f = 1;
    tiers.forEach(function (t) { if (t.id === tierId) f = t.factor; });
    return f;
  }
  function tierName(tierId) {
    var n = '';
    tiers.forEach(function (t) { if (t.id === tierId) n = t.name; });
    return n;
  }
  function gradeName(gradeId) {
    var n = '';
    grades.forEach(function (g) { if (g.id === gradeId) n = g.name; });
    return n;
  }
  function modeName(modeId) {
    var n = modeId;
    if (window.DATA && DATA.modes) {
      DATA.modes.forEach(function (m) { if (m.id === modeId) n = m.name; });
    }
    return n;
  }

  function round100(n) { return Math.round(n / 100) * 100; }

  /* 按房屋信息生成分类预算模板（含装修方式的分类合并/拆分） */
  function budgetTemplate(profile) {
    var area = profile.area || 100;
    var grade = profile.grade || 'mid';
    var f = tierFactor(profile.tier);
    var mode = profile.mode || 'half';

    var base = {};
    rates.forEach(function (r) { base[r.id] = round100(r[grade] * area * f); });

    var cats = [];
    if (mode === 'full') {
      cats.push({ id: 'b-full', name: '全包合同价（施工+主材）', emoji: '📦', planned: base['b-construct'] + base['b-main'] });
    } else if (mode === 'whole') {
      cats.push({ id: 'b-whole', name: '整装合同价（施工+主材+定制）', emoji: '🎁', planned: base['b-construct'] + base['b-main'] + base['b-custom'] });
    } else if (mode === 'clear') {
      cats.push({ id: 'b-labor', name: '人工费', emoji: '👷', planned: round100(base['b-construct'] * 0.5) });
      cats.push({ id: 'b-aux', name: '辅材（水泥沙子腻子等）', emoji: '🪣', planned: round100(base['b-construct'] * 0.45) });
      cats.push({ id: 'b-main', name: '主材建材', emoji: '🧱', planned: base['b-main'] });
    } else { // half 半包
      cats.push({ id: 'b-construct', name: '基础施工（人工+辅材）', emoji: '🧰', planned: base['b-construct'] });
      cats.push({ id: 'b-main', name: '主材建材', emoji: '🧱', planned: base['b-main'] });
    }
    if (mode !== 'whole') {
      cats.push({ id: 'b-custom', name: '定制柜（橱柜+衣柜）', emoji: '🗄️', planned: base['b-custom'] });
    }
    cats.push({ id: 'b-window', name: '门窗封阳台', emoji: '🪟', planned: base['b-window'] });
    cats.push({ id: 'b-appliance', name: '家电', emoji: '📺', planned: base['b-appliance'] });
    cats.push({ id: 'b-furniture', name: '家具', emoji: '🛏️', planned: base['b-furniture'] });
    cats.push({ id: 'b-soft', name: '软装布艺（窗帘灯饰）', emoji: '🪴', planned: base['b-soft'] });
    cats.push({ id: 'b-misc', name: '杂项（清运/保洁/美缝等）', emoji: '🧾', planned: base['b-misc'] });

    var subtotal = 0;
    cats.forEach(function (c) { subtotal += c.planned; });
    cats.push({ id: 'b-reserve', name: '备用金（应急）', emoji: '🧯', planned: round100(subtotal * RESERVE_RATIO) });
    return cats;
  }

  function estimate(profile) {
    var cats = budgetTemplate(profile);
    var total = 0;
    cats.forEach(function (c) { total += c.planned; });
    return { cats: cats, total: round100(total) };
  }

  /* ============ 行情参考价（2025–2026，二线基准） ============ */
  var reference = [
    {
      group: '施工与人工', emoji: '🧰', items: [
        { name: '拆墙拆除', price: '30–80元/㎡', note: '整屋拆旧常打包 2000–6000元/户，垃圾清运另计' },
        { name: '新建墙体', price: '100–200元/㎡', note: '轻体砖含辅料' },
        { name: '铲墙皮', price: '10–15元/㎡', note: '最常被报价单漏掉的项目' },
        { name: '地面找平', price: '24–45元/㎡', note: '铺地板前需要' },
        { name: '贴砖人工', price: '35–80元/㎡', note: '一线 60–90；小规格砖/鱼骨拼人工翻倍' },
        { name: '墙面腻子(2遍含打磨)', price: '25–38元/㎡', note: '石膏找平另计 18–35元/㎡' },
        { name: '乳胶漆涂刷人工', price: '10–20元/㎡', note: '包工包料整体约 35–70元/㎡' },
        { name: '石膏板吊顶', price: '90–200元/㎡', note: '造型顶 120–280；边吊约 80元/延米' },
        { name: '厨卫铝扣板吊顶', price: '50–150元/㎡', note: '两间整包约 2800–3000元' },
        { name: '包立管', price: '300–400元/根', note: '常见漏项，签约前问清' }
      ]
    },
    {
      group: '水电改造', emoji: '⚡', items: [
        { name: '全屋水电（按平米）', price: '60–120元/㎡', note: '一线城市 100–200；100㎡全改约 1–2万' },
        { name: '电路（按位）', price: '约120元/位', note: '空调插座约 240元/位' },
        { name: '水路（按位）', price: '150–250元/位', note: '按位计价适合局部改造，最透明' },
        { name: '电路（按米）', price: '25–60元/米', note: '按米最易低报高结，防重复收开槽费' },
        { name: '水路（按米）', price: '35–90元/米', note: '暗管含开槽取高值' }
      ]
    },
    {
      group: '防水与美缝', emoji: '💧', items: [
        { name: '防水（含料含工）', price: '40–90元/㎡', note: '单个卫生间包工包料 450–800元' },
        { name: '淋浴区防水上返1.8m', price: '常被列为加项', note: '签约时确认是否包含' },
        { name: '沉箱回填', price: '180–300元/㎡', note: '炭渣便宜、陶粒贵但更好' },
        { name: '美缝（包工包料）', price: '10–60元/㎡', note: '常见 20–40；全屋 600–2000元；自购环氧彩砂更耐用' }
      ]
    },
    {
      group: '瓷砖与地板', emoji: '🧱', items: [
        { name: '瓷砖·经济', price: '30–60元/㎡', note: '出租房水平' },
        { name: '瓷砖·中档', price: '60–150元/㎡', note: '自住主流；100㎡全屋砖约 0.8–1万' },
        { name: '瓷砖·高端', price: '150–300+元/㎡', note: '一线品牌/通体大理石纹' },
        { name: '强化地板', price: '40–260元/㎡', note: '杂牌40–100，一线品牌150–260' },
        { name: '实木复合地板', price: '130–450元/㎡', note: '自住推荐档' },
        { name: '实木地板', price: '200–800+元/㎡', note: '需保养，预算高再选' },
        { name: '地板铺装人工', price: '20–80元/㎡', note: '部分品牌含安装，买前问清' }
      ]
    },
    {
      group: '定制柜', emoji: '🗄️', items: [
        { name: '颗粒板定制（促销套餐）', price: '400–800元/投影㎡', note: '大牌套餐：欧派688起/志邦699等，注意套餐外加项' },
        { name: '颗粒板定制（常规）', price: '800–1200元/投影㎡', note: '' },
        { name: '多层实木定制', price: '1200–1800元/投影㎡', note: '防潮更好，卫生间旁衣柜推荐' },
        { name: '实木/高端定制', price: '1300–2000+元/投影㎡', note: '' },
        { name: '橱柜·双饰面', price: '900–1600元/延米', note: '烤漆 1100–1700；实木 2200–4100' },
        { name: '常见加价项', price: '抽屉150–400元/个', note: '玻璃门+50元/㎡、上门测量200–500元——下单前问总价' }
      ]
    },
    {
      group: '门窗', emoji: '🚪', items: [
        { name: '模压门/工艺门', price: '400–850元/樘', note: '出租档' },
        { name: '实木复合门', price: '900–2500元/樘', note: '自住主流，门套是否含要问清' },
        { name: '实木/原木门', price: '1500–10000元/樘', note: '' },
        { name: '断桥铝封阳台·主流', price: '400–1000元/㎡', note: '低于500元/㎡的"断桥铝"多半料不足' },
        { name: '断桥铝·品牌中端', price: '1000–1500元/㎡', note: '型材壁厚≥1.8mm，PA66隔热条' },
        { name: '系统窗·高端', price: '1500–3800元/㎡', note: '临街选夹胶玻璃隔音' }
      ]
    },
    {
      group: '卫浴', emoji: '🚿', items: [
        { name: '马桶·普通', price: '500–1500元', note: '智能马桶 2000–5500' },
        { name: '花洒', price: '100–300元', note: '恒温花洒 900+，天天用建议上恒温' },
        { name: '浴室柜', price: '1000–5000元', note: '' },
        { name: '淋浴房', price: '700–2500元', note: '高端 5000+；全套洁具中档约 1–1.3万' }
      ]
    },
    {
      group: '家电', emoji: '📺', items: [
        { name: '分体空调（挂机）', price: '1500–4000元/台', note: '柜机 4000–8000' },
        { name: '中央空调·一拖四', price: '2–3.5万（国产）', note: '合资/日系 3–5万+' },
        { name: '冰箱', price: '2000–6000元', note: '高端嵌入式 8000+' },
        { name: '洗衣机', price: '1000–5000元', note: '洗烘套装另议' },
        { name: '烟机灶具套装', price: '2000–5000元', note: '集成灶 5000–15000' },
        { name: '热水器', price: '1000–3000元', note: '燃气 13–16L 主流 1000–1500' },
        { name: '洗碗机', price: '2000–6500元', note: '13套嵌入式主流 4000–6500' },
        { name: '电视', price: '2000–8000元', note: '' },
        { name: '全屋家电合计', price: '经济2–3.5万 / 中档4–6万', note: '高端含中央空调 8–12万' }
      ]
    },
    {
      group: '收尾与杂项', emoji: '🧹', items: [
        { name: '开荒保洁', price: '4–15元/㎡', note: '100㎡ 约 400–1500元' },
        { name: '灯具全屋', price: '1000–5000元', note: '网购性价比最高，安装费射灯约5元/个' },
        { name: '开关插座全屋', price: '1500–2000元', note: '约70个，中端面板40–150元/个' },
        { name: '五金+龙头全屋', price: '约1500元', note: '地漏芯/角阀/软管网购便宜一半' },
        { name: '垃圾清运', price: '300–800元/户', note: '' },
        { name: '材料上楼/搬运', price: '300–800元', note: '无电梯按层加价' },
        { name: '管理费', price: '合同价5–15%', note: '装修公司收，签约前确认比例' },
        { name: 'CMA空气检测', price: '300–800元', note: '3–5个点位，入住前必做' }
      ]
    }
  ];

  return {
    tiers: tiers,
    grades: grades,
    rates: rates,
    reference: reference,
    tierFactor: tierFactor,
    tierName: tierName,
    gradeName: gradeName,
    modeName: modeName,
    budgetTemplate: budgetTemplate,
    estimate: estimate
  };
})();
