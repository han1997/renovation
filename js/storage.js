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
      customTasks: [],    // [{id, stageId, text, spaceId?, date?}]
      stageOverride: {},  // { stageId: 'done' }  手动整段完成
      budget: { categories: [], expenses: [] },
      checks: {},         // 验收清单勾选 { itemId: true }
      notes: [],          // [{id, title, text, updatedAt}]
      contacts: [],       // [{id, name, role, phone, note}]
      // 空间需求：[{id, presetId, name, emoji, desc, stageIds:[], tasks:[{stageId,text}], budgetCat, budgetNote, custom, createdAt}]
      spaces: [],
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
      ['customTasks', 'notes', 'contacts', 'spaces'].forEach(function (key) {
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

  /* ---------- 空间需求 ↔ 派生任务同步 ----------
   * customTasks 中带 spaceId 的条目，即为某空间需求派生出的任务。
   * 已打卡（tasksDone[id] 为真）的派生任务，在被清理时转为普通自定义任务
   * （仅清除 spaceId 标记，保留任务本身与打卡记录），避免丢失历史进度。
   */

  // 同步某空间的派生任务：清理旧派生（保留已打卡的为普通任务）后按 space.tasks 重新派生
  function syncSpaceTasks(space) {
    var sid = space.id;
    var kept = [];
    var preservedKeys = {}; // 已保留的「stageId||text」，避免重新派生时重复
    state.customTasks.forEach(function (t) {
      if (t.spaceId === sid) {
        if (state.tasksDone[t.id]) {
          var c = {}; for (var k in t) c[k] = t[k];
          delete c.spaceId;
          kept.push(c);
          preservedKeys[c.stageId + '||' + c.text] = true;
        }
        // 未打卡的旧派生任务：丢弃（稍后重新派生）
      } else {
        kept.push(t);
      }
    });
    state.customTasks = kept;
    (space.tasks || []).forEach(function (tk) {
      var key = tk.stageId + '||' + tk.text;
      if (preservedKeys[key]) { return; } // 该任务已保留为普通任务，不重复
      state.customTasks.push({ id: uid('ct'), stageId: tk.stageId, text: tk.text, spaceId: sid, date: null });
    });
  }

  // 删除某空间的派生任务：未打卡的直接删除，已打卡的转为普通自定义任务
  function removeSpaceTasks(spaceId) {
    var kept = [];
    state.customTasks.forEach(function (t) {
      if (t.spaceId === spaceId) {
        if (state.tasksDone[t.id]) {
          var c = {}; for (var k in t) c[k] = t[k];
          delete c.spaceId;
          kept.push(c);
        }
      } else {
        kept.push(t);
      }
    });
    state.customTasks = kept;
  }

  return {
    get state() { return state; },
    get storageOk() { return storageOk; },
    save: save,
    uid: uid,
    exportJSON: exportJSON,
    importJSON: importJSON,
    reset: reset,
    syncSpaceTasks: syncSpaceTasks,
    removeSpaceTasks: removeSpaceTasks
  };
})();
