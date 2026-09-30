package gg.kembel.dui.demo;

import gg.kembel.dui.core.Canvas;
import java.util.*;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;

final class WarpItems {
  static Map<String, ItemStack> stacks(WarpState s, Canvas c) {
    var result = new HashMap<String, ItemStack>();
    for (var item : c.items) {
      int index = Integer.parseInt(item.id().substring(5));
      var stack = new ItemStack(Material.PAPER);
      var meta = stack.getItemMeta();
      meta.setItemModel(
          new NamespacedKey(
              "dui_demo",
              "warps/"
                  + WarpState.DESTINATIONS.get(index).id()
                  + (index == s.selected ? "_selected" : "")));
      stack.setItemMeta(meta);
      result.put(item.id(), stack);
    }
    return result;
  }
}
