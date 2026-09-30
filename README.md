# dui-demo

**Work in Progress.** A Paper example plugin and real-client integration suite for [dui](https://github.com/Kembel-Entertainment/dui). Built by [Kembel Entertainment](https://kembel.gg).

The plugin embeds `gg.kembel.dui:dui-paper:0.1.0-SNAPSHOT`. It has no dependency on dui implementation internals. Its own templates, sample state, network service and asset generators demonstrate the public API. No client mod is required to use the menus.

## Build

Use Java 25 and a local dui checkout:

```sh
./gradlew -PduiSource=../dui build
```

Without `duiSource`, Gradle resolves the normal Maven dependency. Run `./gradlew publishToMavenLocal` in dui first; then `./gradlew build` here works without a sibling checkout. No remote Maven package has been published yet.

Normal builds run unit tests and do not provision or start Minecraft. The separate `e2e` project is configured only when explicitly invoked by the integration runner.

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
| `/dui slots spacious` | Shared-shader reels, lever, preview outcomes, demo chips and exactly-once payout |
| `/dui videos spacious` | Live YouTube feed and runtime RGB thumbnails |
| `/dui reload` | Validate and reload every template; requires `dui-demo.reload` |

The short commands `/uikit`, `/uishop`, `/dailyrewards`, `/slots` and `/uivideos` are also available. Reward, shop, slot and video commands accept `compact`/`spacious`. `/uivideos refresh` checks the feed again.

Active templates are copied to `run/server/plugins/dui-demo/ui/`. Change those templates and reload; the pack remains unchanged. Java supplies view data, business actions and own artwork only. The QR is generated at runtime and points to the demo YouTube video; it is not a real payment system. Balances and rewards are demo-only and never grant real items.

## Real-client tests

```sh
./gradlew -PduiSource=../dui -PminecraftJar=.cache/minecraft-26.2-client.jar \
  -PacceptEula=true e2e
./gradlew -PduiSource=../dui -PminecraftJar=.cache/minecraft-26.2-client.jar \
  -PacceptEula=true -Pscenario=videos e2e
./gradlew -PduiSource=../dui -PminecraftJar=.cache/minecraft-26.2-client.jar \
  -PacceptEula=true -Pscenario=videos -PliveVideos e2e
```

Scenarios: `showcase`, `shop`, `rewards`, `slots`, `confetti`, `videos`. The runner starts an isolated server, launches muted clients sequentially and shuts down its own processes. It refuses occupied ports. A graphical display is required; the automated client is locally verified on macOS ARM64. Unit tests are portable Java tests; other client platforms are not yet verified.

By default, E2E videos use deterministic own gradient images and feed entries; the ordinary demo uses the live Minecraft YouTube channel. `liveVideos` opts into the real network test. Temporary test config and operator access are restored after the run.

Reports, screenshots, layout metadata and galleries are under `build/reports/e2e/<scenario>/`. Tests inspect received vanilla widgets, click actual coordinates, compare runtime image pixels, verify animation/focus behaviour and exercise application state.

`run/`, `.cache/`, reports, downloaded game artifacts and generated resourcepacks are ignored. CI is prepared to run builds/unit tests once the companion dui repository is available on GitHub; it starts no game.

See [MIT](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md).
