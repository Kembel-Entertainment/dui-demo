# Third-party notices

The MIT license applies to original dui sources and original procedural assets. Dependencies and game assets retain their respective licenses and terms.

- **Gson**: Apache License 2.0, https://github.com/google/gson. Used for JSON metadata; the demo bundles this dependency.
- **Gradle Wrapper**: Apache License 2.0, https://github.com/gradle/gradle. The checked-in wrapper bootstraps the pinned Gradle distribution.
- **Paper / Adventure**: provided by the server; not bundled into the plugin. See https://github.com/PaperMC/Paper and https://github.com/KyoriPowered/adventure.
- **Minecraft**: client JARs, font artwork and vanilla item definitions are external, versioned build inputs. They are not licensed under dui's MIT license and are not checked into these repositories. The generated local pack derives shifted fonts and item wrappers from those inputs; do not treat generated game-derived material as original MIT artwork.
- **ZXing core 3.5.4**: Apache License 2.0, https://github.com/zxing/zxing. The demo bundles its QR encoder; its license is retained under `META-INF/licenses/ZXing/LICENSE`.
- **Fabric tooling and API**: development-only input automation; not part of the Paper plugin. See https://github.com/FabricMC.

- **Velvet Hold’em illustration**: original asset generated for this demo with the built-in imagegen tool, not extracted from another server or game. The saved source and exact art brief are in `docs/holdem-art.md`. Original demo artwork is supplied under this repository’s MIT license.

- **mGBA / Libretro core**: MPL 2.0, by endrift and contributors. Unmodified, prebuilt macOS ARM64 and Linux x64 cores (`0.11-212-7a12d6d`) are bundled in `src/main/resources/gba/native/`, not in the Minecraft resource pack. Archive checksums and binary filenames are in `manifest.json`; `mGBA-LICENSE` contains the license. Corresponding source is available at https://github.com/libretro/mgba/tree/7a12d6d4b9acb14c0ae62c9166b6a2f3d08007f6. Build artifacts came from https://buildbot.libretro.com/nightly/apple/osx/arm64/latest/mgba_libretro.dylib.zip and https://buildbot.libretro.com/nightly/linux/x86_64/latest/mgba_libretro.so.zip (retrieved 2026-10-02). These binaries retain MPL 2.0; the demo's MIT license does not replace it. No commercial game ROM or Nintendo BIOS is included.
