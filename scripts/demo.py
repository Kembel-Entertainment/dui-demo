#!/usr/bin/env python3
"""Explicit local Paper runner and independent, muted Minecraft integration tests."""
import argparse, hashlib, json, os, shutil, socket, subprocess, time, uuid
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
SERVER = ROOT / 'run/server'
PLUGIN = SERVER / 'plugins/dui-demo'
REPORT = ROOT / 'build/reports/e2e'
SERVER_PORT = int(os.environ.get('DUI_DEMO_PORT', '25584'))
PACK_PORT = int(os.environ.get('DUI_PACK_PORT', '25585'))
if not (1024 <= SERVER_PORT <= 65535 and 1024 <= PACK_PORT <= 65535) or SERVER_PORT == PACK_PORT:
    raise ValueError('Demo and pack ports must be distinct ports in 1024..65535')
PAPER_SHA256 = 'b1d8f6bfa1b6101fa8e947b53041cb3bdf5540e7b83b6547ca19ba7edefeb083'
PAPER_URL = f'https://fill-data.papermc.io/v1/objects/{PAPER_SHA256}/paper-26.2-129.jar'

def java():
    return str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else shutil.which('java')

def download(url, path, algorithm, digest):
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        temporary = path.with_suffix(path.suffix + '.pending')
        subprocess.run(['curl', '-fsSL', '--retry', '2', '--max-time', '180', url, '-o', str(temporary)], check=True)
        if hashlib.new(algorithm, temporary.read_bytes()).hexdigest() != digest:
            raise RuntimeError('Download checksum mismatch: ' + str(path))
        temporary.replace(path)
    if hashlib.new(algorithm, path.read_bytes()).hexdigest() != digest:
        raise RuntimeError('Cache checksum mismatch: ' + str(path))
    return path

def check_ports():
    for port in (SERVER_PORT, PACK_PORT):
        with socket.socket() as s:
            try:
                s.bind(('127.0.0.1', port))
            except OSError:
                raise RuntimeError(f'Local port {port} is occupied; stop that demo instance first.')

def prepare(accept):
    check_ports()
    SERVER.mkdir(parents=True, exist_ok=True)
    paper = download(PAPER_URL, ROOT / '.cache/paper-26.2-129.jar', 'sha256', PAPER_SHA256)
    shutil.copyfile(paper, SERVER / 'paper.jar')
    eula = SERVER / 'eula.txt'
    if accept:
        eula.write_text('eula=true\n')
    elif not eula.exists():
        eula.write_text('eula=false\n')
    (SERVER / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=' + str(SERVER_PORT) + '\nonline-mode=false\nwhite-list=false\nenforce-secure-profile=false\ndifficulty=peaceful\ngamemode=creative\nforce-gamemode=true\nlevel-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\nmotd=dui-demo / Paper 26.2\n')

def install():
    check_ports()
    artifacts = list((ROOT / 'build/libs').glob('dui-demo-*.jar'))
    if len(artifacts) != 1:
        raise RuntimeError('Expected one built demo JAR')
    pack = ROOT / 'build/pack'
    metadata = json.loads((pack / 'dui.json').read_text())
    if hashlib.sha1((pack / 'dui.zip').read_bytes()).hexdigest() != metadata['sha1']:
        raise RuntimeError('Pack metadata does not match the built ZIP')
    targets = [(artifacts[0], SERVER / 'plugins' / artifacts[0].name), (pack / 'dui.zip', PLUGIN / 'pack/dui.zip'), (pack / 'dui.json', PLUGIN / 'pack/dui.json')]
    for source, target in targets:
        target.parent.mkdir(parents=True, exist_ok=True)
        temporary = target.with_suffix(target.suffix + '.pending')
        shutil.copyfile(source, temporary)
        temporary.replace(target)

def server_command():
    if 'eula=true' not in (SERVER / 'eula.txt').read_text():
        raise RuntimeError('Accept the Minecraft EULA in run/server/eula.txt before starting, or use -PacceptEula=true.')
    return [java(), '-Xms512M', '-Xmx2G', '-Djava.awt.headless=true', '-jar', 'paper.jar', '--nogui']

def run_e2e(scenario, live):
    check_ports()
    REPORT.mkdir(parents=True, exist_ok=True)
    PLUGIN.mkdir(parents=True, exist_ok=True)
    config = PLUGIN / 'config.yml'
    previous = config.read_bytes() if config.exists() else None
    config.write_text(f'pack:\n  bind-address: 127.0.0.1\n  port: {PACK_PORT}\n  public-url: http://127.0.0.1:{PACK_PORT}/dui.zip\nvideos:\n  live: ' + str(live).lower() + '\n')
    op = uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:SlotTest').digest(), version=3)
    ops = SERVER / 'ops.json'
    old_ops = ops.read_bytes() if ops.exists() else b'[]'
    ops.write_text(json.dumps([dict(uuid=str(op), name='SlotTest', level=4, bypassesPlayerLimit=False)]))
    log_path = REPORT / 'server.log'
    server = None
    try:
        with log_path.open('w') as log:
            server = subprocess.Popen(server_command(), cwd=SERVER, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
            deadline = time.monotonic() + 120
            while time.monotonic() < deadline:
                text = log_path.read_text()
                if server.poll() is not None:
                    raise RuntimeError('Demo server exited; see ' + str(log_path))
                if 'DUI_DEMO_READY' in text and 'Done (' in text:
                    break
                time.sleep(0.5)
            else:
                raise RuntimeError('Demo server startup timed out')
            scenarios = ['showcase', 'shop', 'rewards', 'advent', 'warps', 'roulette', 'blackjack', 'poker', 'slots', 'confetti', 'videos', 'protocol'] if scenario == 'all' else [scenario]
            for name in scenarios:
                output = REPORT / name
                if output.exists():
                    shutil.rmtree(output)
                output.mkdir(parents=True)
                (output / 'run.json').write_text(json.dumps(dict(scenario=name, startedAt=time.time(), liveVideos=live)))
                game = ROOT / 'run/clients' / name
                game.mkdir(parents=True, exist_ok=True)
                (game / 'options.txt').write_text('lang:en_us\nguiScale:2\nsoundCategory_master:0.0\nskipMultiplayerWarning:true\nonboardAccessibility:false\nstartedCleanly:true\nmaxFps:30\ntutorialStep:none\n')
                print('Running muted client:', name, flush=True)
                command = [str(ROOT / 'gradlew'), '-p', str(ROOT / 'e2e'), 'runTestClient', '-Pscenario=' + name, '-Pe2ePort=' + str(SERVER_PORT), '--console=plain']
                if os.environ.get('JAVA_HOME'):
                    command.insert(1, '-Dorg.gradle.java.home=' + os.environ['JAVA_HOME'])
                with (output / 'client.log').open('w') as client_log:
                    subprocess.run(command, cwd=ROOT, stdout=client_log, stderr=subprocess.STDOUT, check=True, timeout=480)
                subprocess.run(['python3', str(ROOT / 'scripts/verify_e2e.py'), name], cwd=ROOT, check=True)
            print('All requested dui demo E2E scenarios passed.', flush=True)
    finally:
        if server is not None and server.poll() is None:
            server.stdin.write('stop\n')
            server.stdin.flush()
            try:
                server.wait(timeout=30)
            except subprocess.TimeoutExpired:
                server.terminate()
                server.wait(timeout=15)
        ops.write_bytes(old_ops)
        if previous is None:
            config.unlink(missing_ok=True)
        else:
            config.write_bytes(previous)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['prepare', 'install', 'server', 'e2e', 'client-jar'])
    parser.add_argument('--accept-eula', action='store_true')
    parser.add_argument('--scenario', default='all', choices=['all', 'showcase', 'shop', 'rewards', 'advent', 'warps', 'roulette', 'blackjack', 'poker', 'slots', 'confetti', 'videos', 'protocol'])
    parser.add_argument('--live-videos', action='store_true')
    args = parser.parse_args()
    if args.command == 'prepare':
        prepare(args.accept_eula)
    elif args.command == 'install':
        install()
    elif args.command == 'server':
        check_ports()
        os.chdir(SERVER)
        os.execv(java(), server_command())
    elif args.command == 'e2e':
        run_e2e(args.scenario, args.live_videos)
    else:
        metadata = json.loads((ROOT / 'scripts/minecraft.json').read_text())
        client = metadata['downloads']['client']
        print(download(client['url'], ROOT / '.cache/minecraft-26.2-client.jar', 'sha1', client['sha1']))
if __name__ == '__main__':
    main()
