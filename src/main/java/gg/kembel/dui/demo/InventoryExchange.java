package gg.kembel.dui.demo;

import java.util.*;

/** Pure all-or-nothing swap planner. Null means empty. No drops and no partial mutation. */
final class InventoryExchange {
  record Plan<T>(List<T> storage, T equipped) {}

  static <T> Plan<T> equip(List<T> storage, T equipped, int source) {
    if (source < 0 || source >= storage.size() || storage.get(source) == null)
      throw new IllegalArgumentException("That inventory item is no longer available.");
    var copy = new ArrayList<>(storage);
    T next = copy.get(source);
    copy.set(source, equipped);
    return new Plan<>(Collections.unmodifiableList(copy), next);
  }

  static <T> Plan<T> unequip(List<T> storage, T equipped) {
    return unequip(storage, equipped, -1);
  }

  static <T> Plan<T> unequip(List<T> storage, T equipped, int excluded) {
    if (equipped == null)
      return new Plan<>(Collections.unmodifiableList(new ArrayList<>(storage)), null);
    var copy = new ArrayList<>(storage);
    int empty = -1;
    for (int n = 0; n < copy.size(); n++)
      if (n != excluded && copy.get(n) == null) {
        empty = n;
        break;
      }
    if (empty < 0) throw new IllegalArgumentException("Free an inventory slot before unequipping.");
    copy.set(empty, equipped);
    return new Plan<>(Collections.unmodifiableList(copy), null);
  }
}
