# Four consumer-owned arcade demos

Paper 26.2, vanilla clients and dui's matching generated pack. These games are implemented entirely in **dui-demo**. The library source is unchanged. English UI, Compact/Wide, Motion/Still, an in-game Rules screen and transient demo credits are available in every game.

| Command | Demo | Behaviour |
| --- | --- | --- |
| `/horses` or `/dui horses` | Nightfall Derby | Four coloured horses gallop down an illustrated track. Pick a runner, place a wager and start the race. |
| `/wheel` or `/dui wheel` | Prism Wheel | Eight coloured prize slices, a stationary pointer, multiple revolutions and an eased stop. |
| `/coinflip` or `/dui coinflip` | Sun & Moon | Call Heads/Sun or Tails/Moon. The engraved coin rises, turns edge-on repeatedly and lands on its result. |
| `/bookofra` or `/dui bookofra` | Temple of Dawn | An original five-reel book adventure: three rows, ten lines, staggered stops, Wild/Scatter books and expanding-symbol free spins. |

Append `compact` or `spacious` to a command to choose the initial layout. Aliases: `/horserace`, `/pferderennen`, `/gluecksrad`, `/fortunewheel`, `/book`.

## Rules and round ownership

Each game starts with 5,000 credits; bets are 10, 50, 100 or 250. `ArcadeGame` reserves one bet on Play and credits its result once at completion. Repeated Play, reset, selection and bet changes are locked during the round. Resize retains the original animation start. Turning Motion off or closing the menu settles a pending round exactly once. Closing does not restart or refund it. Credits survive menu switches within the player's current server session; disconnect discards the transient demos. Reset restores the demo bankroll and clears bonus progress. Inventory is untouched.

`DemoServices.random()` supplies the server's `SecureRandom`. The result is chosen before the animation, which displays that result. Animation parameters are client-visible. This is a UI playground without a persistent ledger or economy integration.

- **Horses:** probabilities are 40/30/20/10% for Comet/Gold/Blue/Thunder. Correct bets return 2/3/4/8 times the wager respectively, including the original stake. Losing returns zero.
- **Wheel:** all eight slices are equally likely. In order their total-return multipliers are `0, 1, 2, 0, 3, 1, 5, 10`. This deliberately generous demo wheel has an average return of 2.75 times the wager.
- **Coin:** Heads and Tails each have 50% probability. A correct call returns twice the wager; a wrong call returns zero.
- **Temple:** the total bet is divided equally across ten explicitly defined lines. Matching begins at the left reel and requires at least three consecutive matching symbols; Book substitutes as Wild. A line chooses its highest applicable payout. Three, four or five-or-more Books anywhere pay 2/10/50 times the total bet and add ten free spins. Retriggers also add ten. Free spins keep the triggering wager and selected bonus symbol. If that symbol appears in at least three different reels it expands in those reels; the expansion pays its count multiplier on every line, including across gaps, **in addition to ordinary line wins**. This is an original demo ruleset, not a claim to reproduce a commercial game's maths.

Temple line multipliers, applied to one tenth of the total wager:

| Symbol | 3 | 4 | 5 |
| --- | ---: | ---: | ---: |
| A/K/Q/J/10 | 5 | 10 | 25 |
| Lotus | 10 | 40 | 100 |
| Scarab | 15 | 60 | 150 |
| Explorer | 20 | 100 | 300 |
| Book | 20 | 100 | 500 |

Symbols are independently drawn per cell with weights `18,18,16,16,14,8,7,4,3` in the table's symbol order. Lines and weights are explicit Java data in `TempleSlotsGame`. Original art and names are supplied by this project.

Temple's explicit **Bonus demo** button grants ten free spins with Scarab as the expanding symbol, so reviewers can explore the bonus without waiting for a random trigger. It keeps ordinary spins random and uses the same bonus rules and animation. It is intentionally a playground control.

## Public library integration

`ArcadeMenu` is a consumer `DemoMenu` using the already-existing `MenuController`, `ActionRouter` and view `TaskScope`. `project()` reads captured state and renders a template; timers and settlement belong to actions/`advance()`/`presented()`. There are no per-frame dialog replacements. The server sends a new view on user actions and settlement, while the client's shader animates between them.

The four `ui/*.html` files describe the game layouts. `casino/chrome.xml` is a shared, authored XML fragment registered with `ComponentRegistry.Builder.template`; it supplies the header, rules toggle, wager controls and bankroll/footer. `ArcadeView` supplies literal binding maps. The template registry composes this consumer package with `VisualComponents.registry()`.

Specific moving illustrations use supported pack extensions, **not library shader edits**:

| Component | Effect ID | Opcode | Payload A | Payload B |
| --- | --- | ---: | --- | --- |
| `dui-demo-race` | `demo:race` | 9 | winner bits 0–1; visual seed 2–9; selection 10–11 | duration 0–8; racing 9; prior result 10 |
| `dui-demo-prize-wheel` | `demo:prize-wheel` | 10 | target 0–2; previous 3–5 | duration 0–8; spinning 9 |
| `dui-demo-coin` | `demo:coin` | 11 | face bit 0 | duration 0–8; flipping 9 |
| `dui-demo-temple-reel` | `demo:temple-reel` | 12 | three four-bit symbols 0–11; reel index 12–14 | duration 0–8; spinning 9; expanded 10; winning rows 11–13 |

`CasinoComponents` registers these tags as bounded `ShaderEffect.Extension` placements. `DemoPackGenerator` reads the trusted `casino/*.glsl` resources and contributes them through `PackContribution`. The existing diagnostic pulse retains opcode 8. All moving art is procedural vector geometry; there are no prerendered animation frames or borrowed game images. Units use the shared renderer's world-time clock at 20 ticks/second. Static `duration=0` and root `motion=false` provide stable final poses. A game with custom visual code requires rebuilding the demo pack and restarting the server; layout-only HTML changes use `/dui reload`.

To add another screen, compose these components or create another consumer component/pack contribution. Extension opcodes must remain unique, between 8 and 15; 13–15 are currently free in this demo pack. This is an explicit transport limit. Each canvas still respects the default eight-effect budget; Temple uses five effects. Geometry, action bounds and resource costs are checked by the normal public APIs.

## Verification

```sh
./gradlew -PduiSource=../dui test jar buildPack \
  -PminecraftJar=/path/to/minecraft-26.2-client.jar
./gradlew -PduiSource=../dui -Pscenario=casino e2e \
  -PminecraftJar=/path/to/minecraft-26.2-client.jar
```

`ArcadeTest` covers advertised returns, reservation and once-only settlement, invalid selections, insufficient funds, left-to-right Wild combinations, the free-spin trigger/retrigger and expansion. The shared menu tests also cover all four new menus as pure projections with registered actions and bounded layouts.

The muted `CasinoClient` uses actual mouse clicks for every game, checks reservation/settlement, resizes during motion, plays an immediate Still round and checks unchanged inventory. `verify_casino.py` measures real GPU pixel motion, coloured artwork, stable Still frames and uncovered Rules screens. Reports/screenshots are under `build/reports/e2e/casino/`.
