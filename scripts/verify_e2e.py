#!/usr/bin/env python3
"""Verify current-run screenshots, native layout and client scenario assertions."""
from pathlib import Path
import sys, json, hashlib, os
from png_pixels import png
ROOT = Path(__file__).resolve().parent.parent
name = sys.argv[1]
out = ROOT / 'build/reports/e2e' / name
(out / 'verification.json').write_text(json.dumps(dict(result='CHECKING', scenario=name)) + '\n')
run = json.loads((out / 'run.json').read_text())
result = json.loads((out / 'client-result.json').read_text())
log = (out / 'client.log').read_text()
assert result['passed'] and (result.get('inventoryUnchanged') or result.get('inventoryConserved')), result
assert '_TEST_COMPLETE' in log and '_TEST_FAILED' not in log
for error in ("Couldn't compile", 'Failed to load required shader', 'Unable to load font', 'DecoderException', 'Missing texture references', 'Missing textures in model'):
    assert error not in log, error
hash = hashlib.sha1((ROOT / os.environ.get('DUI_RUN_DIR','run/server') / 'plugins/dui-demo/pack/dui.zip').read_bytes()).hexdigest()
if name == 'gba':
    files = sorted(out.glob('*.png'))
    assert len(files) >= 3, files
    for file in files:
        assert file.stat().st_mtime >= run['startedAt'], file
    for image_name in ('fullscreen.png', 'camera-turned.png', 'auto-scale.png'):
        width, height, pixel = png(out / image_name)
        colors = {pixel(x, y) for y in range(height//4, 3*height//4, 4) for x in range(width//4, 3*width//4, 4)}
        assert len(colors) >= 128, ('Video image absent or corrupted', image_name, len(colors))
        assert (255,0,255) not in colors, 'Invalid transport symbol visible'
    assert result['visibleFps'] > 50, result
    assert ' / keys 256 / ' in log, 'Held vanilla jump input never reached the worker as GBA A'
    report = dict(result='PASS',scenario=name,client=result,screenshots=[p.name for p in files],packSha1=hash)
    (out / 'verification.json').write_text(json.dumps(report,indent=2)+'\n')
    (out / 'index.html').write_text('<!doctype html><meta charset="utf-8"><title>GBA renderer</title><h1>GBA renderer</h1>'+''.join('<img style="width:90%" src="'+p.name+'">' for p in files))
    print('PASS gba visible FPS',result['visibleFps'])
    sys.exit(0)
screens = []
image_probes = 0
background_probes = 0
image_capacity = 0
background_capacity = 0
occluded_images = []
for file in sorted((out / 'screenshots').glob('*.png')):
    assert file.stat().st_mtime >= run['startedAt'], file
    meta = json.loads(file.with_suffix('.json').read_text())
    layout = meta['layout']
    assert layout['packSha1'] == hash
    images = layout.get('images', [])
    if images:
        _, _, pixel = png(file)
    # Foreground rasters are below native/effect bodies and portraits. A translucent
    # effect's declared bounds conservatively cover it: its alpha changes each frame.
    late_occluders = []
    for kind in ('effects', 'heads', 'items', 'playerModels'):
        for shape in layout.get(kind, []):
            late_occluders.append((shape['x'], shape['y'], shape.get('width', shape.get('size', 0)), shape.get('height', shape.get('size', 0))))
    for index, image in enumerate(images):
        capacity = image['rows'] * image['columns']
        image_capacity = max(image_capacity, capacity)
        if image.get('background', False):
            background_capacity = max(background_capacity, capacity)
        occluders = list(late_occluders)
        if image.get('background', False):
            occluders.extend((p['x'], p['y'], p['width'], p['height']) for p in layout.get('paints', []))
        occluders.extend((i['x'], i['y'], i['width'], i['height']) for position,i in enumerate(images)
                         if not i.get('background', False) and (image.get('background', False) or position > index))
        matched = 0
        probes = 0
        cell = image['pixelSize']
        for y in range(image['rows']):
            for x in range(image['columns']):
                rgb = image['rgb'][y * image['columns'] + x]
                expected = (rgb >> 16, rgb >> 8 & 255, rgb & 255)
                alpha = ((image.get('argb', [])[y * image['columns'] + x] >> 24) & 255) if 'argb' in image else 255
                if alpha == 0:
                    continue
                px = image['x'] + x * cell + min(cell, image['width'] - x * cell) / 2
                py = image['y'] + y * cell + min(cell, image['height'] - y * cell) / 2
                if any(x0-1 <= px < x0+w+1 and y0-1 <= py < y0+h+1 for x0,y0,w,h in occluders):
                    continue
                if alpha != 255:
                    base = None
                    for paint in layout.get('paints', []):
                        if paint.get('text') is None and paint.get('icon') is None and paint['x'] <= px < paint['x']+paint['width'] and paint['y'] <= py < paint['y']+paint['height']:
                            color=paint['color'];base=(color >> 16 & 255,color >> 8 & 255,color & 255)
                    assert base is not None, (file,image['id'],'Alpha probe needs a known background')
                    opacity=min(15,round(alpha/17))/15
                    expected=tuple(round(a*opacity+b*(1-opacity)) for a,b in zip(expected,base))
                actual = pixel(int((meta['canvasX'] + px) * meta['scale']), int((meta['canvasY'] + py) * meta['scale']))
                probes += 1
                matched += max((abs(a - b) for a, b in zip(actual, expected))) <= 3
        if probes == 0:
            assert occluders, (file, image['id'], 'Image produced no samples')
            occluded_images.append(dict(screenshot=file.name,image=image['id']))
            continue
        if image.get("background", False):
            assert probes >= min(501, capacity), (file, probes, "Insufficient visible background")
            background_probes += probes
        assert matched / probes > 0.98, (file, matched, probes)
        image_probes += probes
    screens.append(file.name)
assert screens, 'No screenshots from the client'
if image_capacity:
    assert image_probes >= min(501, image_capacity), 'Capture an unobstructed image view as well as animated/covered views'
if background_capacity:
    assert background_probes >= min(501, background_capacity), 'No unobstructed background-image evidence; check opaque paints above images'
report = dict(result='PASS', scenario=name, client=result, screenshots=screens, runtimeImageProbes=image_probes, backgroundImageProbes=background_probes, occludedImageFrames=occluded_images, packSha1=hash)
if name in ('slots', 'confetti', 'advent', 'warps', 'poker', 'roulette', 'blackjack', 'protocol', 'casino', 'character', 'map', 'dynamic'):
    import subprocess
    subprocess.run([sys.executable, str(ROOT / 'scripts' / ('verify_' + name + '.py'))], check=True)
if name == 'shop':
    import os, subprocess
    command = [str(ROOT / 'gradlew'), 'verifyShopScreenshots', '--console=plain']
    if os.environ.get('JAVA_HOME'):
        command.insert(1, '-Dorg.gradle.java.home=' + os.environ['JAVA_HOME'])
    subprocess.run(command, cwd=ROOT, check=True)
# Publish success only after every scenario-specific check has completed.
(out / 'verification.json').write_text(json.dumps(report, indent=2) + '\n')
cards = ''.join(('<section><h2>' + s.removesuffix('.png') + '</h2><img src="screenshots/' + s + '"></section>' for s in screens))
(out / 'index.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><title>dui demo / ' + name + '</title><style>body{max-width:1100px;margin:40px auto;background:#181c28;color:#edf0f8;font:16px system-ui}img{width:100%}</style><h1>dui demo / ' + name + '</h1>' + cards + '</html>')
print('PASS', name, 'steps=' + str(result['steps']), 'screenshots=' + str(len(screens)), 'runtimeImageProbes=' + str(image_probes))
