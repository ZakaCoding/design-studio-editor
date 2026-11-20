#!/usr/bin/env python3
"""Structural .po check: every msgid has a msgstr, no blank/comment between them, no duplicate msgids."""
import sys
bad=0
for p in sys.argv[1:]:
    L=open(p,encoding='utf-8').read().split('\n'); state=None; ids={}; issues=[]
    for i,l in enumerate(L,1):
        if l.startswith('msgid_plural'): continue
        if l.startswith('msgid '):
            if state=='msgid': issues.append((i,'msgid without msgstr'))
            state='msgid'; ids.setdefault(l,[]).append(i)
        elif l.startswith('msgstr'):
            if state not in ('msgid','msgstr'): issues.append((i,'msgstr without msgid'))
            state='msgstr'
        elif l.startswith('"'): pass
        elif l.strip()=='' or l.startswith('#'):
            if state=='msgid': issues.append((i,'msgid without msgstr'))
            if l.strip()=='': state=None
        else: issues.append((i,'unexpected: '+l[:60]))
    d={k:v for k,v in ids.items() if len(v)>1}
    print(p,'issues',len(issues),'dups',len(d)); [print('  ',x) for x in issues[:10]]; [print('   dup',k,v) for k,v in list(d.items())[:5]]
    bad+=len(issues)+len(d)
sys.exit(1 if bad else 0)
