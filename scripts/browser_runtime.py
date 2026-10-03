#!/usr/bin/env python3
"""Explicit server-local Node/Playwright/Chromium provisioning, never part of a build."""
import argparse, json, shutil, subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser()
parser.add_argument('--directory', type=Path, default=ROOT / 'run/server/plugins/dui-demo/browser/runtime')
args = parser.parse_args()
runtime = args.directory.resolve()
manifest = ROOT / 'src/main/resources/browser/package.json'
runtime.mkdir(parents=True, exist_ok=True)
existing = runtime / 'package.json'
if existing.exists() and json.loads(existing.read_text()) != json.loads(manifest.read_text()):
    raise SystemExit('Runtime package.json differs; choose a fresh directory rather than overwriting it.')
shutil.copy2(manifest, existing)
lock = manifest.with_name('package-lock.json')
if lock.exists(): shutil.copy2(lock, runtime / 'package-lock.json')
subprocess.run(['npm', 'ci' if lock.exists() else 'install', '--no-audit', '--no-fund'], cwd=runtime, check=True)
subprocess.run(['node', str(runtime / 'node_modules/playwright/cli.js'), 'install', 'chromium'], cwd=runtime, check=True)
print(json.dumps({'runtime': str(runtime), 'node': shutil.which('node'), 'installed': True}))
