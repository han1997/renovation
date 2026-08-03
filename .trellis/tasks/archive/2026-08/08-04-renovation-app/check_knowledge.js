/* Cross-layer verification harness for js/data/knowledge.js + prices.js
 * Loads both files with a window shim, then asserts every field shape that the
 * consuming views (guide/stages/home/more/budget/app) expect.
 */
var fs = require('fs');
var path = require('path');
var vm = require('vm');

var root = path.resolve(__dirname, '..', '..', '..');
var sandbox = { window: {}, console: console };
sandbox.global = sandbox;
vm.createContext(sandbox);

function load(rel) {
  var code = fs.readFileSync(path.join(root, rel), 'utf8');
  vm.runInContext(code, sandbox, { filename: rel });
}

load('js/data/knowledge.js');
load('js/data/prices.js');

// In a browser, `window` IS the global object, so `window.DATA = x` also creates
// a bare global `DATA`. prices.js relies on this (line 50: `DATA.modes`). Mirror
// that behaviour in the Node vm sandbox by exposing the globals.
sandbox.DATA = sandbox.window.DATA;
sandbox.PRICES = sandbox.window.PRICES;

var DATA = sandbox.window.DATA;
var PRICES = sandbox.window.PRICES;
var fails = 0;
function ok(cond, msg) { if (cond) console.log('  OK  ' + msg); else { console.log('  FAIL ' + msg); fails++; } }

console.log('=== 1. All 12 top-level fields present ===');
var fields = ['stages','totalDurationNote','checklists','tipTopics','tips','acceptIntro','styles','styleQuiz','materialTimeline','modes','whoBuilds','glossary'];
fields.forEach(function (f) { ok(DATA[f] !== undefined, 'DATA.' + f + ' exists'); });

console.log('=== 2. stages: shape + count ===');
ok(Array.isArray(DATA.stages), 'stages is array');
ok(DATA.stages.length === 14, 'stages length === 14 (got ' + DATA.stages.length + ')');
var stageIds = {};
DATA.stages.forEach(function (st, i) {
  ok(typeof st.id === 'string' && st.id, 'stage[' + i + '].id');
  ok(typeof st.phase === 'string', 'stage[' + i + '].phase');
  ok(typeof st.emoji === 'string', 'stage[' + i + '].emoji');
  ok(typeof st.name === 'string', 'stage[' + i + '].name');
  ok(typeof st.duration === 'string', 'stage[' + i + '].duration');
  ok(typeof st.goal === 'string', 'stage[' + i + '].goal');
  ok(Array.isArray(st.tasks), 'stage[' + i + '].tasks is array');
  ok(Array.isArray(st.warnings), 'stage[' + i + '].warnings is array');
  ok(Array.isArray(st.buy), 'stage[' + i + '].buy is array');
  ok(Array.isArray(st.acceptIds), 'stage[' + i + '].acceptIds is array');
  stageIds[st.id] = true;
  st.tasks.forEach(function (t, j) {
    ok(typeof t.id === 'string' && t.id, 'stage[' + i + '].tasks[' + j + '].id');
    ok(typeof t.text === 'string' && t.text, 'stage[' + i + '].tasks[' + j + '].text');
    if (t.tip !== undefined) ok(typeof t.tip === 'string', 'stage[' + i + '].tasks[' + j + '].tip is string');
  });
  st.buy.forEach(function (b, j) {
    ok(typeof b.item === 'string', 'stage[' + i + '].buy[' + j + '].item');
    ok(typeof b.note === 'string', 'stage[' + i + '].buy[' + j + '].note');
  });
});

console.log('=== 3. totalDurationNote ===');
ok(typeof DATA.totalDurationNote === 'string' && DATA.totalDurationNote, 'totalDurationNote non-empty string');

console.log('=== 4. checklists: shape + acceptIds cross-ref ===');
ok(Array.isArray(DATA.checklists), 'checklists is array');
var clIds = {};
DATA.checklists.forEach(function (cl, i) {
  ok(typeof cl.id === 'string', 'checklist[' + i + '].id');
  ok(typeof cl.emoji === 'string', 'checklist[' + i + '].emoji');
  ok(typeof cl.name === 'string', 'checklist[' + i + '].name');
  if (cl.note !== undefined) ok(typeof cl.note === 'string', 'checklist[' + i + '].note');
  ok(Array.isArray(cl.items), 'checklist[' + i + '].items is array');
  clIds[cl.id] = cl;
  cl.items.forEach(function (it, j) {
    ok(typeof it.id === 'string' && it.id, 'checklist[' + i + '].items[' + j + '].id');
    ok(typeof it.text === 'string' && it.text, 'checklist[' + i + '].items[' + j + '].text');
  });
});
DATA.stages.forEach(function (st) {
  st.acceptIds.forEach(function (aid) {
    ok(!!clIds[aid], 'stage ' + st.id + ' acceptIds -> ' + aid + ' exists in checklists');
  });
});
// every checklist referenced by at least one stage? (informational)
var referenced = {};
DATA.stages.forEach(function (st) { st.acceptIds.forEach(function (a) { referenced[a] = true; }); });
DATA.checklists.forEach(function (cl) {
  ok(referenced[cl.id], 'checklist ' + cl.id + ' is referenced by some stage');
});

console.log('=== 5. tipTopics + tips ===');
ok(Array.isArray(DATA.tipTopics), 'tipTopics is array');
DATA.tipTopics.forEach(function (t) { ok(typeof t === 'string', 'tipTopic ' + t + ' is string'); });
ok(Array.isArray(DATA.tips), 'tips is array');
var topicSet = {};
DATA.tipTopics.forEach(function (t) { topicSet[t] = true; });
var levelSet = { '高危': 1, '重要': 1, '提示': 1 };
DATA.tips.forEach(function (tip, i) {
  ok(typeof tip.title === 'string' && tip.title, 'tips[' + i + '].title');
  ok(typeof tip.body === 'string' && tip.body, 'tips[' + i + '].body');
  ok(typeof tip.topic === 'string', 'tips[' + i + '].topic');
  ok(!!topicSet[tip.topic], 'tips[' + i + '].topic "' + tip.topic + '" is in tipTopics');
  ok(typeof tip.level === 'string', 'tips[' + i + '].level');
  ok(!!levelSet[tip.level], 'tips[' + i + '].level "' + tip.level + '" is one of 高危/重要/提示');
});

console.log('=== 6. acceptIntro ===');
ok(typeof DATA.acceptIntro === 'string' && DATA.acceptIntro, 'acceptIntro non-empty string');

console.log('=== 7. styles ===');
ok(Array.isArray(DATA.styles), 'styles is array');
var styleIds = {};
DATA.styles.forEach(function (st, i) {
  ok(typeof st.id === 'string', 'styles[' + i + '].id');
  ok(typeof st.emoji === 'string', 'styles[' + i + '].emoji');
  ok(typeof st.name === 'string', 'styles[' + i + '].name');
  ok(typeof st.tagline === 'string', 'styles[' + i + '].tagline');
  ok(Array.isArray(st.colors) && st.colors.length, 'styles[' + i + '].colors non-empty array');
  st.colors.forEach(function (c) { ok(/^#[0-9a-fA-F]{6}$/.test(c), 'styles[' + i + '] color ' + c + ' is #hex'); });
  ok(typeof st.cost === 'string', 'styles[' + i + '].cost');
  ok(typeof st.costNote === 'string', 'styles[' + i + '].costNote');
  ok(typeof st.desc === 'string', 'styles[' + i + '].desc');
  ok(typeof st.fit === 'string', 'styles[' + i + '].fit');
  ok(Array.isArray(st.elements), 'styles[' + i + '].elements array');
  ok(Array.isArray(st.pitfalls), 'styles[' + i + '].pitfalls array');
  styleIds[st.id] = true;
});

console.log('=== 8. styleQuiz: scores keys -> styles ids ===');
ok(typeof DATA.styleQuiz === 'object' && Array.isArray(DATA.styleQuiz.questions), 'styleQuiz.questions is array');
DATA.styleQuiz.questions.forEach(function (q, i) {
  ok(typeof q.q === 'string', 'quiz q[' + i + '].q');
  ok(Array.isArray(q.options) && q.options.length, 'quiz q[' + i + '].options non-empty');
  q.options.forEach(function (o, j) {
    ok(typeof o.text === 'string', 'quiz q[' + i + '].opt[' + j + '].text');
    ok(typeof o.scores === 'object', 'quiz q[' + i + '].opt[' + j + '].scores is object');
    for (var k in o.scores) {
      ok(!!styleIds[k], 'quiz q[' + i + '].opt[' + j + '] scores key "' + k + '" -> real style id');
      ok(typeof o.scores[k] === 'number', 'quiz q[' + i + '].opt[' + j + '] scores[' + k + '] is number');
    }
  });
});

console.log('=== 9. materialTimeline ===');
ok(Array.isArray(DATA.materialTimeline), 'materialTimeline is array');
DATA.materialTimeline.forEach(function (p, i) {
  ok(typeof p.emoji === 'string', 'mt[' + i + '].emoji');
  ok(typeof p.period === 'string', 'mt[' + i + '].period');
  ok(typeof p.when === 'string', 'mt[' + i + '].when');
  if (p.note !== undefined) ok(typeof p.note === 'string', 'mt[' + i + '].note');
  ok(Array.isArray(p.items) && p.items.length, 'mt[' + i + '].items non-empty');
  p.items.forEach(function (it, j) {
    ok(typeof it.name === 'string', 'mt[' + i + '].items[' + j + '].name');
    if (it.note !== undefined) ok(typeof it.note === 'string', 'mt[' + i + '].items[' + j + '].note');
    ok(typeof it.lead === 'string', 'mt[' + i + '].items[' + j + '].lead');
  });
});

console.log('=== 10. modes: ids match prices.js expectations ===');
ok(Array.isArray(DATA.modes), 'modes is array');
var modeIds = {};
DATA.modes.forEach(function (m, i) {
  ok(typeof m.id === 'string', 'modes[' + i + '].id');
  ok(typeof m.emoji === 'string', 'modes[' + i + '].emoji');
  ok(typeof m.name === 'string', 'modes[' + i + '].name');
  ok(typeof m.priceShort === 'string', 'modes[' + i + '].priceShort');
  ok(typeof m.short === 'string', 'modes[' + i + '].short (used by wizard)');
  ok(typeof m.desc === 'string', 'modes[' + i + '].desc');
  ok(Array.isArray(m.pros), 'modes[' + i + '].pros array');
  ok(Array.isArray(m.cons), 'modes[' + i + '].cons array');
  ok(typeof m.fit === 'string', 'modes[' + i + '].fit');
  modeIds[m.id] = true;
});
['clear','half','full','whole'].forEach(function (id) {
  ok(!!modeIds[id], 'modes contains id "' + id + '" (prices.js budgetTemplate expects it)');
});
// prices.js modeName lazy-reads DATA.modes
ok(PRICES.modeName('half') !== 'half', 'PRICES.modeName("half") resolves to label via DATA.modes (got ' + PRICES.modeName('half') + ')');
ok(PRICES.modeName('clear') !== 'clear', 'PRICES.modeName("clear") resolves (got ' + PRICES.modeName('clear') + ')');
ok(PRICES.modeName('full') !== 'full', 'PRICES.modeName("full") resolves (got ' + PRICES.modeName('full') + ')');
ok(PRICES.modeName('whole') !== 'whole', 'PRICES.modeName("whole") resolves (got ' + PRICES.modeName('whole') + ')');
// budgetTemplate must run for each mode without error
['clear','half','full','whole'].forEach(function (id) {
  try { PRICES.budgetTemplate({ area: 100, tier: 't2', mode: id, grade: 'mid' }); ok(true, 'budgetTemplate runs for mode=' + id); }
  catch (e) { ok(false, 'budgetTemplate fails for mode=' + id + ': ' + e.message); }
});

console.log('=== 11. whoBuilds ===');
ok(Array.isArray(DATA.whoBuilds), 'whoBuilds is array');
DATA.whoBuilds.forEach(function (w, i) {
  ok(typeof w.emoji === 'string', 'whoBuilds[' + i + '].emoji');
  ok(typeof w.name === 'string', 'whoBuilds[' + i + '].name');
  ok(Array.isArray(w.pros), 'whoBuilds[' + i + '].pros');
  ok(Array.isArray(w.cons), 'whoBuilds[' + i + '].cons');
  ok(typeof w.fit === 'string', 'whoBuilds[' + i + '].fit');
});

console.log('=== 12. glossary ===');
ok(Array.isArray(DATA.glossary), 'glossary is array');
DATA.glossary.forEach(function (g, i) {
  ok(typeof g.term === 'string' && g.term, 'glossary[' + i + '].term');
  ok(typeof g.def === 'string' && g.def, 'glossary[' + i + '].def');
});

console.log('=== 13. Content sanity: no placeholders/stubs ===');
var blob = fs.readFileSync(path.join(root, 'js/data/knowledge.js'), 'utf8');
[/TODO/, /待补充/, /lorem/i, /FIXME/, /占位/, /xxx/i, /placeholder/i].forEach(function (re) {
  ok(!re.test(blob), 'no ' + re + ' placeholder text');
});
// duplicate tip titles?
var titleSeen = {};
DATA.tips.forEach(function (t) { if (titleSeen[t.title]) ok(false, 'duplicate tip title: ' + t.title); titleSeen[t.title] = true; });
// duplicate style ids?
var sidSeen = {};
DATA.styles.forEach(function (s) { if (sidSeen[s.id]) ok(false, 'duplicate style id: ' + s.id); sidSeen[s.id] = true; });
// duplicate stage ids?
var stidSeen = {};
DATA.stages.forEach(function (s) { if (stidSeen[s.id]) ok(false, 'duplicate stage id: ' + s.id); stidSeen[s.id] = true; });

console.log('=== 14. IIFE pattern ===');
ok(/window\.DATA\s*=\s*\(function\s*\(\)\s*\{[\s\S]*\}\)\(\);/.test(blob), 'window.DATA = (function(){...})(); IIFE pattern matches prices.js');

console.log('\n==== RESULT: ' + (fails === 0 ? 'ALL GREEN (' : (fails + ' FAILURES (')) + '0 failures expected) ====');
process.exit(fails === 0 ? 0 : 1);
