#!/usr/bin/env python3
"""Verify the shared actual-client acceptance and known map/video pixels."""
import hashlib
import json
import sys
from pathlib import Path
from png_pixels import png

ROOT = Path(__file__).resolve().parent.parent

def verify(backend):
    out = ROOT / 'build/reports/e2e/platform' / backend
    run = json.loads((out / 'run.json').read_text())
    result = json.loads((out / 'client-result.json').read_text())
    log = (out / 'client.log').read_text()
    assert result['passed'] and result['steps'] == 10 and result['backend'] == backend, result
    assert result['count'] == 1 and result['text'] == 'Both platforms' and result['enabled'] is False, result
    assert result['booleanResponseVerified'] and result['inventoryUnchanged'] and result['closed'], result
    assert result['videoChanges'] >= 3 and 'PLATFORM_TEST_COMPLETE' in log and 'PLATFORM_TEST_FAILED' not in log
    for error in ("Couldn't compile", 'Failed to load required shader', 'Unable to load font', 'DecoderException', 'Missing textures in model'):
        assert error not in log, error
    digest = hashlib.sha1((ROOT / 'build/pack/dui.zip').read_bytes()).hexdigest()
    assert result['packSha1'] == digest
    files = ['screenshots/portable.png', 'screenshots/input-response.png', 'map.png', 'video.png']
    assert all((out / name).stat().st_mtime >= run['startedAt'] for name in files)
    presented = json.loads((out / 'screenshots/input-response.json').read_text())
    assert presented['layout']['bodyCount'] >= (7 if backend == 'minestom' else 3), presented['layout']['bodyCount']
    if backend == 'minestom':
        _, _, figure = png(out / 'screenshots/input-response.png')
        cx, cy, scale = presented['canvasX'], presented['canvasY'], presented['scale']
        boot_pixels = sum(g > r + 20 and b > r + 25
            for y in range(27 + 100, 27 + 144) for x in range(252, 252 + 96)
            for r, g, b in [figure(int((cx + x) * scale), int((cy + y) * scale))])
        assert boot_pixels > 30, ('Native diamond boots missing', boot_pixels)
    # A changing sky also produces changing hashes. Require the producer's green
    # vertical gradient and steady blue channel in the received video image.
    width, height, pixel = png(out / 'video.png')
    top, bottom = pixel(width // 3, height // 9), pixel(width // 3, 5 * height // 6)
    assert bottom[1] > top[1] + 70 and 60 <= top[2] <= 150 and 60 <= bottom[2] <= 150, (top, bottom)
    colors = {pixel(x, y) for y in range(height // 5, 4 * height // 5, 9) for x in range(width // 5, 4 * width // 5, 9)}
    assert len(colors) >= 128, ('Video gradient missing', len(colors))
    video_colors = len(colors)
    # Check the known atlas contains both green land and its blue river.
    width, height, pixel = png(out / 'map.png')
    colors = [pixel(x, y) for y in range(height // 5, 4 * height // 5, 7) for x in range(width // 5, 4 * width // 5, 7)]
    assert sum(g > r + 10 and g > b + 5 for r, g, b in colors) >= 100
    assert sum(b > r + 20 and g > r + 15 for r, g, b in colors) >= 100
    report = dict(result='PASS', backend=backend, client=result, packSha1=digest, screenshots=files, videoColors=video_colors)
    (out / 'verification.json').write_text(json.dumps(report, indent=2) + '\n')
    (out / 'index.html').write_text('<!doctype html><meta charset="utf-8"><title>Portable DUI</title>' +
        ''.join('<h2>' + name + '</h2><img style="max-width:100%" src="' + name + '">' for name in files))
    print('PASS portable dialog / map / video pixels:', backend)

if __name__ == '__main__':
    for backend in sys.argv[1:] or ['paper', 'minestom']:
        if backend not in ('paper', 'minestom'): raise ValueError('Backend must be paper or minestom')
        verify(backend)
