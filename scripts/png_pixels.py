"""Small PNG reader for Minecraft screenshot checks (RGB/RGBA, no dependencies)."""
from functools import cache
import struct
import zlib

@cache
def png(path):
    """Read the 8-bit RGB(A) screenshots without a third-party imaging dependency."""
    data = path.read_bytes(); assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos = 8; packed = bytearray()
    while pos < len(data):
        n = struct.unpack('>I', data[pos:pos+4])[0]; kind = data[pos+4:pos+8]; block = data[pos+8:pos+8+n]; pos += n+12
        if kind == b'IHDR':
            w, h, depth, color, compression, filtering, interlace = struct.unpack('>IIBBBBB', block)
            assert depth == 8 and color in (2, 6) and interlace == 0
        if kind == b'IDAT': packed.extend(block)
    bpp = 4 if color == 6 else 3; stride = w*bpp; raw = zlib.decompress(packed); rows = []; previous = bytearray(stride)
    for y in range(h):
        start = y*(stride+1); f = raw[start]; row = bytearray(raw[start+1:start+1+stride])
        for x in range(stride) if f else ():
            a = row[x-bpp] if x >= bpp else 0; b = previous[x]; c = previous[x-bpp] if x >= bpp else 0
            if f == 1: pred = a
            elif f == 2: pred = b
            elif f == 3: pred = (a+b)//2
            else:
                assert f == 4
                p = a+b-c; distances = (abs(p-a), abs(p-b), abs(p-c)); pred = (a,b,c)[distances.index(min(distances))]
            row[x] = (row[x]+pred)&255
        rows.append(row); previous = row
    return w, h, lambda x, y: tuple(rows[y][x*bpp:x*bpp+3])
