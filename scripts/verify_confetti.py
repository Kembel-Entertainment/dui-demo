#!/usr/bin/env python3
"""Measure the actual canvas overlay outside native item bounds, including its fade and cancellation."""
from datetime import datetime
from pathlib import Path
import json
from png_pixels import png

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'build/reports/e2e/confetti'
started = datetime.fromtimestamp(json.loads((OUT / 'run.json').read_text())['startedAt'])
log = (OUT / 'client.log').read_text()
assert 'CONFETTI_TEST_COMPLETE' in log and 'CONFETTI_TEST_FAILED' not in log
for error in ('Shader compilation failed', 'Failed to load shader', 'Missing textures in model', "Couldn't parse"):
    assert error not in log, error
result = json.loads((OUT / 'client-result.json').read_text())
assert result['passed'] and result['inventoryUnchanged'] and result['muted']

def screenshot(name):
    file = OUT / 'screenshots' / f'{name}.png'
    assert file.stat().st_mtime >= started.timestamp(), name
    meta = json.loads(file.with_suffix('.json').read_text())
    _, _, pixel = png(file)
    def sample(x, y):
        return pixel(int((meta['canvasX']+x+.5)*meta['scale']), int((meta['canvasY']+y+.5)*meta['scale']))
    return meta, sample

def difference(a, b, compact=False):
    meta, first = screenshot(a)
    other, second = screenshot(b)
    assert meta['layout']['width'] == other['layout']['width']
    width, height = meta['layout']['width'], meta['layout']['height']
    items = meta['layout']['items']
    changed = [0, 0]
    for y in range(4, (126 if compact else height-4)):
        for x in range(4, width-4):
            if any(i['x']-1 <= x < i['x']+i['size']+1 and i['y']-1 <= y < i['y']+i['size']+1 for i in items):
                continue
            if first(x,y) != second(x,y):
                changed[int(x >= width/2)] += 1
    return changed

wide = difference('wide-early', 'wide-settled')
late = difference('wide-late', 'wide-settled')
moving = difference('wide-early', 'wide-late')
compact = difference('compact-early', 'motion-disabled', True)
compact_late = difference('compact-late', 'motion-disabled', True)
still = difference('motion-disabled', 'motion-still', True)
clean = difference('wide-settled', 'wide-clean')
assert min(wide) > 30 and min(late) > 30, (wide,late)
assert sum(moving) > 200, moving
assert min(compact) > 10 and min(compact_late) > 10, (compact,compact_late)
assert still == [0,0], still
assert clean == [0,0], f'Effect did not fade completely: {clean}'
for name in ('wide-early','compact-early'):
    assert 'confetti' in screenshot(name)[0]['layout']
for name in ('wide-clean','motion-disabled','motion-still'):
    assert 'confetti' not in screenshot(name)[0]['layout']

report = dict(result='PASS', clientSteps=result['steps'], muted=True, inventoryUnchanged=True,
              changedPixelsOutsideItems=dict(wide=wide, wideLater=late, movement=moving,
                                            compact=compact, compactLater=compact_late,
                                            motionOff=still, afterFade=clean),
              checks=['burst spans both halves of the menu', 'Spacious and Compact', 'actual animated pixels',
                      'full fade before marker cleanup', 'no per-frame dialog replacement',
                      'buttons work through confetti', 'motion off stops burst', 'motion on does not replay',
                      'Escape during burst stays closed past expiry', 'reopen does not replay'])
(OUT / 'confetti-verification.json').write_text(json.dumps(report,indent=2)+'\n')
names = ['wide-early','wide-late','wide-settled','wide-clean','compact-early','compact-late','motion-disabled','motion-still']
cards = ''.join(f'<section><h2>{n.replace("-"," ").title()}</h2><img src="screenshots/{n}.png"></section>' for n in names)
(OUT / 'index.html').write_text('''<!doctype html><html lang="en"><meta charset="utf-8"><title>dui demo / Confetti burst</title>
<style>body{max-width:1100px;margin:40px auto;background:#251e36;color:#fff9ed;font:16px/1.6 system-ui;padding:24px}a{color:#ffdf75}img{width:100%}section{margin:32px 0}</style>
<h1>dui demo / Confetti burst</h1><p>Real Minecraft screenshots: a finite UI shader burst with working controls.</p>
<p><a href="confetti-verification.json">Pixel verification report</a></p>'''+cards+'</html>')
print('PASS:',json.dumps(report))
