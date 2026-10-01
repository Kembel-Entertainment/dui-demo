#!/usr/bin/env python3
"""GPU figure stability and complete geometry, using real current-run screenshots."""
from pathlib import Path
import json
from png_pixels import png
out=Path(__file__).resolve().parent.parent/'build/reports/e2e/character'
def frame(name):
    meta=json.loads((out/'screenshots'/f'{name}.json').read_text()); _,_,pixel=png(out/'screenshots'/f'{name}.png')
    def sample(x,y):return pixel(int((meta['canvasX']+x)*meta['scale']),int((meta['canvasY']+y)*meta['scale']))
    return meta,sample
meta,first=frame('still');later,last=frame('still-later');model=meta['layout']['playerModels'][0]
changed=sum(first(x,y)!=last(x,y) for y in range(model['y'],model['y']+model['height']) for x in range(model['x'],model['x']+model['width']))
assert changed==0,('Still figure changed',changed)
# Figure surface pixels must differ from the bare figure across all armor regions.
bare,bp=frame('bare-wide');armor,ap=frame('full-armor')
regions={}
for name,(dy,h) in {'helmet':(4,45),'chest':(54,63),'legs':(117,45),'boots':(162,45)}.items():
    count=sum(max(abs(a-b) for a,b in zip(bp(x,y),ap(x,y)))>25 for y in range(model['y']+dy,model['y']+dy+h) for x in range(model['x']+35,model['x']+105))
    assert count>100,(name,count)
    regions[name]=count
angles={json.loads(path.read_text())['layout']['facing'] for path in (out/'screenshots').glob('facing-*.json')};angles.add(meta['layout']['facing']);assert angles==set(range(8)),angles
popup,_=frame('head-dropdown');assert popup['layout']['images'], 'Popup removed canvas background'
compact,_=frame('compact');assert compact['layout']['width']==320 and compact['layout']['playerModels'][0]['height']==108
auto,_=frame('auto');assert auto['guiSetting']==0
result=json.loads((out/'client-result.json').read_text());assert result['inventoryConserved']
for condition in ('bindingChecked','fullInventoryChecked','duplicateClickChecked','staleInventoryChecked'): assert result[condition],condition
report=dict(result='PASS',stableFigurePixels=changed,armorRegionPixels=regions,angles=sorted(angles),inventoryComponentsConserved=True,backgroundRetained=True)
(out/'character-verification.json').write_text(json.dumps(report,indent=2)+'\n');print('PASS character GPU',report)
