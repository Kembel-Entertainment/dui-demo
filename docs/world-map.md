# Atlas of Elsewhere

`/dui map` or `/worldmap` opens an original illustrated 1120×640 atlas with three destinations. Look to pan, scroll to zoom, aim for 500ms or left-click to explore, right-click to go back/close, and Shift to leave the seat. `/worldmap still` reduces motion; `/worldmap debug` opens the calibration rectangle. `/worldmap state` reports the active presentation and `/worldmap close` closes it.

The demo owns artwork, layer definitions, target permissions, the menu state machine and `src/main/resources/ui/worldmap-hud.html`. The deployed editable file is `plugins/dui-demo/ui/worldmap-hud.html`; `/dui reload` validates and applies it to already-open maps without a pack download. Invalid templates retain the previous compiled HUD. `WorldMapArt` draws the images; `WorldMapAssets` builds the named geometry and pack contribution; `WorldMapState` projects read-only frames and routes application actions. `WorldMapDemo` uses the public `Dui.openWorldMap` API, session task scope and input callbacks. It contains no display entities, packet interception or custom shader code. The example selects modes and prints a preview message; it starts no game and performs no teleport.

The 20 tiles, target images and option images are included in the same generated dui pack as the other demos. Fonts, negative spacing, common geometry, map/HUD shaders and the Paper input/session adapter are supplied by dui. No crawler, recording, captured resource pack or original-server asset is needed. Changing artwork/static geometry requires a pack build; runtime text, locks, visibility, hover and views use ordinary frame updates.

Build and run with the README's Java 25 commands. The existing local server remains at 127.0.0.1:25584 with its pack at 25585. Opening this demo does not change gamemode or grant items. An explicit `testing.map-fixtures` setting enables an inventory/survival fixture only on a loopback-bound server for the offline `MapTest` profile; ordinary users and production bindings do not enable it.

```sh
./gradlew -PduiSource=../dui -PminecraftJar=.cache/minecraft-26.2-client.jar \
  -PacceptEula=true -Pscenario=map e2e
```

The muted diagnostic client injects ordinary vanilla inputs. It checks a nonzero reference yaw across the ±180° wrap, FOV 30/110, GUI 1/2/Auto, hover, locks, gaze/click selection, rapid slot changes, detail actions, right-click, dismount, external teleport, death/respawn, inventory synchronization, dialog/map replacement, plugin shutdown and disconnect. A concurrent second client checks that foreign map entities and self-only appearance flags do not leak. Screenshots are measured against the shared projection and HUD pixel contract. Reports appear under `build/reports/e2e/map/` and `map-observer/`.

Use `DUI_RUN_DIR=run/isolated/server`, `DUI_DEMO_PORT` and `DUI_PACK_PORT` to run against a separate local server directory and ports while another demo is running. Apply these variables consistently to prepare, install and E2E. Client automation adds no renderer and does not require any client mod for ordinary use. Manual unmodified-client review remains distinct from automated diagnostic evidence.

For a latency run, start the loopback-only fixture in a second terminal:

```sh
python3 scripts/latency_proxy.py --listen 25606 --target 25604 --delay-ms 100
```

Then run the map E2E task with `DUI_RUN_DIR=run/isolated/server DUI_DEMO_PORT=25604 DUI_PACK_PORT=25605 DUI_E2E_PORT=25606 DUI_E2E_DELAY_MS=100`. The last value only records the fixture setting; the proxy introduces the actual delay. The same inputs and screenshot assertions run with an additional 100ms in each direction. Stop the proxy afterwards. `run.json` records the ports and configured delay so direct and delayed results can be distinguished.

See [dui's public world-map contract](https://github.com/Kembel-Entertainment/dui/blob/master/docs/world-map.md) for implementation guidance and transport limits.

The HUD template controls anchors, offsets, sizes, colors, surface transparency, text, progress bars, icon selection and control layout. It wraps ordinary `dui-menu` fragments, so normal rows/columns/grids, conditional nodes, bindings and repeats apply. Background surfaces use opacity 0.78; foreground labels/icons remain fully opaque. The pure map state supplies presentation data while `WorldMapDemo` projects the consumer-owned template into `WorldMapFrame.Hud.withPresentation`.

The map client also replaces the open HUD with an unrelated three-anchor design containing seven controls, verifies the new colors and all seven rows, attempts an invalid reload and then restores the original design. It checks that seat/input/display entity IDs and pack metadata remain unchanged. This fixture changes only its isolated server's template file and restores it after the run.
