#!/usr/bin/env python3
"""Run the same client acceptance against an isolated Paper or Minestom host."""
import argparse, json, os, shutil, subprocess, time
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--platform', choices=['paper', 'minestom'], required=True)
    parser.add_argument('--scenario', default='platform')
    args = parser.parse_args()
    if not os.environ.get('JAVA_HOME'):
        raise RuntimeError('Set JAVA_HOME to a Java 25 JDK')
    port = 25684 if args.platform == 'paper' else 25686
    pack_port = port + 1
    server_dir = ROOT / ('run/server' if args.platform == 'paper' else 'run/minestom')
    reports = ROOT / ('build/reports/e2e/' + args.scenario)
    if args.scenario == 'platform': reports = reports / args.platform
    reports.mkdir(parents=True, exist_ok=True)
    (reports / 'run.json').write_text(json.dumps(dict(scenario=args.scenario, platform=args.platform,
        startedAt=time.time(), serverPort=port, clientPort=port, liveVideos=False, networkDelayMsPerDirection=0)))
    for file in ['client-result.json', 'failure.txt', 'map.png', 'video.png', 'portable.png', 'input-response.png']:
        (reports / file).unlink(missing_ok=True)
    game = ROOT / ('run/clients/' + args.scenario + '-' + args.platform)
    game.mkdir(parents=True, exist_ok=True)
    (game / 'options.txt').write_text('lang:en_us\nguiScale:2\nsoundCategory_master:0.0\nskipMultiplayerWarning:true\nonboardAccessibility:false\nstartedCleanly:true\nmaxFps:30\ntutorialStep:none\n')
    java = str(Path(os.environ['JAVA_HOME']) / 'bin/java')
    if args.platform == 'paper':
        config = server_dir / 'plugins/dui-demo/config.yml'
        config.parent.mkdir(parents=True, exist_ok=True)
        config.write_text(f'pack:\n  bind-address: 127.0.0.1\n  port: {pack_port}\n  public-url: http://127.0.0.1:{pack_port}/dui.zip\nvideos:\n  live: false\n')
        command = [java, '-Xms512M', '-Xmx2G', '-Djava.awt.headless=true', '-jar', 'paper.jar', '--nogui']
        ready = 'DUI_DEMO_READY'
    else:
        server_dir.mkdir(parents=True, exist_ok=True)
        libs = ROOT / 'minestom-server/build/install/minestom-server/lib'
        command = [java, '-Xmx1G', '-Djava.awt.headless=true',
            '-Ddui.demo.pack=' + str(ROOT / 'build/pack'),
            '-Ddui.demo.reports=' + str(server_dir / 'plugins/dui-demo/layouts'),
            '-Ddui.demo.port=' + str(port), '-Ddui.demo.pack-port=' + str(pack_port),
            '-cp', str(libs / '*'), 'gg.kembel.dui.demo.minestom.MinestomDemo']
        ready = 'DUI_MINESTOM_READY'
    server_log = reports / (args.platform + '-server.log')
    with server_log.open('w') as log:
        server = subprocess.Popen(command, cwd=server_dir, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
        try:
            (server_dir / 'plugins/dui-demo/layouts/PlatformTest.json').unlink(missing_ok=True)
            deadline = time.monotonic() + 180
            while time.monotonic() < deadline:
                if server.poll() is not None: raise RuntimeError('Server exited; see ' + str(server_log))
                text = server_log.read_text()
                if ready in text and (args.platform == 'minestom' or 'Done (' in text): break
                time.sleep(.5)
            else: raise RuntimeError('Server startup timed out; see ' + str(server_log))
            print('Host ready:', args.platform, port, flush=True)
            command = [str(ROOT / 'gradlew'), '-Dorg.gradle.java.home=' + os.environ['JAVA_HOME'],
                '-p', str(ROOT / 'e2e'), 'runTestClient', '-Pscenario=' + args.scenario,
                '-Pe2ePlatform=' + args.platform,
                '-Pe2eOutput=' + str(reports), '-Pe2eGameDirectory=' + str(game),
                '-Pe2ePort=' + str(port), '-Pe2eServerDirectory=' + str(server_dir), '--console=plain']
            with (reports / 'client.log').open('w') as client_log:
                subprocess.run(command, cwd=ROOT, stdout=client_log, stderr=subprocess.STDOUT, check=True, timeout=480)
            result = reports / 'client-result.json'
            if not result.exists() or not json.loads(result.read_text()).get('passed'):
                raise RuntimeError('Client acceptance failed; see ' + str(reports))
            print('PASS', args.platform, args.scenario, result.read_text(), flush=True)
            shutil.copyfile(result, reports / (args.platform + '-result.json'))
            if args.scenario == 'platform':
                subprocess.run(['python3', str(ROOT / 'scripts/verify_platform.py'), args.platform], check=True)
        finally:
            if server.poll() is None:
                server.stdin.write('stop\n'); server.stdin.flush()
                try: server.wait(timeout=40)
                except subprocess.TimeoutExpired: server.terminate(); server.wait(timeout=15)

if __name__ == '__main__': main()
