package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.time.LocalDate;
import java.util.*;
import java.util.random.RandomGenerator;
import org.bukkit.inventory.ItemStack;

/** Application ports. Menus never own a plugin, player, file path or network executor. */
interface DemoServices {
  MenuTemplate template(String name);

  String viewerName();

  long tick();

  LocalDate date();

  RandomGenerator random();

  Map<String, ItemStack> viewerItems(
      java.util.function.Function<org.bukkit.entity.Player, Map<String, ItemStack>> factory);

  void message(String text);

  void save(String collection, Object state);

  SlotState copySlots(SlotState state);

  void settleAfter(long ticks);

  VideoProvider videos();

  TaskScope viewTasks();

  TaskScope tasks();

  void refresh();
}
