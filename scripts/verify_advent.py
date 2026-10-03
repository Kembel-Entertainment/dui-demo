#!/usr/bin/env python3
"""Assert visible native motion, finite confetti and the session-only gift contract."""
import json, os, zipfile
from pathlib import Path
from png_pixels import png

OUT = Path(__file__).resolve().parent.parent / 'build/reports/e2e/advent'
result = json.loads((OUT / 'client-result.json').read_text())
assert result['giftsOpened'] == 24 and result['muted'] and result['inventoryUnchanged']

def frame(name):
    file = OUT / 'screenshots' / (name + '.png')
    meta = json.loads(file.with_suffix('.json').read_text())
    _, _, pixel = png(file)
    def sample(x, y):
        return pixel(int((meta['canvasX'] + x + .5) * meta['scale']),
                     int((meta['canvasY'] + y + .5) * meta['scale']))
    return meta, sample

def difference(a, b, rect):
    first, p = frame(a)
    second, q = frame(b)
    assert first['layout']['width'] == second['layout']['width']
    x, y, w, h = rect
    return sum(p(xx, yy) != q(xx, yy) for yy in range(y, y+h) for xx in range(x, x+w))

lid = difference('lid-early', 'lid-late', (20, 63, 150, 153))
reward = difference('reward-early', 'reward-late', (50, 117, 72, 72))
party = difference('reveal', 'party-late', (9, 63, 459, 180))
still = difference('motion-off', 'still-frame', (9, 63, 459, 180))
assert lid > 100, lid
assert reward > 30, reward
assert party > 30, party
assert still == 0, still

# The final pose must show the actual bow above the closed-box position, including
# after timed effects stop. A changing picture alone cannot prove correct placement.
settled, sample = frame('open-settled')
layout = settled['layout']
assert layout['state']['effectsFinished'], 'Capture the settled pose'
body = next(i for i in layout['items'] if i['id'] == 'parcel_body')
top = next(i for i in layout['items'] if i['id'] == 'parcel_lid')
assert top['y'] < body['y'], 'Lid settled back onto the box'
root = Path(__file__).resolve().parent.parent
pack = root / os.environ.get('DUI_RUN_DIR', 'run/server') / 'plugins/dui-demo/pack/dui.zip'
with zipfile.ZipFile(pack) as archive:
    _, _, art = png(archive.read(f"assets/dui_demo/textures/item/advent/lid_{layout['selected'] % 6}.png"))
lid_color = art(36, 40)
raised_pixels = sum(
    max(abs(a-b) for a,b in zip(sample(x,y),lid_color)) <= 3
    for y in range(max(0, top['y']), int(body['y'] + body['size'] * 19 / 96))
    for x in range(top['x'], top['x'] + top['size']))
assert raised_pixels > 100, (raised_pixels, 'Raised lid artwork missing from its destination')

# Particle drawing belongs to the stage, with local emission coordinates.
for name in ('reward-late', 'reveal', 'party-late', 'compact-reveal', 'auto-reveal'):
    meta, _ = frame(name)
    l = meta['layout']
    effect = next(e for e in l['effects'] if e['id'] == 'gift_confetti')
    assert effect['y'] >= 27, (name, 'Particles cover heading')
    assert effect['y'] + effect['height'] <= l['height'] - 27, (name, 'Particles cover controls')
    parcel = next((i for i in l['items'] if i['id'] == 'parcel_body'), None)
    if parcel:
        assert effect['x'] + (effect['parameters']['a'] & 511) == parcel['x'] + parcel['size'] // 2
        assert effect['y'] + (effect['parameters']['b'] & 511) == parcel['y'] + parcel['size'] // 2
reset, _ = frame('reset-after-close')
assert reset['layout']['phase'] == 'BOARD' and reset['layout']['selected'] == 0
report = json.loads((OUT / 'verification.json').read_text())
report['animatedPixels'] = dict(lid=lid, rewardPop=reward, confetti=party, motionOff=still)
report['raisedLidPixels'] = raised_pixels
report['checks'] = ['all 24 gifts open', 'replay without a ledger', 'native lid and reward animation',
                    'independent confetti', 'no per-frame replacement', 'small window and Auto GUI scale',
                    'Escape/explicit close cancel timers', 'selection resets on reopen', 'no inventory grant',
                    'lid remains raised after settling', 'confetti stays in the stage with local emission coordinates']
(OUT / 'verification.json').write_text(json.dumps(report, indent=2) + '\n')
print('PASS advent animation:', report['animatedPixels'])
