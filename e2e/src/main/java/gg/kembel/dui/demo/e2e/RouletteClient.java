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

/** Real clicks, an unbiased live spin, wheel/ball/chip GPU motion, ledger and close safety. */
final class RouletteClient {
  private static final Path OUT = Paths.output();
  private static final String[] STEPS = {
    "SHOT:lobby",
    "READABLE_SCALE",
    "SHOT:labels-wide",
    "chip_5",
    "bet_n_23",
    "bet_n_0",
    "bet_d_3",
    "bet_red",
    "bet_c_1",
    "SHOT:chips-wide",
    "roulette_clear",
    "REFERENCE_SCALE",
    "chip_5",
    "PLACE_ALL",
    "SHOT:bets",
    "roulette_spin",
    "SPIN:spin-early",
    "SPIN:spin-middle",
    "SPIN:spin-late",
    "SAME_SCREEN",
    "PAYOUT:payout-early",
    "PAYOUT:payout-late",
    "SAME_SCREEN",
    "WAIT_SETTLED",
    "LEDGER",
    "SHOT:result",
    "roulette_repeat",
    "SHOT:repeated",
    "roulette_clear",
    "SHOT:cleared",
    "roulette_motion",
    "SHOT:motion-off",
    "SHOT:still-frame",
    "SAME_SCREEN",
    "roulette_repeat",
    "roulette_spin",
    "LEDGER_STILL",
    "SHOT:instant-result",
    "SMALL_WINDOW",
    "SHOT:compact",
    "READABLE_SCALE",
    "SHOT:labels-compact",
    "bet_n_23",
    "bet_n_0",
    "bet_d_3",
    "bet_red",
    "bet_c_1",
    "SHOT:chips-compact",
    "roulette_clear",
    "REFERENCE_SCALE",
    "roulette_motion",
    "bet_n_0",
    "roulette_spin",
    "SPIN:compact-early",
    "SPIN:compact-late",
    "SAME_SCREEN",
    "ESCAPE",
    "WAIT_CLOSED",
    "REOPEN",
    "SHOT:after-close",
    "AUTO_WINDOW",
    "SHOT:auto",
    "CLOSE",
    "WAIT_CLOSED"
  };
  private int ticks, stage, changed, placed, spinAt;
  private String inventory;
  private Screen previous, receivedScreen, pendingResponse;
  private int receivedAt;

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    if (ticks > 6000 || stage > 0 && ticks - changed > 1800) {
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
        var server = new ServerData("Roulette fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        advance();
        return;
      }
      if (stage == 1) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        inventory = mc.player.getInventory().getNonEquipmentItems().toString();
        mc.getConnection().sendCommand("dui roulette spacious");
        advance();
        return;
      }
      if (stage == 2 && ticks - changed < 240) return;
      if (stage == STEPS.length + 2) {
        if (mc.gui.screen() instanceof DialogScreen<?>
            || !inventory.equals(mc.player.getInventory().getNonEquipmentItems().toString()))
          throw new IllegalStateException("Inventory/close regression");
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
                        "payoutVerified",
                        true)));
        System.out.println("ROULETTE_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2) return;
      String step = STEPS[stage - 2];
      if (step.startsWith("SHOT:") && ticks - changed < 35) return;
      if (step.equals("WAIT_CLOSED") && ticks - changed < 190) return;
      if (step.startsWith("SHOT:")) {
        if (!received(mc)) return;
        snapshot(mc, step.substring(5));
        if (step.equals("SHOT:motion-off")) previous = mc.gui.screen();
      } else if (step.startsWith("SPIN:")) {
        int wait =
            step.contains("middle")
                ? 65
                : step.endsWith("late") ? (step.startsWith("SPIN:compact") ? 20 : 130) : 8;
        if (ticks - spinAt < wait
            || !received(mc)
            || !layout().get("phase").getAsString().equals("SPINNING")) return;
        snapshot(mc, step.substring(5));
        if (step.endsWith("early")) previous = mc.gui.screen();
      } else if (step.startsWith("PAYOUT:")) {
        if (!layout().get("phase").getAsString().equals("PAYOUT")
            || !received(mc)
            || ticks - changed < 6) return;
        snapshot(mc, step.substring(7));
        if (step.endsWith("early")) previous = mc.gui.screen();
      } else
        switch (step) {
          case "READABLE_SCALE" -> {
            mc.getWindow().setWindowed(1280, 900);
            mc.options.guiScale().set(Paths.referenceScale(mc) * 2);
            mc.resizeGui();
          }
          case "REFERENCE_SCALE" -> {
            boolean compact = layout().get("width").getAsInt() == 320;
            mc.getWindow().setWindowed(compact ? 640 : 1280, compact ? 480 : 900);
            mc.options.guiScale().set(Paths.referenceScale(mc));
            mc.resizeGui();
          }
          case "PLACE_ALL" -> {
            if (!received(mc) || ticks - changed < 6) return;
            if (layout().get("stake").getAsInt() != placed * 5) return;
            if (placed < 37) {
              hit(mc, "bet_n_" + placed++);
              changed = ticks;
              return;
            }
          }
          case "WAIT_SETTLED" -> {
            if (layout().get("busy").getAsBoolean() || !received(mc)) return;
          }
          case "LEDGER", "LEDGER_STILL" -> {
            if (!received(mc) || layout().get("busy").getAsBoolean()) return;
            int round = step.equals("LEDGER") ? 1 : 2;
            if (layout().get("balance").getAsLong() != 5000 - round * 5
                || layout().get("return").getAsInt() != 180
                || layout().get("rounds").getAsInt() != round
                || layout().get("stake").getAsInt() != 0)
              throw new IllegalStateException(
                  "Roulette ledger mismatch " + layout().get("balance"));
          }
          case "SAME_SCREEN" -> {
            if (previous != mc.gui.screen())
              throw new IllegalStateException("Animation replaced the dialog per frame");
          }
          case "SMALL_WINDOW" -> {
            mc.getWindow().setWindowed(640, 480);
            mc.options.guiScale().set(Paths.referenceScale(mc));
            mc.resizeGui();
            mc.getConnection().sendCommand("dui roulette compact");
          }
          case "AUTO_WINDOW" -> {
            mc.getWindow().setWindowed(1440, 1000);
            mc.options.guiScale().set(0);
            mc.resizeGui();
          }
          case "REOPEN" -> mc.getConnection().sendCommand("dui roulette compact");
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
              throw new IllegalStateException("Timer reopened the closed table");
          }
          default -> {
            if (!received(mc)) return;
            hit(mc, step);
            if (step.equals("roulette_spin")) spinAt = ticks;
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
    System.out.println("ROULETTE_TEST_STAGE " + stage);
  }

  private void fail(Minecraft mc, Exception e) {
    System.err.println("ROULETTE_TEST_FAILED stage=" + stage);
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
            Files.readString(Paths.plugin().resolve("layouts/RouletteTest.json")))
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
    // Chip selection can change only styles, leaving the raw text hash identical.
    // Await the response to the last click before using its replacement callbacks.
    if (mc.gui.screen() == pendingResponse) return false;
    pendingResponse = null;
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

  private void hit(Minecraft mc, String id) throws Exception {
    validate(mc);
    var data = layout();
    JsonObject h = null;
    for (var entry : data.getAsJsonArray("hits"))
      if (entry.getAsJsonObject().get("id").getAsString().equals(id)) h = entry.getAsJsonObject();
    if (h == null || h.get("action").getAsString().isEmpty())
      throw new IllegalStateException("Missing or locked hit: " + id);
    var w = canvas(mc);
    pendingResponse = mc.gui.screen();
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
