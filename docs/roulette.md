# Riviera Roulette

A playable, original European single-zero table for **Paper 26.2 + an unmodified client** accepting dui’s matching resource pack. One local player per table. English UI, transient demo credits; no economy integration or inventory grants.

## Play

`/roulette [compact|spacious]` or `/dui roulette [compact|spacious]` opens the table. Start with 5,000 credits. Choose a 5/25/100/500 chip, then click a number or outside field to add that amount. Hover shows the accumulated stake and odds. Table limit: 5,000 credits per round. **Undo** returns the last placement, **Clear** returns all reserved stakes, **Repeat** restores the previous round’s bet amounts when affordable. Spin requires a stake; all bet/chip/reset/undo/repeat actions lock until settlement. The last nine results appear in history (seven in Compact).

- Straight numbers 0–36: 35:1 profit, **36× stake returned**.
- Dozens 1–12 / 13–24 / 25–36 and columns: 2:1 profit, **3× returned**.
- Red/Black, Odd/Even, 1–18/19–36: 1:1 profit, **2× returned**.
- Zero is green and wins only its straight bet; it loses every outside bet. This preset does not offer La Partage, En Prison, double zero, splits, streets, corners or six-lines.

`Return` includes the already reserved winning stakes; `Net = return − total round stake`. For example, 25 credits on 17 reserves 25; winning returns 900, net +875. Multiple winning bets pay independently. The basic bet definitions/payout ratios are also described in [New Jersey’s official game rules, section 13:69F-5.1/5.2](https://www.nj.gov/oag/ge/docs/Regulations/CHAPTER69F.pdf); this demo specifically uses the full-loss single-zero convention stated above.

## Motion and design

The original **Riviera** design uses a warm ivory surround, serif wordmark, forest-green printed felt, walnut rotor, brass rings/dividers, inset pockets and an ivory ball. It is a code-native vector illustration style, not a photograph or an SVG/browser runtime. `RouletteArt` draws its original static shapes/gradients/chip images with Java2D; `RouletteView` supplies them as runtime RGB. They are not resource-pack assets, and there are no borrowed source UI images.

The reusable `dui-wheel` shared GLSL preset renders the complete wheel procedurally. Its 37 correctly ordered pockets and radial numbers rotate clockwise while the ball counter-rotates along a fixed outer track. The ball gradually drops, bounces and captures into the winning pocket; the final pocket and ball settle below the twelve-o’clock marker. Default spin: 160 world ticks (8 seconds). This is visual presentation of the server-selected result, not a physics simulation deciding outcomes. Chip toss and payout use the existing `dui-chip-stack` transfer component. No per-frame dialog replacement.

**Motion/Still** settles a pending spin exactly once and displays a static result; normal spins with motion off resolve immediately. **Compact/Wide** changes geometry while preserving an active event’s original clock. Wide is 480×324; Compact is 320×153 and fits the tested Auto-scale budget. The server cannot discover a client’s GUI scale; these are explicit preferences. The illustrated betting field uses `dui-hitbox` for precise horizontal bounds and full 9-pixel vertical click rows.

## Implementation boundary

- `RouletteGame`: pure Java ledger, validated bet IDs, reservation/refund, phase locks, unbiased `RandomGenerator.nextInt(37)`, history and once-only payout. The ordinary plugin supplies `SecureRandom`; the public UI has no scripted-win switch. `balance` is a long; table stakes/individual round returns are bounded by the 5,000-credit table limit.
- `RouletteView`: small public binding maps, both layouts, repeated cells/history/chips, wheel parameters and animation bounds. No gambling rules in the template.
- `RouletteArt`: original Java2D vector graphics. RGB corner colours match the particular felt or chip-selector cell. All active sampled image pixels fit dui’s 16,384-pixel budget, even with a chip on all 49 available fields.
- `ui/roulette.html`: styles/layout, `dui-wheel`, repeated surfaces/text/runtime images, independent hitboxes and existing chip-transfer effects. The ordinary canvas uses one carrier and at most two effects.
- `DuiDemoPlugin.roulette`: application controller. Cancels the previous completion task, renders a revision and schedules at most one continuation guarded by player/session/section/event generation. Close, disconnect and menu switch invalidate work and discard this transient table. Resizing does not draw a new result or change the payout. An update at spin completion starts chip payout; its completion returns to betting.

The outcome is selected once when Spin is accepted and is necessarily supplied to the visual shader. It is not a hidden/provably-fair real-money protocol. No persistent ledger is claimed: reopening/reset restores the demo bankroll.

## Validation and reproduction

```sh
./gradlew -PduiSource=../dui test
./gradlew -PduiSource=../dui -Pscenario=roulette \
  -PminecraftJar=/path/to/verified/minecraft-26.2-client.jar e2e
```

The five game tests cover all numbers, outside coverage/zero, reservation/refunds, invalid actions, limits, overlapping payouts, stale finish calls, Motion off, reset invalidation, all visible chips and both layouts. dui’s wheel/hitbox tests validate bounds, every encoded target and the unchanged shared transport.

The muted client fixture uses genuine mouse coordinates. It bets five credits on **every number**, so an unbiased live spin must return 180 from 185 reserved, leaving 4,995; no forced server outcome is needed. It checks locked gameplay, repeat/clear, a second still-mode round, Compact/Auto, inventory unchanged and close cancellation. Screenshot verification measures actual wheel/ball/payout pixel motion, a completely stable Motion-off view, and the settled ivory ball beneath the fixed marker. Reports and the gallery live under `build/reports/e2e/roulette/`. Test-only Fabric automation is not needed by ordinary clients.

After updating source templates, explicitly copy the edited `roulette.html` to the active `run/server/plugins/dui-demo/ui/` file before reload; startup deliberately preserves existing edited templates. A Java or shader change requires a rebuilt plugin/pack and server restart. Adding table variants through view data/HTML does not need a separate shader per menu.
