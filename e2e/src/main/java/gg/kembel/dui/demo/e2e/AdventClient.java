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

/** Muted real-client test. Atlas animations must progress without replacement dialogs. */
final class AdventClient {
  private static final Path OUT = Paths.output();
  private static JsonObject tickLayout;
  private static final String[] STEPS = {
    "SHOT:calendar",
    "gift_24",
    "FAST:lid-early",
    "FAST:lid-late",
    "SAME_SCREEN",
    "WAIT_REVEAL",
    "FAST:reward-early",
    "FAST:reward-late",
    "SAME_SCREEN",
    "SHOT:reveal",
    "SHOT:party-late",
    "SAME_SCREEN",
    "WAIT_SETTLED",
    "SHOT:open-settled",
    "advent_again",
    "WAIT_REVEAL",
    "SHOT:replayed",
    "advent_back",
    "ALL_GIFTS",
    "advent_motion",
    "gift_1",
    "REVEALED_NOW",
    "SHOT:motion-off",
    "SHOT:still-frame",
    "SAME_SCREEN",
    "advent_back",
    "SMALL_WINDOW",
    "SHOT:compact",
    "gift_13",
    "SHOT:compact-reveal",
    "advent_back",
    "AUTO_WINDOW",
    "SHOT:auto",
    "gift_7",
    "SHOT:auto-reveal",
    "advent_back",
    "advent_motion",
    "gift_8",
    "ESCAPE",
    "WAIT_CLOSED",
    "REOPEN",
    "SHOT:reset-after-close",
    "gift_4",
    "CLOSE",
    "WAIT_CLOSED"
  };
  private int ticks, stage, changed, motionToggles, gift = 1;
  private final Set<Integer> opened = new HashSet<>();
  private String before;
  private Screen previous;

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    ticks++;
    tickLayout = null;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    if (ticks > 5000 || stage > 0 && ticks - changed > 700) {
      fail(mc, new IllegalStateException("Timeout stage=" + stage));
      return;
    }
    if (mc.gui.overlay() != null || ticks - changed < 4) return;
    try {
      if (stage == 0 && ticks > 80) {
        Files.createDirectories(OUT.resolve("screenshots"));
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.options.guiScale().set(Paths.referenceScale(mc));
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var server = new ServerData("Advent fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        advance();
        return;
      }
      if (stage == 1) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        before = mc.player.getInventory().getNonEquipmentItems().toString();
        mc.getConnection().sendCommand("dui advent spacious");
        advance();
        return;
      }
      if (stage == 2 && ticks - changed < 360) return;
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
                        true,
                        "motionToggles",
                        motionToggles,
                        "giftsOpened",
                        opened.size())));
        System.out.println("ADVENT_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2) return;
      String step = STEPS[stage - 2];
      int wait = step.equals("SHOT:reveal") || step.equals("SHOT:party-late") ? 30 : 40;
      if ((step.startsWith("SHOT:") || step.equals("WAIT_CLOSED")) && ticks - changed < wait)
        return;
      if (step.startsWith("FAST:")) {
        if (ticks - changed < 6) return;
        snapshot(mc, step.substring(5));
        if (step.equals("FAST:lid-early") || step.equals("FAST:reward-early"))
          previous = mc.gui.screen();
      } else if (step.startsWith("SHOT:")) {
        snapshot(mc, step.substring(5));
        if (step.equals("SHOT:reveal") || step.equals("SHOT:motion-off"))
          previous = mc.gui.screen();
      } else
        switch (step) {
          case "WAIT_REVEAL" -> {
            if (!layout().get("phase").getAsString().equals("REVEALED") || !received(mc)) return;
          }
          case "WAIT_SETTLED" -> {
            if (!layout().getAsJsonObject("state").get("effectsFinished").getAsBoolean()
                || !received(mc)) return;
          }
          case "REVEALED_NOW" -> {
            if (!layout().get("phase").getAsString().equals("REVEALED"))
              throw new IllegalStateException("Motion-off must reveal immediately");
          }
          case "ALL_GIFTS" -> {
            if (ticks - changed < 12 || !received(mc)) return;
            String phase = layout().get("phase").getAsString();
            if (gift > 24) {
              if (!phase.equals("BOARD")) {
                hit(mc, "advent_back");
                changed = ticks;
                return;
              }
            } else if (phase.equals("BOARD")) {
              hit(mc, "gift_" + gift);
              opened.add(gift);
              changed = ticks;
              return;
            } else if (phase.equals("OPENING")) return;
            else {
              hit(mc, "advent_back");
              gift++;
              changed = ticks;
              return;
            }
          }
          case "advent_motion" -> {
            hit(mc, "advent_motion");
            motionToggles++;
          }
          case "SAME_SCREEN" -> {
            if (previous != mc.gui.screen())
              throw new IllegalStateException("Animation replaced the dialog");
          }
          case "SMALL_WINDOW" -> {
            mc.getWindow().setWindowed(640, 480);
            mc.options.guiScale().set(Paths.referenceScale(mc));
            mc.resizeGui();
            mc.getConnection().sendCommand("dui advent compact");
          }
          case "AUTO_WINDOW" -> {
            mc.getWindow().setWindowed(1440, 1000);
            mc.options.guiScale().set(0);
            mc.resizeGui();
          }
          case "REOPEN" -> mc.getConnection().sendCommand("dui advent compact");
          case "ESCAPE" ->
              mc.gui
                  .screen()
                  .keyPressed(
                      new net.minecraft.client.input.KeyEvent(
                          com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE, 27, 0));
          case "CLOSE" ->
              click(
                  mc,
                  widgets(mc.gui.screen()).stream()
                      .filter(w -> w.getMessage().getString().equals("Close calendar"))
                      .findFirst()
                      .orElseThrow());
          case "WAIT_CLOSED" -> {
            if (mc.gui.screen() instanceof DialogScreen<?>)
              throw new IllegalStateException("Dialog reopened after close");
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
        "ADVENT_TEST_FAILED stage="
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
    System.out.println("ADVENT_TEST_STAGE " + stage);
  }

  private static JsonObject layout() throws Exception {
    // One coherent report per client tick: phase transitions may update the file mid-click.
    if (tickLayout == null)
      tickLayout =
          JsonParser.parseString(
                  Files.readString(Paths.plugin().resolve("layouts/AdventTest.json")))
              .getAsJsonObject();
    return tickLayout;
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

  // Layout diagnostics are written before the replacement packet reaches the client.
  // Wait for receipt rather than clicking a previous revision's widgets.
  private static boolean received(Minecraft mc) throws Exception {
    if (!(mc.gui.screen() instanceof DialogScreen<?>)) return false;
    var data = layout();
    int expected = data.getAsJsonArray("items").size();
    return canvas(mc).getHeight() == data.get("height").getAsInt() + 8
        && widgets(mc.gui.screen()).stream().filter(it -> it instanceof ItemDisplayWidget).count()
            == (expected == 0 ? 0 : expected + 1);
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
