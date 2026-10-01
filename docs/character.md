# Aster character sheet

Open `/character` or `/dui character`, optionally `compact` or `spacious`. This original metal-and-gilt character sheet uses ordinary HTML DUI components and the library's `dui-player-model`. The reference screenshots inspired its structure; their art and branding are not included.

The four armor slots and main/off-hand slots represent the **real player inventory**. A slot opens a rich picker with native item icons, rarity borders and tooltips. Wide shows four candidates per page; Compact shows two. Choose an item to equip immediately, use Unequip to return it, or click outside to dismiss. The figure shows captured skin, outer layers and supported vanilla armor, including dyed leather. Turn buttons cycle eight directions; Motion/Still controls GPU idle. Hand item icons stay native; the figure does not display their geometry in v1.

`/character kit` is a separate explicit action requiring `dui-demo.character-kit` (OP by default). It creates 14 named sample items with five PDC rarity levels and dyed leather. It first checks capacity. Opening the sheet generates no items and changes no game mode. Using the kit does not auto-equip it. Normal item metadata/vanilla rarity supplies the fallback border.

## Responsibilities

- `CharacterMenu`: typed actions, appearance resolution, owner-thread capture and ten-tick scoped polling. Only an inventory/attribute change triggers polling refresh. Close cancels UI work; equipment remains the actual persistent Minecraft inventory.
- `character.html`: all layout, button placement, model sizing, slot presentation, stats and dropdown content. `CharacterView` supplies pure bindings; `CharacterArt` generates original cached illustrated backgrounds.
- `EquipmentPort`: captured snapshots and application side effects; portable menu tests use an empty port.
- `PaperEquipment`: full inventory/component fingerprint, vanilla slot compatibility, Binding Curse checks and inventory mutation. Exchanges clone the stacks and preserve their PDC, lore, names, dye, wear, enchantments and counts. Stale inventory fingerprints reject the action. Duplicate DUI callback tokens are consumed once. Unequip preflights a free storage slot; main-hand unequip excludes the selected hotbar slot. Swaps work even with full storage, because the source slot receives the displaced item. Stacked armor is split only after return capacity is verified. No drops are used.
- `InventoryExchange`: pure swap/return planner, tested for full inventories, metadata retention, invalid sources, protected main-hand slots and 1,000 random conservation checks.

Attributes are live vanilla health, armor, toughness, attack damage, attack speed and movement speed. They are not cosmetic RPG stats invented by the demo. Slot selection is a demo controller; the library has no item grants, permissions, rarity or inventory mutation.

## Validation and limits

`./gradlew -PduiSource=../dui test` runs all 17 demo projections and their actions. `-Pscenario=character e2e` uses an isolated, muted Minecraft client with real clicks and inventory updates. It explicitly clears only its offline `CharacterTest` fixture, creates a permitted kit, equips dyed leather and all armor slots, tests picker pagination, eight angles, Still, Unequip, Compact and GUI Auto, checks inventory quantities/components are conserved, and verifies stale-inventory and duplicate-click protection, Curse of Binding in Survival and full-inventory rejection/swaps. Reports and current-run screenshots go to `build/reports/e2e/character`.

The shared renderer's complete API and v1 limitations are in [dui/docs/player-model.md](https://github.com/Kembel-Entertainment/dui/blob/master/docs/player-model.md). Production identity/profile handling remains the server's responsibility. The pinned target is vanilla Minecraft 26.2; other versions and renderer mods are not verified.
