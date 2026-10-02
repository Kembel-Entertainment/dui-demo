# Advent / Gift drop

Open `/dui advent spacious`, `/dui advent compact`, or `/advent`. This is a replayable visual playground: every box is available, with no date checks or collected ledger. Only the selected day, current phase and transient timing live in the player's session. Closing/back/section changes discard that selection. Preferences are session-only too. No real items are given. Diagnostic action/layout reports are separate from gameplay persistence.

## Responsibility boundaries

| Layer | Source | Responsibility |
| --- | --- | --- |
| Template | [advent.html](../src/main/resources/ui/advent.html) | Shelf, repeated boxes, colours, hit regions, native layers, motion presets, particles and controls |
| View data | [AdventView](../src/main/java/gg/kembel/dui/demo/AdventView.java) | Irregular packed shelf coordinates, Compact/Spacious presentation, raster maps and resolved labels |
| Session | [AdventState](../src/main/java/gg/kembel/dui/demo/AdventState.java) | BOARD → OPENING → REVEALED, current selection, replay/cancellation generation and event times |
| Artwork | [AdventArt](../src/main/java/gg/kembel/dui/demo/AdventArt.java) | Original RGB parcel tiles, pixel lettering/snowy roof, six body/lid model palettes |
| Native media | [AdventItems](../src/main/java/gg/kembel/dui/demo/AdventItems.java) | Decorative parcel models and actual vanilla reward ItemStacks |
| Controller | [DuiDemoPlugin](../src/main/java/gg/kembel/dui/demo/DuiDemoPlugin.java) | Binds actions through the public API, guards delayed updates, and cleans finite effects |
| Library | dui `Motion` plus consumer shaders | Consumer POP/BOUNCE/LIFT factories composed from Motion; consumer-owned confetti shader |

Closed boxes are small runtime RGB illustrations, transmitted by `dui-image`; they are not screenshots of Minecraft items and do not require 24 hidden native dialog bodies. Opening uses separate native body/lid models and a normal vanilla reward model. The shader transforms Minecraft's rendered models; it does not pre-render the reward into the pack. The wordmark and roof are runtime raster artwork. Body/lid models are pack additions and need a pack rebuild when changed.

## Timing and layout

A click starts a 24-tick opening. The body bounces and the lid lifts at client frame rate. A guarded server callback reveals the reward once; it enters with an 18-tick POP and independent 52-piece confetti. One cleanup at 120 ticks displays final poses and stops clock-wrap replay. Updates preserve the initial start time. Back, replay, closing and section changes cancel previous generations. Motion off shows the result immediately.

Spacious mode uses a 480×360 shelf and 480×288 opening. Compact uses a 320×180 shelf and 320×135 opening. It uses static shelf lighting and removes decorative parcel carriers after reveal to leave room for native-widget spacing in small vanilla windows. Gift buttons retain distinct IDs and integer, 9-pixel-aligned hit rows. Runtime artwork stays inside dui's 16,384 sampled-pixel budget.

Change placement/palettes/labels through template/view data. Use the consumer motion factories and contributed particle component for other gift, achievement, inventory or onboarding views. A new menu does not need its own shader. This demo depends only on the public `dui-paper`/`dui-core` API; artwork generation remains demo-owned.

## Validation

`AdventTest` checks all 24 gifts twice, stale callback cancellation, non-overlapping coordinate hits, both layouts, motion settings and image budgets. The `advent` E2E scenario opens every gift in a muted Minecraft client, checks replay and explicit close, tests a 640×480 window and Auto GUI scale, compares runtime image pixels and captures native animation frames. Reports stay in `build/reports/e2e/advent/` and are ignored by Git.
