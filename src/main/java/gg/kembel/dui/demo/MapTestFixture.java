package gg.kembel.dui.demo;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Explicit loopback E2E fixture, never enabled by ordinary map use or production config. */
final class MapTestFixture implements Listener, AutoCloseable {
  private final Map<UUID, ItemStack[]> originals = new HashMap<>();
  private final JavaPlugin plugin;
  private final Map<UUID, GameMode> modes = new HashMap<>();

  MapTestFixture(JavaPlugin plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void join(PlayerJoinEvent event) {
    if (!event.getPlayer().getName().equals("MapTest")) return;
    var p = event.getPlayer();
    originals.put(
        p.getUniqueId(),
        Arrays.stream(p.getInventory().getContents())
            .map(i -> i == null ? null : i.clone())
            .toArray(ItemStack[]::new));
    modes.put(p.getUniqueId(), p.getGameMode());
    p.setGameMode(GameMode.SURVIVAL);
    p.getInventory().setItem(0, new ItemStack(Material.COMPASS));
    p.getInventory().setItem(8, new ItemStack(Material.DIAMOND_SWORD));
    p.getInventory().setItemInOffHand(new ItemStack(Material.CARROT));
    p.getInventory().setHeldItemSlot(0);
    p.updateInventory();
  }

  boolean command(Player player, String mode) {
    if (!originals.containsKey(player.getUniqueId())) return false;
    switch (mode) {
      case "fixtureteleport" -> player.teleport(player.getLocation().add(3, 2, 0));
      case "fixturedeath" -> player.setHealth(0);
      case "fixturedisable" -> plugin.getServer().getPluginManager().disablePlugin(plugin);
      case "fixturesync" -> player.updateInventory();
      default -> {
        return false;
      }
    }
    return true;
  }

  @EventHandler
  public void death(org.bukkit.event.entity.PlayerDeathEvent event) {
    if (originals.containsKey(event.getEntity().getUniqueId())) {
      event.setKeepInventory(true);
      event.getDrops().clear();
    }
  }

  @EventHandler
  public void quit(PlayerQuitEvent event) {
    restore(event.getPlayer());
  }

  private void restore(Player player) {
    var mode = modes.remove(player.getUniqueId());
    if (mode != null) player.setGameMode(mode);
    var items = originals.remove(player.getUniqueId());
    if (items != null) {
      player.getInventory().setContents(items);
      player.updateInventory();
    }
  }

  @Override
  public void close() {
    for (var player : plugin.getServer().getOnlinePlayers()) restore(player);
    HandlerList.unregisterAll(this);
  }
}
