/* ============ 主程序：路由 / 阶段进度计算 / 首次设置向导 ============ */
window.Views = window.Views || {};

window.App = (function () {
  var current = { tab: 'home', param: null };
  var viewEl = null;

  /* ---------- 阶段与任务进度 ---------- */
  function stageTasks(stage) {
    var list = stage.tasks.slice();
    Store.state.customTasks.forEach(function (t) {
      if (t.stageId === stage.id) list.push({ id: t.id, text: t.text, custom: true });
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
  function go(tab, param) {
    current.tab = tab;
    current.param = param === undefined ? null : param;
    window.scrollTo(0, 0);
    render();
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
      if (action === 'nav') { go(el.dataset.target, el.dataset.param || null); return; }
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
      budget: null
    };
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
        html += '<div class="wizard-step-num">第 1 步 / 共 3 步</div>' +
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
        html += '<div class="wizard-step-num">第 2 步 / 共 3 步</div>' +
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
        html += '<div class="wizard-step-num">第 3 步 / 共 3 步</div>' +
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
        } else {
          var b = parseFloat(mask.querySelector('#wz-budget').value);
          if (!b || b <= 0) { UI.toast('请填写总预算'); return; }
          w.budget = b;
          finish();
        }
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
      Store.save();
      mask.remove();
      go('home');
      UI.toast(hadProfile ? '设置已更新 ✅' : '专属装修计划已生成 🎉');
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
    init: init
  };
})();

App.init();
