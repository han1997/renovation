/* ============ 主程序：路由 / 阶段进度计算 / 首次设置向导 ============ */
window.Views = window.Views || {};

window.App = (function () {
  var current = { tab: 'home', param: null };
  var viewEl = null;

  /* ---------- 阶段与任务进度 ---------- */
  function stageTasks(stage) {
    var list = stage.tasks.slice();
    Store.state.customTasks.forEach(function (t) {
      if (t.stageId === stage.id) list.push({ id: t.id, text: t.text, custom: true, spaceId: t.spaceId || null });
    });
    return list;
  }

  function stageProgress(stage) {
    var tasks = stageTasks(stage);
    var done = 0;
    tasks.forEach(function (t) { if (Store.state.tasksDone[t.id]) done++; });
    var overridden = Store.state.stageOverride[stage.id] === 'done';
    var isDone = overridden || (tasks.length > 0 && done === tasks.length);
    return {
      total: tasks.length,
      done: overridden ? tasks.length : done,
      pct: tasks.length ? Math.round((overridden ? tasks.length : done) / tasks.length * 100) : 0,
      isDone: isDone,
      overridden: overridden
    };
  }

  function currentStage() {
    var stages = DATA.stages;
    for (var i = 0; i < stages.length; i++) {
      if (!stageProgress(stages[i]).isDone) {
        return { stage: stages[i], index: i };
      }
    }
    return { stage: stages[stages.length - 1], index: stages.length - 1, allDone: true };
  }

  function allProgress() {
    var total = 0, done = 0;
    DATA.stages.forEach(function (s) {
      var p = stageProgress(s);
      total += p.total; done += p.done;
    });
    return { total: total, done: done, pct: total ? Math.round(done / total * 100) : 0 };
  }

  /* ---------- 路由 ---------- */
  function go(tab, param, opts) {
    opts = opts || {};
    var sameTab = current.tab === tab;
    var hasParam = param !== undefined && param !== null && param !== '';
    var shouldScrollTop = opts.scrollTop === true ||
      (opts.scrollTop !== false && !opts.preserveScroll && !sameTab && !hasParam);
    current.tab = tab;
    current.param = param === undefined ? null : param;
    render();
    if (shouldScrollTop) window.scrollTo(0, 0);
  }

  function rerender() {
    var y = window.scrollY;
    render();
    window.scrollTo(0, y);
  }

  function render() {
    document.querySelectorAll('#tabbar .tab').forEach(function (b) {
      b.classList.toggle('active', b.dataset.tab === current.tab);
    });
    updateTopbar();
    var v = Views[current.tab];
    if (v) v.render(viewEl, current.param);
  }

  function updateTopbar() {
    var sub = document.getElementById('topbar-sub');
    var p = Store.state.profile;
    if (!p) { sub.textContent = '新房装修全流程助手'; return; }
    var cs = currentStage();
    var txt = '';
    if (p.startDate) {
      var diff = UI.daysFromToday(p.startDate);
      if (diff > 0) txt = '距开工还有 ' + diff + ' 天 · ';
      else txt = '开工第 ' + (1 - diff) + ' 天 · ';
    }
    txt += (cs.allDone ? '全部完成 🎉' : '当前：' + cs.stage.name);
    sub.textContent = txt;
  }

  /* ---------- 全局事件代理 ---------- */
  function bindEvents() {
    document.addEventListener('click', function (e) {
      var tabBtn = e.target.closest('[data-tab]');
      if (tabBtn) { go(tabBtn.dataset.tab); return; }
      var el = e.target.closest('[data-action]');
      if (!el) return;
      var action = el.dataset.action;
      if (action === 'go-settings') { go('more'); return; }
      if (action === 'nav') {
        go(el.dataset.target, el.dataset.param || null, { scrollTop: !el.dataset.param });
        return;
      }
      if (action === 'start-wizard') { showWizard(); return; }
      var v = Views[current.tab];
      if (v && v.onAction) v.onAction(action, el, e);
    });
    document.addEventListener('change', function (e) {
      var el = e.target.closest('[data-change]');
      if (!el) return;
      var v = Views[current.tab];
      if (v && v.onChange) v.onChange(el.dataset.change, el, e);
    });
  }

  /* ---------- 空间需求：自定义表单 HTML（向导与「我的」页共用） ---------- */
  function spaceFormHTML(sp) {
    var body = '<div class="field"><label>名称 <span style="color:var(--red)">*</span></label>' +
      '<input id="sp-name" type="text" placeholder="例如：电竞房 / 茶室"' +
      (sp ? ' value="' + UI.esc(sp.name) + '"' : '') + '></div>' +
      '<div class="field"><label>关联阶段 <span style="color:var(--red)">*</span>（至少选 1 个）</label>' +
      '<div class="space-stage-list">';
    DATA.stages.forEach(function (st) {
      var checked = sp && sp.stageIds && sp.stageIds.indexOf(st.id) !== -1;
      body += '<label class="check-line"><input type="checkbox" data-stage="' + st.id + '"' + (checked ? ' checked' : '') + '> ' +
        st.emoji + ' ' + UI.esc(st.name) + '</label>';
    });
    body += '</div></div>' +
      '<div class="field"><label>预算分类（可选）</label><select id="sp-budgetcat"><option value="">不指定</option>' +
      PRICES.rates.map(function (r) {
        return '<option value="' + r.id + '"' + (sp && sp.budgetCat === r.id ? ' selected' : '') + '>' + r.emoji + ' ' + UI.esc(r.name) + '</option>';
      }).join('') +
      '</select></div>' +
      '<div class="field"><label>预算影响提示（可选）</label>' +
      '<input id="sp-budgetnote" type="text" placeholder="例如：约 5000–15000 元"' +
      (sp && sp.budgetNote ? ' value="' + UI.esc(sp.budgetNote) + '"' : '') + '></div>';
    return body;
  }

  /* 读取自定义空间表单中的勾选阶段（向导与「我的」页共用） */
  function readSpaceFormStages(box) {
    var stageIds = [];
    box.querySelectorAll('[data-stage]').forEach(function (cb) { if (cb.checked) stageIds.push(cb.dataset.stage); });
    return stageIds;
  }

  /* 按名称 + 阶段生成派生任务文案 */
  function buildSpaceTasks(name, stageIds) {
    return stageIds.map(function (sid) {
      var st = null; DATA.stages.forEach(function (x) { if (x.id === sid) st = x; });
      return { stageId: sid, text: name + '（' + (st ? st.name : sid) + '）' };
    });
  }

  /* ---------- 首次设置向导 ---------- */
  function showWizard() {
    var p = Store.state.profile || {};
    var w = {
      step: 1,
      area: p.area || '',
      tier: p.tier || 't2',
      mode: p.mode || 'half',
      grade: p.grade || 'mid',
      startDate: p.startDate || '',
      budget: null,
      selectedPresetIds: [],   // 向导中勾选的预设空间需求 id
      customSpaces: []         // 向导中临时新增的自定义空间需求（{name, stageIds, budgetCat, budgetNote}）
    };
    // 编辑场景：把已选过的预设预设空间回填为已勾选
    if (p && Store.state.spaces) {
      Store.state.spaces.forEach(function (sp) {
        if (!sp.custom && sp.presetId && w.selectedPresetIds.indexOf(sp.presetId) === -1) {
          w.selectedPresetIds.push(sp.presetId);
        }
      });
    }
    var mask = document.createElement('div');
    mask.className = 'wizard-mask';
    document.getElementById('modal-root').appendChild(mask);

    function tierOptions() {
      return PRICES.tiers.map(function (t) {
        return '<option value="' + t.id + '"' + (w.tier === t.id ? ' selected' : '') + '>' + t.name + '</option>';
      }).join('');
    }

    function renderStep() {
      var html = '<div class="wizard">';
      if (w.step === 1) {
        html += '<div class="wizard-step-num">第 1 步 / 共 4 步</div>' +
          '<h2>你好呀 👋 先认识一下你的新家</h2>' +
          '<p class="lead">三个小问题，我就能帮你生成专属的装修流程和预算方案。所有信息之后都能改。</p>' +
          '<div class="field"><label>建筑面积（㎡）<span style="color:var(--red)">*</span></label>' +
          '<input id="wz-area" type="number" inputmode="decimal" value="' + UI.esc(w.area) + '" placeholder="例如 98"></div>' +
          '<div class="field"><label>所在城市</label><select id="wz-tier">' + tierOptions() + '</select>' +
          '<div class="hint">用来校准人工与材料的参考价格</div></div>' +
          '<div class="field"><label>预计开工日期（可不填）</label>' +
          '<input id="wz-date" type="date" value="' + UI.esc(w.startDate) + '"></div>' +
          '<div class="wizard-nav"><button class="btn btn-primary" id="wz-next">下一步</button></div>' +
          '<div class="wizard-skip"><button class="link-btn" id="wz-skip">先跳过，用默认设置逛逛 →</button></div>';
      } else if (w.step === 2) {
        html += '<div class="wizard-step-num">第 2 步 / 共 4 步</div>' +
          '<h2>打算怎么装？</h2>' +
          '<p class="lead">四种常见方式，不确定就选「半包」——这是大多数新手的选择，之后可以改。</p>';
        DATA.modes.forEach(function (m) {
          html += '<div class="opt-card' + (w.mode === m.id ? ' selected' : '') + '" data-mode="' + m.id + '">' +
            '<div class="o-name">' + m.emoji + ' ' + UI.esc(m.name) + '<span class="o-price">' + UI.esc(m.priceShort) + '</span></div>' +
            '<div class="o-desc">' + UI.esc(m.short) + '</div></div>';
        });
        html += '<div class="wizard-nav"><button class="btn" id="wz-back">上一步</button>' +
          '<button class="btn btn-primary" id="wz-next">下一步</button></div>';
      } else if (w.step === 3) {
        html += '<div class="wizard-step-num">第 3 步 / 共 4 步</div>' +
          '<h2>想要哪些空间？</h2>' +
          '<p class="lead">勾选你想要的空间，我会把对应任务自动加到流程里。不勾也行，之后在「我的」里随时加。</p>';
        DATA.spaceNeeds.forEach(function (n) {
          var sel = w.selectedPresetIds.indexOf(n.id) !== -1;
          html += '<div class="opt-card' + (sel ? ' selected' : '') + '" data-space="' + n.id + '">' +
            '<div class="o-name">' + n.emoji + ' ' + UI.esc(n.name) + '</div>' +
            '<div class="o-desc">' + UI.esc(n.desc) + '</div></div>';
        });
        html += '<button class="btn btn-ghost btn-block" id="wz-custom-space">＋ 自定义空间需求</button>' +
          '<div class="hint">预设没覆盖你的需求？自定义一个，手动指定关联阶段。</div>' +
          '<div class="wizard-nav"><button class="btn" id="wz-back">上一步</button>' +
          '<button class="btn btn-primary" id="wz-next">下一步</button></div>';
      } else if (w.step === 4) {
        html += '<div class="wizard-step-num">第 4 步 / 共 4 步</div>' +
          '<h2>想装到什么档次？</h2>' +
          '<p class="lead">按 ' + w.area + '㎡ · ' + PRICES.tierName(w.tier) + '给你估个全屋总价（含家电家具）。</p>';
        PRICES.grades.forEach(function (g) {
          var est = PRICES.estimate({ area: w.area, tier: w.tier, mode: w.mode, grade: g.id });
          html += '<div class="opt-card' + (w.grade === g.id ? ' selected' : '') + '" data-grade="' + g.id + '">' +
            '<div class="o-name">' + g.emoji + ' ' + UI.esc(g.name) + '<span class="o-price">约 ' + UI.money(est.total) + '</span></div>' +
            '<div class="o-desc">' + UI.esc(g.desc) + '</div></div>';
        });
        var chosen = PRICES.estimate({ area: w.area, tier: w.tier, mode: w.mode, grade: w.grade });
        html += '<div class="field" style="margin-top:16px"><label>我的总预算（元）</label>' +
          '<input id="wz-budget" type="number" inputmode="decimal" value="' + (w.budget || chosen.total) + '">' +
          '<div class="hint">默认按所选档次估算，可直接改成你心里的数字</div></div>' +
          '<div class="wizard-nav"><button class="btn" id="wz-back">上一步</button>' +
          '<button class="btn btn-primary" id="wz-next">生成我的装修计划 ✨</button></div>';
      }
      html += '</div>';
      mask.innerHTML = html;
      bindStep();
      mask.scrollTop = 0;
    }

    function bindStep() {
      var next = mask.querySelector('#wz-next');
      var back = mask.querySelector('#wz-back');
      var skip = mask.querySelector('#wz-skip');
      mask.querySelectorAll('[data-mode]').forEach(function (el) {
        el.addEventListener('click', function () { w.mode = el.dataset.mode; renderStep(); });
      });
      mask.querySelectorAll('[data-grade]').forEach(function (el) {
        el.addEventListener('click', function () {
          w.grade = el.dataset.grade; w.budget = null; renderStep();
        });
      });
      // 空间需求勾选（多选切换）
      mask.querySelectorAll('[data-space]').forEach(function (el) {
        el.addEventListener('click', function () {
          var pid = el.dataset.space;
          var i = w.selectedPresetIds.indexOf(pid);
          if (i === -1) w.selectedPresetIds.push(pid);
          else w.selectedPresetIds.splice(i, 1);
          renderStep();
        });
      });
      // 自定义空间需求
      var customBtn = mask.querySelector('#wz-custom-space');
      if (customBtn) customBtn.addEventListener('click', function () { openWizardCustomSpace(); });
      if (back) back.addEventListener('click', function () { w.step--; renderStep(); });
      if (skip) skip.addEventListener('click', function () {
        w.area = w.area || 100;
        finish();
      });
      if (next) next.addEventListener('click', function () {
        if (w.step === 1) {
          var area = parseFloat(mask.querySelector('#wz-area').value);
          if (!area || area < 15 || area > 2000) { UI.toast('请填写有效的建筑面积（15–2000㎡）'); return; }
          w.area = area;
          w.tier = mask.querySelector('#wz-tier').value;
          w.startDate = mask.querySelector('#wz-date').value || '';
          w.step = 2; renderStep();
        } else if (w.step === 2) {
          w.step = 3; renderStep();
        } else if (w.step === 3) {
          w.step = 4; renderStep();
        } else {
          var b = parseFloat(mask.querySelector('#wz-budget').value);
          if (!b || b <= 0) { UI.toast('请填写总预算'); return; }
          w.budget = b;
          finish();
        }
      });
    }

    /* 向导内：自定义空间需求弹窗（与「我的」页共用同样的表单结构） */
    function openWizardCustomSpace() {
      var body = App.spaceFormHTML(null);
      var m = UI.modal({
        title: '＋ 自定义空间需求',
        body: body,
        actions: [
          { label: '取消' },
          { label: '添加', cls: 'btn-primary', onClick: function (close) {
            var name = m.el.querySelector('#sp-name').value.trim();
            var stageIds = App.readSpaceFormStages(m.el);
            if (!name) { UI.toast('请填写名称'); return; }
            if (!stageIds.length) { UI.toast('至少选一个关联阶段'); return; }
            var budgetCat = m.el.querySelector('#sp-budgetcat').value;
            var budgetNote = m.el.querySelector('#sp-budgetnote').value.trim();
            w.customSpaces.push({
              name: name, stageIds: stageIds, budgetCat: budgetCat, budgetNote: budgetNote
            });
            close();
            UI.toast('已加入本次清单，点「下一步」继续');
          }}
        ]
      });
    }

    function finish() {
      var s = Store.state;
      var hadProfile = !!s.profile;
      var est = PRICES.estimate({ area: w.area, tier: w.tier, mode: w.mode, grade: w.grade });
      s.profile = {
        area: w.area, tier: w.tier, mode: w.mode, grade: w.grade,
        startDate: w.startDate || null,
        totalBudget: w.budget || est.total,
        styleId: (s.profile && s.profile.styleId) || null,
        createdAt: (s.profile && s.profile.createdAt) || UI.today()
      };
      // 生成 / 更新预算分类（保留已有支出）
      var tpl = PRICES.budgetTemplate(s.profile);
      if (!hadProfile || !s.budget.categories.length) {
        s.budget.categories = tpl;
      } else {
        // 已有分类：按 id 更新推荐金额，新增缺失的，保留自定义分类
        tpl.forEach(function (c) {
          var exist = null;
          s.budget.categories.forEach(function (e) { if (e.id === c.id) exist = e; });
          if (exist) exist.planned = c.planned;
          else s.budget.categories.push(c);
        });
      }
      // 同步空间需求：保留已有自定义空间；预设按本次勾选增删；向导内新增的自定义一并写入
      reconcileSpaces(s, hadProfile);
      Store.save();
      mask.remove();
      go('home');
      UI.toast(hadProfile ? '设置已更新 ✅' : '专属装修计划已生成 🎉');
    }

    /* 把向导的空间勾选结果回写到 Store.state.spaces，并同步派生任务。
     * - 已有自定义空间（custom:true）一律保留，向导不管理它们。
     * - 预设：本次未勾选的，删除其派生任务（已打卡转普通任务）并移出列表；
     *         本次新勾选的，新增并派生任务；原本就有的，原样保留。
     * - 向导内新增的自定义空间：追加写入并派生任务。
     */
    function reconcileSpaces(s, hadProfile) {
      var next = [];
      // 保留所有已有自定义空间
      s.spaces.forEach(function (sp) {
        if (sp.custom) next.push(sp);
      });
      // 处理预设：未勾选的清理任务，已勾选的原样保留
      var keptPresetIds = {};
      s.spaces.forEach(function (sp) {
        if (!sp.custom && sp.presetId && w.selectedPresetIds.indexOf(sp.presetId) !== -1) {
          next.push(sp);
          keptPresetIds[sp.presetId] = true;
        } else if (!sp.custom && sp.presetId && w.selectedPresetIds.indexOf(sp.presetId) === -1) {
          Store.removeSpaceTasks(sp.id);
        }
      });
      // 新勾选的预设：新增并派生
      w.selectedPresetIds.forEach(function (pid) {
        if (keptPresetIds[pid]) return;
        var p = null;
        DATA.spaceNeeds.forEach(function (x) { if (x.id === pid) p = x; });
        if (!p) return;
        var sp = {
          id: Store.uid('sp'), presetId: p.id, name: p.name, emoji: p.emoji, desc: p.desc || '',
          stageIds: p.stageIds.slice(),
          tasks: p.tasks.map(function (tk) { return { stageId: tk.stageId, text: tk.text }; }),
          budgetCat: p.budgetCat, budgetNote: p.budgetNote || '',
          custom: false, createdAt: UI.today()
        };
        next.push(sp);
        Store.syncSpaceTasks(sp);
      });
      // 向导内新增的自定义空间
      w.customSpaces.forEach(function (c) {
        var sp = {
          id: Store.uid('sp'), presetId: null, name: c.name, emoji: '🪟', desc: '',
          stageIds: c.stageIds.slice(), tasks: buildSpaceTasks(c.name, c.stageIds),
          budgetCat: c.budgetCat || '', budgetNote: c.budgetNote || '',
          custom: true, createdAt: UI.today()
        };
        next.push(sp);
        Store.syncSpaceTasks(sp);
      });
      s.spaces = next;
    }

    renderStep();
  }

  /* ---------- 启动 ---------- */
  function init() {
    viewEl = document.getElementById('view');
    bindEvents();
    go('home');
    if (!Store.state.profile) {
      showWizard();
    }
    if (!Store.storageOk) {
      setTimeout(function () {
        UI.toast('⚠️ 当前浏览器无法保存数据（可能是隐私模式），记录将在关闭后丢失', 4200);
      }, 800);
    }
  }

  return {
    get current() { return current; },
    go: go,
    rerender: rerender,
    stageTasks: stageTasks,
    stageProgress: stageProgress,
    currentStage: currentStage,
    allProgress: allProgress,
    showWizard: showWizard,
    spaceFormHTML: spaceFormHTML,
    readSpaceFormStages: readSpaceFormStages,
    buildSpaceTasks: buildSpaceTasks,
    init: init
  };
})();

App.init();
