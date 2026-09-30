package gg.kembel.dui.demo;

import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.banner.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;

/** Independent catalogue examples, backed by ordinary vanilla models and the viewer's profile. */
final class ShowcaseItems {
  static Map<String, ItemStack> stacks(Player viewer, ShowcaseState state) {
    var grass = new ItemStack(Material.GRASS_BLOCK, state.stack);
    var head = new ItemStack(Material.PLAYER_HEAD);
    var skull = (SkullMeta) head.getItemMeta();
    skull.setPlayerProfile(viewer.getPlayerProfile());
    head.setItemMeta(skull);
    var tool = new ItemStack(Material.DIAMOND_PICKAXE);
    tool.addEnchantment(Enchantment.EFFICIENCY, 3);
    var wear = (Damageable) tool.getItemMeta();
    wear.setDamage(state.damage);
    tool.setItemMeta(wear);
    var banner = new ItemStack(Material.BLUE_BANNER);
    var pattern = (BannerMeta) banner.getItemMeta();
    pattern.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
    pattern.addPattern(new Pattern(DyeColor.ORANGE, PatternType.CIRCLE));
    banner.setItemMeta(pattern);
    describe(grass, "Grass block", "A native block model with a sample stack count.");
    describe(head, "Profile head", "A 3D head using the current viewer's skin.");
    describe(tool, "Sample tool", "Enchantment glint and adjustable durability.");
    describe(banner, "dui banner", "Two pattern layers on a vanilla banner model.");
    return Map.of("kit_grass", grass, "kit_head", head, "kit_tool", tool, "kit_banner", banner);
  }

  private static void describe(ItemStack stack, String name, String detail) {
    var meta = stack.getItemMeta();
    meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
    meta.lore(List.of(Component.text(detail).decoration(TextDecoration.ITALIC, false)));
    stack.setItemMeta(meta);
  }
}
