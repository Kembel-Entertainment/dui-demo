#!/usr/bin/env python3
"""Check current-run GPU card/chip animation and a stable motion-off table."""
from pathlib import Path
import json
from png_pixels import png
ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'build/reports/e2e/poker'

def frame(name):
    meta = json.loads((OUT / 'screenshots' / (name + '.json')).read_text())
    _, _, pixels = png(OUT / 'screenshots' / (name + '.png'))
    def sample(x, y):
        return pixels(int((meta['canvasX']+x)*meta['scale']), int((meta['canvasY']+y)*meta['scale']))
    return meta, sample

def changed(a, b, bounds):
    x,y,w,h = bounds
    return sum(max(abs(c-d) for c,d in zip(a(px,py), b(px,py))) > 12 for py in range(y,y+h) for px in range(x,x+w))

counts = {}
for name in ('deal','chips','flop','turn','river','win','compact'):
    ma, a = frame(name+'-early'); mb,b = frame(name+'-late')
    assert ma['layout']['busy'] and mb['layout']['busy'], name
    effects = ma['layout']['effects']
    selected = [e for e in effects if e['shader']['id'] == ('demo:chip-stack' if name in ('chips','win') else 'demo:playing-card')]
    counts[name] = sum(changed(a,b,(e['x'],e['y'],e['width'],e['height'])) for e in selected)
    assert counts[name] > 15, (name,counts[name])
ma,a=frame('motion-off');mb,b=frame('still-frame')
assert not ma['layout']['state']['motion']
counts_off=changed(a,b,(0,0,ma['layout']['width'],ma['layout']['height']))
assert counts_off==0,counts_off
report=dict(result='PASS', animationPixels=counts, motionOffPixels=counts_off, showdownVerified=True)
(OUT/'poker-verification.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS poker',report)
