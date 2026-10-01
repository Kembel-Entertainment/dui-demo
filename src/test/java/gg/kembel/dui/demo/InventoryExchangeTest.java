package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class InventoryExchangeTest {
  record Gear(String type, int amount, String components) {}

  @Test
  void swapsFullInventoryWithoutLosingItemComponents() {
    var incoming = new Gear("helmet", 1, "enchants+pdc+trim");
    var old = new Gear("helmet", 1, "damaged+named");
    var storage = List.of(incoming, new Gear("sword", 1, "custom"));
    var plan = InventoryExchange.equip(storage, old, 0);
    assertEquals(incoming, plan.equipped());
    assertEquals(old, plan.storage().getFirst());
    assertEquals(incoming, storage.getFirst());
  }

  @Test
  void fullInventoryRejectsUnequipWithoutChangingAnything() {
    var values = List.of("one", "two");
    assertThrows(IllegalArgumentException.class, () -> InventoryExchange.unequip(values, "armor"));
    assertEquals(List.of("one", "two"), values);
  }

  @Test
  void mainHandUnequipCannotReturnToTheSameSlot() {
    var plan = InventoryExchange.unequip(Arrays.asList(null, "stone", null), "sword", 0);
    assertNull(plan.storage().get(0));
    assertEquals("sword", plan.storage().get(2));
    assertThrows(
        IllegalArgumentException.class,
        () -> InventoryExchange.unequip(Arrays.asList(null, "stone"), "sword", 0));
  }

  @Test
  void rejectsEmptyOrInvalidSource() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InventoryExchange.equip(Arrays.asList(null, "one"), "old", 0));
    assertThrows(
        IllegalArgumentException.class, () -> InventoryExchange.equip(List.of("one"), null, 1));
  }

  @Test
  void repeatedRandomSwapsConserveEveryItem() {
    var random = new Random(71);
    var storage = new ArrayList<Integer>();
    for (int n = 0; n < 36; n++) storage.add(n);
    Integer equipped = 36;
    for (int n = 0; n < 1000; n++) {
      var plan = InventoryExchange.equip(storage, equipped, random.nextInt(36));
      storage = new ArrayList<>(plan.storage());
      equipped = plan.equipped();
      var all = new HashSet<>(storage);
      all.add(equipped);
      assertEquals(37, all.size());
      assertTrue(all.containsAll(java.util.stream.IntStream.range(0, 37).boxed().toList()));
    }
  }
}
