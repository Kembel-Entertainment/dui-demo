#!/usr/bin/env python3
"""Probe native model motion and the fixed viewport in current-run GPU screenshots."""
from pathlib import Path
import json
from png_pixels import png
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'build/reports/e2e/warps'

def frame(name):
    meta=json.loads((OUT/'screenshots'/f'{name}.json').read_text())
    _,_,pixel=png(OUT/'screenshots'/f'{name}.png')
    def sample(x,y):return pixel(int((meta['canvasX']+x)*meta['scale']),int((meta['canvasY']+y)*meta['scale']))
    return meta,sample

def clip_bounds(meta):
    c=next(iter(meta['layout']['clips'].values()))
    return c['x'],c['y'],c['width'],c['height']

counts={}
for direction in ('left','right','auto'):
    a,pa=frame(direction+'-early');b,pb=frame(direction+'-late')
    assert a['layout']['moving'] and b['layout']['moving'],direction
    assert a['layout']['width']==b['layout']['width']
    x,y,w,h=clip_bounds(a)
    count=sum(max(abs(c-d) for c,d in zip(pa(px,py),pb(px,py)))>12 for py in range(y+3,y+h-3) for px in range(x+3,x+w-3))
    assert count>120,(direction,count)
    counts[direction]=count
    # No native pixels may leak onto the parchment beside the fixed viewport.
    for meta,sample in ((a,pa),(b,pb)):
        for px in (x-2,x+w+2):
            probes=[sample(px,py) for py in range(y+6,y+h-6)]
            expected=(0xEB,0xC9,0x9F)
            assert all(max(abs(c-d) for c,d in zip(p,expected))<=4 for p in probes),(direction,px,probes[:5])

a,pa=frame('motion-off');b,pb=frame('still-frame')
x,y,w,h=clip_bounds(a)
changed=sum(max(abs(c-d) for c,d in zip(pa(px,py),pb(px,py)))>12 for py in range(y,y+h) for px in range(x,x+w))
assert changed==0,changed
report=dict(result='PASS',animationPixels=counts,motionOffPixels=changed,viewportClipped=True)
(OUT/'warp-verification.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS warps',report)
