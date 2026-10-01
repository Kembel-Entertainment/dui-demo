package gg.kembel.dui.demo;

import gg.kembel.dui.paper.*;
import java.security.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;

/**
 * Owner-thread inventory transactions. Fingerprints include quantities and every item component.
 */
final class PaperEquipment implements EquipmentPort {
  static final NamespacedKey RARITY = new NamespacedKey("dui-demo", "rarity");
  static final int[] COLORS = {0xCACAD1, 0x68C685, 0x60AAF0, 0xB88CF9, 0xF3BC64};
  static final String[] RARITIES = {"Common", "Uncommon", "Rare", "Epic", "Legendary"};
  private final Player player;

  PaperEquipment(Player player) {
    this.player = player;
  }

  private void main() {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Equipment must run on the server thread");
  }

  private static ItemStack clean(ItemStack item) {
    return item == null || item.getType().isAir() ? null : item.clone();
  }

  private ItemStack equipped(Slot slot) {
    var i = player.getInventory();
    return clean(
        switch (slot) {
          case HEAD -> i.getHelmet();
          case CHEST -> i.getChestplate();
          case LEGS -> i.getLeggings();
          case FEET -> i.getBoots();
          case HAND -> i.getItemInMainHand();
          case OFFHAND -> i.getItemInOffHand();
        });
  }

  private void set(Slot slot, ItemStack item) {
    var i = player.getInventory();
    switch (slot) {
      case HEAD -> i.setHelmet(item);
      case CHEST -> i.setChestplate(item);
      case LEGS -> i.setLeggings(item);
      case FEET -> i.setBoots(item);
      case HAND -> i.setItemInMainHand(item);
      case OFFHAND -> i.setItemInOffHand(item);
    }
  }

  private List<ItemStack> storage() {
    return Arrays.stream(player.getInventory().getStorageContents())
        .map(PaperEquipment::clean)
        .toList();
  }

  private static String fingerprint(PlayerInventory i) {
    try {
      var hash = MessageDigest.getInstance("SHA-256");
      hash.update((byte) i.getHeldItemSlot());
      for (var item : i.getContents()) {
        if (item == null || item.getType().isAir()) hash.update((byte) 0);
        else {
          hash.update((byte) 1);
          byte[] bytes = item.serializeAsBytes();
          hash.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
          hash.update(bytes);
        }
      }
      return HexFormat.of().formatHex(hash.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  private static Entry entry(int index, ItemStack item) {
    var meta = item.getItemMeta();
    String name =
        meta.hasDisplayName()
            ? PlainTextComponentSerializer.plainText().serialize(meta.displayName())
            : item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    int rarity =
        meta.getPersistentDataContainer().getOrDefault(RARITY, PersistentDataType.INTEGER, -1);
    if (rarity < 0 || rarity > 4)
      rarity =
          switch (vanillaRarity(item)) {
            case "UNCOMMON" -> 1;
            case "RARE" -> 2;
            case "EPIC" -> 3;
            default -> 0;
          };
    int durability = 100;
    if (meta instanceof Damageable damage && item.getType().getMaxDurability() > 0)
      durability =
          Math.max(
              0,
              (item.getType().getMaxDurability() - damage.getDamage())
                  * 100
                  / item.getType().getMaxDurability());
    return new Entry(index, name, RARITIES[rarity], COLORS[rarity], durability, item);
  }

  private static String vanillaRarity(ItemStack item) {
    var rarity =
        item.getDataOrDefault(
            io.papermc.paper.datacomponent.DataComponentTypes.RARITY,
            org.bukkit.inventory.ItemRarity.COMMON);
    if (item.getEnchantments().isEmpty()) return rarity.name();
    return switch (rarity) {
      case COMMON, UNCOMMON -> "RARE";
      case RARE -> "EPIC";
      case EPIC -> "EPIC";
    };
  }

  private String stat(Attribute a) {
    var attribute = player.getAttribute(a);
    return String.format(Locale.ROOT, "%.2f", attribute == null ? 0 : attribute.getValue());
  }

  public Snapshot capture() {
    main();
    var storage = new ArrayList<Entry>();
    var i = player.getInventory();
    for (int n = 0; n < 36; n++) {
      var item = clean(i.getItem(n));
      if (item != null) storage.add(entry(n, item));
    }
    var equipped = new EnumMap<Slot, Entry>(Slot.class);
    for (var slot : Slot.values()) {
      var item = equipped(slot);
      if (item != null) equipped.put(slot, entry(-1, item));
    }
    return new Snapshot(
        fingerprint(i),
        i.getHeldItemSlot(),
        storage,
        equipped,
        Map.of(
            "Health",
            String.format(
                Locale.ROOT,
                "%.0f / %.0f",
                player.getHealth(),
                player.getAttribute(Attribute.MAX_HEALTH).getValue()),
            "Armor",
            stat(Attribute.ARMOR),
            "Toughness",
            stat(Attribute.ARMOR_TOUGHNESS),
            "Attack",
            stat(Attribute.ATTACK_DAMAGE),
            "Attack speed",
            stat(Attribute.ATTACK_SPEED),
            "Speed",
            stat(Attribute.MOVEMENT_SPEED)),
        PlayerAppearance.capture(player));
  }

  private void check(Snapshot expected, Slot target) {
    main();
    if (!fingerprint(player.getInventory()).equals(expected.fingerprint()))
      throw new IllegalArgumentException("Your inventory changed. Choose the item again.");
    var current = equipped(target);
    if (current != null
        && current.containsEnchantment(Enchantment.BINDING_CURSE)
        && player.getGameMode() != GameMode.CREATIVE
        && target.ordinal() < 4)
      throw new IllegalArgumentException("Curse of Binding prevents changing this armor.");
  }

  public void equip(Snapshot expected, Slot target, int source) {
    check(expected, target);
    var values = storage();
    if (source < 0 || source >= 36 || values.get(source) == null)
      throw new IllegalArgumentException("That inventory item is unavailable.");
    var incoming = values.get(source);
    if (!target.accepts(incoming))
      throw new IllegalArgumentException("This item does not fit that slot.");
    if (source == expected.held() && target == Slot.HAND) return;
    if (target.ordinal() < 4 && incoming.getAmount() > 1) {
      var remainder = incoming.clone();
      remainder.setAmount(incoming.getAmount() - 1);
      var one = incoming.clone();
      one.setAmount(1);
      var split = new ArrayList<>(values);
      split.set(source, remainder);
      var returned = InventoryExchange.unequip(split, equipped(target));
      player.getInventory().setStorageContents(returned.storage().toArray(ItemStack[]::new));
      set(target, one);
      return;
    }
    var plan = InventoryExchange.equip(values, equipped(target), source);
    player.getInventory().setStorageContents(plan.storage().toArray(ItemStack[]::new));
    set(target, plan.equipped());
  }

  public void unequip(Snapshot expected, Slot target) {
    check(expected, target);
    if (target == Slot.HAND) {
      var values = new ArrayList<>(storage());
      values.set(expected.held(), null);
      var plan = InventoryExchange.unequip(values, equipped(target), expected.held());
      player.getInventory().setStorageContents(plan.storage().toArray(ItemStack[]::new));
      return;
    }
    var plan = InventoryExchange.unequip(storage(), equipped(target));
    player.getInventory().setStorageContents(plan.storage().toArray(ItemStack[]::new));
    set(target, null);
  }

  static void kit(Player player) {
    if (!player.hasPermission("dui-demo.character-kit")) {
      player.sendMessage("You need permission to create the demo kit.");
      return;
    }
    var items = new ArrayList<ItemStack>();
    Material[] types = {
      Material.LEATHER_HELMET,
      Material.IRON_HELMET,
      Material.DIAMOND_HELMET,
      Material.NETHERITE_HELMET,
      Material.TURTLE_HELMET,
      Material.LEATHER_CHESTPLATE,
      Material.IRON_CHESTPLATE,
      Material.DIAMOND_CHESTPLATE,
      Material.NETHERITE_CHESTPLATE,
      Material.IRON_LEGGINGS,
      Material.DIAMOND_BOOTS,
      Material.IRON_SWORD,
      Material.DIAMOND_SWORD,
      Material.SHIELD
    };
    for (int n = 0; n < types.length; n++) {
      var item = new ItemStack(types[n]);
      var meta = item.getItemMeta();
      int rarity = n % 5;
      meta.displayName(
          Component.text(
              new String[] {"Ash", "Moss", "Azure", "Violet", "Dawn"}[rarity]
                  + " / "
                  + types[n].name().toLowerCase(Locale.ROOT).replace('_', ' '),
              net.kyori.adventure.text.format.TextColor.color(COLORS[rarity])));
      meta.getPersistentDataContainer().set(RARITY, PersistentDataType.INTEGER, rarity);
      if (meta instanceof LeatherArmorMeta leather) leather.setColor(Color.fromRGB(0x7865C5));
      meta.lore(List.of(Component.text("Aster / equipment demo")));
      item.setItemMeta(meta);
      items.add(item);
    }
    long empty =
        Arrays.stream(player.getInventory().getStorageContents())
            .filter(i -> i == null || i.getType().isAir())
            .count();
    if (empty < items.size()) {
      player.sendMessage("Free " + items.size() + " inventory slots for the demo kit.");
      return;
    }
    for (var item : items) player.getInventory().addItem(item);
    player.sendMessage("Aster demo kit added. Open /character to equip it.");
  }
}
