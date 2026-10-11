# Desktop authoring with dui studio

The sibling [dui-studio](https://github.com/Kembel-Entertainment/dui-studio) project is a standalone Tauri/Svelte editor. Its Java preview engine calls DUI's public template, scene, map, motion and video APIs. DUI itself is unchanged by this integration.

This demo owns `studio-preview`, its `DemoStudioProvider`, fixtures, component schemas, theme and shader registrations. The provider reuses `ShopView`, `AdventView`, `RouletteView`, `CharacterView` and the other real presentation factories. It runs no Paper actions, payouts, inventory updates, Chromium, mGBA or FFmpeg. Live pixel screens use a procedural `VideoFrame` fixture; native items, profile heads and body/armor models use your private local Minecraft 26.3 JAR.

With Java 25 and local checkouts of both DUI and Studio:

```sh
./gradlew :studio-preview:prepareStudio \
  -PduiSource=../dui -PduiStudioSource=../dui-studio
```

This optional task creates ignored `build/studio/` adapter/dependency JARs and `dui.studio.local.json`. Normal demo builds do not depend on Studio or include its SDK. The tracked `dui.studio.json` only names the project and local profile.

Open the demo folder in Studio and select the local Minecraft JAR. Each existing template has portable wide/compact fixtures. GLSL files declared by the provider preview directly against their corresponding template; edits and compile errors appear without rebuilding the adapter. JSON overrides and the scrub clock change presentation data only.

The runtime and preview share these consumer-authored values:

- `src/main/resources/worldmap/atlas.map.json`: public `WorldMapDefinition` geometry and opening settings.
- `src/main/resources/worldmap/atlas.targets.json`: demo destinations and unlock/record fixtures.
- `src/main/resources/animations/advent.timeline.json`: opening/reveal segments consumed by `AdventState`.

## Save and reload on Paper

Studio saves the original project files. The server must read those same files, rather than its independent deployment copies. In the **local development server** `run/server/plugins/dui-demo/config.yml`, set:

```yaml
templates:
  source-directory: "/absolute/path/to/dui-demo/src/main/resources/ui"
```

Restart the server once after changing this setting. Thereafter: edit an HTML template in Studio, **Save**, then run **`/dui reload`** (permission `dui-demo.reload`). No Gradle build, install or file-copy step is needed for these changes. The command reports the active source directory. All dialogue templates, `worldmap-hud.html`, and GBA/Cinema/Browser selection/control and screen templates participate. Templates compile and validate before replacing the previous set; on failure, fix the reported file and reload again. Active dialogue views and map/stream HUDs refresh; selection dialogs without a running emulator/video are refreshed when reopened. Existing game, emulator, decoder and browser state is preserved.

Changing a live pixel screen's geometry or transport settings requires closing its active sessions before reloading; HUD-only edits can update during playback. An unset/empty `templates.source-directory` uses `plugins/dui-demo/ui` as usual and preserves local deployment edits. Relative configured directories resolve against the plugin data folder. Invalid/missing source files fail explicitly and do not fall back to stale deployment copies. Bundled defaults are never written into a configured source directory.

HTML layout, text, colors, controls and references to **existing** pack assets can reload this way. GLSL code, new assets, pack geometry/registrations and the `worldmap`/`animations` JSON resources currently require rebuilding/installing the demo/pack and a server restart; they are not HTML hot reload. Java component/behavior changes need rebuilding the plugin. Build the Studio adapter and reload its renderer after Java/classpath resource changes. No proprietary export format or generated Paper plugin is involved.

Keep ROMs, videos, downloaded skins, generated packs and runtime caches in the already ignored local directories documented by their own guides. Studio does not copy them into tracked sources.
