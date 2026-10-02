#!/usr/bin/env python3
"""Validate actual RGBA/native consumer rendering and runtime map geometry."""
from pathlib import Path
import json,math
from zipfile import ZipFile
from png_pixels import png
root=Path(__file__).resolve().parent.parent
out=root/'build/reports/e2e/dynamic'
first=json.loads((out/'screenshots/01-initial.json').read_text())
moving=json.loads((out/'screenshots/02-moving.json').read_text())
assert first['layout']['hits'][0]['x'] != moving['layout']['hits'][0]['x']
model=first['layout']['playerModels'][0]
assert model['renderer']=='proof:puppet' and (model['width'],model['height'])==(90,135)
width,height,pixel=png(out/'screenshots/01-initial.png')
# Own camera family must produce a full body, not just the native 8x8 profile glyph.
x0=int((first['canvasX']+model['x'])*first['scale']);y0=int((first['canvasY']+model['y'])*first['scale'])
occupied=[]
for y in range(y0,y0+int(model['height']*first['scale'])):
    occupied.append(sum(max(abs(a-b) for a,b in zip(pixel(x,y),(16,28,42)))>12 for x in range(x0,x0+int(model['width']*first['scale']))))
assert sum(v>3 for v in occupied)>model['height']*first['scale']*.55, 'Registered full body camera did not render'
reports=[]
colors=[(51,136,255),(240,109,192),(80,213,160),(255,202,85)]
for name in ('05-map-initial','06-map-moved','07-map-aimed'):
    meta=json.loads((out/f'screenshots/{name}.json').read_text());state=meta['layout']['layerStates']['calibration']
    width,height,pixel=png(out/f'screenshots/{name}.png')
    zoom=meta['layout']['zoom'];scale=height/(200+zoom*15)
    cx=width/2+(state['x']-560-meta['yaw']*8)*scale
    cy=height/2+(state['y']-320-meta['pitch']*8)*scale
    expected=[cx-(state['width']/2-1)*scale,cy-(state['height']/2-1)*scale,cx+(state['width']/2-1)*scale,cy+(state['height']/2-1)*scale]
    def match(actual,wanted):
        factor=sum(a*b for a,b in zip(actual,wanted))/sum(b*b for b in wanted)
        return .5<=factor<=1.04 and max(abs(a-b*factor) for a,b in zip(actual,wanted))<=4
    points=[(x,y) for y in range(max(0,math.floor(expected[1]-8)),min(height,math.ceil(expected[3]+8)))
        for x in range(max(0,math.floor(expected[0]-8)),min(width,math.ceil(expected[2]+8))) if any(match(pixel(x,y),c) for c in colors)]
    assert points, (name,'Runtime layer missing in rendered client')
    actual=[min(x for x,y in points),min(y for x,y in points),max(x for x,y in points),max(y for x,y in points)]
    error=max(abs(a-b) for a,b in zip(actual,expected));assert error<4,(name,actual,expected,error)
    reports.append(dict(frame=name,maxErrorPixels=error))
transparent=json.loads((out/'screenshots/08-map-transparent.json').read_text())
assert transparent['layout']['layerStates']['calibration']['opacity']==.5
_,_,opaque=png(out/'screenshots/07-map-aimed.png')
_,_,transparent_pixels=png(out/'screenshots/08-map-transparent.png')
# Vignette darkens the outer screen. An edge background sample is not the
# underpaint at a central marker cell. Use the matching pack's source background
# and fit only multiplicative brightness at each probe from the known opaque
# cell, keeping the same hue/alpha residual bounds as the geometry checks.
width,height,_=png(out/'screenshots/08-map-transparent.png')
with ZipFile(root/'build/pack/dui.zip') as pack:
    background=png(pack.read('assets/demo/textures/world-map/elsewhere/images/background.png'))[2](0,0)
alpha=round(transparent['layout']['layerStates']['calibration']['opacity']*255)/255
alpha_probes=[]
for (dx,dy),source in zip(((-30,-12),(30,-12),(-30,12),(30,12)),colors):
    x=round(width/2+dx*height/380);y=round(height/2+dy*height/380)
    actual_opaque=opaque(x,y)
    factor=sum(a*b for a,b in zip(actual_opaque,source))/sum(b*b for b in source)
    assert .5<=factor<=1.04 and max(abs(a-b*factor) for a,b in zip(actual_opaque,source))<=4, (x,y,actual_opaque,source,factor)
    wanted=tuple(round((a*alpha+b*(1-alpha))*factor) for a,b in zip(source,background))
    actual=transparent_pixels(x,y)
    error=max(abs(a-b) for a,b in zip(actual,wanted))
    assert error<=5,(x,y,actual,wanted)
    alpha_probes.append(dict(position=[x,y],actual=actual,expected=wanted,brightness=factor,maxError=error))
print('PASS independent consumer, full-body camera, RGBA groups, map position/size/input',reports)
(out/'dynamic-verification.json').write_text(json.dumps(dict(result='PASS',mapFrames=reports,alphaProbes=alpha_probes),indent=2)+'\n')
