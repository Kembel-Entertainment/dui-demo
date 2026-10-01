package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.entity.EquipmentSlot;

/** Real mouse clicks and server inventory changes; the test client contains no UI renderer. */
final class CharacterClient {
  private int ticks, stage, changed;
  private String conserved, staleHead;
  private final Path out = Paths.output();

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private JsonObject data() throws Exception {
    return JsonParser.parseString(
            Files.readString(Paths.plugin().resolve("layouts/CharacterTest.json")))
        .getAsJsonObject();
  }

  private void step() {
    stage++;
    changed = ticks;
    System.out.println("CHARACTER_TEST_STAGE " + stage);
  }

  private static String inventory(Minecraft mc) {
    var all = new TreeMap<String, Integer>();
    for (int n = 0; n < mc.player.getInventory().getContainerSize(); n++) {
      var item = mc.player.getInventory().getItem(n);
      if (!item.isEmpty())
        all.merge(item.getItem() + "|" + item.getComponents(), item.getCount(), Integer::sum);
    }
    return all.toString();
  }

  private void check(Minecraft mc) {
    if (!Objects.equals(conserved, inventory(mc)))
      throw new IllegalStateException(
          "Inventory quantities/components changed: " + inventory(mc) + " vs " + conserved);
  }

  private void shot(Minecraft mc, JsonObject d, String name) throws Exception {
    RealClientHarness.snapshot(mc, out, name, d);
  }

  private static String candidate(JsonObject d, int index) {
    return d.getAsJsonArray("hits").asList().stream()
        .map(JsonElement::getAsJsonObject)
        .filter(h -> h.get("action").getAsString().equals("character_equip"))
        .map(h -> h.get("id").getAsString())
        .skip(index)
        .findFirst()
        .orElseThrow();
  }

  private static boolean wearing(Minecraft mc, EquipmentSlot slot) {
    return !mc.player.getItemBySlot(slot).isEmpty();
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    try {
      if (ticks > 3600) throw new IllegalStateException("Character test timeout stage=" + stage);
      if (mc.gui.overlay() != null || ticks - changed < 30) return;
      if (stage == 0 && ticks > 80) {
        mc.options.guiScale().set(Paths.referenceScale(mc));
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var server = new ServerData("Character fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        step();
        return;
      }
      if (stage == 1 && mc.player != null && mc.gui.screen() instanceof DialogScreen<?>) {
        mc.player.connection.sendCommand("clear @s");
        mc.player.connection.sendCommand("character spacious");
        step();
        return;
      }
      if (stage < 2 || !(mc.gui.screen() instanceof DialogScreen<?>)) return;
      var d = data();
      if (!d.get("section").getAsString().equals("character")) return;
      switch (stage) {
        case 2 -> {
          if (ticks - changed < 45) return;
          shot(mc, d, "bare-wide");
          mc.player.connection.sendCommand("character kit");
          step();
        }
        case 3 -> {
          if (ticks - changed < 30) return;
          conserved = inventory(mc);
          if (mc.player.getInventory().countItem(net.minecraft.world.item.Items.IRON_HELMET) != 1)
            throw new IllegalStateException("Explicit OP kit missing");
          RealClientHarness.hit(mc, d, "slot_CHEST");
          step();
        }
        case 4 -> {
          if (!d.get("popup").getAsString().equals("CHEST")) return;
          shot(mc, d, "chest-dropdown");
          RealClientHarness.hit(mc, d, candidate(d, 0));
          step();
        }
        case 5 -> {
          if (!wearing(mc, EquipmentSlot.CHEST) || !d.get("popup").getAsString().isEmpty()) return;
          check(mc);
          shot(mc, d, "dyed-leather");
          RealClientHarness.hit(mc, d, "slot_HEAD");
          step();
        }
        case 6 -> {
          if (!d.get("popup").getAsString().equals("HEAD")) return;
          shot(mc, d, "head-dropdown");
          RealClientHarness.hit(mc, d, "character_next");
          step();
        }
        case 7 -> {
          if (d.get("page").getAsInt() != 1) return;
          shot(mc, d, "head-page-two");
          RealClientHarness.hit(mc, d, candidate(d, 0));
          step();
        }
        case 8 -> {
          if (!wearing(mc, EquipmentSlot.HEAD) || !d.get("popup").getAsString().isEmpty()) return;
          check(mc);
          RealClientHarness.hit(mc, d, "slot_LEGS");
          step();
        }
        case 9 -> {
          if (!d.get("popup").getAsString().equals("LEGS")) return;
          RealClientHarness.hit(mc, d, candidate(d, 0));
          step();
        }
        case 10 -> {
          if (!wearing(mc, EquipmentSlot.LEGS) || !d.get("popup").getAsString().isEmpty()) return;
          check(mc);
          RealClientHarness.hit(mc, d, "slot_FEET");
          step();
        }
        case 11 -> {
          if (!d.get("popup").getAsString().equals("FEET")) return;
          RealClientHarness.hit(mc, d, candidate(d, 0));
          step();
        }
        case 12 -> {
          if (!wearing(mc, EquipmentSlot.FEET) || !d.get("popup").getAsString().isEmpty()) return;
          check(mc);
          shot(mc, d, "full-armor");
          RealClientHarness.hit(mc, d, "character_motion");
          step();
        }
        case 13 -> {
          if (d.get("motion").getAsBoolean()) return;
          shot(mc, d, "still");
          step();
        }
        case 14 -> {
          if (ticks - changed < 40) return;
          shot(mc, d, "still-later");
          RealClientHarness.hit(mc, d, "character_right");
          step();
        }
        case 15, 16, 17, 18, 19, 20, 21 -> {
          int expected = (stage - 13) % 8;
          if (d.get("facing").getAsInt() != expected) return;
          check(mc);
          shot(mc, d, "facing-" + expected);
          RealClientHarness.hit(mc, d, "character_right");
          step();
        }
        case 22 -> {
          if (d.get("facing").getAsInt() != 1) return;
          RealClientHarness.hit(mc, d, "slot_CHEST");
          step();
        }
        case 23 -> {
          if (!d.get("popup").getAsString().equals("CHEST")) return;
          RealClientHarness.hit(mc, d, "character_unequip");
          step();
        }
        case 24 -> {
          if (wearing(mc, EquipmentSlot.CHEST) || !d.get("popup").getAsString().isEmpty()) return;
          check(mc);
          shot(mc, d, "unequipped");
          RealClientHarness.hit(mc, d, "character_size");
          step();
        }
        case 25 -> {
          if (d.get("width").getAsInt() != 320) return;
          check(mc);
          shot(mc, d, "compact");
          RealClientHarness.hit(mc, d, "slot_HEAD");
          step();
        }
        case 26 -> {
          if (!d.get("popup").getAsString().equals("HEAD")) return;
          shot(mc, d, "compact-dropdown");
          var canvas = RealClientHarness.canvas(mc);
          RealClientHarness.clickAt(mc, canvas.getX() + 12, canvas.getY() + 36);
          step();
        }
        case 27 -> {
          if (!d.get("popup").getAsString().isEmpty()) return;
          mc.options.guiScale().set(0);
          mc.resizeGui();
          step();
        }
        case 28 -> {
          check(mc);
          shot(mc, d, "auto");
          mc.options.guiScale().set(Paths.referenceScale(mc));
          mc.resizeGui();
          mc.player.connection.sendCommand("gamemode survival");
          mc.player.connection.sendCommand("enchant @s binding_curse 1");
          mc.player.connection.sendCommand("character spacious");
          step();
        }
        case 29 -> {
          if (d.get("width").getAsInt() != 480) return;
          conserved = inventory(mc);
          RealClientHarness.hit(mc, d, "slot_HEAD");
          step();
        }
        case 30 -> {
          if (!d.get("popup").getAsString().equals("HEAD")) return;
          RealClientHarness.hit(mc, d, candidate(d, 0));
          RealClientHarness.hit(mc, d, candidate(d, 0));
          step();
        }
        case 31 -> {
          if (!d.get("popup").getAsString().isEmpty()) return;
          check(mc);
          RealClientHarness.hit(mc, d, "slot_HEAD");
          step();
        }
        case 32 -> {
          if (!d.get("popup").getAsString().equals("HEAD")) return;
          RealClientHarness.hit(mc, d, "character_unequip");
          step();
        }
        case 33 -> {
          if (!d.get("status").getAsString().contains("Binding")) return;
          check(mc);
          if (!wearing(mc, EquipmentSlot.HEAD))
            throw new IllegalStateException("Binding item was removed");
          shot(mc, d, "binding-blocked");
          mc.player.connection.sendCommand("gamemode creative");
          mc.player.connection.sendCommand("character spacious");
          for (int n = 0; n < 36; n++)
            if (mc.player.getInventory().getItem(n).isEmpty())
              mc.player.connection.sendCommand(
                  "item replace entity @s "
                      + (n < 9 ? "hotbar." + n : "inventory." + (n - 9))
                      + " with minecraft:stone 64");
          step();
        }
        case 34 -> {
          if (ticks - changed < 45) return;
          for (int n = 0; n < 36; n++)
            if (mc.player.getInventory().getItem(n).isEmpty())
              throw new IllegalStateException("Full inventory fixture missing slot " + n);
          conserved = inventory(mc);
          RealClientHarness.hit(mc, d, "slot_HEAD");
          step();
        }
        case 35 -> {
          if (!d.get("popup").getAsString().equals("HEAD")) return;
          RealClientHarness.hit(mc, d, "character_unequip");
          step();
        }
        case 36 -> {
          if (!d.get("status").getAsString().contains("Free an inventory slot")) return;
          check(mc);
          shot(mc, d, "full-inventory-blocked");
          RealClientHarness.hit(mc, d, candidate(d, 1));
          step();
        }
        case 37 -> {
          if (!d.get("popup").getAsString().isEmpty()) return;
          check(mc);
          shot(mc, d, "full-inventory-swap");
          RealClientHarness.hit(mc, d, "slot_HEAD");
          step();
        }
        case 38 -> {
          if (!d.get("popup").getAsString().equals("HEAD")) return;
          staleHead =
              mc.player.getItemBySlot(EquipmentSlot.HEAD).toString()
                  + mc.player.getItemBySlot(EquipmentSlot.HEAD).getComponents();
          mc.player.connection.sendCommand("item replace entity @s hotbar.0 with minecraft:dirt");
          RealClientHarness.hit(mc, d, candidate(d, 0));
          step();
        }
        case 39 -> {
          if (mc.player.getInventory().getItem(0).getItem() != net.minecraft.world.item.Items.DIRT)
            return;
          String actual =
              mc.player.getItemBySlot(EquipmentSlot.HEAD).toString()
                  + mc.player.getItemBySlot(EquipmentSlot.HEAD).getComponents();
          if (!actual.equals(staleHead))
            throw new IllegalStateException("Stale inventory selection equipped another item");
          conserved = inventory(mc);
          shot(mc, d, "stale-inventory-rejected");
          step();
        }
        case 40 -> {
          check(mc);
          var result = new JsonObject();
          result.addProperty("passed", true);
          result.addProperty("inventoryConserved", true);
          result.addProperty("muted", true);
          result.addProperty("steps", 41);
          result.addProperty("bindingChecked", true);
          result.addProperty("fullInventoryChecked", true);
          result.addProperty("duplicateClickChecked", true);
          result.addProperty("staleInventoryChecked", true);
          Files.writeString(out.resolve("client-result.json"), result.toString());
          System.out.println("CHARACTER_TEST_COMPLETE");
          mc.stop();
          stage = 41;
        }
      }
    } catch (Exception error) {
      try {
        Files.writeString(out.resolve("failure.txt"), error.toString());
      } catch (Exception ignored) {
      }
      error.printStackTrace();
      System.out.println("CHARACTER_TEST_FAILED");
      mc.stop();
      stage = 99;
    }
  }
}
