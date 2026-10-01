package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Genuine mouse clicks through all four consumer games, both layouts and round settlement. */
final class CasinoClient {
  private static final String[] GAMES = {"horses", "wheel", "coinflip", "bookofra"};
  private int ticks, changed, game = -1, phase, steps, receivedAt;
  private long before, wager, clock, secondBefore;
  private String inventory;
  private boolean secondFree;
  private Screen pending, received, animated;
  private final Path out = Paths.output();

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private JsonObject data() throws Exception {
    return JsonParser.parseString(
            Files.readString(Paths.plugin().resolve("layouts/CasinoTest.json")))
        .getAsJsonObject();
  }

  private boolean ready(Minecraft mc, JsonObject d) throws Exception {
    if (!(mc.gui.screen() instanceof DialogScreen<?>) || mc.gui.screen() == pending) return false;
    pending = null;
    var canvas = RealClientHarness.canvas(mc);
    String hash =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(canvas.getMessage().getString().getBytes(StandardCharsets.UTF_8)));
    if (!hash.equals(d.get("componentTextHash").getAsString())
        || canvas.getHeight() != d.get("height").getAsInt() + 8) return false;
    if (received != mc.gui.screen()) {
      received = mc.gui.screen();
      receivedAt = ticks;
      return false;
    }
    return ticks - receivedAt >= 3;
  }

  private void step() {
    phase++;
    steps++;
    changed = ticks;
    System.out.println("CASINO_TEST_STAGE " + game + ":" + phase);
  }

  private void click(Minecraft mc, JsonObject d, String id) {
    pending = mc.gui.screen();
    RealClientHarness.hit(mc, d, id);
  }

  private void shot(Minecraft mc, JsonObject d, String name) throws Exception {
    RealClientHarness.snapshot(mc, out, GAMES[game] + "-" + name, d);
  }

  private static void check(boolean value, String message) {
    if (!value) throw new IllegalStateException(message);
  }

  private void next(Minecraft mc) {
    game++;
    phase = 0;
    changed = ticks;
    if (game < GAMES.length) {
      pending = mc.gui.screen();
      mc.player.connection.sendCommand("dui " + GAMES[game] + " spacious");
    }
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    try {
      if (ticks > 6000 || game >= 0 && ticks - changed > 600)
        throw new IllegalStateException("Casino timeout game=" + game + " phase=" + phase);
      if (mc.gui.overlay() != null || ticks - changed < 8) return;
      if (game == -1) {
        if (phase == 0 && ticks > 80) {
          mc.options.guiScale().set(Paths.referenceScale(mc));
          mc.getWindow().setWindowed(1280, 900);
          mc.resizeGui();
          org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
          var server = new ServerData("Casino fixture", Paths.server(), ServerData.Type.OTHER);
          server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
          ConnectScreen.startConnecting(
              new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
          step();
          return;
        }
        if (phase == 1 && mc.player != null && mc.gui.screen() instanceof DialogScreen<?>) {
          inventory = mc.player.getInventory().getNonEquipmentItems().toString();
          next(mc);
        }
        return;
      }
      if (game == GAMES.length) {
        if (phase == 0) {
          mc.gui.setScreen(null);
          step();
          return;
        }
        check(
            inventory.equals(mc.player.getInventory().getNonEquipmentItems().toString()),
            "Inventory changed");
        Files.writeString(
            out.resolve("client-result.json"),
            new Gson()
                .toJson(
                    Map.of(
                        "passed",
                        true,
                        "steps",
                        steps,
                        "inventoryUnchanged",
                        true,
                        "muted",
                        true,
                        "games",
                        4,
                    "settlements",
                    9)));
        System.out.println("CASINO_TEST_COMPLETE");
        mc.stop();
        game++;
        return;
      }
      var d = data();
      if (!d.get("section").getAsString().equals(GAMES[game]) || !ready(mc, d)) return;
      boolean busy = d.get("busy").getAsBoolean();
      switch (phase) {
        case 0 -> {
          if (ticks - changed < 30) return;
          shot(mc, d, "wide");
          if (game == 0 || game == 2) click(mc, d, "choose_1");
          step();
        }
        case 1 -> {
          if (game == 0 || game == 2)
            check(d.get("selected").getAsInt() == 1, "Choice not delivered");
          before = d.get("credits").getAsLong();
          wager = d.get("wager").getAsLong();
          click(mc, d, "play");
          step();
        }
        case 2 -> {
          check(busy, "Round not pending");
          check(d.get("credits").getAsLong() == before - wager, "Reservation mismatch");
          clock = d.get("startedAt").getAsLong();
          shot(mc, d, "early");
          animated = mc.gui.screen();
          step();
        }
        case 3 -> {
          if (ticks - changed < 35) return;
          check(mc.gui.screen() == animated, "Per-frame dialog replacement");
          shot(mc, d, "late");
          click(mc, d, "size");
          step();
        }
        case 4 -> {
          check(d.get("compact").getAsBoolean(), "Resize failed");
          check(d.get("startedAt").getAsLong() == clock, "Resize restarted round");
          shot(mc, d, "compact-spin");
          step();
        }
        case 5 -> {
          if (busy) return;
          check(d.get("rounds").getAsInt() == 1, "Duplicate or missing settlement");
          check(
              d.get("credits").getAsLong() == before - wager + d.get("returned").getAsLong(),
              "Payout mismatch");
          shot(mc, d, "result");
          click(mc, d, "motion");
          step();
        }
        case 6 -> {
          check(!d.get("motion").getAsBoolean(), "Motion toggle failed");
          shot(mc, d, "still");
          step();
        }
        case 7 -> {
          if (ticks - changed < 35) return;
          shot(mc, d, "still-later");
          secondBefore = d.get("credits").getAsLong();
          secondFree = d.has("freeSpins") && d.get("freeSpins").getAsInt() > 0;
          click(mc, d, "play");
          step();
        }
        case 8 -> {
          check(!busy && d.get("rounds").getAsInt() == 2, "Still round didn't settle immediately");
          check(
              d.get("credits").getAsLong()
                  == secondBefore - (secondFree ? 0 : wager) + d.get("returned").getAsLong(),
              "Still payout mismatch");
          shot(mc, d, "instant");
          click(mc, d, "help");
          step();
        }
        case 9 -> {
          check(d.get("help").getAsBoolean(), "Rules failed");
          shot(mc, d, "rules-compact");
          click(mc, d, "size");
          step();
        }
        case 10 -> {
          check(!d.get("compact").getAsBoolean(), "Wide rules resize failed");
          shot(mc, d, "rules-wide");
          click(mc, d, "help");
          step();
        }
        case 11 -> {
          check(!d.get("help").getAsBoolean(), "Rules didn't close");
          if (game == 3) {
            click(mc, d, "bonus_demo");
            step();
          } else next(mc);
        }
        case 12 -> {
          check(d.get("freeSpins").getAsInt() == 10, "Bonus preview didn't grant ten spins");
          before = d.get("credits").getAsLong();
          shot(mc, d, "bonus-ready");
          click(mc, d, "play");
          step();
        }
        case 13 -> {
          check(!busy && d.get("rounds").getAsInt() == 3, "Bonus round didn't settle");
          check(
              d.get("credits").getAsLong() == before + d.get("returned").getAsLong(),
              "Free spin charged a wager");
          check(d.get("freeSpins").getAsInt() >= 9, "Free spin not consumed");
          shot(mc, d, "bonus-result");
          next(mc);
        }
      }
    } catch (Exception e) {
      try {
        Files.createDirectories(out);
        Files.writeString(out.resolve("failure.txt"), e.toString());
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      System.out.println("CASINO_TEST_FAILED");
      mc.stop();
      game = 99;
    }
  }
}
