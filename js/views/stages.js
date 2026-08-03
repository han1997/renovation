/* ============ 流程视图：阶段时间轴 / 任务打卡 ============ */
window.Views = window.Views || {};

Views.stages = (function () {
  var expandedId; // 当前展开的阶段（undefined = 尚未初始化）

  function render(el, param) {
    if (param) { expandedId = param; }
    if (expandedId === undefined) expandedId = App.currentStage().stage.id;

    var ap = App.allProgress();
    var cs = App.currentStage();
    var html = '<div class="card">' +
      '<div class="card-title">🧭 装修全流程<span class="right tiny muted">' + UI.esc(DATA.totalDurationNote) + '</span></div>' +
      UI.barHTML(ap.pct, { cls: 'ok' }) +
      '<div class="between mt8 tiny muted"><span>已完成 ' + ap.done + ' / ' + ap.total + ' 项</span><span>' + ap.pct + '%</span></div>' +
      '</div>';

    var lastPhase = null;
    DATA.stages.forEach(function (st, i) {
      if (st.phase !== lastPhase) {
        html += '<div class="phase-label">' + UI.esc(st.phase) + '</div>';
        lastPhase = st.phase;
      }
      html += stageHTML(st, i, cs);
    });

    el.innerHTML = html;
    // 展开当前项后滚动到可视范围（仅当由参数跳转）
    if (param) {
      var target = el.querySelector('[data-stage-anchor="' + param + '"]');
      if (target) setTimeout(function () { target.scrollIntoView({ block: 'start', behavior: 'smooth' }); }, 60);
    }
  }

  function stageHTML(st, i, cs) {
    var p = App.stageProgress(st);
    var isCurrent = cs.stage.id === st.id && !cs.allDone;
    var cls = p.isDone ? 'done' : (isCurrent ? 'doing' : '');
    var statusBadge = p.isDone ? '<span class="badge badge-ok">已完成</span>' :
      isCurrent ? '<span class="badge badge-warn">进行中</span>' :
        (p.done > 0 ? '<span class="badge badge-info">部分完成</span>' : '<span class="badge badge-plain">未开始</span>');
    var open = expandedId === st.id;

    var html = '<div class="t-item ' + cls + '" data-stage-anchor="' + st.id + '" style="scroll-margin-top:70px">' +
      '<div class="t-dot">' + (p.isDone ? '✓' : (i + 1)) + '</div>' +
      '<div class="card stage-card" style="margin-bottom:0">' +
      '<div class="stage-head" data-action="toggle-stage" data-id="' + st.id + '">' +
      '<span style="font-size:19px">' + st.emoji + '</span>' +
      '<div><div class="stage-name">' + UI.esc(st.name) + '</div>' +
      '<div class="tiny muted">' + UI.esc(st.duration) + ' · ' + p.done + '/' + p.total + ' 项</div></div>' +
      '<div class="stage-meta">' + statusBadge + '</div>' +
      '</div>';

    if (open) html += stageBody(st, p, isCurrent);
    html += '</div></div>';
    return html;
  }

  function stageBody(st, p, isCurrent) {
    var s = Store.state;
    var html = '<div class="stage-body">';
    html += '<div class="small muted mb8">🎯 ' + UI.esc(st.goal) + '</div>';

    // 任务清单
    App.stageTasks(st).forEach(function (t) {
      var done = !!s.tasksDone[t.id];
      var date = s.taskDates[t.id];
      var dateChip = '';
      if (date) {
        var diff = UI.daysFromToday(date);
        dateChip = '<span class="date-chip' + (!done && diff < 0 ? ' overdue' : '') + '">' + UI.dateCN(date) + '</span> ';
      }
      html += '<div class="task-item' + (done ? ' checked' : '') + '">' +
        '<label style="display:flex;gap:9px;flex:1;cursor:pointer;min-width:0">' +
        '<input type="checkbox" data-change="task" data-id="' + t.id + '"' + (done ? ' checked' : '') + '>' +
        '<span class="task-text">' + UI.esc(t.text) +
        (t.spaceId ? ' <span class="badge badge-plain tiny" title="来自空间需求">🪟</span>' : '') +
        (dateChip ? '<div class="mt4">' + dateChip + '</div>' : '') + '</span>' +
        '</label>' +
        '<span class="task-ops">' +
        (t.tip ? '<button data-action="task-tip" data-id="' + t.id + '" data-stage="' + st.id + '" title="小贴士">💡</button>' : '') +
        '<button data-action="task-date" data-id="' + t.id + '" title="设置日期">📅</button>' +
        (t.custom ? '<button data-action="del-task" data-id="' + t.id + '" title="删除">✕</button>' : '') +
        '</span></div>';
    });
    html += '<button class="btn btn-sm btn-ghost mt4" data-action="add-task" data-id="' + st.id + '">＋ 添加自己的任务</button>';

    // 避坑要点
    if (st.warnings && st.warnings.length) {
      html += '<details class="fold"' + (isCurrent ? ' open' : '') + '><summary>⚠️ 避坑要点（' + st.warnings.length + '）</summary><div class="fold-body"><ul class="warn-list">';
      st.warnings.forEach(function (wtext) { html += '<li>' + UI.esc(wtext) + '</li>'; });
      html += '</ul></div></details>';
    }

    // 本阶段要买/要订
    if (st.buy && st.buy.length) {
      html += '<details class="fold"><summary>🛒 本阶段要订 / 要买（' + st.buy.length + '）</summary><div class="fold-body"><table class="tbl">';
      st.buy.forEach(function (b) {
        html += '<tr><td style="white-space:nowrap;font-weight:600">' + UI.esc(b.item) + '</td><td class="muted">' + UI.esc(b.note) + '</td></tr>';
      });
      html += '</table></div></details>';
    }

    // 验收清单入口
    if (st.acceptIds && st.acceptIds.length) {
      html += '<div class="row wrap mt8">';
      st.acceptIds.forEach(function (cid) {
        var cl = null;
        DATA.checklists.forEach(function (c) { if (c.id === cid) cl = c; });
        if (cl) html += '<button class="btn btn-sm" data-action="nav" data-target="guide" data-param="accept:' + cid + '">📋 ' + UI.esc(cl.name) + '</button>';
      });
      html += '</div>';
    }

    // 整段完成 / 重新打开
    html += '<div class="mt12">';
    if (!p.isDone) {
      html += '<button class="btn btn-sm" data-action="stage-done" data-id="' + st.id + '">本阶段全部搞定，一键完成 ✓</button>';
    } else if (p.overridden) {
      html += '<button class="btn btn-sm" data-action="stage-reopen" data-id="' + st.id + '">重新打开本阶段</button>';
    }
    html += '</div></div>';
    return html;
  }

  function onChange(kind, el) {
    if (kind !== 'task') return;
    var id = el.dataset.id;
    var s = Store.state;
    if (el.checked) s.tasksDone[id] = true;
    else delete s.tasksDone[id];
    Store.save();
    // 检查是否刚好完成整个阶段
    if (el.checked) {
      DATA.stages.forEach(function (st) {
        var owns = App.stageTasks(st).some(function (t) { return t.id === id; });
        if (owns) {
          var p = App.stageProgress(st);
          if (p.isDone) UI.toast('🎉 「' + st.name + '」全部完成！');
        }
      });
    }
    App.rerender();
  }

  function findTask(taskId) {
    var found = null;
    DATA.stages.forEach(function (st) {
      App.stageTasks(st).forEach(function (t) { if (t.id === taskId) found = { stage: st, task: t }; });
    });
    return found;
  }

  function onAction(action, el) {
    var s = Store.state;
    var id = el.dataset.id;

    if (action === 'toggle-stage') {
      expandedId = (expandedId === id ? null : id);
      App.rerender();

    } else if (action === 'task-date') {
      var cur = s.taskDates[id] || '';
      UI.formModal({
        title: '📅 设置计划日期',
        fields: [{ key: 'date', label: '计划日期（清空则取消提醒）', type: 'date', value: cur, hint: '到期前 7 天会出现在首页「待办提醒」里' }],
        onSubmit: function (vals, close) {
          if (vals.date) s.taskDates[id] = vals.date;
          else delete s.taskDates[id];
          Store.save(); close(); App.rerender();
        }
      });

    } else if (action === 'task-tip') {
      var f = findTask(id);
      if (f && f.task.tip) {
        UI.modal({ title: '💡 小贴士', body: '<div style="font-size:14px;line-height:1.8">' + UI.esc(f.task.tip) + '</div>', actions: [{ label: '知道了', cls: 'btn-primary' }] });
      }

    } else if (action === 'add-task') {
      UI.formModal({
        title: '＋ 添加自定义任务',
        fields: [
          { key: 'text', label: '任务内容', type: 'text', required: true, placeholder: '例如：约瓷砖店周六量尺' },
          { key: 'date', label: '计划日期（可选）', type: 'date' }
        ],
        onSubmit: function (vals, close) {
          var tid = Store.uid('ct');
          s.customTasks.push({ id: tid, stageId: id, text: vals.text });
          if (vals.date) s.taskDates[tid] = vals.date;
          Store.save(); close(); App.rerender();
          UI.toast('已添加 ✅');
        }
      });

    } else if (action === 'del-task') {
      UI.confirmDlg('删除任务', '确定删除这条自定义任务吗？', function () {
        s.customTasks = s.customTasks.filter(function (t) { return t.id !== id; });
        delete s.tasksDone[id];
        delete s.taskDates[id];
        Store.save(); App.rerender();
      }, '删除');

    } else if (action === 'stage-done') {
      s.stageOverride[id] = 'done';
      Store.save(); App.rerender();
      UI.toast('🎉 阶段完成，继续加油！');

    } else if (action === 'stage-reopen') {
      delete s.stageOverride[id];
      Store.save(); App.rerender();
    }
  }

  return { render: render, onAction: onAction, onChange: onChange };
})();
