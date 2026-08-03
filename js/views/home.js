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

    /* ---- 待办提醒卡 ---- */
    html += remindersCard(cs);

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

  function remindersCard(cs) {
    var s = Store.state;
    var items = [];
    // 收集所有设置了日期、未完成的任务
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

    var html = '<div class="card"><div class="card-title">⏰ 待办提醒</div>';
    if (items.length) {
      items.slice(0, 6).forEach(function (it) {
        var chip;
        if (it.diff < 0) chip = '<span class="date-chip overdue">逾期 ' + (-it.diff) + ' 天</span>';
        else if (it.diff === 0) chip = '<span class="date-chip overdue">今天</span>';
        else chip = '<span class="date-chip">' + (it.diff === 1 ? '明天' : UI.dateCN(it.date)) + '</span>';
        html += '<div class="task-item" data-action="nav" data-target="stages" data-param="' + it.stage.id + '" style="cursor:pointer">' +
          '<span style="font-size:15px">' + it.stage.emoji + '</span>' +
          '<span class="task-text">' + UI.esc(it.task.text) + '<div class="tiny muted">' + UI.esc(it.stage.name) + '</div></span>' +
          chip + '</div>';
      });
      if (items.length > 6) html += '<div class="tiny muted center mt4">还有 ' + (items.length - 6) + ' 条，去流程页查看</div>';
    } else {
      // 没有带日期的待办：推荐当前阶段接下来要做的事
      var next = [];
      if (!cs.allDone) {
        App.stageTasks(cs.stage).forEach(function (t) {
          if (!s.tasksDone[t.id] && next.length < 3) next.push(t);
        });
      }
      if (next.length) {
        html += '<div class="tiny muted mb8">最近没有设置日期的待办，当前阶段接下来可以做：</div>';
        next.forEach(function (t) {
          html += '<div class="task-item" data-action="nav" data-target="stages" data-param="' + cs.stage.id + '" style="cursor:pointer">' +
            '<span>👉</span><span class="task-text">' + UI.esc(t.text) + '</span></div>';
        });
        html += '<div class="tiny muted mt4">小提示：在流程页给任务点 📅 设置日期，就会在这里提醒你</div>';
      } else {
        html += '<div class="empty"><span class="e-icon">🎉</span>暂无待办，休息一下吧</div>';
      }
    }
    html += '</div>';
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

  return { render: render, onAction: onAction };
})();
