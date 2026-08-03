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

    /* ---- 空间需求 ---- */
    html += '<div class="card"><div class="card-title">🪟 空间需求' +
      '<button class="link-btn right" data-action="add-space">＋ 添加</button></div>';
    if (!s.spaces.length) {
      html += '<div class="empty small">还没添加空间需求。想要衣帽间、电竞房、中西双厨？<br>在这里登记，会自动加到对应流程阶段。</div>';
    } else {
      s.spaces.forEach(function (sp) {
        var stageBadges = (sp.stageIds || []).map(function (sid) {
          var st = null; DATA.stages.forEach(function (x) { if (x.id === sid) st = x; });
          return st ? '<span class="badge badge-plain">' + UI.esc(st.name) + '</span>' : '';
        }).join(' ');
        html += '<div class="note-item" data-action="edit-space" data-id="' + sp.id + '">' +
          '<div class="between"><span class="note-title">' + (sp.emoji || '🪟') + ' ' + UI.esc(sp.name) +
          (sp.custom ? ' <span class="badge badge-plain">自定义</span>' : '') + '</span>' +
          '<button class="link-btn tiny" data-action="del-space" data-id="' + sp.id + '" style="padding:0 2px">✕</button></div>' +
          '<div class="note-preview" style="white-space:normal">' + stageBadges + '</div></div>';
      });
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

    } else if (action === 'add-space') {
      openAddSpaceModal();

    } else if (action === 'edit-space') {
      var sp = null; s.spaces.forEach(function (x) { if (x.id === id) sp = x; });
      if (!sp) return;
      if (sp.custom) openEditCustomSpace(sp);
      else openViewPresetSpace(sp);

    } else if (action === 'del-space') {
      var spDel = null; s.spaces.forEach(function (x) { if (x.id === id) spDel = x; });
      if (!spDel) return;
      UI.confirmDlg('删除空间需求', '确定删除「' + UI.esc(spDel.name) + '」？<br>已派生的任务会一并删除（已打卡的会保留为普通任务）。', function () {
        Store.removeSpaceTasks(id);
        s.spaces = s.spaces.filter(function (x) { return x.id !== id; });
        Store.save(); App.rerender();
        UI.toast('已删除');
      }, '删除');

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

  /* ---------- 空间需求弹窗 ---------- */
  function rateNameOf(catId) {
    var name = '';
    if (!catId) return name;
    PRICES.rates.forEach(function (r) { if (r.id === catId) name = r.emoji + ' ' + r.name; });
    return name;
  }

  /* 添加：预设速点 + 自定义表单 */
  function openAddSpaceModal() {
    var s = Store.state;
    var addedPresetIds = {};
    s.spaces.forEach(function (sp) { if (sp.presetId) addedPresetIds[sp.presetId] = true; });
    var available = DATA.spaceNeeds.filter(function (n) { return !addedPresetIds[n.id]; });

    var body = '<div class="small muted mb8">点预设可直接添加；预设不够就在下面自定义一个。</div>';
    if (available.length) {
      body += '<div class="space-preset-grid">';
      available.forEach(function (n) {
        body += '<button class="preset-chip" data-preset="' + n.id + '">' + n.emoji + ' ' + UI.esc(n.name) + '</button>';
      });
      body += '</div>';
    } else {
      body += '<div class="small muted mb8">预设已全部添加，可在下面自定义。</div>';
    }
    body += '<div style="border:0;border-top:1px solid var(--line);margin:12px 0"></div>' +
      '<div class="small" style="font-weight:600;margin-bottom:8px">＋ 自定义空间需求</div>' +
      App.spaceFormHTML(null);

    var m = UI.modal({
      title: '🪟 添加空间需求',
      body: body,
      actions: [
        { label: '取消' },
        { label: '保存自定义', cls: 'btn-primary', onClick: function (close) {
          var name = m.el.querySelector('#sp-name').value.trim();
          var stageIds = App.readSpaceFormStages(m.el);
          if (!name) { UI.toast('请填写名称'); return; }
          if (!stageIds.length) { UI.toast('至少选一个关联阶段'); return; }
          var budgetCat = m.el.querySelector('#sp-budgetcat').value;
          var budgetNote = m.el.querySelector('#sp-budgetnote').value.trim();
          var sp = {
            id: Store.uid('sp'), presetId: null, name: name, emoji: '🪟', desc: '',
            stageIds: stageIds, tasks: App.buildSpaceTasks(name, stageIds),
            budgetCat: budgetCat, budgetNote: budgetNote,
            custom: true, createdAt: UI.today()
          };
          s.spaces.push(sp);
          Store.syncSpaceTasks(sp);
          Store.save(); close(); App.rerender();
          UI.toast('已添加，相关任务已加到流程 ✅');
        }}
      ]
    });

    // 预设速点：直接添加并关闭
    m.el.querySelectorAll('[data-preset]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var pid = btn.dataset.preset;
        var p = null; DATA.spaceNeeds.forEach(function (x) { if (x.id === pid) p = x; });
        if (!p) return;
        var sp = {
          id: Store.uid('sp'), presetId: p.id, name: p.name, emoji: p.emoji, desc: p.desc || '',
          stageIds: p.stageIds.slice(),
          tasks: p.tasks.map(function (tk) { return { stageId: tk.stageId, text: tk.text }; }),
          budgetCat: p.budgetCat, budgetNote: p.budgetNote || '',
          custom: false, createdAt: UI.today()
        };
        s.spaces.push(sp);
        Store.syncSpaceTasks(sp);
        Store.save(); m.close(); App.rerender();
        UI.toast('已添加「' + p.name + '」 ✅');
      });
    });
  }

  /* 编辑预设空间：只读详情 + 删除 */
  function openViewPresetSpace(sp) {
    var stageNames = (sp.stageIds || []).map(function (sid) {
      var st = null; DATA.stages.forEach(function (x) { if (x.id === sid) st = x; });
      return st ? st.name : sid;
    });
    var body = '<div class="small muted mb8">' + UI.esc(sp.desc || '') + '</div>' +
      '<table class="tbl">' +
      '<tr><td class="muted">关联阶段</td><td>' + stageNames.map(function (n) { return '<span class="badge badge-plain">' + UI.esc(n) + '</span>'; }).join(' ') + '</td></tr>' +
      '<tr><td class="muted">预算分类</td><td>' + UI.esc(rateNameOf(sp.budgetCat) || '—') + '</td></tr>' +
      '<tr><td class="muted">预算提示</td><td>' + UI.esc(sp.budgetNote || '—') + '</td></tr>' +
      '</table>';
    UI.modal({
      title: (sp.emoji || '🪟') + ' ' + UI.esc(sp.name),
      body: body,
      actions: [
        { label: '关闭' },
        { label: '删除', cls: 'btn-danger', onClick: function (close) {
          close();
          UI.confirmDlg('删除空间需求', '确定删除「' + UI.esc(sp.name) + '」？<br>已派生的任务会一并删除（已打卡的会保留为普通任务）。', function () {
            Store.removeSpaceTasks(sp.id);
            Store.state.spaces = Store.state.spaces.filter(function (x) { return x.id !== sp.id; });
            Store.save(); App.rerender();
            UI.toast('已删除');
          }, '删除');
        }}
      ]
    });
  }

  /* 编辑自定义空间：可改名/改阶段/改预算，保存后重新派生任务 */
  function openEditCustomSpace(sp) {
    var m = UI.modal({
      title: '编辑空间需求',
      body: App.spaceFormHTML(sp),
      actions: [
        { label: '取消' },
        { label: '删除', cls: 'btn-danger', onClick: function (close) {
          close();
          UI.confirmDlg('删除空间需求', '确定删除「' + UI.esc(sp.name) + '」？<br>已派生的任务会一并删除（已打卡的会保留为普通任务）。', function () {
            Store.removeSpaceTasks(sp.id);
            Store.state.spaces = Store.state.spaces.filter(function (x) { return x.id !== sp.id; });
            Store.save(); App.rerender();
            UI.toast('已删除');
          }, '删除');
        }},
        { label: '保存', cls: 'btn-primary', onClick: function (close) {
          var name = m.el.querySelector('#sp-name').value.trim();
          var stageIds = App.readSpaceFormStages(m.el);
          if (!name) { UI.toast('请填写名称'); return; }
          if (!stageIds.length) { UI.toast('至少选一个关联阶段'); return; }
          var budgetCat = m.el.querySelector('#sp-budgetcat').value;
          var budgetNote = m.el.querySelector('#sp-budgetnote').value.trim();
          sp.name = name;
          sp.stageIds = stageIds;
          sp.tasks = App.buildSpaceTasks(name, stageIds);
          sp.budgetCat = budgetCat;
          sp.budgetNote = budgetNote;
          Store.syncSpaceTasks(sp);
          Store.save(); close(); App.rerender();
          UI.toast('已保存，任务已同步 ✅');
        }}
      ]
    });
  }

  return { render: render, onAction: onAction, onChange: onChange };
})();
