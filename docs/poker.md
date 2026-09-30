# Velvet Hold’em

An original four-seat no-limit Texas Hold’em playground: one human and Violet, Cedar and Nova. All UI copy is English. Paper 26.2 + an unmodified client accepting the generated dui pack. No money, economy plugin, inventory grant, shared table, calendar restriction, or persisted poker state.

## Play

`/dui poker spacious` or `/poker` opens the illustrated table. `/dui poker compact` fits the Auto-scale content budget. **Deal a hand** shuffles a full deck; the dealer rotates between funded seats. Blinds are 10/20, stacks start at 1,000 each. Fold, Check/Call, Raise and All-in are server validated. The plus/minus picker adjusts a total street bet, not an increment. The action buttons lock during card/chip animation and bot turns. **Motion/Still** settles the visible pose immediately. **Reset table** discards the current hand and refills 4×1,000 demo chips. Closing, disconnecting, or changing menu resets the transient table.

**Showcase hand** is explicitly scripted: a fresh table, human AH/KH and a royal-flush runout. Bots call/check in this mode. It is useful to inspect every animation without relying on a random rare hand. This does not influence ordinary shuffled hands.

The betting-reopening convention follows [Poker TDA rules 45 and 49 and their illustration addendum](https://www.pokertda.com/view-poker-tda-rules/). This demo implements mechanical no-limit action rules, not human-dealer procedures or tournament administration.

## Separation of concerns

- `HoldemGame`: pure Java authoritative deck, turn order, burns, streets, legal betting, refunds, side-pots, split pots and payout. Heads-up dealer posts the small blind and acts first before the flop; big blind acts first afterwards. Full raises reopen betting; short all-ins require additional calls but reopen prior actors only after a cumulative full raise. A lone funded player cannot raise into a dry side pot. Unmatched excess is refunded. Odd split chips go clockwise from the dealer. Award happens once, before presentation.
- `PokerHand`: exhaustively evaluates the best five of five to seven cards. Categories, kickers, flushes and ace-low straights compare lexicographically. No score approximations at showdown.
- `PokerBots`: modest stochastic hand-strength personalities using only their own two cards and the public board. They do not peek at other hands or the future deck. They are demo opponents, not solver-grade poker AI.
- `PokerState`: transient table and animation event/generation. One root world-time clock; finite card modes settle before later events. State mutations are separate from animation completion.
- `PokerView`: public template data/geometry. Opponent card IDs are absent from template data before showdown. Runtime anonymous backs reveal rank/suit only when allowed.
- `ui/poker.html`: layout, styling, bindings, repeated seats/cards and actions. Cards/chips are public library tags, not a poker-specific shader fork.
- `DuiDemoPlugin.poker`: ordinary dui consumer/controller. One scheduled continuation, guarded by active session, section, player, pack and generation. No per-frame replacement packets. Close/reset invalidate pending bot work, so old tasks cannot reopen or pay a table.

Seven procedural card effects (five community/two human) plus one chip effect use the eight-effect shared transport limit. Bot portraits and small hand previews are generated as runtime RGB rasters. The table illustration is also runtime RGB on a **background** image layer: native paints/text can appear above it. None of these rasters is a new per-menu resource-pack texture. The pack contains only the generic rendering primitives and shaders. [dui’s component guide](https://github.com/Kembel-Entertainment/dui/blob/master/docs/components.md) documents all attributes.

## Design and asset provenance

A petrol velvet table, aubergine rail, brass/gold edge, ivory cards, mint decisions and coral risk buttons. The original [table artwork](../src/main/resources/art/holdem-table.png) was created for this demo with the built-in **imagegen** tool (new-image mode); it contains no third-party logo or source UI. The reproducible art brief is recorded in [holdem-art.md](holdem-art.md). Runtime decoding/cropping is performed by `RasterImage`, with the same image sample limit as every dui consumer. Card faces, backs, suits, ranks, shadows and transfers are computed by reusable GLSL. Bot pixel portraits/mini-card previews are original demo Java drawing code.

## Validation

`./gradlew -PduiSource=../dui test` checks hand ranking, hidden bindings, legal action order, short/cumulative raises, heads-up blinds, side-pots/refunds/splits, many seeded bot games, chip conservation and both templates. `e2e -Pscenario=poker -PminecraftJar=<verified 26.2 jar>` runs a muted Minecraft client with genuine mouse events through all streets, a 240-chip scripted payout, card/flip/chip pixel-motion probes, Motion off, Compact/Auto and close cancellation. This fixture mod is test-only; ordinary users need no client mod.

Reports and screenshots: `build/reports/e2e/poker/`. Background screenshot probes exclude documented overlapping paints/effects/foreground images; they still require hundreds of visible matching RGB samples per frame. Foreground rasters remain verified normally.
