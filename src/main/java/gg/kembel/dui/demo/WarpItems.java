package gg.kembel.dui.demo;

import gg.kembel.dui.core.Canvas;
import java.util.*;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;

final class WarpItems {
  static Map<String, ItemStack> stacks(WarpState s) {
    var result = new HashMap<String, ItemStack>();
    for (int i = 0; i < WarpState.DESTINATIONS.size(); i++) result.put(key(i), stack(s, i));
    return result;
  }

  static Map<String, ItemStack> stacks(WarpState s, Canvas c) {
    var result = new HashMap<String, ItemStack>();
    for (var item : c.items) {
      int index = Integer.parseInt(item.id().substring(5));
      result.put(item.id(), stack(s, index));
    }
    return result;
  }

  private static ItemStack stack(WarpState s, int index) {
    var stack = new ItemStack(Material.PAPER);
    var meta = stack.getItemMeta();
    meta.setItemModel(
        new NamespacedKey(
            "dui_demo",
            "warps/"
                + WarpState.DESTINATIONS.get(index).id()
                + (index == s.selected ? "_selected" : "")));
    stack.setItemMeta(meta);
    return stack;
  }

  static String key(int index) {
    return "card_" + index;
  }
}
