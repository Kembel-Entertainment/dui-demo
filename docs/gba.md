# Pocket Arcade / GBA (work in progress)

This demo uses **the existing mGBA Libretro core**, not a new GBA emulator. No native
compilation or download is needed during server startup. Java 25's Foreign Function &
Memory API connects a small frontend to the unmodified binary in a separate JVM.

## Try it

- `/gba` or `/dui gba`: cartridge menu.
- `/gba play color-controls`: the included original, self-authored test cartridge.
- `/gba play filename`: a local `filename.gba` from `plugins/dui-demo/gba/roms/`.
- `/gba resume`, `/gba save`, `/gba stats`, `/gba close`.

The menu displays the first five cartridges; the play command also opens other filenames.
Use a simple filename containing letters, digits, dots, hyphens or underscores, such as
`my-game.gba`. Open the menu first to accept the matching resource pack before playing.
No Pokémon cartridge or Nintendo BIOS is supplied. Put your cartridge in the ROM directory;
it is loaded only on the server and is never included in the client resource pack.
The test cartridge draws a moving color ramp; held GBA keys change its colors.
Its assembly source is `src/test/fixtures/gba/color-controls.s`, and the optional reproducible
fixture builder is `python3 scripts/gba-homebrew.py` (uses an already installed clang).

## Controls

| Default Minecraft binding | GBA input |
| --- | --- |
| W / S / A / D | D-pad up / down / left / right |
| Space (jump) | A, held |
| Ctrl (sprint) | B, held |
| Hotbar 1 / 2 | Start / Select, short pulse |
| Hotbar 3 / 4 | L / R, short pulse |
| Hotbar 8 | Pause and open menu |
| F1 | Hide/show native HUD for clean fullscreen |
| Shift (sneak) | Save and leave |

These are logical Minecraft bindings, not raw physical key capture. Remapped Minecraft
controls apply. Hotbar 9 is the neutral anchor while playing. Audio is discarded in v1.
Opening another DUI screen or disconnecting stops the worker and saves its state.
The game stays at its real timing; dropping old video frames does not slow the emulator.

## Ownership

`ui/gba.html` owns menu design, and `ui/gba-screen.html` owns the fullscreen viewport,
source format, background, frame limit and optional DUI HUD. `gba.*` in config controls
the concurrent-session limit (default 2) and per-player map-byte budget (default 16 MiB/s).
The library owns only the general streaming surface and its cleanup/input mechanics.
The demo owns Libretro, workers, controls, cartridges, saves and all GBA assumptions.

`GbaWorker` runs in a separate Java process with a 128 MiB Java heap limit. Each player
has a separate process and save directory:
`gba/saves/<player UUID>/<ROM SHA-256>/`. A checkpoint contains its ROM hash and core
version and resumes only when both match. Battery memory is retained independently.
Writes use a flushed temporary file, atomic replacement where supported and a backup.
Checkpoints happen every 30 seconds, on request and on graceful exit. A killed/crashed
worker recovers from its last complete checkpoint; a forced kill cannot promise the
final unsaved seconds. The JVM is process isolation, not an operating-system sandbox.

Native audio callbacks drain samples without output. Frame color conversion preserves
the selected Libretro pixel format exactly into RGB888; color correction, frame skipping
and interframe blending are disabled. Length limits and CRC checks protect the binary
IPC, and the frame mailbox is bounded to one latest frame. Paper handlers never wait
on worker I/O. A blocked worker gets a three-second shutdown deadline.

## Bundled core

Verified Libretro buildbot artifacts downloaded 2026-10-02, built 2026-10-01:
`mGBA 0.11-212-7a12d6d`. The plugin contains macOS ARM64 and Linux x64 binaries in small
ZIPs, outside the resource pack. Other server architectures currently fail with a clear
message. The archive SHA-256 values, binary filenames and corresponding source commit are
in `src/main/resources/gba/native/manifest.json`. Extraction verifies both the archive
and any existing extracted binary. mGBA's MPL 2.0 license ships alongside the artifacts;
these third-party files are not relicensed under this repository's MIT license.

Corresponding source: https://github.com/libretro/mgba/tree/7a12d6d4b9acb14c0ae62c9166b6a2f3d08007f6

## Validation

`./gradlew check` checks IPC corruption/length handling along with existing demo tests.
After building the plugin JAR, `python3 scripts/gba-smoke.py` loads the bundled native core,
checks original-color frames and input, saves a checkpoint and resumes it in a fresh worker.
`./gradlew e2e -Pscenario=gba ...` runs an input-only Fabric fixture on the ordinary
Vanilla renderer, with audio muted. It records fullscreen screenshots, actual pixel
changes, menu/resume and camera/seat/inventory cleanup. The worker's native output is
about 59.73 Hz. That number alone is **not a measured visible Minecraft frame rate**.
Measured results and platform limits must be reported separately.

On macOS ARM64, Paper/Minecraft 26.2 at 1280×900 with a 120 FPS render cap, the original
color/input cartridge produced **348 visible pixel changes in approximately six seconds
(57.94 visible FPS)** across 715 sampled render frames. Held Vanilla jump reached the worker
as GBA A; menu/resume, GUI scale Auto and inventory/seat cleanup passed. A separate native
smoke test passed checkpoint recovery. This is a local test of the self-authored cartridge,
not a Pokémon compatibility run or a performance promise for every client/network. Audio
is silent and this test uses F1 for the unobstructed fullscreen image. Rapidly changing
full-color frames can use several MiB/s before network compression; budget per player.
