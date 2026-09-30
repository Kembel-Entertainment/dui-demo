#!/usr/bin/env python3
"""Verify actual GPU wheel/ball/chip motion, an exact live payout and a stable still frame."""
from pathlib import Path
import json
from png_pixels import png
OUT=Path(__file__).resolve().parent.parent/'build/reports/e2e/roulette'
def frame(name):
    meta=json.loads((OUT/'screenshots'/f'{name}.json').read_text())
    _,_,pixel=png(OUT/'screenshots'/f'{name}.png')
    def sample(x,y):
        return pixel(int((meta['canvasX']+x)*meta['scale']),int((meta['canvasY']+y)*meta['scale']))
    return meta,sample
def changes(a,b,e):
    return sum(max(abs(c-d) for c,d in zip(a(x,y),b(x,y)))>15 for y in range(e['y'],e['y']+e['height']) for x in range(e['x'],e['x']+e['width']))
motion={}
for early,late,name,kind in [('spin-early','spin-middle','wheel','WHEEL'),('spin-middle','spin-late','ball-drop','WHEEL'),('payout-early','payout-late','payout','CHIP_STACK'),('compact-early','compact-late','compact','WHEEL')]:
    ma,a=frame(early);mb,b=frame(late)
    e=next(e for e in ma['layout']['effects'] if e['kind']==kind)
    motion[name]=changes(a,b,e);assert motion[name]>20,(name,motion[name])
ma,a=frame('motion-off');mb,b=frame('still-frame')
still=changes(a,b,dict(x=0,y=0,width=ma['layout']['width'],height=ma['layout']['height']))
assert still==0,still
meta,sample=frame('result');layout=meta['layout']
assert layout['balance']==4995 and layout['return']==180 and layout['rounds']==1 and layout['stake']==0
e=next(e for e in layout['effects'] if e['kind']=='WHEEL')
assert e['parameter0']&63==layout['result']
# The ivory ball must be visible in the winning pocket under the fixed twelve-o'clock marker.
cx=e['x']+e['width']/2;cy=e['y']+e['height']/2-e['width']*.47*.60
ball=sample(cx,cy)
assert min(ball)>170,('Ball missing at the settled target',ball)
report=dict(result='PASS',animationPixels=motion,motionOffPixels=still,livePayoutVerified=True,settledBallVerified=True)
(OUT/'roulette-verification.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS roulette',report)
