# Third-party notices

The MIT license applies to original dui sources and original procedural assets. Dependencies and game assets retain their respective licenses and terms.

- **Gson**: Apache License 2.0, https://github.com/google/gson. Used for JSON metadata; the demo bundles this dependency.
- **Gradle Wrapper**: Apache License 2.0, https://github.com/gradle/gradle. The checked-in wrapper bootstraps the pinned Gradle distribution.
- **Paper / Adventure**: provided by the server; not bundled into the plugin. See https://github.com/PaperMC/Paper and https://github.com/KyoriPowered/adventure.
- **Minecraft**: client JARs, font artwork and vanilla item definitions are external, versioned build inputs. They are not licensed under dui's MIT license and are not checked into these repositories. The generated local pack derives shifted fonts and item wrappers from those inputs; do not treat generated game-derived material as original MIT artwork.
- **ZXing core 3.5.4**: Apache License 2.0, https://github.com/zxing/zxing. The demo bundles its QR encoder; its license is retained under `META-INF/licenses/ZXing/LICENSE`.
- **Fabric tooling and API**: development-only input automation; not part of the Paper plugin. See https://github.com/FabricMC.
