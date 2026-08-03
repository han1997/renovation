/* ============ 我的：房屋信息 / 笔记 / 通讯录 / 数据管理 ============ */
window.Views = window.Views || {};

Views.more = (function () {

  function render(el) {
    var s = Store.state;
    var html = '';

    /* ---- 房屋信息 ---- */
    html += '<div class="card"><div class="card-title">🏠 我的房子' +
      '<button class="link-btn right" data-action="edit-profile">编辑</button></div>';
    if (s.profile) {
      var styleName = '未选定';
      if (s.profile.styleId) {
        DATA.styles.forEach(function (st) { if (st.id === s.profile.styleId) styleName = st.emoji + ' ' + st.name; });
      }
      html += '<table class="tbl">' +
        '<tr><td class="muted">建筑面积</td><td class="num"><b>' + s.profile.area + ' ㎡</b></td></tr>' +
        '<tr><td class="muted">城市档位</td><td class="num">' + UI.esc(PRICES.tierName(s.profile.tier)) + '</td></tr>' +
        '<tr><td class="muted">装修方式</td><td class="num">' + UI.esc(PRICES.modeName(s.profile.mode)) + '</td></tr>' +
        '<tr><td class="muted">装修档次</td><td class="num">' + UI.esc(PRICES.gradeName(s.profile.grade)) + '</td></tr>' +
        '<tr><td class="muted">开工日期</td><td class="num">' + (s.profile.startDate ? UI.dateCN(s.profile.startDate) : '未设置') + '</td></tr>' +
        '<tr><td class="muted">总预算</td><td class="num">' + UI.money(s.profile.totalBudget) + '</td></tr>' +
        '<tr><td class="muted">装修风格</td><td class="num">' + UI.esc(styleName) + '</td></tr>' +
        '</table>';
    } else {
      html += '<div class="empty"><span class="e-icon">🏠</span>还没设置房屋信息</div>' +
        '<button class="btn btn-primary btn-block" data-action="start-wizard">开始设置 →</button>';
    }
    html += '</div>';

    /* ---- 笔记 ---- */
    html += '<div class="card"><div class="card-title">📝 装修笔记' +
      '<button class="link-btn right" data-action="add-note">＋ 新建</button></div>';
    if (!s.notes.length) {
      html += '<div class="empty small">记点什么吧：跟工长聊的口头承诺、看中的型号、量的尺寸…<br>好记性不如烂笔头。</div>';
    } else {
      s.notes.slice().sort(function (a, b) { return (b.updatedAt || '').localeCompare(a.updatedAt || ''); })
        .forEach(function (n) {
          html += '<div class="note-item" data-action="edit-note" data-id="' + n.id + '">' +
            '<div class="between"><span class="note-title">' + UI.esc(n.title) + '</span><span class="tiny muted">' + UI.esc(n.updatedAt || '') + '</span></div>' +
            '<div class="note-preview">' + UI.esc((n.text || '').slice(0, 50)) + '</div></div>';
        });
    }
    html += '</div>';

    /* ---- 通讯录 ---- */
    html += '<div class="card"><div class="card-title">📇 装修通讯录' +
      '<button class="link-btn right" data-action="add-contact">＋ 添加</button></div>';
    if (!s.contacts.length) {
      html += '<div class="empty small">把工长、设计师、各家建材商的电话存在这里，<br>工地上要找人时不用翻聊天记录。</div>';
    } else {
      s.contacts.forEach(function (c) {
        html += '<div class="note-item" data-action="edit-contact" data-id="' + c.id + '">' +
          '<div class="between"><span class="note-title">' + UI.esc(c.name) +
          ' <span class="badge badge-plain">' + UI.esc(c.role || '') + '</span></span>' +
          (c.phone ? '<a class="link-btn" href="tel:' + UI.esc(c.phone) + '" onclick="event.stopPropagation()">📞 ' + UI.esc(c.phone) + '</a>' : '') +
          '</div>' + (c.note ? '<div class="note-preview">' + UI.esc(c.note) + '</div>' : '') + '</div>';
      });
    }
    html += '</div>';

    /* ---- 数据管理 ---- */
    html += '<div class="card"><div class="card-title">💾 数据管理</div>' +
      '<div class="tiny muted mb8">所有数据只保存在这台设备的浏览器里。换设备 / 换浏览器前，记得先导出备份。</div>' +
      '<div class="row wrap">' +
      '<button class="btn btn-sm" data-action="export-json">⬆️ 导出备份</button>' +
      '<button class="btn btn-sm" data-action="import-json">⬇️ 导入备份</button>' +
      '<button class="btn btn-sm" data-action="export-csv2">📄 导出支出 CSV</button>' +
      '<button class="btn btn-sm btn-danger" data-action="reset-all">🗑️ 清空全部数据</button>' +
      '</div>' +
      '<input type="file" id="import-file" accept=".json,application/json" class="hidden" data-change="import-file">' +
      '</div>';

    /* ---- 关于 ---- */
    html += '<div class="card"><div class="card-title">ℹ️ 关于装修管家</div>' +
      '<div class="small muted" style="line-height:1.9">' +
      '一个陪你从收房走到入住的装修助手：流程打卡、预算记账、避坑提醒、验收清单，全都离线可用。<br><br>' +
      '<b>📱 想在手机上用？</b><br>双击项目里的 <b>phone-server.bat</b>（需与电脑连同一 Wi-Fi），按提示在手机浏览器打开网址即可。注意：手机和电脑的数据各自独立，可以用「导出备份 → 导入备份」互相同步。<br><br>' +
      '<b>⚠️ 温馨提示</b><br>App 内所有价格为 2025–2026 年网络公开行情的大致区间，仅供参考，请以本地实际报价为准。装修有大量非标准情况，重大决定（拆墙、改结构、防水）请务必咨询专业人士。' +
      '</div></div>';

    el.innerHTML = html;
  }

  /* ---------- 事件 ---------- */
  function onAction(action, el) {
    var s = Store.state;
    var id = el.dataset.id;

    if (action === 'edit-profile') {
      if (!s.profile) { App.showWizard(); return; }
      UI.formModal({
        title: '编辑房屋信息',
        fields: [
          { key: 'area', label: '建筑面积（㎡）', type: 'number', required: true, value: s.profile.area },
          { key: 'tier', label: '城市档位', type: 'select', options: PRICES.tiers.map(function (t) { return { value: t.id, label: t.name }; }), value: s.profile.tier },
          { key: 'mode', label: '装修方式', type: 'select', options: DATA.modes.map(function (m) { return { value: m.id, label: m.name }; }), value: s.profile.mode },
          { key: 'grade', label: '装修档次', type: 'select', options: PRICES.grades.map(function (g) { return { value: g.id, label: g.name }; }), value: s.profile.grade },
          { key: 'startDate', label: '开工日期', type: 'date', value: s.profile.startDate || '' }
        ],
        onSubmit: function (vals, close) {
          var changed = vals.area !== s.profile.area || vals.tier !== s.profile.tier ||
            vals.mode !== s.profile.mode || vals.grade !== s.profile.grade;
          s.profile.area = vals.area;
          s.profile.tier = vals.tier;
          s.profile.mode = vals.mode;
          s.profile.grade = vals.grade;
          s.profile.startDate = vals.startDate || null;
          Store.save(); close(); App.rerender();
          if (changed) {
            UI.confirmDlg('要重算预算吗？',
              '面积 / 城市 / 方式 / 档次变了，各分类的推荐预算也会不一样。<br>要按新信息重算分类预算吗？（支出记录不受影响）',
              function () {
                var tpl = PRICES.budgetTemplate(s.profile);
                tpl.forEach(function (t) {
                  var exist = null;
                  s.budget.categories.forEach(function (c) { if (c.id === t.id) exist = c; });
                  if (exist) exist.planned = t.planned;
                  else s.budget.categories.push(t);
                });
                var sum = 0;
                s.budget.categories.forEach(function (c) { sum += c.planned || 0; });
                s.profile.totalBudget = sum;
                Store.save(); App.rerender();
                UI.toast('预算已按新信息重算 ✅');
              }, '重算');
          }
        }
      });

    } else if (action === 'add-note' || action === 'edit-note') {
      var note = null;
      if (action === 'edit-note') s.notes.forEach(function (n) { if (n.id === id) note = n; });
      UI.formModal({
        title: note ? '编辑笔记' : '📝 新建笔记',
        fields: [
          { key: 'title', label: '标题', type: 'text', required: true, value: note ? note.title : '', placeholder: '例如：水电交底记录' },
          { key: 'text', label: '内容', type: 'textarea', value: note ? note.text : '', placeholder: '写点什么…' }
        ],
        extraAction: note ? {
          label: '删除', cls: 'btn-danger',
          onClick: function (close) {
            UI.confirmDlg('删除笔记', '确定删除「' + UI.esc(note.title) + '」？', function () {
              s.notes = s.notes.filter(function (n) { return n.id !== id; });
              Store.save(); close(); App.rerender();
            }, '删除');
          }
        } : null,
        onSubmit: function (vals, close) {
          if (note) { note.title = vals.title; note.text = vals.text || ''; note.updatedAt = UI.today(); }
          else s.notes.push({ id: Store.uid('nt'), title: vals.title, text: vals.text || '', updatedAt: UI.today() });
          Store.save(); close(); App.rerender();
        }
      });

    } else if (action === 'add-contact' || action === 'edit-contact') {
      var ct = null;
      if (action === 'edit-contact') s.contacts.forEach(function (c) { if (c.id === id) ct = c; });
      UI.formModal({
        title: ct ? '编辑联系人' : '📇 添加联系人',
        fields: [
          { key: 'name', label: '姓名 / 称呼', type: 'text', required: true, value: ct ? ct.name : '', placeholder: '例如：张工' },
          { key: 'role', label: '角色', type: 'text', value: ct ? ct.role : '', placeholder: '例如：工长 / 瓷砖商家 / 设计师' },
          { key: 'phone', label: '电话', type: 'text', value: ct ? ct.phone : '' },
          { key: 'note', label: '备注', type: 'text', value: ct ? ct.note : '', placeholder: '例如：说好含两次上门' }
        ],
        extraAction: ct ? {
          label: '删除', cls: 'btn-danger',
          onClick: function (close) {
            UI.confirmDlg('删除联系人', '确定删除「' + UI.esc(ct.name) + '」？', function () {
              s.contacts = s.contacts.filter(function (c) { return c.id !== id; });
              Store.save(); close(); App.rerender();
            }, '删除');
          }
        } : null,
        onSubmit: function (vals, close) {
          if (ct) { ct.name = vals.name; ct.role = vals.role || ''; ct.phone = vals.phone || ''; ct.note = vals.note || ''; }
          else s.contacts.push({ id: Store.uid('ct'), name: vals.name, role: vals.role || '', phone: vals.phone || '', note: vals.note || '' });
          Store.save(); close(); App.rerender();
        }
      });

    } else if (action === 'export-json') {
      Store.exportJSON();
      UI.toast('备份文件已下载 📦');

    } else if (action === 'import-json') {
      document.getElementById('import-file').click();

    } else if (action === 'export-csv2') {
      if (!s.budget.expenses.length) { UI.toast('还没有支出记录'); return; }
      Views.budget.exportCSV();

    } else if (action === 'reset-all') {
      UI.confirmDlg('⚠️ 清空全部数据',
        '进度、支出、笔记、通讯录将<b>全部删除且无法恢复</b>。<br>建议先「导出备份」。真的要清空吗？',
        function () {
          UI.confirmDlg('最后确认', '再确认一次：清空所有数据？', function () {
            Store.reset();
            App.go('home');
            App.showWizard();
          }, '清空');
        }, '继续');
    }
  }

  function onChange(kind, el) {
    if (kind !== 'import-file') return;
    var file = el.files && el.files[0];
    if (!file) return;
    Store.importJSON(file, function (ok, err) {
      el.value = '';
      if (ok) { UI.toast('导入成功 ✅'); App.go('home'); }
      else UI.toast('❌ ' + err, 3600);
    });
  }

  return { render: render, onAction: onAction, onChange: onChange };
})();
