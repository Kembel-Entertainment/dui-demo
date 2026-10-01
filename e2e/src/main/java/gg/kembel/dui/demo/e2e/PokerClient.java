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
 * Real mouse input, actual betting streets, procedural cards/chips, motion off, Auto layout, close
 * safety.
 */
final class PokerClient {
  private static final Path OUT = Paths.output();
  private static final String[] STEPS = {
    "SHOT:lobby",
    "poker_showcase",
    "FAST:deal-early",
    "FAST:deal-late",
    "SAME_SCREEN",
    "WAIT_HERO",
    "SHOT:preflop",
    "poker_plus",
    "RAISE_60",
    "poker_raise",
    "FAST:chips-early",
    "FAST:chips-late",
    "SAME_SCREEN",
    "WAIT_FLOP",
    "FAST:flop-early",
    "FAST:flop-late",
    "SAME_SCREEN",
    "WAIT_HERO",
    "SHOT:flop",
    "poker_call",
    "FAST:turn-early",
    "FAST:turn-late",
    "SAME_SCREEN",
    "WAIT_HERO",
    "SHOT:turn",
    "poker_call",
    "FAST:river-early",
    "FAST:river-late",
    "SAME_SCREEN",
    "WAIT_HERO",
    "SHOT:river",
    "poker_call",
    "FAST:win-early",
    "FAST:win-late",
    "SAME_SCREEN",
    "WAIT_SETTLED",
    "ROYAL",
    "SHOT:showdown",
    "poker_motion",
    "SHOT:motion-off",
    "SHOT:still-frame",
    "SAME_SCREEN",
    "poker_deal",
    "WAIT_HERO",
    "SHOT:normal",
    "poker_fold",
    "WAIT_ENDED",
    "SHOT:folded",
    "SMALL_WINDOW",
    "SHOT:compact-lobby",
    "poker_motion",
    "poker_showcase",
    "FAST:compact-early",
    "FAST:compact-late",
    "SAME_SCREEN",
    "AUTO_PLAY",
    "SHOT:compact-showdown",
    "AUTO_WINDOW",
    "SHOT:auto",
    "poker_deal",
    "ESCAPE",
    "WAIT_CLOSED",
    "REOPEN",
    "SHOT:after-close",
    "CLOSE",
    "WAIT_CLOSED"
  };
  private int ticks, stage, changed;
  private String inventory;
  private Screen previous, receivedScreen;
  private int receivedAt;

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    if (ticks > 8500 || stage > 0 && ticks - changed > 2200) {
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
        var server = new ServerData("Poker fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        advance();
        return;
      }
      if (stage == 1) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        inventory = mc.player.getInventory().getNonEquipmentItems().toString();
        mc.getConnection().sendCommand("dui poker spacious");
        advance();
        return;
      }
      if (stage == 2 && ticks - changed < 360) return;
      if (stage == STEPS.length + 2) {
        if (mc.gui.screen() instanceof DialogScreen<?>
            || !inventory.equals(mc.player.getInventory().getNonEquipmentItems().toString()))
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
                        "showdownVerified",
                        true)));
        System.out.println("POKER_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2) return;
      String step = STEPS[stage - 2];
      if ((step.startsWith("SHOT:") || step.equals("WAIT_CLOSED")) && ticks - changed < 40) return;
      if (step.startsWith("FAST:")) {
        if (ticks - changed < 6 || !layout().get("busy").getAsBoolean() || !received(mc)) return;
        snapshot(mc, step.substring(5));
        if (step.endsWith("early")) previous = mc.gui.screen();
      } else if (step.startsWith("SHOT:")) {
        if (!received(mc)) return;
        snapshot(mc, step.substring(5));
        if (step.equals("SHOT:motion-off")) previous = mc.gui.screen();
      } else
        switch (step) {
          case "WAIT_SETTLED" -> {
            if (layout().get("busy").getAsBoolean() || !received(mc)) return;
          }
          case "WAIT_HERO" -> {
            if (layout().get("busy").getAsBoolean()
                || layout().get("actor").getAsInt() != 0
                || !received(mc)) return;
          }
          case "WAIT_FLOP" -> {
            if (!layout().get("street").getAsString().equals("FLOP") || !received(mc)) return;
          }
          case "WAIT_ENDED" -> {
            if (!layout().get("street").getAsString().equals("SHOWDOWN")
                || layout().get("busy").getAsBoolean()
                || !received(mc)) return;
          }
          case "RAISE_60" -> {
            if (layout().getAsJsonObject("state").get("raiseTarget").getAsInt() != 60
                || !received(mc)) {
              if (ticks - changed > 60)
                throw new IllegalStateException("Raise picker did not adjust");
              return;
            }
          }
          case "ROYAL" -> {
            var game = layout().getAsJsonObject("state").getAsJsonObject("game");
            if (!game.get("street").getAsString().equals("SHOWDOWN")
                || game.getAsJsonObject("hands").getAsJsonObject("0").get("category").getAsInt()
                    != 8
                || game.getAsJsonArray("payouts").get(0).getAsInt() != 240)
              throw new IllegalStateException("Scripted showdown or payout incorrect: " + game);
            if (java.util.stream.StreamSupport.stream(
                        game.getAsJsonArray("stacks").spliterator(), false)
                    .mapToInt(JsonElement::getAsInt)
                    .sum()
                != 4000) throw new IllegalStateException("Lost chips");
          }
          case "AUTO_PLAY" -> {
            if (!received(mc)) return;
            var current = layout();
            if (layout().get("street").getAsString().equals("SHOWDOWN")
                && !layout().get("busy").getAsBoolean()) break;
            if (!current.get("busy").getAsBoolean()
                && current.get("actor").getAsInt() == 0
                && ticks - changed > 25) {
              hit(mc, "poker_call", current);
              changed = ticks;
            }
            return;
          }
          case "SAME_SCREEN" -> {
            if (previous != mc.gui.screen())
              throw new IllegalStateException("Animation replaced the dialog per frame");
          }
          case "SMALL_WINDOW" -> {
            mc.getWindow().setWindowed(640, 480);
            mc.options.guiScale().set(Paths.referenceScale(mc));
            mc.resizeGui();
            mc.getConnection().sendCommand("dui poker compact");
          }
          case "AUTO_WINDOW" -> {
            mc.getWindow().setWindowed(1440, 1000);
            mc.options.guiScale().set(0);
            mc.resizeGui();
          }
          case "REOPEN" -> mc.getConnection().sendCommand("dui poker compact");
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
                      .filter(w -> w.getMessage().getString().equals("Leave table"))
                      .findFirst()
                      .orElseThrow());
          case "WAIT_CLOSED" -> {
            if (mc.gui.screen() instanceof DialogScreen<?>)
              throw new IllegalStateException("Stale bot timer reopened closed table");
          }
          default -> {
            if (!received(mc)) return;
            hit(mc, step);
          }
        }
      advance();
    } catch (Exception e) {
      fail(mc, e);
    }
  }

  private void advance() {
    stage++;
    changed = ticks;
    System.out.println("POKER_TEST_STAGE " + stage);
  }

  private void fail(Minecraft mc, Exception e) {
    System.err.println("POKER_TEST_FAILED stage=" + stage);
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

  private static JsonObject layout() throws Exception {
    return JsonParser.parseString(
            Files.readString(Paths.plugin().resolve("layouts/PokerTest.json")))
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

  // Layout diagnostics are written before the replacement packet reaches the client.
  // Wait for receipt rather than clicking a previous revision's widgets.
  private boolean received(Minecraft mc) throws Exception {
    if (!(mc.gui.screen() instanceof DialogScreen<?>)) return false;
    var data = layout();
    int expected = data.getAsJsonArray("items").size();
    String hash =
        HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                    .digest(
                        canvas(mc)
                            .getMessage()
                            .getString()
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    boolean ready =
        hash.equals(data.get("componentTextHash").getAsString())
            && canvas(mc).getHeight() == data.get("height").getAsInt() + 8
            && widgets(mc.gui.screen()).stream()
                    .filter(it -> it instanceof ItemDisplayWidget)
                    .count()
                == (expected == 0 ? 0 : expected + 1);
    if (!ready) return false;
    // Screenshot reads the previous framebuffer at END_CLIENT_TICK. Let the new screen render.
    if (receivedScreen != mc.gui.screen()) {
      receivedScreen = mc.gui.screen();
      receivedAt = ticks;
      return false;
    }
    return ticks - receivedAt >= 2;
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
    hit(mc, id, layout());
  }

  private static void hit(Minecraft mc, String id, JsonObject data) throws Exception {
    validate(mc);
    JsonObject h = null;
    for (var entry : data.getAsJsonArray("hits"))
      if (entry.getAsJsonObject().get("id").getAsString().equals(id)) h = entry.getAsJsonObject();
    if (h == null || h.get("action").getAsString().isEmpty())
      throw new IllegalStateException("Missing or locked hit: " + id);
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
