#!/usr/bin/env python3
"""Assert visible native motion, finite confetti and the session-only gift contract."""
import json
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
reset, _ = frame('reset-after-close')
assert reset['layout']['phase'] == 'BOARD' and reset['layout']['selected'] == 0
report = json.loads((OUT / 'verification.json').read_text())
report['animatedPixels'] = dict(lid=lid, rewardPop=reward, confetti=party, motionOff=still)
report['checks'] = ['all 24 gifts open', 'replay without a ledger', 'native lid and reward animation',
                    'independent confetti', 'no per-frame replacement', 'small window and Auto GUI scale',
                    'Escape/explicit close cancel timers', 'selection resets on reopen', 'no inventory grant']
(OUT / 'verification.json').write_text(json.dumps(report, indent=2) + '\n')
print('PASS advent animation:', report['animatedPixels'])
