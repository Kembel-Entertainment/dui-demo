# Public library migration

The demos are consumers of dui-core, dui-paper and optional dui-components, with dui-test used only by tests. Their rules, accounts, asset generators, HTTP/cache files and own extension function remain in this repository.

DemoMenus registers one factory per menu. Each concrete menu declares its aliases, resources, preparation, pure projection, typed actions, cleanup and validator. DuiDemoPlugin derives commands/reload/dispatch from that catalogue and binds each menu to a real MenuDefinition and MenuController. No demo uses MenuController.present or a compatibility presentation wrapper. Each session uses controller refresh and view/session task scopes. Slot settlement retains an application-owned lifetime; game event generations still guard authoritative state transitions.

| Demo | Public abstractions exercised |
| --- | --- |
| Showcase | Registry, optional visuals, checkbox/dropdown, scene coverage, native items/heads |
| Shop | Shared cart fragment, CollectionView/Page, dynamic QR, bounded cache |
| Rewards | Controller scopes, shared native pop, particles |
| Advent | Spanning GridLayout, LayoutProfile, AnimationTimeline, generic native motion |
| Warps | Carousel, profile, native clip and shared slide motion |
| Slots | Optional reel/lever/lights/particles; durable settlement separate from UI jobs |
| Videos | CollectionView, template wrapping, CachedResourceProvider, scoped async completion |
| Poker | Optional cards/chips, reusable motion/presets, consumer rule engine |
| Blackjack | CardStrip, RenderBudget, optional cards/chips, scoped phase expiry |
| Roulette | Optional wheel and chip visuals; rule/ledger validation remains consumer-owned |
| Field Journal | Public controller, typed actions, 12-entry collection, chrome, token theme, RGBA provider |
| Protocol Lab | 14-effect batches, own demo:pulse contribution, generic native/procedural motion, popup occlusion |

`/dui acceptance compact` and `/dui acceptance spacious` open the unrelated Journal. Change the entries or profile capacity without library edits. `/dui protocol` opens the renderer acceptance lab, including Animate/Motion off and a dropdown over a runtime image, portrait and native item.

All integration fixtures share RealClientHarness for traversal, canvas lookup, mouse coordinates and screenshots. Protocol tests sample actual custom-shader pixels, generic movement, still-mode stability and popup visibility. Reports are generated under build/reports/e2e and not committed. Pure tests use MenuHarness/RenderAssertions/FakeScheduler for portable action/layout/lifetime assertions.

The current backend supports fixed phases and whole-object popup coverage, not CSS z-index or arbitrary masks. Generic motion has bounded tracks; detailed card/wheel/reel presets remain available. Original low-level aliases are compatibility APIs; templates install VisualComponents.registry() and prefer namespaced dui-visual-* components. See dui's application-api and compatibility guides for exact contracts.


## Implement another demo

Add a concrete `DemoMenu` beside `ShopMenu`, `AdventMenu`, or `AcceptanceMenu`, then add its factory to `DemoMenus.factories()`. Define the Paper command in plugin.yml if it needs an independent command; `/dui <id>` is derived automatically. No library changes or new plugin presentation branch are needed.

The lifecycle is explicit:

1. `prepare(state, compact)` initialises entry/navigation state. Constructors register actions and do no I/O.
2. `capture(state)` records the clock, UTC date and viewer name, calls `advance(state)`, and captures native item resources. `advance` owns overdue phase transitions, feed/cache snapshots and date rollover. `captureItems` uses the injected viewer port; it never changes player inventory.
3. `project(state)` returns a `MenuView`. It reads the captured state/resources, builds template data, and composes options. It must not change application state, read the live player/clock, save files, fetch media or schedule tasks.
4. An action is decoded by `ActionRouter`, applies application rules and captures the next frame. The real `MenuController` refreshes once. Input actions validate `DialogResponseView` independently; invalid payloads are rejected without saving.
5. `onPresented` exports diagnostics, then invokes `presented(state, canvas)` for scoped timers and async work. `DemoMenu.later` uses `viewTasks` and refreshes through the host after expiry. Video fetch completions use `tasks().latest` so an obsolete request cannot refresh a closed session.
6. `closed(state)` performs application cleanup after a server-known close. UI scopes expire through the library. Slots keep their durable reservation/settlement outside these scopes; their effect deadline uses the actual compiled canvas lifetime, including payout particles.

The showcase owns four explicit screens (showcase, setup, form, confirmation) within the same projected menu. Ordinary actions change that screen and the controller refreshes. Native Cancel closes before dispatch; only an explicit return action asks the host to open the showcase again. Normal Close actions remain closed. Vanilla does not report every Escape or screen replacement to Paper; controller activity represents server-known state, so consumers should avoid unnecessary late refreshes.

`DemoServices` contains application ports for templates, captured viewer assets, persistence, random input, video service and scoped jobs. Template lookup must return an already compiled template; compilation/file reads happen at startup or explicit reload. Its real adapter lives in the plugin; portable tests inject a fake clock, scheduler, provider and persistence. Game engines remain separate (`HoldemGame`, `BlackjackGame`, `RouletteGame`, etc.); the menu classes contain their UI orchestration rather than moving business rules into dui.

## Consumer architecture checks

`DemoMenusTest` exercises all twelve real menu factories in both profiles. It asserts repeated projections preserve application state (including transient animation fields), rendering performs no external I/O or lifecycle effects, every visible callback is registered, malformed actions cannot write state, animation jobs expire on close, late video results cannot reopen a menu, and a failed slot reservation preserves the prior ledger. Showcase pages and dropdowns are traversed through their actual routes. Native inputs, native resources and shader pixels still require the real-client suite.
