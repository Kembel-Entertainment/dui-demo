package gg.kembel.dui.demo;

import java.util.*;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;

final class AdventItems {
  static Map<String, ItemStack> stacks(AdventState s) {
    if (s.phase == AdventState.Phase.BOARD) return Map.of();
    var result = new HashMap<String, ItemStack>();
    for (String part : List.of("body", "lid")) {
      var stack = new ItemStack(Material.PAPER);
      var meta = stack.getItemMeta();
      meta.setItemModel(new NamespacedKey("dui_demo", "advent/" + part + "_" + (s.selected % 6)));
      stack.setItemMeta(meta);
      result.put("parcel_" + part, stack);
    }
    if (s.phase == AdventState.Phase.REVEALED)
      result.put("advent_reward", new ItemStack(Material.valueOf(s.gift().material())));
    return result;
  }
}
