#!/usr/bin/env python3
"""Compare actual vanilla-rendered map pixels to the common projection and HUD contract."""
from pathlib import Path
import functools, json, math, struct, zlib
from zipfile import ZipFile
ROOT=Path(__file__).resolve().parent.parent
@functools.lru_cache(maxsize=2)
def png(path):
    data=path if isinstance(path,bytes) else path.read_bytes(); assert data[:8]==b'\x89PNG\r\n\x1a\n'; offset=8; compressed=[]
    while offset<len(data):
        size=struct.unpack_from('>I',data,offset)[0];kind=data[offset+4:offset+8];content=data[offset+8:offset+8+size];offset+=size+12
        if kind==b'IHDR':width,height,depth,color,_,_,interlace=struct.unpack('>IIBBBBB',content)
        if kind==b'IDAT':compressed.append(content)
    assert depth==8 and color in (2,6) and interlace==0
    bpp=4 if color==6 else 3; stride=width*bpp; raw=zlib.decompress(b''.join(compressed));prev=bytearray(stride);rows=[]
    for y in range(height):
        start=y*(stride+1);f=raw[start];row=bytearray(raw[start+1:start+1+stride])
        for i in (range(stride) if f else ()):
            a=row[i-bpp] if i>=bpp else 0;b=prev[i];c=prev[i-bpp] if i>=bpp else 0
            if f==1: prediction=a
            elif f==2:prediction=b
            elif f==3:prediction=(a+b)//2
            elif f==4:
                p=a+b-c;pa=abs(p-a);pb=abs(p-b);pc=abs(p-c);prediction=a if pa<=pb and pa<=pc else b if pb<=pc else c
            elif f==0:prediction=0
            else:raise ValueError('Invalid PNG filter')
            row[i]=(row[i]+prediction)&255
        rows.append(row);prev=row
    return width,height,bpp,rows

def calibration_color(actual,expected):
    # World-space drawings receive Vanilla's multiplicative vignette before GUI
    # overlays. Night/dark environments change its strength, not the projection.
    # Fit only that brightness factor; reject changed hue with a tight residual.
    brightness=sum(a*b for a,b in zip(actual,expected))/sum(b*b for b in expected)
    return .5<=brightness<=1.04 and max(abs(a-b*brightness) for a,b in zip(actual,expected))<=4

def rectangle(path,yaw=0,pitch=0,zoom=12):
    width,height,bpp,rows=png(path);colors={(51,136,255),(240,109,192),(80,213,160),(255,202,85)}
    scale=height/(200+zoom*15);center=[width/2-yaw*8*scale,height/2-pitch*8*scale]
    expected=[center[0]-71*scale,center[1]-35*scale,center[0]+71*scale,center[1]+35*scale]
    points=[]
    for y in range(max(0,math.floor(expected[1]-8)),min(height,math.ceil(expected[3]+8))):
        for x in range(max(0,math.floor(expected[0]-8)),min(width,math.ceil(expected[2]+8))):
            pixel=rows[y][x*bpp:x*bpp+3]
            if any(calibration_color(pixel,color) for color in colors):points.append((x,y))
    assert points,'No calibration rectangle: '+str(path)
    bounds=[min(x for x,y in points),min(y for x,y in points),max(x for x,y in points),max(y for x,y in points)]
    error=max(abs(a-b) for a,b in zip(bounds,expected));assert error<4,(path,bounds,expected,error)
    # Orientation matters as well as dimensions: the protocol stamp must be stable across atlas allocations.
    probes=[]
    for dy in (-12,12):
        for dx in (-24,24):
            x=round(center[0]+dx*scale);y=round(center[1]+dy*scale);probes.append(tuple(rows[y][x*bpp:x*bpp+3]))
    assert all(calibration_color(actual,expected) for actual,expected in zip(probes,[(51,136,255),(240,109,192),(80,213,160),(255,202,85)])),(path,probes)
    return {'file':path.name,'size':[width,height],'bounds':bounds,'expected':expected,'maxErrorPixels':error,'cornersCorrect':True,'vanillaVignetteAccountedFor':True}

def hud_pixels(path):
    width,height,bpp,rows=png(path)
    # The footer must cover the actual hotbar, not just remove its item icons.
    probes=[(x,height-5) for x in (width//2-80,width//2,width//2+80)]
    assert all(tuple(rows[y][x*bpp:x*bpp+3])==(16,29,40) for x,y in probes),('Native hotbar / footer leak',path)
    title=sum(max(abs(a-b) for a,b in zip(row[x:x+3],(245,227,184)))<20 for row in rows[5:int(height*.083)] for x in range(0,len(row),bpp))
    assert title>50,('Live title missing / covered',path,title)
    # Native health/experience colors must be absent; the own controls are gold/grey.
    leaks=0;icon_pixels=0
    gui_scale=json.loads(path.with_suffix('.json').read_text())['scale']
    native_left=round(width/2-104*gui_scale);native_right=round(width/2+104*gui_scale)
    for y,row in enumerate(rows[int(height*.85):],start=int(height*.85)):
        for x in range(0,len(row),bpp):
            r,g,b=row[x:x+3]
            # Own turquoise icon accent after the shader's cream tint. This is
            # intentionally green, but is neither a native heart nor the XP bar.
            if (r,g,b)==(142,214,201):icon_pixels+=1;continue
            if y>=height-47*gui_scale and native_left<=x//bpp<native_right:leaks+=int(r>80 and r>g*1.5 and r>b*1.5 or g>100 and g>r*1.3 and g>b*1.3)
    assert leaks==0,('Native health / experience leak',path,leaks)
    assert icon_pixels>60,('Dedicated pixel icons missing / covered',path,icon_pixels)
    side=tuple(rows[height-5][20*bpp:20*bpp+3])
    assert side!=(16,29,40),('Footer is unexpectedly opaque outside the HUD cover',path)
    return {'file':path.name,'hotbarCovered':True,'liveTitlePixels':title,'dedicatedIconAccentPixels':icon_pixels,'nativeHealthExperiencePixels':leaks,'translucentFooterSidePixel':side}

def tile_edges(path):
    # This own illustration has a flat mountain band across x=672 at these y values.
    # Sample every physical pixel around the join, including the rasterization boundary.
    width,height,bpp,rows=png(path)
    with ZipFile(ROOT/'build/pack/dui.zip') as pack:
        _,_,source_bpp,source=png(pack.read('assets/demo/textures/world-map/elsewhere/images/tile_8.png'))
    scale=height/380
    checks=0
    for map_y in (290,300,310):
        expected=tuple(source[map_y-160][:3])
        x=round(width/2+(672-560)*scale)
        y=round(height/2+(map_y-320)*scale)
        for px in range(x-2,x+3):
            actual=tuple(rows[y][px*bpp:px*bpp+3])
            assert max(abs(a-b) for a,b in zip(actual,expected))<=3,('Visible tile seam',path,px,y,actual,expected)
            checks+=1
    return {'probes':checks,'matchesOwnSourceImage':True}

def custom_hud(path):
    width,height,bpp,rows=png(path)
    scale=max(1,min(width//480,height//360))
    def pixel(x,y): return tuple(rows[round(y)][round(x)*bpp:round(x)*bpp+3])
    probes=[((12+2)*scale,(12+2)*scale,(255,94,173)),
            (width/2-60*scale+2*scale,height/2-13*scale+2*scale,(63,180,255)),
            (width-192*scale+2*scale,height-138*scale+2*scale,(168,85,247))]
    for x,y,color in probes:
        actual=pixel(x,y)
        assert max(abs(a-b) for a,b in zip(actual,color))<4,('Template anchor/color failed',path,(x,y),actual,color)
    # Every one of seven rows must contain live white glyphs, including rows 5..7.
    counts=[]
    for row in range(7):
        start_x=round(width-186*scale);start_y=round(height-138*scale+row*18*scale)
        count=sum(max(abs(a-b) for a,b in zip(pixel(x,y),(255,255,255)))<4
            for y in range(start_y,start_y+18*scale) for x in range(start_x,start_x+120*scale))
        assert count>40,('Missing custom control row',path,row,count)
        counts.append(count)
    return {'file':path.name,'arbitraryAnchorsAndColors':True,'sevenControlRows':counts}

out=ROOT/'build/reports/e2e/map'
calibrations=[];hud_checks=[]
for path in sorted((out/'screenshots').glob('*.png')):
    metadata=json.loads(path.with_suffix('.json').read_text())
    if 'rectangle' in path.name:
        calibrations.append(rectangle(path,metadata['yaw'],metadata['pitch'],metadata['zoom']))
    if not any(value in path.name for value in ('restored','dialog','template-custom','template-invalid')):
        hud_checks.append(hud_pixels(path))
assert len(calibrations)==6, calibrations
assert len(hud_checks)>=10, hud_checks
custom_checks=[custom_hud(out/'screenshots'/name) for name in ('08-hud-template-custom.png','09-hud-template-invalid-retained.png')]
result={'customHudChecks':custom_checks,'result':'PASS','calibrations':calibrations,'hudChecks':hud_checks,
        'tileEdges':tile_edges(out/'screenshots/10-overview.png'),
        'maximumCoordinateError':max(v['maxErrorPixels'] for v in calibrations)}
(out/'map-verification.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS map projection/HUD',len(calibrations),'calibrations,',len(hud_checks),'HUD images')
