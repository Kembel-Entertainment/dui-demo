package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import gg.kembel.dui.demo.e2e.mixin.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/**
 * Verifies a finite menu-wide shader burst, click-through controls and closing during its lifetime.
 */
final class ConfettiClient {
  private static final Path OUT = Paths.output();
  private static final String[] STEPS = {
    "reward_reset",
    "MOTION_ON",
    "reward_claim",
    "BURST:wide-early:12",
    "BURST:wide-late:32",
    "BURST:wide-settled:112",
    "CLEANUP",
    "SHOT:wide-clean",
    "reward_next",
    "SMALL_WINDOW",
    "reward_claim",
    "BURST:compact-early:12",
    "BURST:compact-late:32",
    "reward_motion",
    "SHOT:motion-disabled",
    "SHOT:motion-still",
    "reward_motion",
    "NO_BURST",
    "reward_next",
    "reward_claim",
    "ESCAPE",
    "WAIT_CLOSED",
    "REOPEN",
    "NO_BURST",
    "CLOSE",
    "WAIT_CLOSED"
  };
  private int ticks, stage, changed, claimedAt;
  private String before;
  private Screen burstScreen;

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    if (ticks > 3500 || stage > 0 && ticks - changed > 600) {
      fail(mc, new IllegalStateException("Timeout stage=" + stage));
      return;
    }
    if (mc.gui.overlay() != null || ticks - changed < 5) return;
    try {
      if (stage == 0 && ticks > 80) {
        Files.createDirectories(OUT.resolve("screenshots"));
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.options.guiScale().set(Paths.referenceScale(mc));
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var server = new ServerData("Confetti fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        advance();
        return;
      }
      if (stage == 1) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        before = mc.player.getInventory().getNonEquipmentItems().toString();
        mc.getConnection().sendCommand("dailyrewards spacious");
        advance();
        return;
      }
      if (stage == 2 && ticks - changed < 120) return;
      if (stage == STEPS.length + 2) {
        if (mc.gui.screen() instanceof DialogScreen<?>
            || !before.equals(mc.player.getInventory().getNonEquipmentItems().toString()))
          throw new IllegalStateException("Inventory or close regression");
        Files.writeString(
            OUT.resolve("client-result.json"),
            new Gson()
                .toJson(
                    Map.of(
                        "passed",
                        true,
                        "steps",
                        STEPS.length,
                        "inventoryUnchanged",
                        true,
                        "muted",
                        true)));
        System.out.println("CONFETTI_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2) return;
      String step = STEPS[stage - 2];
      if (step.startsWith("BURST:")) {
        var parts = step.split(":");
        if (ticks - claimedAt < Integer.parseInt(parts[2])) return;
        if (parts[1].endsWith("early")) {
          burstScreen = mc.gui.screen();
          if (!layout().has("confetti")) throw new IllegalStateException("Missing burst metadata");
        } else if (burstScreen != mc.gui.screen())
          throw new IllegalStateException("Per-frame dialog replacement");
        snapshot(mc, parts[1]);
      } else if (step.startsWith("SHOT:")) {
        if (ticks - changed < 24) return;
        snapshot(mc, step.substring(5));
      } else
        switch (step) {
          case "CLEANUP" -> {
            if (ticks - claimedAt < 160) return;
            if (layout().has("confetti"))
              throw new IllegalStateException("Burst marker not retired");
            if (layout().getAsJsonObject("state").get("stars").getAsInt() != 50)
              throw new IllegalStateException("Claim total");
          }
          case "MOTION_ON" -> {
            if (!layout().getAsJsonObject("state").get("motion").getAsBoolean())
              hit(mc, "reward_motion");
          }
          case "NO_BURST" -> {
            if (layout().has("confetti")) throw new IllegalStateException("Burst replayed");
          }
          case "reward_claim" -> {
            hit(mc, step);
            claimedAt = ticks;
          }
          case "SMALL_WINDOW" -> {
            mc.getWindow().setWindowed(640, 480);
            mc.options.guiScale().set(Paths.referenceScale(mc));
            mc.resizeGui();
            mc.getConnection().sendCommand("dailyrewards compact");
          }
          case "REOPEN" -> mc.getConnection().sendCommand("dailyrewards compact");
          case "ESCAPE" -> {
            if (ticks - claimedAt < 12) return;
            ((FixtureKeyboardAccess) mc.keyboardHandler)
                .dui$key(
                    mc.getWindow().handle(),
                    1,
                    new net.minecraft.client.input.KeyEvent(
                        com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE, 27, 0));
          }
          case "CLOSE" ->
              click(
                  mc,
                  widgets(mc.gui.screen()).stream()
                      .filter(w -> w.getMessage().getString().equals("Close rewards"))
                      .findFirst()
                      .orElseThrow());
          case "WAIT_CLOSED" -> {
            if (ticks - changed < 160) return;
            if (mc.gui.screen() instanceof DialogScreen<?>)
              throw new IllegalStateException("Expired burst reopened the dialog");
          }
          default -> hit(mc, step);
        }
      advance();
    } catch (Exception e) {
      fail(mc, e);
    }
  }

  private void fail(Minecraft mc, Exception e) {
    System.err.println(
        "CONFETTI_TEST_FAILED stage="
            + stage
            + " step="
            + (stage >= 2 && stage < STEPS.length + 2 ? STEPS[stage - 2] : "setup"));
    e.printStackTrace();
    Screenshot.takeScreenshot(
        mc.gameRenderer.mainRenderTarget(),
        img -> {
          try (img) {
            img.writeToFile(OUT.resolve("failure.png"));
          } catch (Exception ignored) {
          }
        });
    mc.stop();
  }

  private void advance() {
    stage++;
    changed = ticks;
    System.out.println("CONFETTI_TEST_STAGE " + stage);
  }

  private static JsonObject layout() throws Exception {
    return JsonParser.parseString(
            Files.readString(Paths.plugin().resolve("layouts/ConfettiTest.json")))
        .getAsJsonObject();
  }

  private static List<AbstractWidget> widgets(GuiEventListener parent) {
    return RealClientHarness.widgets(parent);
  }

  private static FocusableTextWidget canvas(Minecraft mc) {
    return widgets(mc.gui.screen()).stream()
        .filter(w -> w instanceof FocusableTextWidget)
        .map(w -> (FocusableTextWidget) w)
        .max(Comparator.comparingInt(AbstractWidget::getHeight))
        .orElseThrow();
  }

  private static void validate(Minecraft mc) throws Exception {
    var data = layout();
    var w = canvas(mc);
    int width = data.get("width").getAsInt(), height = data.get("height").getAsInt();
    double x = w.getX() + (w.getWidth() - width - 2) / 2.0;
    if (mc.font.split(w.getMessage(), width + 4).stream()
                .mapToInt(mc.font::width)
                .max()
                .orElseThrow()
            != width + 2
        || w.getHeight() != height + 8) throw new IllegalStateException("Text flow mismatch");
    if (x < 0
        || x + width > mc.getWindow().getGuiScaledWidth()
        || w.getY() + w.getPadding() < 33
        || w.getY() + w.getPadding() + height > mc.getWindow().getGuiScaledHeight() - 33)
      throw new IllegalStateException("Canvas clipped");
    var items =
        widgets(mc.gui.screen()).stream().filter(it -> it instanceof ItemDisplayWidget).toList();
    int expected = data.getAsJsonArray("items").size();
    if (items.size() != (expected == 0 ? 0 : expected + 1))
      throw new IllegalStateException("Missing item carriers");
    for (var item : items)
      if (item.getY() < 33
          || item.getY() + item.getHeight() > mc.getWindow().getGuiScaledHeight() - 33)
        throw new IllegalStateException("Native item clipped");
  }

  private static void hit(Minecraft mc, String id) throws Exception {
    validate(mc);
    var data = layout();
    JsonObject h = null;
    for (var entry : data.getAsJsonArray("hits"))
      if (entry.getAsJsonObject().get("id").getAsString().equals(id)) h = entry.getAsJsonObject();
    if (h == null) throw new IllegalStateException("Missing hit: " + id);
    var w = canvas(mc);
    clickAt(
        mc,
        w.getX()
            + (w.getWidth() - data.get("width").getAsInt() - 2) / 2.0
            + h.get("x").getAsInt()
            + h.get("width").getAsInt() / 2.0,
        w.getY() + w.getPadding() + h.get("y").getAsInt() + h.get("height").getAsInt() / 2.0);
  }

  private static void click(Minecraft mc, AbstractWidget w) {
    clickAt(mc, w.getX() + w.getWidth() / 2.0, w.getY() + w.getHeight() / 2.0);
  }

  private static void move(Minecraft mc, double x, double y) {
    RealClientHarness.move(mc, x, y);
  }

  private static void clickAt(Minecraft mc, double x, double y) {
    RealClientHarness.clickAt(mc, x, y);
  }

  private static void snapshot(Minecraft mc, String name) throws Exception {
    validate(mc);
    var data = layout();
    var w = canvas(mc);
    var meta = new JsonObject();
    meta.addProperty("screenWidth", mc.getWindow().getGuiScaledWidth());
    meta.addProperty("screenHeight", mc.getWindow().getGuiScaledHeight());
    meta.addProperty("scale", mc.getWindow().getGuiScale());
    meta.addProperty("guiSetting", mc.options.guiScale().get());
    meta.addProperty("canvasX", w.getX() + (w.getWidth() - data.get("width").getAsInt() - 2) / 2.0);
    meta.addProperty("canvasY", w.getY() + w.getPadding());
    meta.add("layout", data);
    Files.writeString(OUT.resolve("screenshots/" + name + ".json"), meta.toString());
    Screenshot.takeScreenshot(
        mc.gameRenderer.mainRenderTarget(),
        img -> {
          try (img) {
            img.writeToFile(OUT.resolve("screenshots/" + name + ".png"));
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }
}
