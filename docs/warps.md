# Wayfarer / Atlas

Open `/dui warps spacious`, `/dui warps compact` or `/warps`. Four original realms—Willow Vale, Bloomhaven, Ember Keep and Astral Reach—sit in a wooden/parchment atlas. Arrows and side-card clicks slide the strip in either direction; it wraps across the last/first realm. Numbered tabs offer direct selection in Spacious mode. The middle card and Enter portal button confirm a visual demo destination. No teleport, inventory grant, economy or persisted warp state is involved. Ignored layout/action reports contain diagnostic snapshots; they are never restored as gameplay state.

## Consumer and library boundaries

| Source | Role |
| --- | --- |
| [warps.html](../src/main/resources/ui/warps.html) | Wood/parchment surfaces, styles, repeated cards, fixed clip viewport, controls and hit regions |
| [WarpView](../src/main/java/gg/kembel/dui/demo/WarpView.java) | Layout bindings, strip positions/IDs, labels, Compact/Spacious geometry |
| [WarpState](../src/main/java/gg/kembel/dui/demo/WarpState.java) | Session-only selection, circular steps, transit lock, preview and cancellation generation |
| [WarpArt](../src/main/java/gg/kembel/dui/demo/WarpArt.java) | Own pixel landscapes/seals, selected/unselected brass frames and generated model/texture additions |
| [WarpItems](../src/main/java/gg/kembel/dui/demo/WarpItems.java) | Decorative native ItemStacks keyed by template placement ID |
| [DuiDemoPlugin](../src/main/java/gg/kembel/dui/demo/DuiDemoPlugin.java) | Actions through the public API, a guarded finite completion callback and template reload |
| dui `ItemTransition.SLIDE` / `ItemClip` | Reusable GPU motion and fixed native-pixel clipping; no warp-specific shader |

The illustrated cards are consumer-owned textured models, not pre-rendered Minecraft items. Only eight card variants (four scenes × normal/selected) are added to the demo resourcepack. All scenery is authored procedurally in WarpArt; none of the reference's assets, logos or names is shipped. Models/textures require pack regeneration; layout, labels, positions and timing can change through template/view data without new shaders.

## Motion and input

The resting strip uses three native carriers. An arrow advances the selected index and submits four target placements, including one off-canvas outgoing/incoming card. A signed distance shifts every native quad from its prior position to its target over 24 world ticks with cubic easing. The fixed viewport discards pixels beyond the paper's card region, including fully hidden carrier pixels.

Only the click update and one completion update are sent. The client shader draws the intermediate frames. The completion removes the hidden fourth carrier and unlocks fixed controls; it checks player/session/menu/generation before applying. Closing, switching menus, size changes and Motion off cancel previous work. Resting views omit transitions, preventing clock-wrap replay. Portal preview briefly bounces the selected model using the same library preset.

Vanilla dialogs send discrete actions, not pointer down/move/up, swipe deltas or scroll-wheel events. This demo provides a swipe-like carousel through arrows/card clicks, not true mouse/touch dragging. The card pictures themselves are visual native item layers; the template's separate stationary hits provide clicks. Input is locked while those pictures move so callbacks cannot target moving artwork incorrectly.

Spacious is 480×252 with 126-pixel native quads and 108-pixel steps; Compact is 320×108 with 63-pixel quads and 66-pixel steps. Actual portraits occupy part of each square quad. Compact keeps three resting/four moving carriers to fit vanilla's invisible body spacing on small windows/Auto GUI scale. Motion off switches immediately and keeps a stable frame. UI text is English.

For actual server warps, replace the preview action in your consumer with your own destination lookup, permission/current-state checks and teleport handling. The library only renders and routes actions.

## Validation

`WarpTest` checks circular selection, timing locks, stale callbacks, both layouts, fixed hit coordinates, off-canvas carrier clips, Motion off and original art variants. The `warps` real-client scenario clicks actual coordinates, visits all four destinations, tests both motion directions/wrap/card/tabs, verifies no per-frame dialog replacement, exercises close/Escape during transit, and captures Compact/small-window/Auto frames. Pixel probes check moving native pictures, static Motion off frames and no pixels leaking outside the viewport. Reports/screenshots are ignored files under `build/reports/e2e/warps/`.
