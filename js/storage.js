/* ============ 本地存储层（localStorage + 导入导出） ============ */
window.Store = (function () {
  var KEY = 'zhuangxiu_guanjia_v1';

  function defaults() {
    return {
      ver: 1,
      // 房屋与装修基本信息：{area, tier, mode, grade, styleId, startDate, totalBudget}
      profile: null,
      tasksDone: {},      // { taskId: true }
      taskDates: {},      // { taskId: 'YYYY-MM-DD' }
      customTasks: [],    // [{id, stageId, text, date}]
      stageOverride: {},  // { stageId: 'done' }  手动整段完成
      budget: { categories: [], expenses: [] },
      checks: {},         // 验收清单勾选 { itemId: true }
      notes: [],          // [{id, title, text, updatedAt}]
      contacts: [],       // [{id, name, role, phone, note}]
      quiz: null          // {styleId, at}
    };
  }

  var storageOk = true;
  function load() {
    var s = defaults();
    try {
      var raw = localStorage.getItem(KEY);
      if (!raw) return s;
      var p = JSON.parse(raw);
      for (var k in s) { if (p[k] !== undefined) s[k] = p[k]; }
      // 保证嵌套结构完整
      if (!s.budget || typeof s.budget !== 'object') s.budget = { categories: [], expenses: [] };
      if (!Array.isArray(s.budget.categories)) s.budget.categories = [];
      if (!Array.isArray(s.budget.expenses)) s.budget.expenses = [];
      ['customTasks', 'notes', 'contacts'].forEach(function (key) {
        if (!Array.isArray(s[key])) s[key] = [];
      });
      ['tasksDone', 'taskDates', 'stageOverride', 'checks'].forEach(function (key) {
        if (!s[key] || typeof s[key] !== 'object') s[key] = {};
      });
      return s;
    } catch (e) {
      console.warn('读取本地数据失败', e);
      return defaults();
    }
  }

  var state = load();

  // 检测 localStorage 是否可用（隐私模式下可能不可用）
  try {
    localStorage.setItem(KEY + '_test', '1');
    localStorage.removeItem(KEY + '_test');
  } catch (e) { storageOk = false; }

  var timer = null;
  function persist() {
    try { localStorage.setItem(KEY, JSON.stringify(state)); }
    catch (e) { console.warn('保存失败', e); }
  }
  function save() {
    if (!storageOk) return;
    clearTimeout(timer);
    timer = setTimeout(persist, 150);
  }
  window.addEventListener('beforeunload', function () {
    if (storageOk) { clearTimeout(timer); persist(); }
  });

  var uidSeq = 0;
  function uid(prefix) {
    uidSeq++;
    return (prefix || 'id') + '_' + Date.now().toString(36) + '_' + uidSeq + Math.random().toString(36).slice(2, 6);
  }

  function exportJSON() {
    var data = JSON.stringify(state, null, 2);
    var d = new Date();
    var name = '装修管家备份_' + d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate() + '.json';
    UI.download(name, data, 'application/json');
  }

  function importJSON(file, cb) {
    var reader = new FileReader();
    reader.onload = function () {
      try {
        var p = JSON.parse(reader.result);
        if (!p || typeof p !== 'object' || p.ver === undefined) {
          cb(false, '文件格式不对，请选择本应用导出的备份文件');
          return;
        }
        var s = defaults();
        for (var k in s) { if (p[k] !== undefined) s[k] = p[k]; }
        state = s;
        // 直接同步写入
        clearTimeout(timer);
        persist();
        cb(true);
      } catch (e) {
        cb(false, '文件解析失败：' + e.message);
      }
    };
    reader.onerror = function () { cb(false, '文件读取失败'); };
    reader.readAsText(file, 'utf-8');
  }

  function reset() {
    state = defaults();
    clearTimeout(timer);
    persist();
  }

  return {
    get state() { return state; },
    get storageOk() { return storageOk; },
    save: save,
    uid: uid,
    exportJSON: exportJSON,
    importJSON: importJSON,
    reset: reset
  };
})();
