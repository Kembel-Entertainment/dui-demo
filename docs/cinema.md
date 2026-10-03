# Silent local video / Cinema

`/cinema` or `/dui cinema` opens the file selection. Put videos in the server's
`plugins/dui-demo/media/files/` directory. For this repository's development server,
the path is `run/server/plugins/dui-demo/media/files/`. Use simple filenames such as
`night-drive-720p.mp4` and `jazz-1080p60.mp4`. MP4, MKV, WebM and MOV are supported.
The menu lists the first five files; commands can open the rest. New files appear
when the menu opens, without restarting. Symbolic links and external paths are excluded.

Install FFmpeg and ffprobe on the **server**, or configure their executable paths
under `media.ffmpeg` and `media.ffprobe`. These are external runtime prerequisites;
the plugin does not download native binaries. Audio is disabled with FFmpeg's `-an`.

- `/cinema play night-drive-720p`: start from the beginning.
- `/cinema play jazz-1080p60 512 30`: choose output width and FPS explicitly.
- `/cinema pause`: toggle pause without losing the playback position.
- `/cinema resume`: return from the selection menu.
- `/cinema restart`, `/cinema stats`, `/cinema close`.
- `/cinema seek 20`: restart decoding at the specified timestamp.
- `/cinema speed 1.5`: change playback speed (0.25..4), preserving position and pause.
- `/cinema play jazz-1080p60 384 30 20`: choose output and start timestamp together.
- `/cinema play jazz-1080p60 1024 60 20 BGR555`: 1024x576, 60 requested FPS,
  starting at 20 seconds, with the faster 15-bit color mode.
- `/cinema play jazz-1080p60 768 60 0 RGB888`: full 24-bit color at 768x432.
  The optional last argument selects the color format; otherwise `media.format` applies.
- Hotbar **1** pauses, **2** replays, **8** opens the menu, **Shift** leaves.
- **A/D** seek -/+5 seconds per press; **W/S** step faster/slower from 0.25x to 4x.
  These are vanilla logical left/right/forward/backward inputs, so custom key bindings apply.
  Holding a key applies one adjustment until release. Opposing held directions cancel.
  Seeking while paused updates one preview frame, then stays paused. Seeking, replay and
  speed changes replace the FFmpeg decoder while retaining the seat, textures and surface.
- **F1** hides the native HUD/crosshair for an unobstructed picture.

Source and transport resolutions are different. A 1920x1080/60 input is decoded on
the server and scaled to the selected output. Defaults are 384 pixels wide, preserving
aspect ratio, at 30 FPS in RGB888 with a 16 MiB/s per-player map-byte budget. A 384x216 RGB888
surface uses 25 map textures. More output pixels/FPS need more bandwidth and client
texture uploads; an input marked "1080p" does not imply a 1080p Minecraft stream.
The player now plans its output FPS using the library's conservative full-frame cost,
and reports the effective FPS when the requested value exceeds the byte budget. At
1024x576 RGB888 and 16 MiB/s, this is about 6.35 FPS. 60 full updates/s would require
approximately 151 MiB/s of uncompressed map payloads; changing FFmpeg flags cannot
remove that transport/client-upload cost. A 256x144 output fits 60 FPS within the same
budget. Actual client performance can still be lower than the planning limit.

`RGB888` preserves the decoded 8-bit-per-channel RGB values. `BGR555` deliberately
quantizes to 5 bits per channel (32 rather than 256 levels), then uses DUI's existing
lossless transport for those 15-bit values. It does not reduce spatial resolution or
playback speed. At 1024x576 it uses 81 maps instead of 161, reducing native client
map conversion, texture uploads and symbol reconstruction. The HUD states 15-bit or
24-bit color explicitly. No new shader or library changes are needed for this choice.

## Local smooth playback profile

The loopback development server uses this explicitly chosen runtime profile:

```yaml
media:
  format: BGR555
  width: 1024
  fps: 60
  bytes-per-second: 201326592 # 192 MiB/s of uncompressed map patch allowance per viewer
```

Its `config/paper-global.yml` uses `misc.compression-level: 1`, retaining packet
compression with less encoding work. This is a server setting, not a plugin or
library setting. The demo's default configuration remains the smaller RGB888 profile;
the plugin never rewrites an external server's network settings. The larger allowance
does not imply every stream uses all of it. Actual map payload rate depends on moving
pixels, format and FPS, and wire bytes also depend on compression. This profile is
measured for local playback; Internet links and multiple viewers need separate budgets.

Use full RGB888 at 768x432 when color precision matters more than the final spatial
resolution. Minecraft does not impose a universal 20/30 FPS cap on these surfaces;
its native per-map updates and texture uploads still cost client time at large sizes.

The shared DUI transport spaces accepted patch bytes continuously rather than filling
a one-second budget and then stopping. This removes the periodic burst/wait pattern
for any pixel producer. FFmpeg drops/duplicates frames before scaling, and speed uses
`setpts=(PTS-STARTPTS)/speed` followed by the selected output FPS. Speed changes source
time per output frame; they do not multiply the surface's FPS or bandwidth demand.

`/cinema stats` distinguishes source metadata, prepared frames and DUI transport
statistics. Preparation time measures RGB conversion in Java, not all FFmpeg CPU
time. Actual visible frame rate requires client measurement. Diagnostics are written
to `layouts/<player UUID>-cinema.json`; no screenshots or video copies go into the pack.
Each diagnostic includes `sampledAtNanos`; source/network counter rates must use the
interval between those server snapshots rather than a separate client measurement
interval, since diagnostics are sampled twice per second.

## Ownership and future browser sources

Application controls and decoding live in **dui-demo**. The library supplies
`VideoSurfaceTemplate`, `VideoFrame`, `VideoSurfaceSession.submit()` and logical input.
`cinema.html` owns the selection design; `cinema-screen.html` owns viewport, color,
scaling, limits and the optional HUD. Frames are sent over private vanilla map packets.
The library's source-neutral `VideoPacer` and frame-cost planning helpers are shared
transport behavior, with no cinema controls, file catalogue or FFmpeg dependency.

The consumer's `FrameSource` interface separates a pixel producer from the surface.
`FfmpegFrameSource` is the first producer. A future headless-browser adapter can produce
RGB frames and receive `SurfaceInput` through the same boundary. Browser capture,
navigation and browser-specific input translation would belong in the consumer.
There is currently **no headless-browser implementation**. The current surface exposes
logical movement/jump/sneak/sprint and hotbar pulses; it does not expose arbitrary keys,
mouse coordinates, mouse buttons or wheel events. Browser pointer control needs a
separate input design, such as the world map's gaze/click geometry or an explicit cursor.

FFmpeg starts only after the resource pack and surface are ready. Decoding runs on a
separate thread; the OS pipe and DUI's latest-frame mailbox bound buffered frames.
Pausing blocks reading and freezes the playback clock. The last frame remains visible
at EOF. Closing, changing to another DUI screen, death, teleport, disconnect or plugin
shutdown closes the decoder and restores the player's seat/appearance/held slot.

`run/` and all `*.mp4`, `*.mkv`, `*.webm`, `*.mov` files are ignored. Local videos are
neither committed nor packaged into the plugin JAR or resource pack. Tests generate
their own small original clips using FFmpeg; no media binary fixture is versioned.

## Validation

`./gradlew check` tests fragmented/truncated RGB reads, exact channel order, catalogue
isolation, template composition, real silent decoding, pause/resume and EOF.
`-Pscenario=cinema e2e` uses ordinary vanilla rendering with muted audio, verifies
moving pixels, real hotbar pause, A/D seek and W/S speed via vanilla inputs, unchanged
seat while adjusting playback, menu/resume, replay, end-of-file and player cleanup.
Set `DUI_MEDIA_BENCHMARKS=night-drive-720p,jazz-1080p60` to measure additional local
clips at the same output settings; they must already exist in the test server's
`media/files/` directory. Measured visible rates are local observations, not guarantees.
An optional `:width:fps` suffix selects another requested output, for example
`DUI_MEDIA_BENCHMARKS=jazz-1080p60:1024:60`. The report includes the maximum interval
between visible changes, so a periodic stall cannot hide behind an average FPS value.
Append `:BGR555` or `:RGB888` to select a format. `DUI_MEDIA_TEST_BYTES` overrides the
isolated test server's media budget. `DUI_MEDIA_TEST_FORMAT` selects its default format.
`DUI_MEDIA_BENCHMARK_ONLY=true` runs measurement cases without claiming the skipped
controls/EOF checks passed. `DUI_MEDIA_WINDOW=1920x1080` and
`DUI_MEDIA_BENCHMARK_SECONDS=20` select window size and observation duration.
`DUI_E2E_COMPRESSION_LEVEL=1` temporarily tests another Paper compression level and
restores the original global config afterwards; `DUI_E2E_COMPRESSION_THRESHOLD=-1`
is an explicit comparison with network compression disabled.
Benchmarks start at 20 seconds to skip static intros and sample nine screen regions,
so changing pictures outside the center are counted. This measures visible pixel
updates; identical consecutive source frames correctly cause no texture update.

Local measured run on macOS ARM64, official bundled Java runtime, Paper/Minecraft 26.2, 1280x900 client window,
120 FPS render cap, 384x216 RGB888 output at 30 FPS, approximately six seconds per clip:

| Input | Visible pixel updates per second |
| --- | --- |
| Original generated moving fixture | 30.18 |
| Local 1280x720 / 29.97 FPS clip | 30.11 |
| Local 1920x1080 / 59.94 FPS clip | 30.26 |

Small finite-window/boundary differences can put the estimate slightly above 30.
These are downscaled output measurements, not native 720p/1080p transmission. Pause
kept the source frame counter fixed; only the initial observer sample changed. Menu,
resume, replay, natural EOF, Auto GUI scale, held slot and inventory restoration passed.
At requested 1024x576 / 60 FPS, the budget planner selected 6.35 FPS. The six-second
real-client sample measured 6.46 visible updates/s with a longest gap of 0.174 seconds;
there were no one-second budget stalls. A/D and W/S were verified through ordinary
vanilla input packets, including paused seeking and speed changes without remounting.
The observer restores OpenGL pixel-pack state around reads. Set `DUI_E2E_JAVA` to
an alternative client Java executable when checking a specific runtime; it does not
change the build JDK or server runtime. An earlier Temurin test run aborted in native
GC/malloc code; the complete run above passed using the official client runtime.

Additional macOS ARM64 measurements at a 1920x1080 client window, official Java,
one local viewer and a 256 MiB/s map allowance (six-second samples except the last):

| Output / color | Compression | Visible updates/s |
| --- | --- | --- |
| 1024x576 / RGB888 / requested 60 FPS | Server default | 46.93 |
| 1024x576 / RGB888 / requested 30 FPS | Server default | 30.24 |
| 768x432 / RGB888 / requested 60 FPS | Server default | 59.84 |
| 512x288 / RGB888 / requested 60 FPS | Server default | 60.06 |
| 1024x576 / RGB888 / requested 60 FPS | Level 1 | 51.73 |
| 1024x576 / RGB888 / requested 60 FPS | Disabled | 52.80 |
| 1024x576 / BGR555 / requested 60 FPS | Level 1 | 59.85 |

The last sample ran for 20 seconds, with a 21.7 ms 95th-percentile visible-frame gap
and a 60.7 ms maximum gap. Reducing map count improved client throughput after packet
delivery was already near 60 FPS. Disabling compression did not remove the remaining
RGB888 client cost. FFmpeg alone decoded/scaled the same 1080p source to 1024x576
RGB24 at about 187 FPS in a separate six-second-source benchmark. These observations
describe this machine and clip, not a guaranteed performance tier for all clients.
Paper documents the [compression level](https://docs.papermc.io/paper/reference/global-configuration/#misc_compression-level)
and the [packet compression threshold](https://docs.papermc.io/paper/reference/server-properties/#network-compression-threshold).
