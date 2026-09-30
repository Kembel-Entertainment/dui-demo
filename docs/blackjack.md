# Monarch Blackjack

Play `/blackjack [compact|spacious]` or `/dui blackjack`. Original sapphire felt, brass rail, geometric crown, illustrated chip rack and a visible card shoe. All UI is English. Ordinary users need **Paper 26.2, an unmodified Minecraft client and the matching dui resource pack**.

Choose 5/25/100/500 credits, then **Deal**. Start with 5,000 transient demo credits. **Hit** draws; **Stand** ends your hand; **Double** doubles the stake and takes exactly one card; **Split** separates equal-rank pairs. A highlight and hand tabs identify each hand. After settlement, select tabs to inspect split results. Long hands have previous/next card controls: all cards remain in the rules engine, while the viewport respects dui's eight-effect budget. Leave/reopen to reset the demo; no economy, actual inventory rewards or saved player bankroll.

The six-deck variant pays natural blackjack **3:2**, stands the dealer on **all 17, including soft 17**, checks the dealer's opening blackjack before offering moves, allows double after splitting and up to four hands. Split aces receive one additional card each; aces cannot be resplit. A split 21 is an ordinary 21 and pays 1:1 when winning. No insurance or surrender. These are explicit demo rule choices; blackjack variants differ. Compare [official game rules, chapter 13:69F-2](https://www.nj.gov/oag/ge/docs/Regulations/CHAPTER69F.pdf).

Money is integer **half-credits**. A 25-credit natural reserves 25, returns 62.5 and profits 37.5. `Return` includes reserved stakes; `Net` subtracts all original/split/double reservations. Pushes return the corresponding stake. Settlement is atomic and once-only; a redraw or delayed completion cannot award twice.

**Demo deal** is explicitly scripted and cycles split eights, a natural, soft seventeen, a push and a hit/hold example, using the same rules engine. The button's tooltip names the next case. Ordinary **Deal** always uses a shuffled six-deck shoe; after a scripted preview it reshuffles. Production randomness is `SecureRandom`, independent of animation.

## Library boundary and animation

- `BlackjackGame`: pure rules, six-deck shoe, ace totals, legal actions, bankroll and settlement. Dealer hole and shoe are private. `visibleDealer()` returns an unknown `-1` until reveal; no hidden rank enters canvas bindings, client transport or layout diagnostics.
- `BlackjackState`: transient event sequencing. Opening deal lasts 54 ticks: player/upcard/player/hole leave the shoe in order with 0/8/16/24 tick delays, each flying for 28 ticks. Hit/Double and dealer draws use the same flight; split cards are staggered, reveal flips the hole, settlement transfers chips.
- `BlackjackView`: public maps, geometry, card windows, legality and result labels. At most three dealer cards + four human cards + one chip stack = eight shared effects, one carrier. No per-frame dialog replacement.
- `ui/blackjack.html`: styles, conditions/repeats, runtime background image, generic `dui-playing-card` and `dui-chip-stack` components, native text and callback buttons.
- `BlackjackArt`: original Java2D shapes/gradients, rasterized and supplied at runtime. This is vector illustration style rather than an SVG browser; the static table is not a pack asset. Cards are procedural GLSL rather than 52 baked PNGs.

The library adds **`animation="fly" card-height="54"`** to the existing card primitive. Allocate a rectangle from the destination slot to the shoe; the card travels from its top-right to bottom-left. Card height is independent of the flight rectangle. Mode 3 uses the existing card transport slot: no new effect kind, transport width, carrier, or per-menu shader. A matching rebuilt pack is required.

**Motion/Still** drains only automatic stages, stopping for the player's next decision; reduced motion does not choose Hit or Stand. Compact/Wide preserves the active event clock. Layouts are 320×153 / 480×324. GUI scale remains an explicit player preference because vanilla servers cannot read it. Completion callbacks check the session, section and event generation. Close, disconnect and menu switches cancel/discard the transient table.

## Validation

```sh
./gradlew -PduiSource=../dui test
./gradlew -PduiSource=../dui e2e -Pscenario=blackjack \
  -PminecraftJar=/path/to/verified/26.2-client.jar
```

Pure tests cover ace evaluation, natural versus split 21, soft-17 stand, dealer peek, push/bust, split aces, four-hand cap, double after split, atomic invalid moves, exact odd-stake returns, hidden card data, pagination and hundreds of shuffled ledger-consistent rounds. Library tests verify flight parameters/lifetime. The muted test-only Fabric client clicks actual UI coordinates and measures GPU flight/flip/chip motion, exact split/double and 3:2 returns, stable still mode, Compact/Auto, inventory unchanged and close cancellation. Reports/gallery: `build/reports/e2e/blackjack/`.

Startup preserves edited runtime templates. After editing source HTML, copy **only** `blackjack.html` to `run/server/plugins/dui-demo/ui/` before reload. Java/shader changes require a rebuilt plugin/pack and restart.
