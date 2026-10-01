#!/usr/bin/env python3
"""Validate that consumer shaders render, animate and settle in actual Minecraft frames."""
from pathlib import Path
import json
from png_pixels import png
OUT=Path(__file__).resolve().parent.parent/'build/reports/e2e/casino'

def frame(name):
    meta=json.loads((OUT/'screenshots'/f'{name}.json').read_text())
    _,_,pixel=png(OUT/'screenshots'/f'{name}.png')
    def sample(x,y):
        return pixel(int((meta['canvasX']+x)*meta['scale']),int((meta['canvasY']+y)*meta['scale']))
    return meta,sample

motion={}
for name in ('horses','wheel','coinflip','bookofra'):
    early,a=frame(name+'-early');late,b=frame(name+'-late')
    effects=early['layout']['effects']
    assert len(effects)==(5 if name=='bookofra' else 1)
    changed=colours=0
    for effect in effects:
        assert 'id' in effect['kind'] and effect['kind']['code'] in (9,10,11,12),effect
        for y in range(effect['y'],effect['y']+effect['height']):
            for x in range(effect['x'],effect['x']+effect['width']):
                pa=a(x,y);pb=b(x,y)
                changed+=max(abs(c-d) for c,d in zip(pa,pb))>18
                colours+=max(pa)-min(pa)>35
    assert changed>80,(name,changed,'No GPU animation')
    assert colours>100,(name,colours,'Missing consumer illustration')
    first,a=frame(name+'-still');second,b=frame(name+'-still-later')
    stable=sum(max(abs(c-d) for c,d in zip(a(x,y),b(x,y)))>12
        for y in range(first['layout']['height']) for x in range(first['layout']['width']))
    assert stable==0,(name,stable,'Motion off is unstable')
    motion[name]=dict(animationPixels=changed,illustrationPixels=colours,motionOffPixels=stable)
    for suffix in ('rules-wide','rules-compact'):
        meta=json.loads((OUT/'screenshots'/f'{name}-{suffix}.json').read_text())
        assert not meta['layout']['effects'],'Rules are covered by an effect'
report=dict(result='PASS',games=4,consumerEffects=4,libraryChanges=0,gpu=motion)
(OUT/'casino-verification.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS casino',report)
