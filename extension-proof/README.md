# Independent extension proof

This consumer imports only published DUI APIs. It supplies its own ring shader, named typed properties, glyph image and control skins. The ring uses more than 30 parameter bits. No demo renderer, palette or library source is imported.

Run `./gradlew :extension-proof:test -PduiSource=../dui` from the demo root. Its pack contribution is also installed in the demo resource pack. See `ExtensionProof.java` for a complete, small example.

The dynamic proof also registers a bitmap font and an independent player camera/limb pose. Its measured `proof-rows` component accepts a typed list; a template group moves art and hits together. The demo Paper adapter registers its own backend and samples the common timeline. Open `/dui extensions`, replay its animation, or run `/worldmap dynamic`. The `dynamic` E2E scenario checks real-client rendering, alpha, moved hits, replacement cancellation, runtime map geometry, clicks and cleanup.
