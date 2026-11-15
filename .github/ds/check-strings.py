#!/usr/bin/env python3
"""Check that every key of .github/ds/strings.json is in the translations.

strings.json is the table the nginx sub_filter overlay used to apply
(zakacoding-cloud penpot-login/strings.json). In the fork the same text lives in
frontend/translations/{en,id}.po. Each key must exist with exactly the "new" text.

  check-strings.py --bundle bundles/frontend/js   # built translation.<lang>.js (CI)
  check-strings.py --po frontend/translations     # the .po sources
"""
import argparse, json, pathlib, re, sys

here = pathlib.Path(__file__).resolve().parent
ap = argparse.ArgumentParser()
ap.add_argument('--strings', default=str(here / 'strings.json'))
g = ap.add_mutually_exclusive_group(required=True)
g.add_argument('--bundle', help='dir with translation.<lang>.js')
g.add_argument('--po', help='dir with <lang>.po')
a = ap.parse_args()

def load_bundle(d, lang):
    t = (pathlib.Path(d) / f'translation.{lang}.js').read_text(encoding='utf-8')
    m = re.match(r'\s*export default (\{.*\});?\s*$', t, re.S)
    if not m:
        sys.exit(f'translation.{lang}.js: unexpected format')
    return json.loads(m.group(1))

def unq(s):
    return json.loads('"' + s + '"')  # po escapes are JSON-compatible for our text

def load_po(d, lang):
    out, cur, field = {}, None, None
    for line in (pathlib.Path(d) / f'{lang}.po').read_text(encoding='utf-8').splitlines():
        m = re.match(r'(msgid|msgstr) "(.*)"$', line)
        if m:
            field = m.group(1)
            if field == 'msgid':
                cur = unq(m.group(2)); out[cur] = ''
            else:
                out[cur] += unq(m.group(2))
        elif line.startswith('"') and cur is not None and field == 'msgstr':
            out[cur] += unq(line[1:-1])
        else:
            field = None
    return out

table = json.load(open(a.strings))['strings']
bad = 0
for lang, keys in table.items():
    have = load_bundle(a.bundle, lang) if a.bundle else load_po(a.po, lang)
    for k, v in keys.items():
        got = have.get(k)
        if got is None:
            print(f'MISSING {lang} {k}'); bad += 1
        elif got != v['new']:
            print(f'DIFFERS {lang} {k}: {got!r} != {v["new"]!r}'); bad += 1
n = sum(len(v) for v in table.values())
print(f'{n - bad}/{n} strings.json keys present')
sys.exit(1 if bad else 0)
