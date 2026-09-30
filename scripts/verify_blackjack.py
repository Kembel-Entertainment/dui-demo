#!/usr/bin/env python3
"""Check actual card flight/reveal/payout pixels and exact scripted blackjack ledgers."""
from pathlib import Path
import json
from png_pixels import png
OUT=Path(__file__).resolve().parent.parent/'build/reports/e2e/blackjack'
def frame(name):
    meta=json.loads((OUT/'screenshots'/f'{name}.json').read_text());_,_,pixel=png(OUT/'screenshots'/f'{name}.png')
    def sample(x,y):return pixel(int((meta['canvasX']+x)*meta['scale']),int((meta['canvasY']+y)*meta['scale']))
    return meta,sample
motion={}
for name in ['deal','split','double','hit','flip','dealer','payout','compact']:
    ma,a=frame(name+'-early');mb,b=frame(name+'-late')
    assert ma['layout']['componentTextHash']==mb['layout']['componentTextHash'],name
    es=ma['layout']['effects'];mask=set()
    for e in es:
        if e['kind'] in ('PLAYING_CARD','CHIP_STACK'):
            mask.update((x,y) for y in range(e['y'],e['y']+e['height']) for x in range(e['x'],e['x']+e['width']))
    motion[name]=sum(max(abs(c-d) for c,d in zip(a(x,y),b(x,y)))>15 for x,y in mask)
    assert motion[name]>20,(name,motion[name])
ma,a=frame('motion-off');mb,b=frame('still-frame')
still=sum(max(abs(c-d) for c,d in zip(a(x,y),b(x,y)))>15 for y in range(ma['layout']['height']) for x in range(ma['layout']['width']))
assert still==0,still
for name,half in [('result',10150),('natural',10225),('soft17',10275),('after-close',10000)]:
    meta,_=frame(name);assert meta['layout']['balanceHalf']==half,(name,meta['layout']['balanceHalf'])
meta,_=frame('player');assert meta['layout']['dealer'][1]==-1
hole=next(e for e in meta['layout']['effects'] if e['id']=='dealer_1');assert hole['parameter0']&63==63
report=dict(result='PASS',animationPixels=motion,motionOffPixels=still,holeCardPrivate=True,splitDoubleLedger=True,naturalHalfCredits=True)
(OUT/'blackjack-verification.json').write_text(json.dumps(report,indent=2)+'\n');print('PASS blackjack',report)
