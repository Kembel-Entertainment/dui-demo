#!/usr/bin/env python3
"""GPU assertions for the versioned public motion/batching/coverage contracts."""
from pathlib import Path
import json
from png_pixels import png
OUT=Path(__file__).resolve().parent.parent/'build/reports/e2e/protocol'
def frame(name):
 meta=json.loads((OUT/'screenshots'/f'{name}.json').read_text());_,_,pixel=png(OUT/'screenshots'/f'{name}.png')
 def sample(x,y):return pixel(int((meta['canvasX']+x)*meta['scale']),int((meta['canvasY']+y)*meta['scale']))
 return meta,sample
meta,sample=frame('batched');effects=meta['layout']['effects'];assert len(effects)==14
for effect in effects:
 if effect['id'].startswith('light_'):
  rgb=sample(effect['x']+effect['width']/2,effect['y']+effect['height']/2)
  assert rgb[0]>100 and rgb[1]>50,(effect['id'],rgb)
# Consumer-owned GLSL contribution must render too; this is not merely a manifest assertion.
pulse=sample(192,117);assert pulse[0]>130 and pulse[2]>60,pulse
a,pa=frame('motion-early');b,pb=frame('motion-final')
changed={}
for label,(x,y,w,h) in {'card':(9,81,90,81),'native':(240,117,60,36)}.items():
 count=sum(max(abs(a-b) for a,b in zip(pa(px,py),pb(px,py)))>12 for py in range(y,y+h) for px in range(x,x+w));assert count>20,(label,count);changed[label]=count
# Motion-off must be stable across different client game times, including custom effects.
a,pa=frame('still');b,pb=frame('still-later');still=sum(max(abs(a-b) for a,b in zip(pa(x,y),pb(x,y)))>12 for y in range(27,153) for x in range(12,348));assert still==0,still
popup,pp=frame('popup');assert not popup['layout']['images'];assert not popup['layout']['heads'];assert 'native' not in [i['id'] for i in popup['layout']['items']]
# No native/object pixels may cover the popup interior at the previous sample locations.
for point in [(274,135),(296,135)]:
 rgb=pp(*point);assert max(abs(a-b) for a,b in zip(rgb,(0x22,0x23,0x2b)))<5,(point,rgb)
journal,_=frame('journal-page-two');assert journal['layout']['page']==1
wide,_=frame('journal-wide');assert wide['layout']['width']==480
result=json.loads((OUT/'client-result.json').read_text());report=dict(result='PASS',effects=14,builtInLights=12,consumerShader=True,animationPixels=changed,motionOffPixels=still,popupCoverage=True,averageSampledFps=result.get('averageSampledFps'),fpsCap=30,componentBytes=meta['layout']['componentBytes'],dialogBodies=meta['layout'].get('bodyCount'),renderNanos=meta['layout'].get('renderNanos'))
(OUT/'protocol-verification.json').write_text(json.dumps(report,indent=2)+'\n');print('PASS protocol',report)
