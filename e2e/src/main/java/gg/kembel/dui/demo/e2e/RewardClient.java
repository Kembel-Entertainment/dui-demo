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
final class RewardClient {
  private static final Path OUT = Paths.output();
  private static final String[] STEPS = {
    "reward_reset",
    "MOTION_ON",
    "SHOT:calendar",
    "SHOT:idle-motion",
    "SAME_SCREEN",
    "reward_day_6",
    "SHOT:locked-preview",
    "reward_claim",
    "STARS:0",
    "reward_day_0",
    "reward_claim",
    "STARS:50",
    "SHOT:celebration",
    "SHOT:confetti-motion",
    "SAME_SCREEN",
    "reward_claim",
    "reward_claim",
    "STARS:50",
    "SHOT:collected",
    "reward_next",
    "reward_claim",
    "STARS:125",
    "reward_next",
    "reward_claim",
    "reward_next",
    "reward_claim",
    "reward_next",
    "reward_claim",
    "reward_next",
    "reward_claim",
    "reward_next",
    "SHOT:finale-ready",
    "reward_claim",
    "STARS:1050",
    "SHOT:week-complete",
    "reward_next",
    "SHOT:new-week",
    "reward_claim",
    "STARS:1100",
    "reward_next",
    "reward_next",
    "SHOT:missed-day",
    "reward_claim",
    "STARS:1150",
    "reward_motion",
    "SHOT:motion-off",
    "SHOT:still-frame",
    "SAME_SCREEN",
    "SMALL_WINDOW",
    "SHOT:compact",
    "AUTO_WINDOW",
    "SHOT:auto",
    "REOPEN",
    "STARS:1150",
    "CLOSE",
    "WAIT_CLOSED"
  };
  private int ticks, stage, changed, motionToggles;
  private String before;
  private Screen previous;

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    if (ticks > 5000 || stage > 0 && ticks - changed > 700) {
      fail(mc, new IllegalStateException("Timeout stage=" + stage));
      return;
    }
    if (mc.gui.overlay() != null || ticks - changed < 16) return;
    try {
      if (stage == 0 && ticks > 80) {
        Files.createDirectories(OUT.resolve("screenshots"));
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.options.guiScale().set(Paths.referenceScale(mc));
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var server = new ServerData("Rewards fixture", Paths.server(), ServerData.Type.OTHER);
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
                        motionToggles)));
        System.out.println("REWARD_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2) return;
      String step = STEPS[stage - 2];
      if ((step.startsWith("SHOT:") || step.equals("WAIT_CLOSED")) && ticks - changed < 40) return;
      if (step.startsWith("SHOT:")) {
        snapshot(mc, step.substring(5));
        if (step.equals("SHOT:calendar")
            || step.equals("SHOT:celebration")
            || step.equals("SHOT:motion-off")) previous = mc.gui.screen();
      } else if (step.startsWith("STARS:")) {
        if (layout().getAsJsonObject("state").get("stars").getAsLong()
            != Long.parseLong(step.substring(6))) throw new IllegalStateException(step);
      } else
        switch (step) {
          case "MOTION_ON" -> {
            if (!layout().getAsJsonObject("state").get("motion").getAsBoolean()) {
              hit(mc, "reward_motion");
              motionToggles++;
            }
          }
          case "reward_motion" -> {
            hit(mc, "reward_motion");
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
            mc.getConnection().sendCommand("dailyrewards compact");
          }
          case "AUTO_WINDOW" -> {
            mc.getWindow().setWindowed(1440, 1000);
            mc.options.guiScale().set(0);
            mc.resizeGui();
          }
          case "REOPEN" -> mc.getConnection().sendCommand("dailyrewards compact");
          case "CLOSE" ->
              click(
                  mc,
                  widgets(mc.gui.screen()).stream()
                      .filter(w -> w.getMessage().getString().equals("Close rewards"))
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
        "REWARD_TEST_FAILED stage="
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
    System.out.println("REWARD_TEST_STAGE " + stage);
  }

  private static JsonObject layout() throws Exception {
    return JsonParser.parseString(
            Files.readString(Paths.plugin().resolve("layouts/RewardTest.json")))
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
