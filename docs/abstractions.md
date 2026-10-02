# Using the design-neutral library

This consumer uses dui-core and dui-paper at runtime and dui-pack only to build assets. dui-test supplies portable test helpers. There is no preset library dependency. The separate extension-proof project imports no demo classes.

| Consumer code | Role |
| --- | --- |
| DemoTemplates / DemoUiTheme / DemoTheme | Consumer normalization, skins, tokens and legacy palette mapping |
| DemoWidgets / DemoGlyphs | Concrete widget painting and owned glyph pictures/bindings |
| DemoVisualComponents / component schemas | Consumer domain tags and chrome fragments |
| DemoShaders / DemoEffectComponent | Logical typed shader declarations and invocations |
| DemoMotion | Concrete animation presets assembled from generic Motion |
| DemoPackGenerator / shader resources | Trusted GLSL functions/modules, static assets and map definitions |
| Controllers / rules / services | Commands, state, gameplay, transactions and bounded external resources |
| extension-proof / ExtensionLab | Independent measured/typed component, shared animated geometry, font/camera definitions and custom Paper body lowering |
| ExtensionMapLab | Runtime map marker geometry/opacity through public layer state and selection APIs |

Add a demo component to this registry or include a separate registry package. Use PropertySchema for scalar properties and ComponentContract/ValueCodec for structured properties; provide a measurer when natural size depends on content. Register its shader/glyph/font/model render family through a PackContribution when necessary. Pack metadata assigns addresses and validates exact contracts; applications never choose numerical opcodes. A trusted BodyBackend can lower additional consumer RenderPrimitive types without editing the library.

Playing cards, chips, roulette/reel art, particles, skill trees and decorative equipment slots live here. Native ItemStacks, heads/player models, layout, control behavior, clips, Motion, template bindings and private map lifecycle come from dui. Map/HUD placement, opacity, colors, icons and opening/pulse configuration remain consumer-owned.

No rendering work happens in network service futures: publish through scoped owner-thread tasks and render immutable snapshots. SceneGroup shares sampled supported transforms between visuals and hits; DialogSession.animate handles interruption and stable actions across animation frames. Core budgets and unsupported capabilities fail explicitly. Current limits include fixed backend phases, nine-unit hit rows, whole-object popup occlusion, backend-specific group capabilities and finite transport fields. Runtime map layers use a later overlay pass rather than arbitrary cross-layer ordering.

See the library's [dynamic composition guide](https://github.com/Kembel-Entertainment/dui/blob/master/docs/dynamic-composition.md), extensions, migration-0.2 and LLM guides for APIs and extension-proof for an independent implementation. Client tests exercise all demos with the combined pack; builds do not start Minecraft automatically.
