# The same presentation on Paper and Minestom

Use Java 25 and DUI 0.3. The Paper host is pinned to `26.3.build.170-beta`; Minestom uses the immutable official 26.3 snapshot `26_3-20261007.224755-10`. The stable 26.2 Minestom artifact is incompatible with the 26.3 client.

`platform-showcase` contains one shared `MenuDefinition`, template, state reducer, map and video producer. It depends only on `dui-runtime`. Paper and `minestom-server` provide native viewers, items and appearance snapshots at their boundary. The demo contains a native player model and item, a canvas click, text/boolean inputs, a private atlas and a moving pixel video. Host-specific game transactions stay in their respective application integrations.

Build the library and combined pack:

```sh
./gradlew -PduiSource=../dui build
python3 scripts/demo.py client-jar
./gradlew -PduiSource=../dui -PminecraftJar=.cache/minecraft-26.3-client.jar buildPack
```

For Paper, use the ordinary `runDemo` workflow described in the README. For Minestom:

```sh
./gradlew -PduiSource=../dui :minestom-server:run
```

Connect a 26.3 client to `127.0.0.1:25586` and accept the pack served on `25587`. Run `/platform`, `/platform map` or `/platform video`; `/platform close` releases the current presentation. Enter `stop` in the server terminal to shut down. This explicit local example starts its own offline Minestom host and loopback HTTP endpoint; the library starts neither.

The Minestom example supplies a diamond and four sample armor items on spawn, including dyed leather. The same snapshots are displayed in the shared player portrait; map/video presentation preserves this inventory.

The actual-client acceptance uses isolated test ports `25684/25685` for Paper and `25686/25687` for Minestom. Prepare Paper with `DUI_DEMO_PORT=25684 DUI_PACK_PORT=25685 python3 scripts/demo.py prepare`, accept the EULA in that test instance and install the current plugin/pack with the same port variables. Then run:

```sh
./gradlew -PduiSource=../dui :minestom-server:installDist
python3 scripts/platform_e2e.py --platform paper
python3 scripts/platform_e2e.py --platform minestom
python3 scripts/verify_platform.py
```

Set `JAVA_HOME` to Java 25. These tests require a desktop display, launch muted clients and stop their own hosts. They verify the same ten-step flow, an increment click, changed text and boolean responses, visible atlas/video pixels, cleanup and unchanged inventory. Reports and screenshots are retained separately in `build/reports/e2e/platform/paper` and `minestom`. The Paper `protocol`, `character` and `map` scenarios cover the broader renderer, equipment and spatial lifecycle contracts.
