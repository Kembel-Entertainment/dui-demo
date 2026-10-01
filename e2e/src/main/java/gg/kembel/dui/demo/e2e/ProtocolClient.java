package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

final class ProtocolClient {
  private int ticks, stage, changed, fpsSamples;
  private long fpsTotal;
  private String inventory;
  private final Path out = Paths.output();

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private JsonObject data() throws Exception {
    return JsonParser.parseString(
            Files.readString(Paths.plugin().resolve("layouts/ProtocolTest.json")))
        .getAsJsonObject();
  }

  private static String inventory(Minecraft mc) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++)
      result.append(mc.player.getInventory().getItem(i)).append(';');
    return result.toString();
  }

  private void step() {
    stage++;
    changed = ticks;
    System.out.println("PROTOCOL_TEST_STAGE " + stage);
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    try {
      if (mc.player != null && mc.gui.screen() instanceof DialogScreen<?>) {
        fpsSamples++;
        fpsTotal += mc.getFps();
      }
      if (ticks > 2400)
        throw new IllegalStateException("Protocol scenario timed out stage=" + stage);
      if (mc.gui.overlay() != null || ticks - changed < 8) return;
      if (stage == 0 && ticks > 80) {
        mc.options.guiScale().set(Paths.referenceScale(mc));
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var server = new ServerData("Protocol fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        step();
        return;
      }
      if (stage == 1 && mc.player != null && mc.gui.screen() instanceof DialogScreen<?>) {
        inventory = inventory(mc);
        mc.player.connection.sendCommand("dui protocol");
        step();
        return;
      }
      if (stage < 2 || !(mc.gui.screen() instanceof DialogScreen<?>)) return;
      var d = data();
      if (stage < 9 && !d.get("section").getAsString().equals("protocol")) return;
      if (stage == 2) {
        if (ticks - changed < 40) return;
        if (d.getAsJsonArray("effects").size() != 14)
          throw new IllegalStateException(
              "Expected fourteen effects including a consumer-owned shader");
        RealClientHarness.snapshot(mc, out, "batched", d);
        RealClientHarness.hit(mc, d, "animate");
        step();
      } else if (stage == 3) {
        RealClientHarness.snapshot(mc, out, "motion-early", d);
        step();
      } else if (stage == 4) {
        if (ticks - changed < 30) return;
        RealClientHarness.snapshot(mc, out, "motion-final", d);
        RealClientHarness.hit(mc, d, "motion");
        step();
      } else if (stage == 5) {
        if (d.get("motion").getAsBoolean()) return;
        RealClientHarness.snapshot(mc, out, "still", d);
        step();
      } else if (stage == 6) {
        if (ticks - changed < 35) return;
        RealClientHarness.snapshot(mc, out, "still-later", d);
        RealClientHarness.hit(mc, d, "popup");
        step();
      } else if (stage == 7) {
        if (!d.get("popup").getAsBoolean()) return;
        RealClientHarness.snapshot(mc, out, "popup", d);
        RealClientHarness.hit(mc, d, "popup_option_1");
        step();
      } else if (stage == 8) {
        if (d.get("popup").getAsBoolean()) return;
        mc.player.connection.sendCommand("dui acceptance");
        step();
      } else if (stage == 9) {
        if (!d.get("section").getAsString().equals("acceptance")) return;
        RealClientHarness.snapshot(mc, out, "journal", d);
        RealClientHarness.hit(mc, d, "next");
        step();
      } else if (stage == 10) {
        if (d.get("page").getAsInt() != 1) return;
        RealClientHarness.snapshot(mc, out, "journal-page-two", d);
        mc.player.connection.sendCommand("dui acceptance spacious");
        step();
      } else if (stage == 11) {
        if (d.get("width").getAsInt() != 480) return;
        RealClientHarness.snapshot(mc, out, "journal-wide", d);
        if (!inventory.equals(inventory(mc)))
          throw new IllegalStateException("Protocol UI modified player inventory");
        var result = new JsonObject();
        result.addProperty("passed", true);
        result.addProperty("inventoryUnchanged", true);
        result.addProperty("muted", true);
        result.addProperty("steps", 12);
        result.addProperty("averageSampledFps", (double) fpsTotal / Math.max(1, fpsSamples));
        result.addProperty("fpsSamples", fpsSamples);
        Files.writeString(out.resolve("client-result.json"), result.toString());
        System.out.println("PROTOCOL_TEST_COMPLETE");
        mc.stop();
        stage = 12;
      }
    } catch (Exception e) {
      try {
        Files.createDirectories(out);
        Files.writeString(out.resolve("failure.txt"), e.toString());
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      System.out.println("PROTOCOL_TEST_FAILED");
      mc.stop();
      stage = 12;
    }
  }
}
