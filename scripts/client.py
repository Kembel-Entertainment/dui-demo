#!/usr/bin/env python3
"""Launch the verified, unmodified client, muted, only against the localhost demo."""
import argparse, hashlib, json, os, platform, shutil, struct, uuid
from pathlib import Path
from demo import ROOT, download, java
METADATA = json.loads((ROOT / 'scripts/minecraft.json').read_text())

def allowed(library, osname, arch):
    result = 'rules' not in library
    for rule in library.get('rules', []):
        target = rule.get('os', {})
        if target.get('name', osname) != osname:
            continue
        if 'arch' in target and target['arch'] not in (arch, 'aarch64' if arch == 'arm64' else arch):
            continue
        if rule.get('features'):
            continue
        result = rule['action'] == 'allow'
    return result

def artifact_file(artifact, library):
    target = ROOT / '.cache/vanilla/libraries' / artifact['path']
    if not target.exists():
        group, name, version, *_ = library['name'].split(':')
        candidates = [Path.home() / 'Library/Application Support/PrismLauncher/libraries' / artifact['path'], Path.home() / '.local/share/PrismLauncher/libraries' / artifact['path']]
        candidates += list((Path.home() / '.gradle/caches/modules-2/files-2.1' / group / name / version).glob('**/' + Path(artifact['path']).name))
        for p in candidates:
            if p.is_file() and hashlib.sha1(p.read_bytes()).hexdigest() == artifact['sha1']:
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(p, target)
                break
    return download(artifact['url'], target, 'sha1', artifact['sha1'])

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--username', default='DuiDemo')
    parser.add_argument('--prepare-only', action='store_true')
    parser.add_argument('--minecraft-jar', type=Path)
    args = parser.parse_args()
    system = platform.system()
    osname = {'Darwin': 'osx', 'Linux': 'linux', 'Windows': 'windows'}[system]
    arch = platform.machine().lower()
    arm = arch in ('arm64', 'aarch64')
    libraries = []
    for library in METADATA['libraries']:
        if not allowed(library, osname, arch):
            continue
        name = library['name']
        if 'natives-macos' in name and name.endswith('arm64') != arm:
            continue
        if 'natives-linux' in name and ('arm64' in name) != arm:
            continue
        if 'natives-windows' in name and ('arm64' in name) != arm:
            continue
        artifact = library.get('downloads', {}).get('artifact')
        if artifact:
            libraries.append(str(artifact_file(artifact, library)))
    client = METADATA['downloads']['client']
    jar = args.minecraft_jar or download(client['url'], ROOT / '.cache/minecraft-26.2-client.jar', 'sha1', client['sha1'])
    if hashlib.sha1(jar.read_bytes()).hexdigest() != client['sha1']:
        raise RuntimeError('Wrong vanilla client JAR')
    libraries.append(str(jar.resolve()))
    assets = ROOT / '.cache/vanilla/assets'
    index = METADATA['assetIndex']
    index_file = download(index['url'], assets / 'indexes' / (index['id'] + '.json'), 'sha1', index['sha1'])
    for obj in json.loads(index_file.read_text())['objects'].values():
        digest = obj['hash']
        path = assets / 'objects' / digest[:2] / digest
        candidates = [Path.home() / 'Library/Application Support/PrismLauncher/assets/objects' / digest[:2] / digest, Path.home() / '.local/share/PrismLauncher/assets/objects' / digest[:2] / digest]
        if not path.exists():
            for candidate in candidates:
                if candidate.is_file() and hashlib.sha1(candidate.read_bytes()).hexdigest() == digest:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(candidate, path)
                    break
        download('https://resources.download.minecraft.net/' + digest[:2] + '/' + digest, path, 'sha1', digest)
    if args.prepare_only:
        print('Verified vanilla client inputs are ready; no game was started.')
        return
    game = ROOT / 'run/vanilla-client'
    game.mkdir(parents=True, exist_ok=True)
    options = dict(lang='en_us', guiScale='2', skipMultiplayerWarning='true', onboardAccessibility='false', startedCleanly='true', maxFps='30', tutorialStep='none')
    if (game / 'options.txt').exists():
        for line in (game / 'options.txt').read_text().splitlines():
            key, sep, value = line.partition(':')
            if sep:
                options[key] = value
    options['soundCategory_master'] = '0.0'
    (game / 'options.txt').write_text(''.join((key + ':' + value + '\n' for key, value in options.items())))

    def string(value):
        raw = value.encode()
        return struct.pack('>H', len(raw)) + raw

    def tag(name, value):
        return b'\x08' + string(name) + string(value)
    server = tag('name', 'dui-demo / Vanilla') + tag('ip', '127.0.0.1:25584') + b'\x01' + string('acceptTextures') + b'\x01\x00'
    (game / 'servers.dat').write_bytes(b'\n\x00\x00\t' + string('servers') + b'\n' + struct.pack('>i', 1) + server + b'\x00')
    offline_id = uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:' + args.username).encode()).digest(), version=3)
    command = [java()] + (['-XstartOnFirstThread'] if system == 'Darwin' else []) + ['-Xms512M', '-Xmx2G', '--enable-native-access=ALL-UNNAMED', '--sun-misc-unsafe-memory-access=allow', '-Dorg.lwjgl.system.SharedLibraryExtractPath=' + str(game / 'natives/lwjgl'), '-Dio.netty.native.workdir=' + str(game / 'natives/netty'), '-cp', os.pathsep.join(libraries), METADATA['mainClass'], '--username', args.username, '--uuid', offline_id.hex, '--accessToken', '0', '--version', '26.2', '--versionType', 'release', '--gameDir', str(game), '--assetsDir', str(assets), '--assetIndex', index['id'], '--width', '1280', '--height', '900', '--quickPlayMultiplayer', '127.0.0.1:25584']
    (game / 'launch-proof.json').write_text(json.dumps(dict(clientSha1=client['sha1'], mods=[], muted=True, username=args.username, server='127.0.0.1:25584'), indent=2))
    os.chdir(game)
    os.execv(java(), command)
if __name__ == '__main__':
    main()
