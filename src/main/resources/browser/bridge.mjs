import { createRequire } from 'node:module';
import { resolve } from 'node:path';
import { createInterface } from 'node:readline';
import { once } from 'node:events';

// stdout is a bounded binary stream: type:u8, length:u32be, then PNG or JSON.
// No screenshots, cookies or page contents are written to disk.
const args = Object.fromEntries(process.argv.slice(2).reduce((all, value, i, list) => {
  if (i % 2 === 0) all.push([value.replace(/^--/, ''), list[i + 1]]);
  return all;
}, []));
const { chromium } = createRequire(resolve(args.runtime, 'package.json'))('playwright');
const width = Number(args.width), height = Number(args.height), fps = Number(args.fps);
let zoomPercent = Number(args.zoom ?? 100);
if (!Number.isInteger(width) || !Number.isInteger(height) || width < 128 || width > 2048
    || height < 64 || height > 1152 || !Number.isFinite(fps) || fps < 1 || fps > 30)
  throw new Error('Invalid browser dimensions/FPS');
if (!Number.isInteger(zoomPercent) || zoomPercent < 50 || zoomPercent > 250) throw new Error('Zoom 50..250 percent');
function url(value) {
  const parsed = new URL(value);
  if (!['http:', 'https:'].includes(parsed.protocol) || parsed.username || parsed.password)
    throw new Error('Use an HTTP(S) URL without credentials');
  return parsed.href;
}
let browser, page, cdp, closing = false, paused = false, loading = false;
let error = '', lastAction = '', actionId = 0, queue = [], pendingState = null;
let cursor = { x: width / 2, y: height / 2 }, nextCapture = 0, nextState = 0;
function state() {
  pendingState = { url: page?.url() ?? '', title: '', loading, error, lastAction, actionId, cursor, width, height, zoomPercent };
}
async function message(type, value) {
  const payload = Buffer.isBuffer(value) ? value : Buffer.from(JSON.stringify(value));
  const header = Buffer.alloc(5); header.writeUInt8(type, 0); header.writeUInt32BE(payload.length, 1);
  // One writer only. Waiting for drain prevents a slow consumer from growing memory.
  const writtenHeader = process.stdout.write(header);
  const writtenPayload = process.stdout.write(payload);
  if (!writtenHeader || !writtenPayload) await once(process.stdout, 'drain');
}
async function close() {
  if (closing) return;
  closing = true;
  await browser?.close().catch(() => {});
  process.exit(0);
}
for (const signal of ['SIGTERM', 'SIGINT']) process.on(signal, close);
process.stdin.on('end', close);
process.stdout.on('error', close);
// Metrics and screenshot clips must change atomically: never emit a half-resized frame.
let surfaceWork = Promise.resolve();
function onSurface(work) {
  const next = surfaceWork.then(work, work);
  surfaceWork = next.catch(() => {});
  return next;
}
async function zoom(percent) {
  if (!Number.isInteger(percent) || percent < 50 || percent > 250) throw new Error('Zoom 50..250 percent');
  const scale = percent / 100;
  await cdp.send('Emulation.setDeviceMetricsOverride', {
    width: Math.ceil(width / scale), height: Math.ceil(height / scale),
    deviceScaleFactor: scale, mobile: false
  });
  zoomPercent = percent;
  await page.mouse.move(cursor.x / scale, cursor.y / scale);
}
async function picture() {
  return onSurface(async () => {
    const scale = zoomPercent / 100;
    const metrics = await cdp.send('Page.getLayoutMetrics');
    const viewport = metrics.cssVisualViewport;
    const result = await cdp.send('Page.captureScreenshot', {
      format: 'png', fromSurface: true, captureBeyondViewport: false,
      clip: { x: viewport.pageX, y: viewport.pageY, width: Math.ceil(width / scale), height: Math.ceil(height / scale), scale: 1 }
    });
    return Buffer.from(result.data, 'base64');
  });
}
async function action(command) {
  switch (command.action) {
    case 'navigate': loading = true; state(); await page.goto(url(command.url), { waitUntil: 'domcontentloaded', timeout: 20000 }); break;
    case 'back': await page.goBack({ waitUntil: 'domcontentloaded', timeout: 20000 }); break;
    case 'forward': await page.goForward({ waitUntil: 'domcontentloaded', timeout: 20000 }); break;
    case 'reload': await page.reload({ waitUntil: 'domcontentloaded', timeout: 20000 }); break;
    case 'pointer': cursor = { x: Math.max(0, Math.min(width - 1, command.x)), y: Math.max(0, Math.min(height - 1, command.y)) };
      await page.mouse.move(cursor.x * 100 / zoomPercent, cursor.y * 100 / zoomPercent); break;
    case 'click': await page.mouse.click(command.x * 100 / zoomPercent, command.y * 100 / zoomPercent); break;
    case 'zoom': await zoom(command.percent); break;
    case 'scroll': await page.mouse.wheel(0, Math.max(-2000, Math.min(2000, command.dy)) * 100 / zoomPercent); break;
    case 'type': await page.keyboard.insertText(String(command.text).slice(0, 512));
      if (command.enter) await page.keyboard.press('Enter'); break;
    case 'key': if (!['Enter', 'Escape', 'Backspace', 'Space', 'Tab'].includes(command.key)) throw new Error('Unsupported key');
      await page.keyboard.press(command.key); break;
    case 'pause': paused = !!command.paused; break;
    case 'close': await close(); return;
    default: throw new Error('Unknown browser action');
  }
  loading = false; error = ''; lastAction = command.action; actionId = command.id; state();
}
async function runActions() {
  while (!closing) {
    const command = queue.shift();
    if (!command) { await new Promise(r => setTimeout(r, 5)); continue; }
    try { await onSurface(() => action(command)); }
    catch (failure) { loading = false; error = String(failure.message).slice(0, 180); lastAction = command.action; actionId = command.id; state(); }
  }
}
try {
  browser = await chromium.launch({ headless: true, args: ['--mute-audio', '--autoplay-policy=no-user-gesture-required'] });
  // A fresh in-memory context for every viewer; no personal browser profile is used.
  const context = await browser.newContext({ viewport: { width, height }, deviceScaleFactor: 1,
    acceptDownloads: false, colorScheme: 'dark', locale: 'en-US' });
  page = await context.newPage();
  cdp = await context.newCDPSession(page);
  await zoom(zoomPercent);
  page.on('dialog', dialog => dialog.dismiss());
  page.on('download', download => download.cancel());
  page.on('popup', async popup => {
    const target = popup.url(); await popup.close();
    if (target.startsWith('http')) queue.push({ action: 'navigate', url: target, id: ++actionId });
  });
  page.on('framenavigated', frame => { if (frame === page.mainFrame()) state(); });
  page.on('domcontentloaded', () => { loading = false; state(); });
  const lines = createInterface({ input: process.stdin, crlfDelay: Infinity });
  lines.on('line', line => {
    try {
      if (line.length > 8192) throw new Error('Browser command too large');
      const command = JSON.parse(line);
      // Cursor updates can replace other pending cursor updates, never discrete actions.
      if (command.action === 'pointer') queue = queue.filter(value => value.action !== 'pointer');
      if (queue.length >= 64) throw new Error('Browser is busy; try again shortly');
      queue.push(command);
    } catch (failure) { error = failure.message; state(); }
  });
  void runActions();
  queue.push({ action: 'navigate', url: url(args.url), id: 0 }); state();
  while (!closing) {
    if (performance.now() >= nextState) { state(); nextState = performance.now() + 500; }
    if (pendingState) {
      const update = pendingState; pendingState = null;
      update.title = await page.title().catch(() => '');
      await message(2, update);
    }
    const now = performance.now();
    if (paused || now < nextCapture) { await new Promise(r => setTimeout(r, 5)); continue; }
    nextCapture = now + 1000 / fps;
    try {
      const frame = await picture();
      await message(1, frame);
    } catch (failure) {
      // Transient navigation races do not terminate an otherwise healthy viewer.
      if (page.isClosed()) throw failure;
      await new Promise(r => setTimeout(r, 50));
    }
  }
} catch (failure) {
  closing = true;
  process.stderr.write(String(failure.stack ?? failure) + '\n');
  await browser?.close().catch(() => {});
  process.exit(1);
}
