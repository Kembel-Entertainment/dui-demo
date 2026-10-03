# dui-demo

**Work in Progress.** A Paper example plugin and real-client integration suite for [dui](https://github.com/Kembel-Entertainment/dui). Built by [Kembel Entertainment](https://kembel.gg).

The plugin embeds `gg.kembel.dui:dui-paper:0.2.0-SNAPSHOT`. It has no dependency on dui implementation internals. Its own templates, sample state, network service and asset generators demonstrate the public API. No client mod is required to use the menus.

## Build

Use Java 25 and a local dui checkout:

```sh
./gradlew -PduiSource=../dui build
```

Without `duiSource`, Gradle resolves the normal Maven dependency. Run `./gradlew publishToMavenLocal` in dui first; then `./gradlew build` here works without a sibling checkout. No remote Maven package has been published yet.

Normal builds run unit tests and do not provision or start Minecraft. The separate `e2e` project is configured only when explicitly invoked by the integration runner.

Gradle supplies the exact current JAR to `installDemo`, so older files in `build/libs` do not confuse installation. With the local server stopped, installation preserves older versioned demo JARs under `run/server/plugin-backups/` before deploying the new version. Direct script calls can select an artifact with `python3 scripts/demo.py install --plugin-jar /path/to/dui-demo.jar`.

## Try the demos

Provide the official Minecraft 26.2 client JAR, or fetch its pinned verified build:

```sh
python3 scripts/demo.py client-jar
./gradlew -PduiSource=../dui -PminecraftJar=.cache/minecraft-26.2-client.jar \
  -PacceptEula=true runDemo
```

`acceptEula=true` records acceptance of the Minecraft EULA for this isolated local server. You can instead set `eula=true` yourself in `run/server/eula.txt`.

For a muted, unmodified localhost client, run `python3 scripts/client.py` in a second terminal with Java 25 available through `JAVA_HOME` or `PATH`. The launcher verifies downloads and uses an offline local profile.

Connect an unmodified Minecraft **26.2** client to **127.0.0.1:25584** and accept the pack. The demo pack is served on loopback port **25585**. All game-mode changes and offline test profiles are confined to that localhost server.

| Command | Example |
| --- | --- |
| `/dui` | Component catalogue, light/dark theme, forms, lists, graph, checkbox, dropdown, player heads and native 3D items |
| `/dui setup` | GUI-scale preference and Compact/Spacious previews |
| `/dui shop spacious` | Sample products, authoritative cart totals and dynamic QR checkout |
| `/dui rewards spacious` | Seven coloured gifts, animated original mascot, finite confetti and persistent demo stars |
| `/dui advent spacious` | Own winter gift wall, 24 replayable gifts, animated lids, native rewards and shader confetti |
| `/dui warps spacious` | Original pixel landscapes, clipped swipe-like carousel, direct selection and portal preview |
| `/dui poker spacious` | Velvet Hold’em: one player, three bots, deal/flip/chip animations, real betting and side-pots |
| `/dui blackjack spacious` | Monarch: animated shoe-to-slot deals, Hit/Stand/Double/Split and demo bankroll |
| `/dui roulette spacious` | Riviera: European wheel, counter-rotating ball, illustrated betting layout, chip animation and demo ledger |
| `/dui slots spacious` | Shared-shader reels, lever, preview outcomes, demo chips and exactly-once payout |
| `/dui videos spacious` | Live YouTube feed and runtime RGB thumbnails |
| `/dui acceptance compact` | Field Journal: unrelated public controller, chrome, stable collection and cached RGBA asset |
| `/dui protocol spacious` | 14-effect batching, consumer shader extension, generic motion and popup coverage lab |
| `/dui extensions` or `/dui extensions play` | Independent measured/typed component, custom font, moving clickable group, RGBA, consumer skin camera/poses and dialog backend |
| `/character spacious` | Aster: live full-body skin and armor, real equipment pickers, rarity borders and vanilla attributes |
| `/character kit` | Explicit OP-only sample gear; opening the sheet grants nothing |
| `/dui map` or `/worldmap` | Original camera-controlled atlas, look-to-pan, scroll zoom, gaze/click details and live pixel HUD |
| `/worldmap dynamic` | Runtime marker position, size and opacity with matching map hit geometry; left-click advances, right-click closes |
| `/dui reload` | Validate and reload every template; requires `dui-demo.reload` |

The short commands `/uikit`, `/uishop`, `/dailyrewards`, `/slots`, `/advent`, `/warps`, `/poker`, `/roulette`, `/blackjack` and `/uivideos` are also available. Reward, Advent, warp, poker, roulette, blackjack, shop, slot and video commands accept `compact`/`spacious`. `/uivideos refresh` checks the feed again.

Active templates are copied to `run/server/plugins/dui-demo/ui/`. Change those templates and reload; the pack remains unchanged. Java supplies view data, business actions and own artwork only. The QR is generated at runtime and points to the demo YouTube video; it is not a real payment system. Casino balances and reward screens are demo-only. The Aster character sheet uses real inventory equipment; its separate OP-only `/character kit` explicitly creates sample items.

When `videos.live` is enabled, the service restores its last successful feed and thumbnails from `run/server/plugins/dui-demo/videos-cache/` before requesting updates. HTTP errors retain that snapshot and show a cached-results status; the saved check time is preserved. Disk reads, decoding and network requests run outside Paper's main thread. Successful downloads replace the cache atomically. The downloaded media is ignored by Git and never added to the resource pack.

## Real-client tests

```sh
./gradlew -PduiSource=../dui -PminecraftJar=.cache/minecraft-26.2-client.jar \
  -PacceptEula=true e2e
./gradlew -PduiSource=../dui -PminecraftJar=.cache/minecraft-26.2-client.jar \
  -PacceptEula=true -Pscenario=videos e2e
./gradlew -PduiSource=../dui -PminecraftJar=.cache/minecraft-26.2-client.jar \
  -PacceptEula=true -Pscenario=videos -PliveVideos e2e
```

Scenarios: `showcase`, `shop`, `rewards`, `advent`, `warps`, `poker`, `roulette`, `blackjack`, `slots`, `confetti`, `videos`, `protocol`, `dynamic`, `casino`, `character`, `map`. The runner starts an isolated server, launches muted clients sequentially and shuts down its own processes. It refuses occupied ports. A graphical display is required; the automated client is locally verified on macOS ARM64. Unit tests are portable Java tests; other client platforms are not yet verified.

By default, E2E videos use deterministic own gradient images and feed entries; the ordinary demo uses the live Minecraft YouTube channel. `liveVideos` opts into real network requests, using the saved snapshot if the upstream feed is unavailable. Temporary test config and operator access are restored after the run.

Reports, screenshots, layout metadata and galleries are under `build/reports/e2e/<scenario>/`. Tests inspect received vanilla widgets, click actual coordinates, compare runtime image pixels, verify animation/focus behaviour and exercise application state.

`run/`, `.cache/`, reports, downloaded game artifacts and generated resourcepacks are ignored. CI is prepared to run builds/unit tests once the companion dui repository is available on GitHub; it starts no game.

See [MIT](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md).

## Advent playground

See [the Advent implementation guide](docs/advent.md). All 24 boxes can be opened immediately and repeatedly. Only the current selection/opening is held in memory; there is no calendar gate, collected state, economy, real inventory grant or persisted Advent player data. Compact mode supports small windows/Auto GUI scale, and Motion off reveals immediately. All UI text is English; the illustrations and palette are generated by this demo's own code.

## Wayfarer atlas

Open `/dui warps spacious`, `/dui warps compact` or `/warps`. [The warp guide](docs/warps.md) explains its own four pixel landscapes, a cyclic native-model strip, generic SLIDE transitions and fixed clipping viewports. Arrow/card clicks animate the strip; Vanilla has no server-visible pointer dragging. Portal entry is an in-memory visual preview, without teleportation, inventory grants or saved warp state. Compact/Motion off controls are included.

## Velvet Hold’em

Open `/dui poker spacious`, `/dui poker compact`, or `/poker`. [The Hold’em guide](docs/poker.md) covers the real four-seat rules, three bots, reusable procedural card/chip components, runtime background artwork, and safe timer lifecycle. Ordinary hands shuffle a full deck; **Showcase hand** explicitly resets the table and scripts a royal-flush demonstration. Every balance is demo chips. Closing or changing menus resets the transient table. Compact and Motion/Still controls are included.

## Riviera Roulette

Open `/roulette`, `/dui roulette spacious` or `/dui roulette compact`. [The Roulette guide](docs/roulette.md) covers the illustrated European table, live unbiased spins, straight/dozen/column/outside bets, chip denominations, Undo/Clear/Repeat, result history and exact payouts. Wheel/ball artwork and motion use the consumer-owned `demo-wheel` shader; hitboxes compose the table independently of its artwork. All balances are transient demo credits. Closing or changing menus resets the table. Compact and Motion/Still controls are included.

## Monarch Blackjack

Open `/blackjack`, `/dui blackjack spacious` or `/dui blackjack compact`. [The Blackjack guide](docs/blackjack.md) explains the original illustrated table, sequential shoe-to-slot card flights, dealer flip, chip settlement, Hit/Stand/Double/Split, exact 3:2 half-credit accounting and transient demo bankroll. Ordinary Deal shuffles six decks; Demo deal explicitly previews scripted examples. Compact and Motion/Still controls are included.

## Library abstraction migration

The [dui roadmap](https://github.com/Kembel-Entertainment/dui/blob/master/docs/roadmap.md) describes the design-neutral boundary. Every demo owns a pure MenuDefinition projection, typed action routes and lifecycle effects, bound through MenuController.refresh and catalogue registration. Consumers use public composition, collections/layout profiles, spans/carousels/strips, tokens, resource providers, scene planning, generated transport, Motion and test helpers. Concrete visual components are registered by this demo. Protocol Lab, Field Journal and extension-proof demonstrate additions through consumer code only. See [the migration guide](docs/abstractions.md). Slot ledger settlement remains application-owned.

Muted integration tests may run alongside a local demo with distinct ports, for example `DUI_DEMO_PORT=25594 DUI_PACK_PORT=25595 python3 scripts/demo.py e2e --scenario shop`. Prepare and install with the same environment. Test clients use the chosen loopback server port.

The compact templates are not guaranteed to fit a 320×240 GUI viewport. Vanilla dialog chrome and native item carrier bodies also consume space. A 640×480 window at GUI scale 2 on an ordinary display supplies only 320×240 units. Select a smaller GUI scale or a larger window; vanilla does not report the viewport to the plugin. Integration fixtures normalize display pixel ratios and record actual GUI bounds, including Auto scale.

## Additional arcade demos

`/horses`, `/wheel`, `/coinflip` and `/bookofra` open four animated games built entirely in this demo using dui's public component and pack extension APIs. Each supports Compact/Wide, Motion/Still, Rules and demo credits. [Rules, architecture and extension contracts](docs/arcade.md).


See [the Aster implementation guide](docs/character.md) for the equipment controller and [the library player-model contract](https://github.com/Kembel-Entertainment/dui/blob/master/docs/player-model.md) for its reusable GPU component.

## Atlas of Elsewhere

`/dui map` and `/worldmap` use dui's public in-game map backend and the same resource pack as every dialog demo. The illustration, target locks, 500ms gaze rule and detail modes belong to this consumer; entity/input/shader/session code belongs to dui. `/worldmap debug`, `still`, `state` and `close` support review. See [the map guide](docs/world-map.md). The demo imports no crawler or captured server assets.

## Design ownership

All concrete widget skins, palettes, icons, motion presets and domain shaders live in this repository. Core supplies public registrations, primitives and validated transport. See [abstractions](docs/abstractions.md) and the independent [extension-proof](extension-proof/README.md) project. Library version 0.2 requires a newly generated protocol-4 pack; no preset compatibility module is used.

The extension labs exercise [dui's dynamic composition API](https://github.com/Kembel-Entertainment/dui/blob/master/docs/dynamic-composition.md) without library source changes. `extension-proof` owns font pixels, camera/limb poses, typed component measurement, group animation and a custom primitive. `ExtensionLab` supplies only the Paper adapter and backend lowering. `ExtensionMapLab` uses public runtime layer state and hit geometry. The `dynamic` client scenario checks these rendered features, interruption, input and cleanup; runtime geometry changes require no new pack, while adding font pixels or model render families requires a pack rebuild.

Experimental mGBA frontend: [Pocket Arcade setup and architecture](docs/gba.md).

Silent local video playback: `/cinema` / `/dui cinema`. See [Cinema setup, controls and the frame-source boundary](docs/cinema.md).
