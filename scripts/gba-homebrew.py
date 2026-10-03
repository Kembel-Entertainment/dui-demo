#!/usr/bin/env python3
"""Build our tiny original test cartridge with installed clang, never builds mGBA."""
import pathlib, struct, subprocess, tempfile
root=pathlib.Path(__file__).resolve().parent.parent
source=root/'src/test/fixtures/gba/color-controls.s'
target=root/'src/main/resources/gba/color-controls.gba'
with tempfile.TemporaryDirectory() as directory:
    obj=pathlib.Path(directory)/'test.o'
    subprocess.run(['clang','-target','armv4t-none-eabi','-c',str(source),'-o',str(obj)],check=True)
    data=obj.read_bytes()
    offset=struct.unpack_from('<I',data,32)[0]
    size,count,names_index=struct.unpack_from('<HHH',data,46)
    headers=[struct.unpack_from('<10I',data,offset+i*size) for i in range(count)]
    names_header=headers[names_index]
    names=data[names_header[4]:names_header[4]+names_header[5]]
    section=next(h for h in headers if names[h[0]:].split(b'\0',1)[0]==b'.text')
    code=data[section[4]:section[4]+section[5]]
    rom=bytearray(192)
    struct.pack_into('<I',rom,0,0xea00002e) # branch to offset 0xc0
    rom[0xa0:0xac]=b'DUI CONTROLS'
    rom[0xac:0xb0]=b'DUIT'
    rom[0xb0:0xb2]=b'00'
    rom[0xb2]=0x96
    rom[0xbd]=(-sum(rom[0xa0:0xbd])-0x19)&255
    rom+=code
    rom+=bytes((-len(rom))%1024)
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(rom)
print(target)
