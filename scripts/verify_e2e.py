#!/usr/bin/env python3
"""Verify current-run screenshots, native layout and client scenario assertions."""
from pathlib import Path
import sys, json, hashlib
from png_pixels import png
ROOT = Path(__file__).resolve().parent.parent
name = sys.argv[1]
out = ROOT / 'build/reports/e2e' / name
run = json.loads((out / 'run.json').read_text())
result = json.loads((out / 'client-result.json').read_text())
log = (out / 'client.log').read_text()
assert result['passed'] and result['inventoryUnchanged'], result
assert '_TEST_COMPLETE' in log and '_TEST_FAILED' not in log
for error in ("Couldn't compile", 'Failed to load required shader', 'Unable to load font', 'DecoderException', 'Missing texture references', 'Missing textures in model'):
    assert error not in log, error
hash = hashlib.sha1((ROOT / 'run/server/plugins/dui-demo/pack/dui.zip').read_bytes()).hexdigest()
screens = []
image_probes = 0
for file in sorted((out / 'screenshots').glob('*.png')):
    assert file.stat().st_mtime >= run['startedAt'], file
    meta = json.loads(file.with_suffix('.json').read_text())
    layout = meta['layout']
    assert layout['packSha1'] == hash
    images = layout.get('images', [])
    if images:
        _, _, pixel = png(file)
    for image in images:
        matched = 0
        probes = 0
        cell = image['pixelSize']
        for y in range(image['rows']):
            for x in range(image['columns']):
                rgb = image['rgb'][y * image['columns'] + x]
                expected = (rgb >> 16, rgb >> 8 & 255, rgb & 255)
                px = image['x'] + x * cell + min(cell, image['width'] - x * cell) / 2
                py = image['y'] + y * cell + min(cell, image['height'] - y * cell) / 2
                actual = pixel(int((meta['canvasX'] + px) * meta['scale']), int((meta['canvasY'] + py) * meta['scale']))
                probes += 1
                matched += max((abs(a - b) for a, b in zip(actual, expected))) <= 3
        assert matched / probes > 0.98, (file, matched, probes)
        image_probes += probes
    screens.append(file.name)
assert screens, 'No screenshots from the client'
report = dict(result='PASS', scenario=name, client=result, screenshots=screens, runtimeImageProbes=image_probes, packSha1=hash)
(out / 'verification.json').write_text(json.dumps(report, indent=2) + '\n')
cards = ''.join(('<section><h2>' + s.removesuffix('.png') + '</h2><img src="screenshots/' + s + '"></section>' for s in screens))
(out / 'index.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><title>dui demo / ' + name + '</title><style>body{max-width:1100px;margin:40px auto;background:#181c28;color:#edf0f8;font:16px system-ui}img{width:100%}</style><h1>dui demo / ' + name + '</h1>' + cards + '</html>')
print('PASS', name, 'steps=' + str(result['steps']), 'screenshots=' + str(len(screens)), 'runtimeImageProbes=' + str(image_probes))
if name in ('slots', 'confetti', 'advent'):
    import subprocess
    subprocess.run([sys.executable, str(ROOT / 'scripts' / ('verify_' + name + '.py'))], check=True)
if name == 'shop':
    import os, subprocess
    command = [str(ROOT / 'gradlew'), 'verifyShopScreenshots', '--console=plain']
    if os.environ.get('JAVA_HOME'):
        command.insert(1, '-Dorg.gradle.java.home=' + os.environ['JAVA_HOME'])
    subprocess.run(command, cwd=ROOT, check=True)
