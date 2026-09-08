# -*- coding: utf-8 -*-
"""Extract decobox.online catalog + requirements data from the minified JS bundle
into Android assets JSON files.

Usage:
    python tools/extract_decobox.py <bundle.js> [--out-dir android/app/src/main/assets]
"""
import argparse
import json
import re
import sys


class JsParseError(Exception):
    pass


class Parser:
    """Recursive-descent parser for the JS object-literal subset we need."""

    def __init__(self, text):
        self.s = text
        self.i = 0
        self.n = len(text)

    def ws(self):
        while self.i < self.n and self.s[self.i] in ' \t\r\n':
            self.i += 1

    def parse_value(self):
        self.ws()
        if self.i >= self.n:
            raise JsParseError('unexpected end')
        c = self.s[self.i]
        if c == '{':
            return self.parse_object()
        if c == '[':
            return self.parse_array()
        if c in '`"\'':
            return self.parse_string(c)
        m = re.match(
            r'!(0|1)|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?|\.\d+(?:[eE][+-]?\d+)?|[A-Za-z_$][\w$]*',
            self.s[self.i:])
        if not m:
            raise JsParseError('cannot parse at %d: %r' % (self.i, self.s[self.i:self.i + 30]))
        tok = m.group(0)
        self.i += len(tok)
        if tok == 'true':
            return True
        if tok == 'false':
            return False
        if tok in ('null', 'undefined'):
            return None
        if tok == '!0':
            return True
        if tok == '!1':
            return False
        try:
            if any(ch in tok for ch in '.eE'):
                return float(tok)
            return int(tok)
        except ValueError:
            raise JsParseError('unknown token %r' % tok)

    def parse_string(self, quote):
        self.i += 1
        out = []
        while self.i < self.n:
            c = self.s[self.i]
            if c == '\\':
                nxt = self.s[self.i + 1] if self.i + 1 < self.n else ''
                mapping = {'n': '\n', 't': '\t', 'r': '\r', '\\': '\\', '/': '/',
                           "'": "'", '"': '"', '`': '`', '0': '\0', 'b': '\b', 'f': '\f'}
                if nxt in mapping:
                    out.append(mapping[nxt])
                    self.i += 2
                    continue
                if nxt == 'u':
                    out.append(chr(int(self.s[self.i + 2:self.i + 6], 16)))
                    self.i += 6
                    continue
                out.append(nxt)
                self.i += 2
                continue
            if c == quote:
                self.i += 1
                return ''.join(out)
            out.append(c)
            self.i += 1
        raise JsParseError('unterminated string')

    def parse_object(self):
        self.i += 1
        obj = {}
        while True:
            self.ws()
            if self.i >= self.n:
                raise JsParseError('unterminated object')
            if self.s[self.i] == '}':
                self.i += 1
                return obj
            if self.s[self.i] in '`"\'':
                key = self.parse_string(self.s[self.i])
            else:
                m = re.match(r'[A-Za-z_$][\w$]*', self.s[self.i:])
                if not m:
                    raise JsParseError('bad key at %d' % self.i)
                key = m.group(0)
                self.i += len(key)
            self.ws()
            if self.i < self.n and self.s[self.i] == ':':
                self.i += 1
            else:
                raise JsParseError('expected colon at %d' % self.i)
            obj[key] = self.parse_value()
            self.ws()
            if self.i < self.n and self.s[self.i] == ',':
                self.i += 1
                continue
            if self.i < self.n and self.s[self.i] == '}':
                self.i += 1
                return obj
            raise JsParseError('expected , or } at %d' % self.i)

    def parse_array(self):
        self.i += 1
        arr = []
        while True:
            self.ws()
            if self.i >= self.n:
                raise JsParseError('unterminated array')
            if self.s[self.i] == ']':
                self.i += 1
                return arr
            arr.append(self.parse_value())
            self.ws()
            if self.i < self.n and self.s[self.i] == ',':
                self.i += 1
                continue
            if self.i < self.n and self.s[self.i] == ']':
                self.i += 1
                return arr
            raise JsParseError('expected , or ] at %d' % self.i)


def parse_literal(text):
    p = Parser(text)
    v = p.parse_value()
    p.ws()
    if p.i != len(text):
        raise JsParseError('trailing content at %d' % p.i)
    return v


def find_matching(data, open_idx, open_ch, close_ch):
    depth = 0
    instr = None
    i = open_idx
    while i < len(data):
        c = data[i]
        if instr is not None:
            if c == '\\':
                i += 2
                continue
            if c == instr:
                instr = None
        else:
            if c in '`"\'':
                instr = c
            elif c == open_ch:
                depth += 1
            elif c == close_ch:
                depth -= 1
                if depth == 0:
                    return i
        i += 1
    return -1


class Cursor:
    """Sequential extractor: each fetch() searches forward from a moving cursor for
    the assignment `[var|,] NAME = <literal>` and advances past the literal."""

    def __init__(self, data):
        self.data = data
        self.pos = 0

    def seek_to(self, kw):
        i = self.data.find(kw, self.pos)
        if i < 0:
            print('FATAL: seek anchor not found:', kw)
            sys.exit(2)
        self.pos = i
        return i

    def fetch(self, name):
        """Find assignment of NAME at/after cursor; return parsed value, advance cursor past its literal end."""
        pat = re.compile(r'(?:var|,)\s*' + re.escape(name) + r'\s*=')
        m = pat.search(self.data, self.pos)
        if not m:
            print('FATAL: assignment not found:', name, 'from', self.pos)
            sys.exit(2)
        i = m.end()
        while i < len(self.data) and self.data[i] in ' \t\r\n':
            i += 1
        c = self.data[i]
        if c == '{':
            end = find_matching(self.data, i, '{', '}')
            val = parse_literal(self.data[i:end + 1])
            self.pos = end + 1
            return val
        if c == '[':
            end = find_matching(self.data, i, '[', ']')
            val = parse_literal(self.data[i:end + 1])
            self.pos = end + 1
            return val
        # primitive until , ; } ( ) boundary
        j = i
        while j < len(self.data) and self.data[j] not in ',;}()':
            if self.data[j] in '`"\'':
                q = self.data[j]
                j += 1
                while j < len(self.data) and self.data[j] != q:
                    if self.data[j] == '\\':
                        j += 2
                        continue
                    j += 1
                j += 1
            else:
                j += 1
        val = parse_literal(self.data[i:j])
        self.pos = j
        return val

    def fetch_func_km(self):
        """Km is `function Km(e){return e<55?[...]:e<95?[...]:e<130?[...]:[...]}`"""
        i = self.data.find('function Km(e)', self.pos)
        if i < 0:
            print('FATAL: Km not found')
            sys.exit(2)
        brace = self.data.find('{', i)
        end = find_matching(self.data, brace, '{', '}')
        body = self.data[brace + 1:end]
        self.pos = end + 1
        return parse_km(body)


def parse_km(body):
    rest = body.strip()
    # strip leading 'return '
    if rest.startswith('return '):
        rest = rest[len('return '):]
    result = []
    thresholds = [55, 95, 130]
    for t in thresholds:
        m = re.match(r'e<' + str(t) + r'\?', rest)
        if not m:
            raise JsParseError('km parse fail at %r' % rest[:60])
        rest = rest[m.end():]
        arr_end = find_array_end(rest)
        rooms = parse_literal(rest[:arr_end + 1])
        result.append({'maxArea': t, 'rooms': rooms})
        rest = rest[arr_end + 1:]
        if rest[:1] == ':':
            rest = rest[1:]
        else:
            break
    if rest.startswith('['):
        arr_end = find_array_end(rest)
        rooms = parse_literal(rest[:arr_end + 1])
        result.append({'maxArea': None, 'rooms': rooms})
    elif rest.strip():
        raise JsParseError('km tail parse fail: %r' % rest[:60])
    return result


def find_array_end(text):
    depth = 0
    instr = None
    for idx, c in enumerate(text):
        if instr is not None:
            if c == '\\':
                continue
            if c == instr:
                instr = None
            continue
        if c in '`"\'':
            instr = c
        elif c == '[':
            depth += 1
        elif c == ']':
            depth -= 1
            if depth == 0:
                return idx
    raise JsParseError('no array end')


def to_jsonable(obj):
    """递归把整数值的 float 转 int(避免 Kotlin Int 字段反序列化 1000.0 失败)。"""
    if isinstance(obj, float):
        return int(obj) if obj.is_integer() else obj
    if isinstance(obj, list):
        return [to_jsonable(v) for v in obj]
    if isinstance(obj, dict):
        return {k: to_jsonable(v) for k, v in obj.items()}
    return obj


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('bundle', help='path to decobox minified JS bundle')
    ap.add_argument('--out-dir', default='android/app/src/main/assets')
    ap.add_argument('--bundle-dir', default=None)
    args = ap.parse_args()

    data = open(args.bundle, encoding='utf-8').read()
    print('bundle chars:', len(data))

    # The wall-gypsum craft price references the constant yf.level (=15).
    # Substitute the bare identifier so the pure-data literal is parseable.
    data = data.replace('unitPrice:yf.level', 'unitPrice:15')

    cur = Cursor(data)

    # --------------------------------------------------------------
    # Requirements: wd = { spaceList, typeListBySpace, functionsByType }
    # (appears at ~388k, BEFORE the materials region ~481k)
    # --------------------------------------------------------------
    cur.seek_to('wd={spaceList')
    idx = cur.pos
    brace = data.find('{', idx)
    end = find_matching(data, brace, '{', '}')
    wd = parse_literal(data[brace:end + 1])
    cur.pos = end + 1

    n_types = sum(len(v) for v in wd['typeListBySpace'].values())
    n_items = sum(len(v) for v in wd['functionsByType'].values())
    print('requirement types:', n_types, 'items:', n_items, 'spaces:', len(wd['spaceList']))
    if n_types != 150 or n_items != 819:
        print('WARN: expected 150 types / 819 items')

    req = {
        'version': '2026-09-07',
        'source': 'decobox.online v1.4.0',
        'spaceList': wd['spaceList'],
        'typeListBySpace': wd['typeListBySpace'],
        'functionsByType': wd['functionsByType'],
    }

    # --------------------------------------------------------------
    # Materials catalog (variables appear in file order; cursor advances)
    # --------------------------------------------------------------
    cur.seek_to('var yf={level:15')
    yf = cur.fetch('yf')
    bf = cur.fetch('bf')                      # space extras
    cf = cur.fetch('Cf')                      # other mains
    af = cur.fetch('Af')                      # door brands
    ff = cur.fetch('Ff')                      # glass options per sliding door
    vf = cur.fetch('Vf')                      # heater brands
    hf = cur.fetch('Hf')                      # heater tiers
    uf = cur.fetch('Uf')                      # heater price matrix
    wf = cur.fetch('Wf')                      # toilet brands
    gf = cur.fetch('Gf')                      # smart-toilet addon prices
    kf = cur.fetch('Kf')                      # toilet kinds
    qf = cur.fetch('qf')                      # toilet price matrix
    yf2 = cur.fetch('Yf')                     # windowsill edge styles
    xf = cur.fetch('Xf')                      # windowsill defaults
    zf = cur.fetch('Zf')                      # multi-pick main ids
    df = cur.fetch('$f')                      # default qtys for multi-pick
    ep = cur.fetch('ep')                      # house extras (cleaning/protection)
    np = cur.fetch('np')                      # extra works (light coves/hvac/curtain)
    ap_obj = cur.fetch('ap')                  # wall/ceiling/floor
    op = cur.fetch('op')                      # house works prices
    sp = cur.fetch('sp')                      # management fee rate
    up = cur.fetch('up')                      # partial items
    hp = cur.fetch('hp')                      # partial fee labels
    km = cur.fetch_func_km()                  # area presets (function Km)
    zm = cur.fetch('Zm')                      # default house

    catalog = {
        'version': '2026-09-07',
        'source': 'decobox.online v1.4.0 (2025-2026 market snapshot)',
        'craft': {'gypsumLevel': yf.get('level'), 'grout': yf.get('grout')},
        'areas': {'wallFactor': 4, 'partialCeilingFactor': 0.6, 'partialCeilingMin': 3},
        'managementFeeRate': sp.get('management'),
        'defaultHouse': zm,
        'roomPresets': km,
        'wall': ap_obj['wall'],
        'ceiling': ap_obj['ceiling'],
        'floor': ap_obj['floor'],
        'spaceExtras': bf,
        'otherMains': cf,
        'houseWorks': op,
        'partialItems': up,
        'partialFees': {
            'demoRatePerSqm': 25, 'protectRatePerSqm': 8, 'protectMin': 300, 'hauling': 800,
        },
        'doors': af,
        'doorGlasses': ff,
        'doorFrameStyles': ['single', 'double'],
        'windowsillEdges': yf2,
        'windowsillDefaults': xf,
        'heaterBrands': vf,
        'heaterTiers': hf,
        'heaterMatrix': uf,
        'toiletBrands': wf,
        'toiletAddonPrices': gf,
        'toiletKinds': kf,
        'toiletMatrix': qf,
        'multiPickIds': zf,
        'defaultQtys': df,
        'houseExtras': ep,
        'extraWorks': np,
    }

    # --------------------------------------------------------------
    # Write outputs
    # --------------------------------------------------------------
    import os
    os.makedirs(args.out_dir, exist_ok=True)
    out_cat = os.path.join(args.out_dir, 'decobox_catalog.json')
    out_req = os.path.join(args.out_dir, 'decobox_requirements.json')
    with open(out_cat, 'w', encoding='utf-8') as f:
        json.dump(to_jsonable(catalog), f, ensure_ascii=False, indent=2)
    with open(out_req, 'w', encoding='utf-8') as f:
        json.dump(to_jsonable(req), f, ensure_ascii=False, indent=2)
    print('wrote', out_cat)
    print('wrote', out_req)

    if args.bundle_dir:
        import shutil
        os.makedirs(args.bundle_dir, exist_ok=True)
        shutil.copy(args.bundle, os.path.join(args.bundle_dir, 'decobox-bundle-v1.4.0.js'))
        print('copied bundle ->', args.bundle_dir)


if __name__ == '__main__':
    main()
