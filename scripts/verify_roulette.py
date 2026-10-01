#!/usr/bin/env python3
"""Verify actual GPU wheel/ball/chip motion, an exact live payout and a stable still frame."""
from pathlib import Path
import json, math
from png_pixels import png
OUT=Path(__file__).resolve().parent.parent/'build/reports/e2e/roulette'
def frame(name):
    meta=json.loads((OUT/'screenshots'/f'{name}.json').read_text())
    _,_,pixel=png(OUT/'screenshots'/f'{name}.png')
    def sample(x,y):
        return pixel(int((meta['canvasX']+x)*meta['scale']),int((meta['canvasY']+y)*meta['scale']))
    return meta,sample
def changes(a,b,e):
    return sum(max(abs(c-d) for c,d in zip(a(x,y),b(x,y)))>15 for y in range(e['y'],e['y']+e['height']) for x in range(e['x'],e['x']+e['width']))

# Inspect rendered glyphs in every pocket, including two-digit widths and the spacing
# next to dividers. This catches malformed/clipped labels that motion probes cannot.
NUMBERS=(0,32,15,19,4,21,2,25,17,34,6,27,13,36,11,30,8,23,10,5,24,16,33,1,20,14,31,9,22,18,29,7,28,12,35,3,26)
WIDE_DIGITS=(
    ('.###.','#...#','#...#','#...#','#...#','#...#','.###.'),
    ('..#..','.##..','..#..','..#..','..#..','..#..','.###.'),
    ('.###.','#...#','....#','...#.','..#..','.#...','#####'),
    ('####.','....#','....#','.###.','....#','....#','####.'),
    ('...#.','..##.','.#.#.','#..#.','#####','...#.','...#.'),
    ('#####','#....','#....','####.','....#','....#','####.'),
    ('.###.','#....','#....','####.','#...#','#...#','.###.'),
    ('#####','....#','...#.','..#..','.#...','.#...','.#...'),
    ('.###.','#...#','#...#','.###.','#...#','#...#','.###.'),
    ('.###.','#...#','#...#','.####','....#','....#','.###.'))
SMALL_DIGITS=(
    ('###','#.#','#.#','#.#','###'),
    ('.#.','.#.','.#.','.#.','###'),
    ('###','..#','###','#..','###'),
    ('###','..#','###','..#','###'),
    ('#.#','#.#','###','..#','..#'),
    ('###','#..','###','..#','###'),
    ('###','#..','###','#.#','###'),
    ('###','..#','..#','..#','..#'),
    ('###','#.#','###','#.#','###'),
    ('###','#.#','###','..#','###'))

def labels(name,detailed):
    meta,sample=frame(name)
    effect=next(e for e in meta['layout']['effects'] if e['kind']=='WHEEL')
    radius=effect['width']*.47
    cx=effect['x']+effect['width']/2;cy=effect['y']+effect['height']/2
    step=math.tau/37
    target=effect['parameter0']&63
    rotation=-NUMBERS.index(target)*step
    digits=WIDE_DIGITS if detailed else SMALL_DIGITS
    columns=len(digits[0][0]);rows=len(digits[0])
    unit=min(2*.735*math.tan(step*.36)/(columns*2+1+rows*math.tan(step*.36)),.105/rows)*radius
    ux=uy=unit
    matched=probes=gap_ink=gap_probes=0
    pockets=[]
    ink_edges=[]
    pitch=1/meta['scale']
    for i,number in enumerate(NUMBERS):
        angle=rotation+i*step
        sin=math.sin(angle);cos=math.cos(angle)
        def at(x,radial):
            return sample(cx+cos*x+sin*radial,cy+sin*x-cos*radial)
        text=[digits[int(d)] for d in str(number)]
        width=len(text)*(columns+1)-1
        pocket_matches=pocket_probes=0
        for row in range(rows):
            expected='.'.join(glyph[row] for glyph in text)
            for col,ch in enumerate(expected):
                rgb=at((col+.5-width/2)*ux,.735*radius-(row+.5-rows/2)*uy)
                ink=rgb[1]>118 and rgb[2]>102
                pocket_matches+=ink==(ch=='#')
                pocket_probes+=1
        ratio=pocket_matches/pocket_probes
        assert ratio>=(.80 if detailed else .70),(name,number,ratio,'Malformed pocket label')
        pockets.append(ratio)
        matched+=pocket_matches;probes+=pocket_probes
        # Measure the actual rendered ink, including rotated/antialiased edges, rather
        # than just checking divider pixels. Adjacent two-digit labels need visible air.
        extent_x=.735*radius*math.tan(step*.445)
        extent_y=rows*unit/2+pitch
        angles=[]
        for iy in range(int(extent_y*2/pitch)+1):
            radial=.735*radius-extent_y+iy*pitch
            for ix in range(int(extent_x*2/pitch)+1):
                x=-extent_x+ix*pitch
                rgb=at(x,radial)
                if rgb[1]>190 and rgb[2]>170:
                    angles.append(math.atan2(x,radial))
        assert angles,(name,number,'Missing rendered pocket numeral')
        ink_edges.append((min(angles),max(angles)))
        for side in (-1,1):
            edge=angle+side*step*.455
            for r in (.690,.710,.735,.760,.780):
                rgb=sample(cx+math.sin(edge)*r*radius,cy-math.cos(edge)*r*radius)
                gap_ink+=rgb[1]>190 and rgb[2]>170
                gap_probes+=1
    assert matched/probes>=(.92 if detailed else .86),(name,matched,probes)
    assert gap_ink/gap_probes<.02,(name,gap_ink,gap_probes,'Labels touch pocket dividers')
    pair_gaps=[]
    for i,number in enumerate(NUMBERS):
        j=(i+1)%len(NUMBERS)
        if number>=10 and NUMBERS[j]>=10:
            gap=(step+ink_edges[j][0]-ink_edges[i][1])/step
            assert gap>=.22,(name,number,NUMBERS[j],gap,'Adjacent two-digit labels are crowded')
            pair_gaps.append(gap)
    # Measure the actual zero's bounding box independently of the glyph-cell probes.
    # Its 5x7 / 3x5 shape must not be squeezed into a tall narrow label again.
    angle=rotation
    points=[]
    extent_x=unit*(columns*.5+1)
    extent_y=unit*(rows*.5+1)
    for iy in range(int(extent_y*2/pitch)+1):
        yy=-extent_y+iy*pitch
        for ix in range(int(extent_x*2/pitch)+1):
            xx=-extent_x+ix*pitch
            radial=.735*radius-yy
            rgb=sample(cx+math.cos(angle)*xx+math.sin(angle)*radial,cy+math.sin(angle)*xx-math.cos(angle)*radial)
            if rgb[1]>190 and rgb[2]>170:
                points.append((xx,yy))
    assert points,(name,'Missing zero glyph')
    aspect=(max(x for x,y in points)-min(x for x,y in points)+pitch)/(max(y for x,y in points)-min(y for x,y in points)+pitch)
    expected=columns/rows
    assert abs(aspect-expected)<.12,(name,aspect,expected,'Stretched wheel numerals')
    return dict(pockets=len(pockets),glyphProbes=probes,matched=matched,matchRatio=matched/probes,dividerInk=gap_ink,zeroAspect=aspect,
        adjacentTwoDigitPairs=len(pair_gaps),minimumPairGapFraction=min(pair_gaps))

def placed_chips(name):
    meta,sample=frame(name)
    layout=meta['layout']
    hits={hit['id']:hit for hit in layout['hits']}
    count=0
    for image in layout['images']:
        if not image['id'].startswith('chip_bet_'):
            continue
        hit=hits[image['id'].removeprefix('chip_')]
        assert hit['x']<image['x'] and image['x']+image['width']<hit['x']+hit['width'],(name,image['id'],'Horizontal chip padding')
        assert hit['y']<image['y'] and image['y']+image['height']<hit['y']+hit['height'],(name,image['id'],'Vertical chip padding')
        count+=1
    assert count==5,(name,count,'Missing number/zero/dozen/outside/column markers')
    return count

motion={}
for early,late,name,kind in [('spin-early','spin-middle','wheel','WHEEL'),('spin-middle','spin-late','ball-drop','WHEEL'),('payout-early','payout-late','payout','CHIP_STACK'),('compact-early','compact-late','compact','WHEEL')]:
    ma,a=frame(early);mb,b=frame(late)
    e=next(e for e in ma['layout']['effects'] if e['kind']==kind)
    motion[name]=changes(a,b,e);assert motion[name]>20,(name,motion[name])
ma,a=frame('motion-off');mb,b=frame('still-frame')
still=changes(a,b,dict(x=0,y=0,width=ma['layout']['width'],height=ma['layout']['height']))
assert still==0,still
meta,sample=frame('result');layout=meta['layout']
assert layout['balance']==4995 and layout['return']==180 and layout['rounds']==1 and layout['stake']==0
e=next(e for e in layout['effects'] if e['kind']=='WHEEL')
assert e['parameter0']&63==layout['result']
# The ivory ball must be visible in the winning pocket under the fixed twelve-o'clock marker.
cx=e['x']+e['width']/2;cy=e['y']+e['height']/2-e['width']*.47*.60
ball=sample(cx,cy)
assert min(ball)>170,('Ball missing at the settled target',ball)
readability=dict(wide=labels('labels-wide',True),compact=labels('labels-compact',False))
chips=dict(wide=placed_chips('chips-wide'),compact=placed_chips('chips-compact'))
report=dict(result='PASS',animationPixels=motion,motionOffPixels=still,livePayoutVerified=True,settledBallVerified=True,numberLabels=readability,placedChips=chips)
(OUT/'roulette-verification.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS roulette',report)
