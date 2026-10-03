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

def chrome_probes(meta,sample):
    """Opaque illustrations must leave the surrounding template and controls visible."""
    layout=meta['layout'];effects=layout.get('effects',[]);paints=layout.get('paints',[])
    probes=0
    for y in range(4,layout['height']-4,4):
        for x in range(4,layout['width']-4,4):
            if any(e['x']-1<=x<e['x']+e['width']+1 and
                   e['y']-1<=y<e['y']+e['height']+1 for e in effects):continue
            top=None
            for paint in paints:
                if paint['x']-1<=x<paint['x']+paint['width']+1 and paint['y']-1<=y<paint['y']+paint['height']+1:
                    top=paint
            if top is None or top.get('text') is not None or top.get('icon') is not None:continue
            if not (top['x']+1<=x<top['x']+top['width']-1 and
                    top['y']+1<=y<top['y']+top['height']-1):continue
            rgb=top['color'];expected=(rgb>>16&255,rgb>>8&255,rgb&255)
            actual=sample(x+.5,y+.5)
            assert max(abs(a-b) for a,b in zip(actual,expected))<=3,(
                layout['section'],x,y,expected,actual,'Shader obscured template outside its component')
            probes+=1
    assert probes>100,(layout['section'],probes,'Insufficient visible template evidence')
    return probes

motion={};chrome=0
for file in sorted((OUT/'screenshots').glob('*.json')):
    meta,sample=frame(file.stem)
    chrome+=chrome_probes(meta,sample)
for name in ('horses','wheel','coinflip','bookofra'):
    early,a=frame(name+'-early');late,b=frame(name+'-late')
    effects=early['layout']['effects']
    assert len(effects)==(5 if name=='bookofra' else 1)
    changed=colours=0
    for effect in effects:
        assert effect['shader']['id'] in ('demo:race','demo:prize-wheel','demo:coin','demo:temple-reel'),effect
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
report=dict(result='PASS',games=4,consumerEffects=4,templatePixelProbes=chrome,gpu=motion)
(OUT/'casino-verification.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS casino',report)
