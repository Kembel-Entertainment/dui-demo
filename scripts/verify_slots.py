#!/usr/bin/env python3
"""Compare actual slot-machine framebuffer regions; reject stale or failed client captures."""
from datetime import datetime
from pathlib import Path
import json
from png_pixels import png

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'build/reports/e2e/slots'
started = datetime.fromtimestamp(json.loads((OUT / 'run.json').read_text())['startedAt'])
log = (OUT / 'client.log').read_text()
assert 'SLOT_TEST_COMPLETE' in log and 'SLOT_TEST_FAILED' not in log
for error in ("Couldn't compile", 'Failed to load required shader', 'Missing textures in model', "Couldn't parse"):
    assert error not in log, error
result = json.loads((OUT / 'client-result.json').read_text())
assert result['passed'] and result['muted'] and result['inventoryUnchanged']
assert result['templateReload'] and result['packUnchanged']
cache = {}
def screenshot(name):
    if name not in cache:
        file = OUT / 'screenshots' / f'{name}.png'
        assert file.stat().st_mtime >= started.timestamp(), name
        meta = json.loads(file.with_suffix('.json').read_text())
        _, _, pixel = png(file)
        def sample(x, y):
            return pixel(int((meta['canvasX']+x+.5)*meta['scale']), int((meta['canvasY']+y+.5)*meta['scale']))
        cache[name] = meta, sample
    return cache[name]
def difference(first, second, area):
    _, a = screenshot(first)
    _, b = screenshot(second)
    x, y, width, height = area
    return sum(a(xx, yy) != b(xx, yy) for yy in range(y,y+height) for xx in range(x,x+width))

def effects(name, kind):
    return [e for e in screenshot(name)[0]['layout']['effects'] if e['shader']['id'] == kind]

def bounds(effect):
    return tuple(effect[key] for key in ('x','y','width','height'))

moving = [difference('wide-pull','wide-spin',bounds(e)) for e in effects('wide-spin','demo:reel')]
assert min(moving) > 200, moving
lever = difference('wide-idle','wide-pull',bounds(effects('wide-pull','demo:lever')[0]))
assert lever > 80, lever
# First reel has stopped at 2.4 s; the third continues until 3.35 s.
sequential = [difference('wide-stop-one','wide-stop-two',bounds(e)) for e in effects('wide-stop-one','demo:reel')]
assert sequential[0] == 0 and sequential[2] > 100, sequential
# Motion off produces byte-identical opaque canvas samples (the bevel corners expose the world).
still = difference('compact-no-motion','compact-still',(2,2,296,140))
assert still == 0, still
# Coins emerge outside reels and lever; this area contains no animated text or buttons.
coins = difference('wide-coins','wide-finished',(27,198,252,22))
compact_coins = difference('compact-coins','compact-finished',(15,40,222,53))
assert coins > 15 and compact_coins > 10, (coins,compact_coins)
# All settled 777 reel centers contain our red seven artwork, not transparent/missing carriers.
_, sample = screenshot('wide-finished')
red = []
for i in range(3):
    red.append(sum((lambda rgb: rgb[0]>150 and rgb[1]<110 and rgb[2]<140)(sample(x,y)) for y in range(101,151) for x in range(42+i*84,90+i*84)))
assert min(red) > 100, red
# HTML-only reload changes the shader's actual pixels, with the same pack hash.
dynamic_reels = effects('dynamic-finished','demo:reel')
assert [bounds(e) for e in dynamic_reels] == [(39,90,60,72),(117,90,60,72),(195,90,60,72)]
_, dynamic = screenshot('dynamic-finished')
dynamic_red = []
for e in dynamic_reels:
    x,y,w,h = bounds(e)
    dynamic_red.append(sum((lambda rgb: rgb[0]>150 and rgb[1]<110 and rgb[2]<140)(dynamic(xx,yy))
        for yy in range(y+h//4,y+3*h//4) for xx in range(x+w//4,x+3*w//4)))
assert min(dynamic_red)>100, dynamic_red

# Force the real native widget into focus. Only marked canvases mask its white border.
def focus_white(name):
    meta,_ = screenshot(name)
    assert meta['focused'], name
    screen_w,screen_h,pixel = png(OUT/'screenshots'/f'{name}.png')
    x,y,w,h = [int(meta[k]) for k in ('widgetX','widgetY','widgetWidth','widgetHeight')]
    edges = {(xx,yy) for yy in (y,y+h-1) for xx in range(x,x+w)}
    edges |= {(xx,yy) for xx in (x,x+w-1) for yy in range(y,y+h)}
    # Compact's padded native widget can extend beyond the viewport while its canvas fits.
    samples = [(int((xx+.5)*meta['scale']),int((yy+.5)*meta['scale'])) for xx,yy in edges]
    return sum(min(pixel(px,py))>245 for px,py in samples if 0<=px<screen_w and 0<=py<screen_h)
white = {name:focus_white(name) for name in ('wide-focused','compact-focused','wide-native-focused')}
assert white['wide-focused']==0 and white['compact-focused']==0, white
assert white['wide-native-focused']>500, white
assert not screenshot('wide-native-focused')[0]['layout']['focusOutlineHidden']
assert screenshot('compact-spin')[0]['guiSetting'] == 0
for name in ('wide-finished','compact-finished'):
    assert screenshot(name)[0]['layout']['effect']['startedAt'] == -1
report = dict(result='PASS',clientSteps=result['steps'],muted=True,inventoryUnchanged=True,
    templateReload=True,packUnchanged=True,focusBorderWhitePixels=white,
    changedPixels=dict(reels=moving,lever=lever,sequentialStops=sequential,coins=coins,compactCoins=compact_coins,motionOff=still,dynamicRed=dynamic_red),
    checks=['real lever clicks','three rolling reels stop sequentially','recognizable 777 artwork','finite coin payout',
            'preview modes preserve chip balance','charged spin settles while closed','Compact with GUI Auto','no per-frame dialog replacement','no inventory changes',
            'HTML reload moves and resizes shader components without rebuilding the pack','white focus border masked on tagged canvases','native focus border remains available'])
(OUT / 'slots-verification.json').write_text(json.dumps(report,indent=2)+'\n')
names = ['wide-idle','wide-focused','wide-pull','wide-spin','wide-stop-one','wide-stop-two','wide-coins','wide-more-coins','wide-finished','wide-pair','wide-miss','compact-spin','compact-coins','compact-finished','compact-focused','compact-payouts','compact-no-motion','dynamic-finished','wide-native-focused']
cards = ''.join(f'<section><h2>{n.replace("-"," ").title()}</h2><img src="screenshots/{n}.png"></section>' for n in names)
(OUT / 'index.html').write_text('''<!doctype html><html lang="en"><meta charset="utf-8"><title>dui demo / Demo arcade</title>
<style>body{max-width:1100px;margin:40px auto;background:#211429;color:#fff0c9;font:16px/1.6 system-ui;padding:24px}a{color:#f1bf68}img{width:100%}section{margin:32px 0}</style>
<h1>dui demo / Demo arcade</h1><p>Real Minecraft screenshots: procedural reels, a clickable lever and gold coin payout.</p>
<p><a href="slots-verification.json">Pixel verification report</a></p>'''+cards+'</html>')
print('PASS:',json.dumps(report))
