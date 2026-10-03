# Live headless browser

`/browser` and `/dui browser` open **https://www.youtube.com/** by default. Every
viewer gets a private, fresh headless Chromium context on the server. The unmodified
Minecraft 26.2 client receives live pixels through DUI's existing video surface.
Page images and videos are not baked into a resource pack or checked into Git.
There is no client mod, personal browser-profile access or persistent login feature.

## Explicit runtime setup

Install Node.js 20+ and run this on the server machine:

```sh
python3 scripts/browser_runtime.py
```

The installer uses the pinned Playwright package/lock, installs into
`run/server/plugins/dui-demo/browser/runtime/`, then installs Playwright's matching
Chromium. Runtime modules, browser binaries, downloads, reports and local videos are
not source assets. A normal Gradle build installs no browser and starts no process.

For another deployment directory use `--directory /path/to/plugins/dui-demo/browser/runtime`.
The Paper process must inherit a PATH containing Node, or set `browser.node` to its
absolute executable. `browser.runtime` can point to that installed runtime. On Linux,
install the platform libraries required by Chromium using your usual provisioning;
see the [official browser installation guide](https://playwright.dev/docs/browsers).

Access requires `dui-demo.browser` (OP by default). The demo permits two concurrent
browser sessions by default. Missing runtime dependencies produce a setup message
when the command runs, without preventing the rest of the plugin from loading.

## Controls

- **Mouse motion**: move the visible browser cursor using the Vanilla player's look deltas.
- **Left click**: click the page at that cursor. **Right click**: open the control menu
  (not the website's context menu). **Mouse wheel**: scroll vertically.
- **WASD** and **Space** remain cursor/click backups; Ctrl makes keyboard movement finer.
- `/browser menu` also opens controls. **Shift** closes the session.
- The menu has Back, Forward, Reload, Scroll, YouTube, Enter, Esc, Space and Tab.
- **Zoom - / Zoom +** change page magnification in 25-percent steps, from 50% to 250%.
  Click the percentage to reset to 100%. The page stays open, with its history and fields.
  `/browser zoom 175` changes it directly. The default is 100%.
- Enter a URL or a YouTube search in **Address or YouTube search**, then choose
  **Open / Search**. Bare domains get HTTPS. Other plain text becomes a search.
- To enter text into a web form, first click its field in the live page, open the
  menu, fill **Text for the focused browser field**, and choose **Type** or
  **Type + Enter**. The browser field retains focus while Minecraft shows its menu.
- **Return to the browser** resumes the same page/context. **Close browser** ends it.
- **F1** hides the Minecraft HUD for an unobstructed page. HUD instructions otherwise
  render from the consumer's `ui/browser-screen.html` template.

Mouse capture is opt-in through DUI's generic `SurfacePointerInput` API. A private
invisible Interaction entity catches attack/interact clicks; look angles arrive as
relative yaw/pitch deltas. This is a virtual browser cursor, not a free OS cursor.
Updates follow Minecraft's look packets and Paper input ticks, not raw desktop events.
Pitch is bounded by the Vanilla camera, and fast slot bursts can lose wheel steps.
The Vanilla protocol cannot distinguish number keys from wheel slot changes, so number
keys also scroll in the default mouse mode. Set `browser.mouse-controls: false` to restore
legacy hotbar controls: 1 click, 2/3 history, 4/5 scroll, 6 reload, 7 home, 8 menu.
Cookie-consent screens are ordinary page controls you can click yourself. No automatic
consent or account login is performed.

Commands also support `/browser open <URL or search>`, `resume`, `back`, `forward`,
`reload`, `home`, `scroll <pixels>`, `zoom <percent>`, `click`, `type <text>`, `key Enter` and `stats`.
Only HTTP(S) addresses without embedded credentials are accepted; there is no file,
JavaScript or shell-execution command. Special keys are limited to Enter, Escape,
Backspace, Space and Tab.

## Rendering and configuration

```yaml
browser:
  home: https://www.youtube.com/
  node: node
  maximum-sessions: 2
  width: 1280
  height: 720
  fps: 12
  zoom: 100
  mouse-controls: true
  # pixels-per-degree: 14.22 # default: viewport width / 90
  format: BGR555
  bytes-per-second: 67108864
```

This is a **12-FPS capture target**, not the optimized FFmpeg 60-FPS video path. PNG
capture, page rendering and decoding have their own costs. The same map transport is
used, with a consumer-configured byte budget and a conservative effective FPS ceiling.
Choose RGB888 for full color, or BGR555 for fewer maps and 32 levels per channel.
Resolution is the **transmitted picture size**, allowed from 128..2048 by 64..1152
subject to the generic tile budget (for example, Full HD BGR555 uses 271 maps).
Increasing resolution adds pixels and detail; use page zoom to enlarge text/buttons.
The default 1280x720 BGR555 profile uses 121 maps, a 64 MiB/s budget and a 12-FPS target.
Resolution/format/budget changes apply on the next new session after restarting the plugin.

Zoom changes Chromium's CSS viewport to `ceil(picture size / zoom)` and its device
pixel ratio to the zoom factor, so responsive pages reflow without CSS injection.
The producer uses the [Chromium device metrics and screenshot APIs](https://chromedevtools.github.io/devtools-protocol/)
to capture the current scroll position. Integer CSS bounds at fractional pixel ratios
can add at most two edge pixels; the adapter crops those before submitting an exact-size
frame, without rescaling content or cursor coordinates. Metrics and capture run serially to prevent malformed frames.
Mouse/click/scroll picture coordinates are converted to CSS pixels using the same factor.
The cursor and DUI transport still use picture pixels. Zoom persists through navigation,
reload and the control menu within one session. No page-image atlas is used. Sound is muted in Chromium (`--mute-audio`)
and no audio is sent through Minecraft. Screenshots are streamed in memory.

Menu and HUD appearance live in `ui/browser.html` and `ui/browser-screen.html`. These
runtime templates are initially copied from the JAR, like the other consumer templates;
existing edits survive deployment. The process bridge is copied to `browser/bridge.mjs`.
To upgrade these runtime copies, explicitly copy the new resource files while stopped.
`/dui reload` reloads catalogue/map templates; restart to pick up browser template changes.

Opening the control menu suspends capture and closes the video seat, retaining the
browser process and focused page field. Resuming reuses the process and submits its
latest frame to a new surface. Closing, disconnecting, death, teleport, another DUI
screen or plugin shutdown closes the producer. Node closes Chromium on SIGTERM;
the Java adapter also bounds shutdown time and cleans surviving child processes.
Player camera, held slot, location and seat cleanup use the library's existing lifecycle.

## Consumer/library boundary

`BrowserFrameSource` implements the consumer-owned
`FrameSource` interface beside the existing FFmpeg source. The library only receives
immutable `VideoFrame` objects and emits logical `SurfaceInput` and optional
`SurfacePointerInput` events. Pointer capture is a general library capability; cursor
coordinates, sensitivity and button/wheel bindings remain consumer-owned. The browser producer owns
Playwright, HTTP navigation, field input, a bounded action queue and cursor artwork.
The consumer owns menu state and controller policy. Neither DUI shaders nor its pack
generator know about browsers or YouTube.

The bridge has a single bounded stdout writer: `type:u8`, `length:u32be`, then PNG
(type 1) or UTF-8 JSON state (type 2). stdin accepts bounded JSON action lines. Java
decodes off the server tick thread, paints the cursor, quantizes to the selected pixel
format and submits a monotonically numbered frame. Slow readers cause pipe backpressure;
there is no accumulating screenshot queue. Pointer updates coalesce; clicks, typing
and navigation retain their ordering. UI/dialog callbacks enqueue actions and return.

`layouts/<UUID>-browser.json` contains ignored local diagnostics: page title/URL,
cursor, source and transport counters. It contains no screenshot or media binary.

## Validation

Normal `./gradlew check` tests cursor edges/clamping, URLs, channel packing, bounded
framing and templates. The optional real browser test runs when `DUI_BROWSER_RUNTIME`
points to an explicitly installed runtime; it serves an original local fixture and
checks live frames, clicks at 100%/150%, fractional zoom dimensions, typing/Enter,
scrolling, history, zoom across reload, pause and shutdown.

```sh
DUI_BROWSER_RUNTIME="$PWD/run/server/plugins/dui-demo/browser/runtime" \
  ./gradlew -PduiSource=../dui :test --tests gg.kembel.dui.demo.browser.BrowserTest
./gradlew -PduiSource=../dui -PminecraftJar=.cache/minecraft-26.2-client.jar \
  -PacceptEula=true -Pscenario=browser e2e
```

The muted real-client scenario uses original fixture pages instead of relying on
YouTube availability. It checks visible page pixels, actual Vanilla mouse motion, attack/interact clicks and wheel
inputs, the HD viewport, address dialog callback, back/forward, scroll, live zoom buttons,
menu transitions and player restoration. Reports live under ignored `build/reports/e2e/browser/`.
