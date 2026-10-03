#!/usr/bin/env python3
"""Headless existing-core/IPC/save test. Uses the built plugin, never builds a native emulator."""
import argparse,hashlib,io,json,os,pathlib,platform,struct,subprocess,tempfile,time,zipfile,zlib
root=pathlib.Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--plugin-jar',type=pathlib.Path)
args=parser.parse_args()
jar=args.plugin_jar.resolve() if args.plugin_jar else max((root/'build/libs').glob('dui-demo-*.jar'),key=lambda p:p.stat().st_mtime)
java=str(pathlib.Path(os.environ.get('JAVA_HOME',''))/'bin/java') if os.environ.get('JAVA_HOME') else 'java'
key='macos-arm64' if platform.system()=='Darwin' and platform.machine()=='arm64' else 'linux-x64'

def send(process,kind,data=b''):
    process.stdin.write(struct.pack('>IB',len(data),kind)+data+struct.pack('>I',zlib.crc32(bytes([kind])+data)))
    process.stdin.flush()

def receive(process):
    header=process.stdout.read(5)
    if len(header)!=5:raise RuntimeError(process.stderr.read().decode())
    size,kind=struct.unpack('>IB',header)
    if size>2*1024*1024:raise RuntimeError('Bad IPC size')
    data=process.stdout.read(size)
    assert struct.unpack('>I',process.stdout.read(4))[0]==zlib.crc32(bytes([kind])+data)
    if kind==4:raise RuntimeError(data)
    return kind,data

with tempfile.TemporaryDirectory(prefix='gba-smoke-') as tmp:
    directory=pathlib.Path(tmp)
    with zipfile.ZipFile(jar) as plugin:
        manifest=json.loads(plugin.read('gba/native/manifest.json'))
        artifact=manifest['artifacts'][key]
        archive=plugin.read('gba/native/'+key+'.zip')
        assert hashlib.sha256(archive).hexdigest()==artifact['sha256']
        with zipfile.ZipFile(io.BytesIO(archive)) as bundle:
            binary=directory/artifact['binary'];binary.write_bytes(bundle.read(artifact['binary']))
        rom=directory/'controls.gba';rom.write_bytes(plugin.read('gba/color-controls.gba'))
    def launch(saves):
        return subprocess.Popen([java,'--enable-native-access=ALL-UNNAMED','-cp',str(jar),
            'gg.kembel.dui.demo.gba.GbaWorker',str(binary),str(rom),str(saves)],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    saves=directory/'player-one'
    process=launch(saves);began=time.monotonic();frames=0;colors=set()
    try:
        while frames<180:
            kind,data=receive(process)
            if kind==2:
                frames+=1
                assert struct.unpack_from('>HH',data,8)==(240,160)
                if frames>30:colors.add(struct.unpack_from('>I',data,12)[0])
                if frames==90:send(process,10,struct.pack('>I',1<<8)) # held A
        assert len(colors)>120,(len(colors),'Static output')
        send(process,14)
        process.communicate(timeout=4)
        assert process.returncode==0
        assert (saves/'resume.state').stat().st_size>1000
        assert not (directory/'player-two').exists()
        checkpoint=(saves/'resume.state').read_bytes()
        resumed=launch(saves)
        try:
            for _ in range(120):
                kind,data=receive(resumed)
                if kind==2 and struct.unpack_from('>Q',data)[0]>=60:break
            send(resumed,14);resumed.communicate(timeout=4)
            assert resumed.returncode==0
            assert (saves/'resume.state.bak').read_bytes()==checkpoint
        finally:
            if resumed.poll() is None:resumed.kill();resumed.communicate()
        print(json.dumps(dict(passed=True,frames=frames,elapsed=round(time.monotonic()-began,2),colors=len(colors),checkpointResume=True,platform=key)))
    finally:
        if process.poll() is None:process.kill();process.communicate()
