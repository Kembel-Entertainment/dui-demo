package gg.kembel.dui.demo;

import java.time.LocalDate;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

final class RewardItems {
  static Map<String, ItemStack> stacks(RewardState state, LocalDate date) {
    var mascot = new ItemStack(Material.PAPER);
    var meta = mascot.getItemMeta();
    meta.setItemModel(
        new NamespacedKey(
            "dui_demo",
            "reward/" + (state.celebrating ? "party" : "idle") + (state.motion ? "" : "_still")));
    mascot.setItemMeta(meta);
    return Map.of(
        "reward_mascot",
        mascot,
        "reward_preview",
        new ItemStack(
            Material.valueOf(
                RewardState.WEEK
                    .get(state.selection(date))
                    .model()
                    .toUpperCase(java.util.Locale.ROOT))));
  }
}
