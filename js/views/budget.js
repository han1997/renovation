/* ============ 预算视图：概览 / 支出明细 / 参考价 ============ */
window.Views = window.Views || {};

Views.budget = (function () {
  var sub = 'ov';        // ov | list | ref
  var filterCat = null;  // 明细页分类筛选

  function catById(id) {
    var found = null;
    Store.state.budget.categories.forEach(function (c) { if (c.id === id) found = c; });
    return found;
  }
  function spentOf(catId) {
    var sum = 0;
    Store.state.budget.expenses.forEach(function (e) { if (e.catId === catId) sum += e.amount || 0; });
    return sum;
  }
  function totalSpent() {
    var sum = 0;
    Store.state.budget.expenses.forEach(function (e) { sum += e.amount || 0; });
    return sum;
  }

  function render(el, param) {
    if (param === 'list' || param === 'ref' || param === 'ov') { sub = param; }
    var s = Store.state;
    if (!s.profile) {
      el.innerHTML = '<div class="card center" style="padding:30px"><div style="font-size:36px">💰</div>' +
        '<p class="muted small" style="margin:10px 0 16px">先完成初始设置，我会按你家的面积和城市生成一份推荐预算。</p>' +
        '<button class="btn btn-primary" data-action="start-wizard">开始设置 →</button></div>';
      return;
    }

    var html = '<div class="seg">' +
      '<button class="' + (sub === 'ov' ? 'active' : '') + '" data-action="sub" data-id="ov">📊 预算概览</button>' +
      '<button class="' + (sub === 'list' ? 'active' : '') + '" data-action="sub" data-id="list">🧾 支出明细</button>' +
      '<button class="' + (sub === 'ref' ? 'active' : '') + '" data-action="sub" data-id="ref">🏷️ 行情参考价</button>' +
      '</div>';

    if (sub === 'ov') html += renderOverview();
    else if (sub === 'list') html += renderList();
    else html += renderRef();

    el.innerHTML = html;
  }

  /* ---------- 概览 ---------- */
  function renderOverview() {
    var s = Store.state;
    var total = s.profile.totalBudget || 0;
    var spent = totalSpent();
    var left = total - spent;
    var pct = total ? Math.round(spent / total * 100) : 0;

    var html = '<div class="card">' +
      '<div class="card-title">📊 总预算<button class="link-btn right" data-action="edit-total">修改</button></div>' +
      '<div class="row" style="gap:16px">' +
      UI.donutHTML(pct, pct + '%', '已使用', pct > 100 ? 'var(--red)' : pct > 85 ? 'var(--amber)' : 'var(--green)') +
      '<div class="stat-grid" style="flex:1;grid-template-columns:1fr">' +
      '<div class="stat"><b>' + UI.money(total) + '</b><span>总预算</span></div>' +
      '<div class="stat"><b>' + UI.money(spent) + '</b><span>已花费 · ' + s.budget.expenses.length + ' 笔</span></div>' +
      '<div class="stat"><b class="' + (left < 0 ? 'red' : 'green') + '">' + UI.money(left) + '</b><span>' + (left < 0 ? '已超支' : '还可以花') + '</span></div>' +
      '</div></div>';
    if (pct > 100) html += '<div class="small mt8" style="color:var(--red)">⚠️ 超支 ' + UI.money(spent - total) + '。看看下面哪个分类爆了，或者调整总预算。</div>';
    html += '</div>';

    // 分类
    var cats = s.budget.categories;
    var plannedSum = 0;
    cats.forEach(function (c) { plannedSum += c.planned || 0; });

    html += '<div class="card"><div class="card-title">🗂️ 分类预算<span class="right tiny muted">点分类看明细</span></div>';
    if (!cats.length) {
      html += '<div class="empty"><span class="e-icon">🗂️</span>还没有分类，点下面按钮生成推荐分类</div>';
    }
    cats.forEach(function (c) {
      var sp = spentOf(c.id);
      var cpct = c.planned ? Math.round(sp / c.planned * 100) : (sp > 0 ? 999 : 0);
      html += '<div class="cat-row" data-action="open-cat" data-id="' + c.id + '">' +
        '<div class="cat-head"><span>' + c.emoji + '</span><span class="cat-name">' + UI.esc(c.name) + '</span>' +
        (cpct > 100 ? '<span class="badge badge-danger">超 ' + UI.money(sp - c.planned) + '</span>' : '') +
        '<span class="cat-nums">' + UI.money(sp) + ' / ' + UI.money(c.planned) + '</span>' +
        '<button class="link-btn" data-action="edit-cat" data-id="' + c.id + '" style="padding:0 2px">✏️</button></div>' +
        '<div class="mt4">' + UI.barHTML(cpct, { auto: true }) + '</div>' +
        '</div>';
    });
    html += '<div class="row wrap mt12">' +
      '<button class="btn btn-sm btn-ghost" data-action="add-cat">＋ 添加分类</button>' +
      '<button class="btn btn-sm" data-action="reset-budget">按推荐比例重算</button></div>';

    if (cats.length) {
      html += '<div class="tiny muted mt8">分类预算合计 ' + UI.money(plannedSum);
      if (Math.abs(plannedSum - (s.profile.totalBudget || 0)) > Math.max(1000, plannedSum * 0.01)) {
        html += '，与总预算相差 ' + UI.money(Math.abs(plannedSum - s.profile.totalBudget)) +
          ' <button class="link-btn tiny" data-action="align-total">把总预算改成合计值</button>';
      }
      html += '</div>';
    }
    html += '</div>';

    html += '<div class="card" style="background:#fffdf6"><div class="small">💡 <b>预算心法</b></div>' +
      '<ul class="plain-list small mt4">' +
      '<li>留 8%–10% 备用金，装修几乎必有计划外支出</li>' +
      '<li>每花一笔当天就记，拖到月底一定记不清</li>' +
      '<li>大额支出（定制柜、家电）先看「行情参考价」再下单，心里有底不挨宰</li>' +
      '</ul></div>';
    return html;
  }

  /* ---------- 支出明细 ---------- */
  function renderList() {
    var s = Store.state;
    var cats = s.budget.categories;
    var html = '<div class="chips">' +
      '<button class="chip' + (!filterCat ? ' active' : '') + '" data-action="filter-cat" data-id="">全部</button>';
    cats.forEach(function (c) {
      html += '<button class="chip' + (filterCat === c.id ? ' active' : '') + '" data-action="filter-cat" data-id="' + c.id + '">' + c.emoji + ' ' + UI.esc(c.name) + '</button>';
    });
    html += '</div>';

    var list = s.budget.expenses.filter(function (e) { return !filterCat || e.catId === filterCat; });
    list.sort(function (a, b) { return (b.date || '').localeCompare(a.date || '') || (b.id > a.id ? 1 : -1); });
    var sum = 0;
    list.forEach(function (e) { sum += e.amount || 0; });

    html += '<div class="card">' +
      '<div class="between mb8"><span class="small muted">' + list.length + ' 笔 · 合计 <b style="color:var(--ink)">' + UI.money(sum) + '</b></span>' +
      '<button class="btn btn-sm btn-primary" data-action="add-expense">＋ 记一笔</button></div>';

    if (!list.length) {
      html += '<div class="empty"><span class="e-icon">🧾</span>还没有记录。买了什么、付了什么款，<br>点「记一笔」马上记下来。</div>';
    } else {
      list.forEach(function (e) {
        var c = catById(e.catId);
        html += '<div class="exp-item" data-action="edit-expense" data-id="' + e.id + '">' +
          '<span style="font-size:18px">' + (c ? c.emoji : '🧾') + '</span>' +
          '<div class="exp-main"><div class="exp-name">' + UI.esc(e.name) + '</div>' +
          '<div class="exp-sub">' + UI.esc(e.date || '') + (c ? ' · ' + UI.esc(c.name) : '') + (e.note ? ' · ' + UI.esc(e.note) : '') + '</div></div>' +
          '<span class="exp-amount">' + UI.moneyFull(e.amount) + '</span></div>';
      });
    }
    html += '</div>';
    if (s.budget.expenses.length) {
      html += '<button class="btn btn-block" data-action="export-csv">⬇️ 导出支出表格（CSV，可用 Excel 打开）</button>';
    }
    return html;
  }

  /* ---------- 参考价 ---------- */
  function renderRef() {
    var s = Store.state;
    var tier = null;
    PRICES.tiers.forEach(function (t) { if (t.id === s.profile.tier) tier = t; });
    var html = '<div class="card" style="background:#fffdf6">' +
      '<div class="small"><b>📌 怎么用这份参考价</b></div>' +
      '<div class="small muted mt4">下面是 2025–2026 年的大致行情（以二线城市为基准）。你选的是「' + (tier ? tier.name : '') + '」' +
      (tier && tier.factor !== 1 ? '，价格可按 ×' + tier.factor + ' 左右换算' : '，可直接对照') +
      '。谈价前扫一眼，报价明显高出区间就该多问几家。价格受品牌、材质、楼层影响，仅供参考。</div></div>';

    PRICES.reference.forEach(function (g, gi) {
      html += '<details class="fold" style="background:#fff;margin-bottom:10px"' + (gi === 0 ? ' open' : '') + '>' +
        '<summary>' + g.emoji + ' ' + UI.esc(g.group) + '</summary><div class="fold-body"><table class="tbl">' +
        '<tr><th>项目</th><th class="num">参考价</th></tr>';
      g.items.forEach(function (it) {
        html += '<tr><td><b>' + UI.esc(it.name) + '</b>' + (it.note ? '<div class="tiny muted">' + UI.esc(it.note) + '</div>' : '') + '</td>' +
          '<td class="num">' + UI.esc(it.price) + '</td></tr>';
      });
      html += '</table></div></details>';
    });
    return html;
  }

  /* ---------- 记账表单（供首页复用） ---------- */
  function openExpenseForm(presetCatId, onDone) {
    var s = Store.state;
    var catOpts = s.budget.categories.map(function (c) { return { value: c.id, label: c.emoji + ' ' + c.name }; });
    if (!catOpts.length) catOpts = [{ value: '', label: '（暂无分类）' }];
    UI.formModal({
      title: '🧾 记一笔支出',
      fields: [
        { key: 'name', label: '花在哪了', type: 'text', required: true, placeholder: '例如：瓷砖定金 / 水电人工费' },
        { key: 'amount', label: '金额（元）', type: 'number', required: true, step: '0.01', placeholder: '0.00' },
        { key: 'catId', label: '分类', type: 'select', options: catOpts, value: presetCatId || (catOpts[0] && catOpts[0].value) },
        { key: 'date', label: '日期', type: 'date', value: UI.today(), required: true },
        { key: 'note', label: '备注（可选）', type: 'text', placeholder: '商家、款项性质（定金/尾款）等' }
      ],
      submitLabel: '记下',
      onSubmit: function (vals, close) {
        s.budget.expenses.push({
          id: Store.uid('ex'), name: vals.name, amount: vals.amount,
          catId: vals.catId, date: vals.date, note: vals.note || ''
        });
        Store.save(); close();
        UI.toast('已记录 ' + UI.moneyFull(vals.amount) + ' ✅');
        checkOverspend(vals.catId);
        if (onDone) onDone(); else App.rerender();
      }
    });
  }

  function checkOverspend(catId) {
    var c = catById(catId);
    if (!c || !c.planned) return;
    var sp = spentOf(catId);
    if (sp > c.planned) {
      setTimeout(function () {
        UI.toast('⚠️ 「' + c.name + '」已超出预算 ' + UI.money(sp - c.planned), 3200);
      }, 900);
    }
  }

  function exportCSV() {
    var s = Store.state;
    var rows = [['日期', '分类', '项目', '金额(元)', '备注']];
    var list = s.budget.expenses.slice().sort(function (a, b) { return (a.date || '').localeCompare(b.date || ''); });
    list.forEach(function (e) {
      var c = catById(e.catId);
      rows.push([e.date || '', c ? c.name : '', e.name, e.amount, e.note || '']);
    });
    var csv = '﻿' + rows.map(function (r) {
      return r.map(function (cell) {
        cell = String(cell === undefined || cell === null ? '' : cell);
        return /[",\n]/.test(cell) ? '"' + cell.replace(/"/g, '""') + '"' : cell;
      }).join(',');
    }).join('\r\n');
    UI.download('装修支出明细.csv', csv, 'text/csv');
    UI.toast('已导出 CSV 📄');
  }

  /* ---------- 事件 ---------- */
  function onAction(action, el) {
    var s = Store.state;
    var id = el.dataset.id;

    if (action === 'sub') { sub = id; App.rerender(); }

    else if (action === 'edit-total') {
      UI.formModal({
        title: '修改总预算',
        fields: [{ key: 'v', label: '总预算（元）', type: 'number', required: true, value: s.profile.totalBudget }],
        onSubmit: function (vals, close) {
          s.profile.totalBudget = vals.v; Store.save(); close(); App.rerender();
        }
      });

    } else if (action === 'align-total') {
      var sum = 0;
      s.budget.categories.forEach(function (c) { sum += c.planned || 0; });
      s.profile.totalBudget = sum; Store.save(); App.rerender();
      UI.toast('总预算已对齐为 ' + UI.money(sum));

    } else if (action === 'open-cat') {
      filterCat = id; sub = 'list'; App.rerender();

    } else if (action === 'edit-cat') {
      var c = catById(id);
      if (!c) return;
      var hasExp = s.budget.expenses.some(function (e) { return e.catId === id; });
      UI.formModal({
        title: '编辑分类',
        fields: [
          { key: 'name', label: '分类名称', type: 'text', required: true, value: c.name },
          { key: 'planned', label: '预算金额（元）', type: 'number', required: true, value: c.planned }
        ],
        extraAction: {
          label: '删除', cls: 'btn-danger',
          onClick: function (close) {
            if (hasExp) { UI.toast('该分类下已有支出记录，请先移动或删除那些记录'); return; }
            UI.confirmDlg('删除分类', '确定删除「' + UI.esc(c.name) + '」？', function () {
              s.budget.categories = s.budget.categories.filter(function (x) { return x.id !== id; });
              Store.save(); close(); App.rerender();
            }, '删除');
          }
        },
        onSubmit: function (vals, close) {
          c.name = vals.name; c.planned = vals.planned;
          Store.save(); close(); App.rerender();
        }
      });

    } else if (action === 'add-cat') {
      UI.formModal({
        title: '＋ 添加分类',
        fields: [
          { key: 'name', label: '分类名称', type: 'text', required: true, placeholder: '例如：智能家居' },
          { key: 'planned', label: '预算金额（元）', type: 'number', required: true }
        ],
        onSubmit: function (vals, close) {
          s.budget.categories.push({ id: Store.uid('bc'), name: vals.name, emoji: '📦', planned: vals.planned });
          Store.save(); close(); App.rerender();
        }
      });

    } else if (action === 'reset-budget') {
      UI.confirmDlg('按推荐比例重算',
        '将按你家 <b>' + s.profile.area + '㎡ · ' + PRICES.tierName(s.profile.tier) + ' · ' + PRICES.gradeName(s.profile.grade) +
        '</b> 重新计算各分类的推荐预算。<br><br>已记的支出<b>不会丢失</b>，自定义分类也会保留，只是各分类的「预算金额」会被重置。',
        function () {
          var tpl = PRICES.budgetTemplate(s.profile);
          tpl.forEach(function (t) {
            var exist = catById(t.id);
            if (exist) exist.planned = t.planned;
            else s.budget.categories.push(t);
          });
          Store.save(); App.rerender();
          UI.toast('已按推荐比例重算 ✅');
        }, '重算');

    } else if (action === 'add-expense') {
      openExpenseForm(filterCat);

    } else if (action === 'filter-cat') {
      filterCat = id || null; App.rerender();

    } else if (action === 'edit-expense') {
      var exp = null;
      s.budget.expenses.forEach(function (x) { if (x.id === id) exp = x; });
      if (!exp) return;
      var catOpts = s.budget.categories.map(function (c2) { return { value: c2.id, label: c2.emoji + ' ' + c2.name }; });
      UI.formModal({
        title: '编辑支出',
        fields: [
          { key: 'name', label: '花在哪了', type: 'text', required: true, value: exp.name },
          { key: 'amount', label: '金额（元）', type: 'number', required: true, step: '0.01', value: exp.amount },
          { key: 'catId', label: '分类', type: 'select', options: catOpts, value: exp.catId },
          { key: 'date', label: '日期', type: 'date', required: true, value: exp.date },
          { key: 'note', label: '备注', type: 'text', value: exp.note }
        ],
        extraAction: {
          label: '删除', cls: 'btn-danger',
          onClick: function (close) {
            UI.confirmDlg('删除支出', '确定删除「' + UI.esc(exp.name) + '」这笔记录？', function () {
              s.budget.expenses = s.budget.expenses.filter(function (x) { return x.id !== id; });
              Store.save(); close(); App.rerender();
            }, '删除');
          }
        },
        onSubmit: function (vals, close) {
          exp.name = vals.name; exp.amount = vals.amount; exp.catId = vals.catId;
          exp.date = vals.date; exp.note = vals.note || '';
          Store.save(); close(); App.rerender();
        }
      });

    } else if (action === 'export-csv') {
      exportCSV();
    }
  }

  return { render: render, onAction: onAction, openExpenseForm: openExpenseForm, exportCSV: exportCSV };
})();
