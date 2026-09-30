package gg.kembel.dui.demo;

import java.util.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

final class ShopItems {
  static Map<String, ItemStack> stacks(Player player) {
    var stacks = new HashMap<String, ItemStack>();
    for (var product : ShopState.PRODUCTS)
      stacks.put(
          "product_" + product.id(),
          new ItemStack(Objects.requireNonNull(Material.matchMaterial(product.model()))));
    var head = new ItemStack(Material.PLAYER_HEAD);
    var meta = (SkullMeta) head.getItemMeta();
    meta.setPlayerProfile(player.getPlayerProfile());
    head.setItemMeta(meta);
    stacks.put("shop_profile", head);
    return stacks;
  }
}
