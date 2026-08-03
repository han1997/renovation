/* ============ UI 工具库：格式化 / 弹窗 / 表单 / Toast ============ */
window.UI = (function () {

  function esc(s) {
    if (s === null || s === undefined) return '';
    return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  /* ---- 金额 ---- */
  function money(n) {
    if (n === null || n === undefined || isNaN(n)) return '—';
    n = Math.round(n);
    if (Math.abs(n) >= 10000) {
      var w = n / 10000;
      var s = (Math.round(w * 100) / 100).toFixed(2).replace(/\.?0+$/, '');
      return s + '万';
    }
    return n.toLocaleString('zh-CN') + '元';
  }
  function moneyFull(n) {
    if (n === null || n === undefined || isNaN(n)) return '—';
    return '¥' + Math.round(n).toLocaleString('zh-CN');
  }

  /* ---- 日期 ---- */
  function today() {
    var d = new Date();
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
  }
  function pad(n) { return (n < 10 ? '0' : '') + n; }
  function parseDate(s) {
    if (!s) return null;
    var p = s.split('-');
    if (p.length !== 3) return null;
    return new Date(+p[0], +p[1] - 1, +p[2]);
  }
  function daysFromToday(s) {
    var d = parseDate(s);
    if (!d) return null;
    var t = parseDate(today());
    return Math.round((d - t) / 86400000);
  }
  function dateCN(s) {
    var d = parseDate(s);
    if (!d) return '';
    var y = new Date().getFullYear();
    return (d.getFullYear() !== y ? d.getFullYear() + '年' : '') + (d.getMonth() + 1) + '月' + d.getDate() + '日';
  }

  /* ---- Toast ---- */
  function toast(msg, ms) {
    var root = document.getElementById('toast-root');
    var el = document.createElement('div');
    el.className = 'toast';
    el.textContent = msg;
    root.appendChild(el);
    setTimeout(function () {
      el.style.transition = 'opacity .3s';
      el.style.opacity = '0';
      setTimeout(function () { el.remove(); }, 320);
    }, ms || 2200);
  }

  /* ---- 进度条 ---- */
  function barHTML(pct, opts) {
    opts = opts || {};
    var p = Math.max(0, Math.min(100, pct || 0));
    var cls = opts.cls || '';
    if (opts.auto) {
      if (pct > 100) cls = 'over';
      else if (pct > 85) cls = 'warn';
      else cls = 'ok';
    }
    return '<div class="bar"><div class="bar-fill ' + cls + '" style="width:' + p + '%"></div></div>';
  }

  function donutHTML(pct, label, sub, color) {
    var p = Math.max(0, Math.min(100, pct || 0));
    var c = color || 'var(--brand)';
    return '<div class="donut" style="background:conic-gradient(' + c + ' ' + (p * 3.6) + 'deg, var(--bg) 0)">' +
      '<div class="donut-hole"><b>' + esc(label) + '</b><span>' + esc(sub || '') + '</span></div></div>';
  }

  /* ---- 弹窗 ---- */
  function modal(opts) {
    var root = document.getElementById('modal-root');
    var mask = document.createElement('div');
    mask.className = 'modal-mask';
    var box = document.createElement('div');
    box.className = 'modal';
    var html = '';
    if (opts.title) html += '<div class="modal-title">' + opts.title + '</div>';
    html += '<div class="modal-body">' + (opts.body || '') + '</div>';
    box.innerHTML = html;

    function close() {
      mask.remove();
      if (opts.onClose) opts.onClose();
    }

    if (opts.actions && opts.actions.length) {
      var bar = document.createElement('div');
      bar.className = 'modal-actions';
      opts.actions.forEach(function (a) {
        var b = document.createElement('button');
        b.className = 'btn ' + (a.cls || '');
        b.innerHTML = a.label;
        b.addEventListener('click', function () {
          if (a.onClick) a.onClick(close);
          else close();
        });
        bar.appendChild(b);
      });
      box.appendChild(bar);
    }

    mask.appendChild(box);
    mask.addEventListener('click', function (e) {
      if (e.target === mask && opts.dismissible !== false) close();
    });
    root.appendChild(mask);
    return { close: close, el: box };
  }

  function confirmDlg(title, msg, onOk, okLabel) {
    modal({
      title: esc(title),
      body: '<div style="font-size:14px;color:#55524b">' + msg + '</div>',
      actions: [
        { label: '取消' },
        { label: okLabel || '确定', cls: 'btn-danger', onClick: function (close) { close(); onOk(); } }
      ]
    });
  }

  /* ---- 通用表单弹窗 ----
   * fields: [{key,label,type:'text'|'number'|'date'|'select'|'textarea',options:[{value,label}],placeholder,required,hint,value,step}]
   * onSubmit(values, close)
   * opts.extraAction: {label, cls, onClick(close)}  额外按钮（如删除）
   */
  function formModal(opts) {
    var body = '';
    opts.fields.forEach(function (f) {
      var v = f.value === undefined || f.value === null ? '' : f.value;
      body += '<div class="field" data-fkey="' + esc(f.key) + '"><label>' + esc(f.label) +
        (f.required ? ' <span style="color:var(--red)">*</span>' : '') + '</label>';
      if (f.type === 'select') {
        body += '<select name="' + esc(f.key) + '">';
        (f.options || []).forEach(function (o) {
          body += '<option value="' + esc(o.value) + '"' + (String(o.value) === String(v) ? ' selected' : '') + '>' + esc(o.label) + '</option>';
        });
        body += '</select>';
      } else if (f.type === 'textarea') {
        body += '<textarea name="' + esc(f.key) + '" placeholder="' + esc(f.placeholder || '') + '">' + esc(v) + '</textarea>';
      } else {
        body += '<input name="' + esc(f.key) + '" type="' + (f.type || 'text') + '"' +
          (f.step ? ' step="' + f.step + '"' : '') +
          (f.type === 'number' ? ' inputmode="decimal"' : '') +
          ' value="' + esc(v) + '" placeholder="' + esc(f.placeholder || '') + '">';
      }
      if (f.hint) body += '<div class="hint">' + esc(f.hint) + '</div>';
      body += '</div>';
    });

    var actions = [{ label: '取消' }];
    if (opts.extraAction) {
      actions.push({
        label: opts.extraAction.label, cls: opts.extraAction.cls || 'btn-danger',
        onClick: function (close) { opts.extraAction.onClick(close); }
      });
    }
    actions.push({
      label: opts.submitLabel || '保存', cls: 'btn-primary',
      onClick: function (close) {
        var box = m.el;
        var vals = {};
        var bad = null;
        opts.fields.forEach(function (f) {
          var input = box.querySelector('[name="' + f.key + '"]');
          var fieldEl = box.querySelector('[data-fkey="' + f.key + '"]');
          fieldEl.classList.remove('invalid');
          var raw = input ? input.value.trim() : '';
          var val = raw;
          if (f.type === 'number') {
            val = raw === '' ? null : parseFloat(raw);
            if (raw !== '' && isNaN(val)) { bad = bad || f; fieldEl.classList.add('invalid'); return; }
          }
          if (f.required && (val === '' || val === null)) {
            bad = bad || f; fieldEl.classList.add('invalid'); return;
          }
          vals[f.key] = val;
        });
        if (bad) { toast('请填写「' + bad.label + '」'); return; }
        opts.onSubmit(vals, close);
      }
    });

    var m = modal({ title: esc(opts.title), body: body, actions: actions, dismissible: false });
    // 自动聚焦第一个输入
    var first = m.el.querySelector('input, textarea, select');
    if (first && first.type !== 'date') setTimeout(function () { try { first.focus(); } catch (e) {} }, 80);
    return m;
  }

  /* ---- 文件下载 ---- */
  function download(filename, text, mime) {
    var blob = new Blob([text], { type: (mime || 'text/plain') + ';charset=utf-8' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url; a.download = filename;
    document.body.appendChild(a);
    a.click();
    setTimeout(function () { a.remove(); URL.revokeObjectURL(url); }, 400);
  }

  return {
    esc: esc, money: money, moneyFull: moneyFull,
    today: today, parseDate: parseDate, daysFromToday: daysFromToday, dateCN: dateCN,
    toast: toast, barHTML: barHTML, donutHTML: donutHTML,
    modal: modal, confirmDlg: confirmDlg, formModal: formModal,
    download: download
  };
})();
