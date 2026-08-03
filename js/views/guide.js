/* ============ 指南视图：避坑 / 验收 / 风格 / 建材日历 / 百科 ============ */
window.Views = window.Views || {};

Views.guide = (function () {
  var sub = 'tips';     // tips | accept | styles | materials | wiki
  var query = '';
  var topic = null;
  var acceptOpen = null;

  function render(el, param) {
    if (param) {
      if (param.indexOf('accept:') === 0) { sub = 'accept'; acceptOpen = param.slice(7); }
      else if (['tips', 'accept', 'styles', 'materials', 'wiki'].indexOf(param) >= 0) { sub = param; }
    }
    var segs = [
      ['tips', '🕳️ 避坑'], ['accept', '📋 验收'], ['styles', '🎨 风格'],
      ['materials', '🛒 买料'], ['wiki', '📖 百科']
    ];
    var html = '<div class="seg">';
    segs.forEach(function (sg) {
      html += '<button class="' + (sub === sg[0] ? 'active' : '') + '" data-action="gsub" data-id="' + sg[0] + '">' + sg[1] + '</button>';
    });
    html += '</div>';

    if (sub === 'tips') html += renderTips();
    else if (sub === 'accept') html += renderAccept();
    else if (sub === 'styles') html += renderStyles();
    else if (sub === 'materials') html += renderMaterials();
    else html += renderWiki();

    el.innerHTML = html;

    if (sub === 'tips') {
      var input = el.querySelector('#g-search');
      if (input) {
        input.addEventListener('input', function () {
          query = input.value.trim();
          var list = el.querySelector('#tip-list');
          if (list) list.innerHTML = tipListHTML();
        });
      }
    }
    if (sub === 'accept' && acceptOpen) {
      var anchor = el.querySelector('[data-cl-anchor="' + acceptOpen + '"]');
      if (anchor) setTimeout(function () { anchor.scrollIntoView({ block: 'start', behavior: 'smooth' }); }, 60);
      acceptOpen = null;
    }
  }

  /* ---------- 避坑指南 ---------- */
  function renderTips() {
    var html = '<input id="g-search" class="search-input" type="search" placeholder="🔍 搜坑：比如 防水、增项、板材…" value="' + UI.esc(query) + '">';
    html += '<div class="chips"><button class="chip' + (!topic ? ' active' : '') + '" data-action="tip-topic" data-id="">全部</button>';
    DATA.tipTopics.forEach(function (t) {
      html += '<button class="chip' + (topic === t ? ' active' : '') + '" data-action="tip-topic" data-id="' + UI.esc(t) + '">' + UI.esc(t) + '</button>';
    });
    html += '</div><div id="tip-list">' + tipListHTML() + '</div>';
    return html;
  }

  function tipListHTML() {
    var q = query.toLowerCase();
    var list = DATA.tips.filter(function (t) {
      if (topic && t.topic !== topic) return false;
      if (!q) return true;
      return (t.title + t.body + t.topic).toLowerCase().indexOf(q) >= 0;
    });
    if (!list.length) return '<div class="empty"><span class="e-icon">🔍</span>没找到相关内容，换个关键词试试</div>';
    var html = '<div class="tiny muted" style="margin:0 4px 8px">' + list.length + ' 条 · 红色=高危坑，黄色=重要，蓝色=提示</div>';
    list.forEach(function (t) {
      var badgeCls = t.level === '高危' ? 'badge-danger' : t.level === '重要' ? 'badge-warn' : 'badge-info';
      html += '<div class="card tip-card lv-' + t.level + '">' +
        '<div class="tip-title">' + UI.esc(t.title) + '<span class="badge ' + badgeCls + '">' + t.level + '</span></div>' +
        '<div class="tip-topic">#' + UI.esc(t.topic) + '</div>' +
        '<div class="tip-body">' + UI.esc(t.body).replace(/\n/g, '<br>') + '</div></div>';
    });
    return html;
  }

  /* ---------- 验收清单 ---------- */
  function renderAccept() {
    var s = Store.state;
    var html = '<div class="card" style="background:#fffdf6"><div class="small">' + UI.esc(DATA.acceptIntro).replace(/\n/g, '<br>') + '</div></div>';
    DATA.checklists.forEach(function (cl) {
      var done = 0;
      cl.items.forEach(function (it) { if (s.checks[it.id]) done++; });
      var allDone = done === cl.items.length;
      var shouldOpen = acceptOpen === cl.id;
      html += '<details class="fold" style="background:#fff;margin-bottom:10px" data-cl-anchor="' + cl.id + '"' + (shouldOpen ? ' open' : '') + ' style="scroll-margin-top:70px">' +
        '<summary>' + cl.emoji + ' ' + UI.esc(cl.name) +
        '<span class="badge ' + (allDone ? 'badge-ok' : 'badge-plain') + '" data-cl-badge="' + cl.id + '" style="margin-left:6px">' + done + '/' + cl.items.length + '</span></summary>' +
        '<div class="fold-body">';
      if (cl.note) html += '<div class="tiny muted mb8">' + UI.esc(cl.note) + '</div>';
      cl.items.forEach(function (it) {
        var c = !!s.checks[it.id];
        html += '<div class="task-item' + (c ? ' checked' : '') + '"><label style="display:flex;gap:9px;flex:1;cursor:pointer">' +
          '<input type="checkbox" data-change="check" data-id="' + it.id + '" data-cl="' + cl.id + '"' + (c ? ' checked' : '') + '>' +
          '<span class="task-text">' + UI.esc(it.text) + '</span></label></div>';
      });
      html += '<button class="btn btn-sm mt8" data-action="reset-checklist" data-id="' + cl.id + '">清空重验</button>' +
        '</div></details>';
    });
    return html;
  }

  /* ---------- 风格 ---------- */
  function renderStyles() {
    var s = Store.state;
    var chosenId = s.profile && s.profile.styleId;
    var quizResult = s.quiz && s.quiz.styleId ? styleById(s.quiz.styleId) : null;

    var html = '<div class="card">' +
      '<div class="card-title">🎯 还没想好装什么风格？</div>';
    if (quizResult) {
      html += '<div class="small">上次测试结果：<b>' + quizResult.emoji + ' ' + UI.esc(quizResult.name) + '</b>' +
        (chosenId ? '' : '，点下方卡片看详情') + '</div>';
    } else {
      html += '<div class="small muted">回答 6 个生活习惯问题，帮你测出最合适的风格。不用懂任何术语。</div>';
    }
    if (chosenId) {
      var cs2 = styleById(chosenId);
      if (cs2) html += '<div class="small mt4">已选定风格：<span class="badge badge-ok">' + cs2.emoji + ' ' + UI.esc(cs2.name) + '</span></div>';
    }
    html += '<button class="btn btn-sm btn-primary mt8" data-action="quiz-start">' + (quizResult ? '重新测一次' : '开始测试（约 1 分钟）') + '</button></div>';

    html += '<div class="tiny muted" style="margin:0 4px 8px">点卡片查看风格详情、造价水平和翻车预警 👇</div>';
    html += '<div class="style-grid">';
    DATA.styles.forEach(function (st) {
      html += '<div class="style-card' + (chosenId === st.id ? ' chosen' : '') + '" data-action="style-detail" data-id="' + st.id + '">' +
        '<div class="s-name">' + st.emoji + ' ' + UI.esc(st.name) + '</div>' +
        '<div class="s-tag">' + UI.esc(st.tagline) + '</div>' +
        '<div class="between"><div class="palette">' +
        st.colors.map(function (c) { return '<span class="dot" style="background:' + c + '"></span>'; }).join('') +
        '</div><span class="tiny muted">💰' + UI.esc(st.cost) + '</span></div></div>';
    });
    html += '</div>';
    return html;
  }

  function styleById(id) {
    var f = null;
    DATA.styles.forEach(function (st) { if (st.id === id) f = st; });
    return f;
  }

  function chooseStyle(id) {
    var s = Store.state;
    if (!s.profile) { UI.toast('请先完成初始设置'); return; }
    s.profile.styleId = id;
    Store.save();
    var st = styleById(id);
    UI.toast('已选定「' + st.name + '」🎨');
    App.rerender();
  }

  function showStyleDetail(id) {
    var st = styleById(id);
    if (!st) return;
    var body = '<div class="s-tag" style="font-size:13px;color:var(--muted)">' + UI.esc(st.tagline) + '</div>' +
      '<div class="palette mt8">' + st.colors.map(function (c) { return '<span class="dot" style="width:22px;height:22px;background:' + c + '"></span>'; }).join('') + '</div>' +
      '<div class="small mt12">' + UI.esc(st.desc) + '</div>' +
      '<div class="small mt12"><b>💰 造价：' + UI.esc(st.cost) + '</b> · ' + UI.esc(st.costNote) + '</div>' +
      '<div class="small mt8"><b>👥 适合：</b>' + UI.esc(st.fit) + '</div>' +
      '<div class="small mt12"><b>🧩 关键元素</b></div><ul class="plain-list small">' +
      st.elements.map(function (x) { return '<li>' + UI.esc(x) + '</li>'; }).join('') + '</ul>' +
      '<div class="small mt8"><b>⚠️ 容易翻车的点</b></div><ul class="warn-list small">' +
      st.pitfalls.map(function (x) { return '<li>' + UI.esc(x) + '</li>'; }).join('') + '</ul>';
    UI.modal({
      title: st.emoji + ' ' + UI.esc(st.name),
      body: body,
      actions: [
        { label: '关闭' },
        { label: '就选它 ✓', cls: 'btn-primary', onClick: function (close) { chooseStyle(st.id); close(); } }
      ]
    });
  }

  /* ---------- 风格测试 ---------- */
  function startQuiz() {
    var qs = DATA.styleQuiz.questions;
    var qi = 0;
    var scores = {};
    var m = UI.modal({ title: '🎨 风格小测试', body: '<div id="quiz-box"></div>', dismissible: true });
    var box = m.el.querySelector('#quiz-box');

    function renderQ() {
      var q = qs[qi];
      var html = '<div class="tiny muted mb8">第 ' + (qi + 1) + ' / ' + qs.length + ' 题</div>' +
        '<div style="font-size:15.5px;font-weight:700;margin-bottom:12px">' + UI.esc(q.q) + '</div>';
      q.options.forEach(function (o, oi) {
        html += '<div class="opt-card" data-oi="' + oi + '"><div class="o-desc" style="color:var(--ink);font-size:14px">' + UI.esc(o.text) + '</div></div>';
      });
      box.innerHTML = html;
      box.querySelectorAll('.opt-card').forEach(function (card) {
        card.addEventListener('click', function () {
          var o = q.options[+card.dataset.oi];
          for (var k in o.scores) { scores[k] = (scores[k] || 0) + o.scores[k]; }
          qi++;
          if (qi < qs.length) renderQ();
          else showResult();
        });
      });
    }

    function showResult() {
      var bestId = null, best = -1;
      DATA.styles.forEach(function (st) {
        var v = scores[st.id] || 0;
        if (v > best) { best = v; bestId = st.id; }
      });
      var st = styleById(bestId);
      Store.state.quiz = { styleId: bestId, at: UI.today() };
      Store.save();
      box.innerHTML = '<div class="center" style="padding:8px 0 4px">' +
        '<div style="font-size:44px">' + st.emoji + '</div>' +
        '<div style="font-size:19px;font-weight:800;margin:6px 0">' + UI.esc(st.name) + '</div>' +
        '<div class="small muted">' + UI.esc(st.tagline) + '</div>' +
        '<div class="palette mt8" style="justify-content:center">' + st.colors.map(function (c) { return '<span class="dot" style="width:20px;height:20px;background:' + c + '"></span>'; }).join('') + '</div>' +
        '<div class="small mt12" style="text-align:left">' + UI.esc(st.desc) + '</div>' +
        '<div class="row mt12" style="gap:10px"><button class="btn" id="qz-detail" style="flex:1">看详情</button>' +
        '<button class="btn btn-primary" id="qz-choose" style="flex:1">就选它 ✓</button></div>' +
        '<button class="link-btn mt8" id="qz-close">先不选，再逛逛</button></div>';
      box.querySelector('#qz-detail').addEventListener('click', function () { m.close(); App.rerender(); showStyleDetail(bestId); });
      box.querySelector('#qz-choose').addEventListener('click', function () { chooseStyle(bestId); m.close(); });
      box.querySelector('#qz-close').addEventListener('click', function () { m.close(); App.rerender(); });
    }

    renderQ();
  }

  /* ---------- 建材购买日历 ---------- */
  function renderMaterials() {
    var html = '<div class="card" style="background:#fffdf6"><div class="small"><b>🛒 为什么要提前订？</b></div>' +
      '<div class="small muted mt4">定制类建材都有生产周期（柜子 30–60 天、门 30–45 天、封窗 15–30 天），等需要安装时才去买就要停工干等。照下面的节奏订货，工期才不会断。</div></div>';
    DATA.materialTimeline.forEach(function (p) {
      html += '<div class="card"><div class="card-title">' + p.emoji + ' ' + UI.esc(p.period) + '<span class="right tiny muted">' + UI.esc(p.when) + '</span></div>';
      if (p.note) html += '<div class="tiny muted mb8">' + UI.esc(p.note) + '</div>';
      html += '<table class="tbl"><tr><th>要订什么</th><th>周期/提前量</th></tr>';
      p.items.forEach(function (it) {
        html += '<tr><td><b>' + UI.esc(it.name) + '</b>' + (it.note ? '<div class="tiny muted">' + UI.esc(it.note) + '</div>' : '') + '</td>' +
          '<td class="num tiny">' + UI.esc(it.lead) + '</td></tr>';
      });
      html += '</table></div>';
    });
    return html;
  }

  /* ---------- 百科：装修方式 / 找谁装 / 名词解释 ---------- */
  function renderWiki() {
    var html = '<div class="card"><div class="card-title">📦 四种装修方式怎么选</div>';
    DATA.modes.forEach(function (mo) {
      html += '<details class="fold"><summary>' + mo.emoji + ' ' + UI.esc(mo.name) +
        '<span class="tiny muted" style="margin-left:6px;font-weight:400">' + UI.esc(mo.priceShort) + '</span></summary>' +
        '<div class="fold-body">' +
        '<div class="small mb8">' + UI.esc(mo.desc) + '</div>' +
        '<div class="small"><b style="color:var(--green)">✓ 优点</b></div><ul class="plain-list small">' +
        mo.pros.map(function (x) { return '<li>' + UI.esc(x) + '</li>'; }).join('') + '</ul>' +
        '<div class="small"><b style="color:var(--red)">✗ 缺点</b></div><ul class="plain-list small">' +
        mo.cons.map(function (x) { return '<li>' + UI.esc(x) + '</li>'; }).join('') + '</ul>' +
        '<div class="small mt4"><b>👥 适合：</b>' + UI.esc(mo.fit) + '</div>' +
        '</div></details>';
    });
    html += '</div>';

    html += '<div class="card"><div class="card-title">🤝 找谁来装</div>';
    DATA.whoBuilds.forEach(function (wv) {
      html += '<details class="fold"><summary>' + wv.emoji + ' ' + UI.esc(wv.name) + '</summary><div class="fold-body">' +
        '<div class="small"><b style="color:var(--green)">✓</b> ' + wv.pros.map(UI.esc).join('；') + '</div>' +
        '<div class="small mt4"><b style="color:var(--red)">✗</b> ' + wv.cons.map(UI.esc).join('；') + '</div>' +
        '<div class="small mt4"><b>适合：</b>' + UI.esc(wv.fit) + '</div></div></details>';
    });
    html += '</div>';

    html += '<div class="card"><div class="card-title">📖 装修黑话词典</div><div class="tiny muted mb8">商家嘴里蹦出来的词，这里都能查到。</div>';
    DATA.glossary.forEach(function (g) {
      html += '<details class="fold"><summary style="font-weight:600">' + UI.esc(g.term) + '</summary>' +
        '<div class="fold-body muted">' + UI.esc(g.def) + '</div></details>';
    });
    html += '</div>';
    return html;
  }

  /* ---------- 事件 ---------- */
  function onChange(kind, el) {
    if (kind !== 'check') return;
    var s = Store.state;
    var id = el.dataset.id;
    if (el.checked) s.checks[id] = true;
    else delete s.checks[id];
    Store.save();
    // 就地更新徽章与样式，避免整页刷新导致折叠面板收起
    var row = el.closest('.task-item');
    if (row) row.classList.toggle('checked', el.checked);
    var clId = el.dataset.cl;
    var cl = null;
    DATA.checklists.forEach(function (c) { if (c.id === clId) cl = c; });
    if (cl) {
      var done = 0;
      cl.items.forEach(function (it) { if (s.checks[it.id]) done++; });
      var badge = document.querySelector('[data-cl-badge="' + clId + '"]');
      if (badge) {
        badge.textContent = done + '/' + cl.items.length;
        badge.className = 'badge ' + (done === cl.items.length ? 'badge-ok' : 'badge-plain');
        if (done === cl.items.length) UI.toast('✅ 「' + cl.name + '」验收完成！');
      }
    }
  }

  function onAction(action, el) {
    var id = el.dataset.id;
    if (action === 'gsub') { sub = id; App.rerender(); }
    else if (action === 'tip-topic') { topic = id || null; App.rerender(); }
    else if (action === 'reset-checklist') {
      var cl = null;
      DATA.checklists.forEach(function (c) { if (c.id === id) cl = c; });
      if (!cl) return;
      UI.confirmDlg('清空重验', '把「' + UI.esc(cl.name) + '」的勾选全部清空？', function () {
        cl.items.forEach(function (it) { delete Store.state.checks[it.id]; });
        Store.save(); App.rerender();
      }, '清空');
    }
    else if (action === 'style-detail') { showStyleDetail(id); }
    else if (action === 'quiz-start') { startQuiz(); }
  }

  return { render: render, onAction: onAction, onChange: onChange };
})();
