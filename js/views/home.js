/* ============ 首页视图：概览 / 提醒 / 快捷入口 ============ */
window.Views = window.Views || {};

Views.home = (function () {

  function render(el) {
    var s = Store.state;
    if (!s.profile) {
      el.innerHTML =
        '<div class="card center" style="padding:36px 20px">' +
        '<div style="font-size:44px">🏠</div>' +
        '<h2 style="margin:10px 0 6px">欢迎使用装修管家</h2>' +
        '<p class="muted small" style="margin-bottom:18px">从收房到入住，一步一步带你把新家装好。<br>先花 1 分钟告诉我你家的情况吧。</p>' +
        '<button class="btn btn-primary" data-action="start-wizard">开始设置 →</button>' +
        '</div>' + quickGrid();
      return;
    }

    var cs = App.currentStage();
    var ap = App.allProgress();
    var html = '';

    /* ---- 进度卡 ---- */
    var dayLine = '';
    if (s.profile.startDate) {
      var diff = UI.daysFromToday(s.profile.startDate);
      dayLine = diff > 0 ? '距开工还有 <b>' + diff + '</b> 天' : '开工第 <b>' + (1 - diff) + '</b> 天';
    } else {
      dayLine = '还没设置开工日期';
    }
    html += '<div class="card">' +
      '<div class="between"><div class="small muted">' + dayLine + '</div>' +
      '<span class="badge ' + (cs.allDone ? 'badge-ok' : 'badge-warn') + '">' +
      (cs.allDone ? '全部完成 🎉' : '第 ' + (cs.index + 1) + ' / ' + DATA.stages.length + ' 阶段') + '</span></div>' +
      '<div style="font-size:20px;font-weight:800;margin:6px 0 2px">' +
      (cs.allDone ? '恭喜！装修流程全部走完' : cs.stage.emoji + ' ' + UI.esc(cs.stage.name)) + '</div>' +
      '<div class="small muted mb8">' + UI.esc(cs.allDone ? '记得通风检测合格再入住哦' : cs.stage.goal) + '</div>' +
      UI.barHTML(ap.pct, { cls: 'ok' }) +
      '<div class="between mt8"><span class="tiny muted">总进度 ' + ap.done + ' / ' + ap.total + ' 项任务</span>' +
      '<button class="btn btn-sm btn-ghost" data-action="nav" data-target="stages" data-param="' + cs.stage.id + '">继续打卡 →</button></div>' +
      '</div>';

    /* ---- 行动中心 ---- */
    html += actionCenter(cs);

    /* ---- 预算卡 ---- */
    var spent = 0;
    s.budget.expenses.forEach(function (e) { spent += e.amount || 0; });
    var total = s.profile.totalBudget || 0;
    var pct = total ? Math.round(spent / total * 100) : 0;
    var left = total - spent;
    html += '<div class="card"><div class="card-title">💰 预算<span class="right tiny muted">' + pct + '% 已使用</span></div>' +
      '<div class="row" style="gap:16px">' +
      UI.donutHTML(pct, pct + '%', '已使用', pct > 100 ? 'var(--red)' : pct > 85 ? 'var(--amber)' : 'var(--green)') +
      '<div style="flex:1">' +
      '<div class="between small"><span class="muted">总预算</span><b>' + UI.money(total) + '</b></div>' +
      '<div class="between small mt4"><span class="muted">已花费</span><b>' + UI.money(spent) + '</b></div>' +
      '<div class="between small mt4"><span class="muted">剩余</span><b style="color:' + (left < 0 ? 'var(--red)' : 'var(--green)') + '">' + UI.money(left) + '</b></div>' +
      '</div></div>' +
      (pct > 100 ? '<div class="small mt8" style="color:var(--red)">⚠️ 已超支 ' + UI.money(spent - total) + '，去预算页看看哪个分类超了</div>' :
        pct > 85 ? '<div class="small mt8" style="color:var(--amber)">⚠️ 预算已使用超过 85%，后面花钱悠着点</div>' : '') +
      '<div class="row mt12"><button class="btn btn-sm btn-primary" data-action="quick-expense">＋ 记一笔</button>' +
      '<button class="btn btn-sm" data-action="nav" data-target="budget">查看预算 →</button></div>' +
      '</div>';

    /* ---- 轻量风险条 ---- */
    html += riskBar(cs);

    /* ---- 当前阶段避坑 ---- */
    if (!cs.allDone && cs.stage.warnings && cs.stage.warnings.length) {
      html += '<div class="card"><div class="card-title">⚠️ 现阶段最容易踩的坑</div><ul class="warn-list small">';
      cs.stage.warnings.slice(0, 3).forEach(function (wtext) {
        html += '<li>' + UI.esc(wtext) + '</li>';
      });
      html += '</ul><button class="btn btn-sm btn-ghost mt4" data-action="nav" data-target="guide" data-param="tips">更多避坑指南 →</button></div>';
    }

    html += quickGrid();
    el.innerHTML = html;
  }

  function collectDatedTasks() {
    var s = Store.state;
    var items = [];
    DATA.stages.forEach(function (st) {
      App.stageTasks(st).forEach(function (t) {
        var d = s.taskDates[t.id];
        if (d && !s.tasksDone[t.id]) {
          var diff = UI.daysFromToday(d);
          if (diff !== null && diff <= 7) items.push({ stage: st, task: t, date: d, diff: diff });
        }
      });
    });
    items.sort(function (a, b) { return a.diff - b.diff; });
    return items;
  }

  function hasIncompleteDatedTasks() {
    var s = Store.state;
    var found = false;
    DATA.stages.some(function (st) {
      return App.stageTasks(st).some(function (t) {
        if (s.tasksDone[t.id] || !s.taskDates[t.id]) return false;
        found = true;
        return true;
      });
    });
    return found;
  }

  function actionItemHTML(it, showDate) {
    var s = Store.state;
    var date = '';
    if (showDate && it.date) {
      var label = it.diff < 0 ? '逾期 ' + (-it.diff) + ' 天' : (it.diff === 0 ? '今天' : (it.diff === 1 ? '明天' : UI.dateCN(it.date)));
      date = '<span class="date-chip' + (it.diff <= 0 ? ' overdue' : '') + '">' + label + '</span>';
    }
    return '<div class="task-item action-task" data-action="nav" data-target="stages" data-param="' + UI.esc(it.stage.id) + '">' +
      '<label style="display:flex;align-items:flex-start;cursor:pointer;padding-top:2px">' +
      '<input type="checkbox" data-change="task" data-id="' + UI.esc(it.task.id) + '"' + (s.tasksDone[it.task.id] ? ' checked' : '') + '>' +
      '</label>' +
      '<span class="task-text" data-action="nav" data-target="stages" data-param="' + UI.esc(it.stage.id) + '">' + UI.esc(it.task.text) + '<div class="tiny muted">' + it.stage.emoji + ' ' + UI.esc(it.stage.name) + '</div></span>' + date + '</div>';
  }

  function actionGroup(title, items) {
    if (!items.length) return '';
    var html = '<div class="action-group"><div class="tiny muted action-group-title">' + title + '</div>';
    items.forEach(function (it) { html += actionItemHTML(it, true); });
    return html + '</div>';
  }

  function actionCenter(cs) {
    var s = Store.state;
    var items = collectDatedTasks();
    var html = '<div class="card action-center"><div class="between"><div class="card-title" style="margin-bottom:0">🎯 今天要做什么</div>' +
      '<span class="tiny muted">勾选即完成</span></div>';
    if (items.length) {
      var overdue = items.filter(function (it) { return it.diff < 0; });
      var today = items.filter(function (it) { return it.diff === 0; });
      var upcoming = items.filter(function (it) { return it.diff > 0; });
      var shownCount = Math.min(overdue.length, 3) + Math.min(today.length, 3) + Math.min(upcoming.length, 3);
      html += actionGroup('逾期', overdue.slice(0, 3));
      html += actionGroup('今天', today.slice(0, 3));
      html += actionGroup('未来 7 天', upcoming.slice(0, 3));
      if (items.length > shownCount) html += '<div class="tiny muted center mt4">还有 ' + (items.length - shownCount) + ' 项近期任务，去流程页查看</div>';
    } else if (!cs.allDone && !hasIncompleteDatedTasks()) {
      var next = [];
      App.stageTasks(cs.stage).forEach(function (t) {
        if (!s.tasksDone[t.id] && next.length < 3) next.push({ stage: cs.stage, task: t });
      });
      if (next.length) {
        html += '<div class="small muted mt8">当前阶段还没有安排日期，先从下面三项开始：</div>';
        next.forEach(function (it) { html += actionItemHTML(it, false); });
        html += '<div class="tiny muted mt4">可进入流程页给任务设置日期，首页会按紧急程度提醒</div>';
      } else {
        html += '<div class="empty"><span class="e-icon">🎉</span>当前阶段暂无未完成任务</div>';
      }
    } else if (!cs.allDone) {
      html += '<div class="empty"><span class="e-icon">📅</span>未来 7 天暂无安排，先去流程页查看后续计划</div>';
    } else {
      html += '<div class="empty"><span class="e-icon">🎉</span>所有任务都完成了，休息一下吧</div>';
    }
    return html + '</div>';
  }

  function riskBar(cs) {
    var s = Store.state;
    var worst = null;
    s.budget.categories.forEach(function (c) {
      var spent = 0;
      s.budget.expenses.forEach(function (e) { if (e.catId === c.id) spent += e.amount || 0; });
      var over = spent - (c.planned || 0);
      if (over > 0 && (!worst || over > worst.over)) worst = { category: c, over: over };
    });
    var html = '';
    if (worst || (!cs.allDone && cs.stage.buy && cs.stage.buy.length)) {
      html = '<div class="risk-bar"><div class="tiny muted risk-label">需要留意</div>';
      if (worst) html += '<div class="risk-item risk-budget" data-action="nav" data-target="budget"><span>💸 预算超支：' + UI.esc(worst.category.name) + '</span><b>' + UI.money(worst.over) + '</b></div>';
      if (!cs.allDone && cs.stage.buy && cs.stage.buy.length) html += '<div class="risk-item risk-purchase" data-action="nav" data-target="guide" data-param="materials"><span>🛒 当前阶段采购：' + UI.esc(cs.stage.buy[0].item) + '</span><b>查看日历 →</b></div>';
      html += '</div>';
    }
    return html;
  }

  function quickGrid() {
    return '<div class="quick-grid" style="margin-bottom:12px">' +
      '<div class="quick" data-action="nav" data-target="guide" data-param="styles"><span class="q-icon">🎨</span><span class="q-label">风格测试</span></div>' +
      '<div class="quick" data-action="nav" data-target="guide" data-param="tips"><span class="q-icon">🕳️</span><span class="q-label">避坑指南</span></div>' +
      '<div class="quick" data-action="nav" data-target="guide" data-param="accept"><span class="q-icon">📋</span><span class="q-label">验收清单</span></div>' +
      '<div class="quick" data-action="nav" data-target="guide" data-param="materials"><span class="q-icon">🛒</span><span class="q-label">建材日历</span></div>' +
      '</div>';
  }

  function onAction(action) {
    if (action === 'quick-expense') {
      Views.budget.openExpenseForm(null, function () { App.rerender(); });
    }
  }

  function onChange(kind, el) {
    if (kind !== 'task') return;
    var id = el.dataset.id;
    if (el.checked) Store.state.tasksDone[id] = true;
    else delete Store.state.tasksDone[id];
    Store.save();
    App.rerender();
  }

  return { render: render, onAction: onAction, onChange: onChange };
})();
