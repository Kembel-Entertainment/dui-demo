package gg.kembel.dui.demo;

import gg.kembel.dui.paper.PlayerAppearance;
import java.util.*;
import org.bukkit.inventory.ItemStack;

/** Side effects are confined to this port; projections consume a captured inventory frame. */
interface EquipmentPort {
  enum Slot {
    HEAD("Head", "HELMET"),
    CHEST("Chest", "CHESTPLATE"),
    LEGS("Legs", "LEGGINGS"),
    FEET("Feet", "BOOTS"),
    HAND("Main hand", ""),
    OFFHAND("Off hand", "");
    final String label, suffix;

    Slot(String label, String suffix) {
      this.label = label;
      this.suffix = suffix;
    }

    boolean accepts(ItemStack item) {
      if (suffix.isEmpty()) return true;
      var equippable = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.EQUIPPABLE);
      return equippable != null
          && equippable.slot()
              == switch (this) {
                case HEAD -> org.bukkit.inventory.EquipmentSlot.HEAD;
                case CHEST -> org.bukkit.inventory.EquipmentSlot.CHEST;
                case LEGS -> org.bukkit.inventory.EquipmentSlot.LEGS;
                case FEET -> org.bukkit.inventory.EquipmentSlot.FEET;
                default -> throw new IllegalStateException();
              };
    }
  }

  record Entry(int index, String name, String rarity, int color, int durability, ItemStack item) {
    public Entry {
      item = item.clone();
    }

    public ItemStack item() {
      return item.clone();
    }
  }

  record Snapshot(
      String fingerprint,
      int held,
      List<Entry> storage,
      Map<Slot, Entry> equipped,
      Map<String, String> stats,
      PlayerAppearance appearance) {
    public Snapshot {
      storage = List.copyOf(storage);
      equipped = Map.copyOf(equipped);
      stats = Map.copyOf(stats);
    }

    static Snapshot empty() {
      return new Snapshot(
          "empty",
          0,
          List.of(),
          Map.of(),
          Map.of(
              "Health",
              "20 / 20",
              "Armor",
              "0",
              "Toughness",
              "0",
              "Attack",
              "1",
              "Speed",
              "0.10",
              "Attack speed",
              "4.00"),
          new PlayerAppearance(null, Arrays.asList(null, null, null, null)));
    }
  }

  Snapshot capture();

  void equip(Snapshot expected, Slot target, int source);

  void unequip(Snapshot expected, Slot target);

  EquipmentPort EMPTY =
      new EquipmentPort() {
        public Snapshot capture() {
          return Snapshot.empty();
        }

        public void equip(Snapshot e, Slot s, int i) {
          throw new IllegalArgumentException("No player inventory");
        }

        public void unequip(Snapshot e, Slot s) {
          throw new IllegalArgumentException("No player inventory");
        }
      };
}
