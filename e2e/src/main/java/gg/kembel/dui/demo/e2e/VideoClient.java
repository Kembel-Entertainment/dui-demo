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

/** Real-client proof: network feed, runtime RGB thumbnails, navigation and URL confirmation. */
final class VideoClient {
  private static final Path OUT = Paths.output(),
      LAYOUT = Paths.plugin().resolve("layouts/VideoTest.json"),
      PACK = Paths.plugin().resolve("pack/dui.zip");
  private static final String[] STEPS = {
    "WAIT:0:2",
    "SHOT:wide-first",
    "video_next",
    "WAIT:1:2",
    "SHOT:wide-next",
    "video_previous",
    "WAIT:0:2",
    "video_refresh",
    "WAIT:0:2",
    "SHOT:wide-refreshed",
    "LINK",
    "LINK_CONFIRM",
    "CANCEL_LINK",
    "WAIT:0:2",
    "THUMB_LINK",
    "LINK_CONFIRM",
    "CANCEL_LINK",
    "WAIT:0:2",
    "SMALL",
    "WAIT:0:1",
    "SHOT:compact-first",
    "video_next",
    "WAIT:1:1",
    "SHOT:compact-next",
    "CLOSE",
    "CLOSED"
  };
  private int ticks, stage, changed;
  private String pack, inventory;

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    if (ticks > 4000 || stage > 0 && ticks - changed > 700) {
      fail(mc, new IllegalStateException("Timeout"));
      return;
    }
    if (mc.gui.overlay() != null || ticks - changed < 6) return;
    try {
      if (stage == 0 && ticks > 80) {
        Files.createDirectories(OUT.resolve("screenshots"));
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.options.guiScale().set(Paths.referenceScale(mc));
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        pack = fingerprint();
        var server =
            new ServerData("Runtime videos fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        advance();
        return;
      }
      if (stage == 1) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        inventory = mc.player.getInventory().getNonEquipmentItems().toString();
        mc.getConnection().sendCommand("uivideos spacious");
        advance();
        return;
      }
      if (stage == 2 && ticks - changed < 360) return;
      if (stage == STEPS.length + 2) {
        if (!inventory.equals(mc.player.getInventory().getNonEquipmentItems().toString())
            || !pack.equals(fingerprint()))
          throw new IllegalStateException("Pack or inventory changed");
        Files.writeString(
            OUT.resolve("client-result.json"),
            new Gson()
                .toJson(
                    Map.of(
                        "passed",
                        true,
                        "steps",
                        STEPS.length,
                        "muted",
                        true,
                        "packUnchanged",
                        true,
                        "inventoryUnchanged",
                        true,
                        "nativeLinkConfirmation",
                        true)));
        System.out.println("VIDEO_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2) return;
      String step = STEPS[stage - 2];
      if (step.startsWith("WAIT:")) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        var data = layout();
        if (!data.has("section") || !data.get("section").getAsString().equals("videos")) return;
        var parts = step.split(":");
        if (data.get("loading").getAsBoolean()
            || data.get("page").getAsInt() != Integer.parseInt(parts[1])) return;
        if (data.getAsJsonArray("images").size() != Integer.parseInt(parts[2]))
          throw new IllegalStateException("Missing live thumbnails: " + data);
        if (!data.get("packSha1").getAsString().equals(pack))
          throw new IllegalStateException("Thumbnail required pack rebuild");
        validate(mc);
      } else if (step.startsWith("SHOT:")) {
        if (ticks - changed < 20) return;
        snapshot(mc, step.substring(5));
      } else
        switch (step) {
          case "LINK" ->
              hit(
                  mc,
                  "watch_"
                      + layout()
                          .getAsJsonArray("videos")
                          .get(0)
                          .getAsJsonObject()
                          .get("id")
                          .getAsString());
          case "THUMB_LINK" ->
              hit(
                  mc,
                  "thumb_"
                      + layout()
                          .getAsJsonArray("videos")
                          .get(0)
                          .getAsJsonObject()
                          .get("id")
                          .getAsString());
          case "LINK_CONFIRM" -> {
            if (!(mc.gui.screen() instanceof ConfirmLinkScreen))
              throw new IllegalStateException("Native URL confirmation missing");
          }
          case "CANCEL_LINK" ->
              click(
                  mc,
                  widgets(mc.gui.screen()).stream()
                      .filter(w -> Set.of("Cancel", "No").contains(w.getMessage().getString()))
                      .findFirst()
                      .orElseThrow());
          case "SMALL" -> {
            mc.getWindow().setWindowed(640, 480);
            mc.options.guiScale().set(0);
            mc.resizeGui();
            mc.getConnection().sendCommand("uivideos compact");
          }
          case "CLOSE" ->
              click(
                  mc,
                  widgets(mc.gui.screen()).stream()
                      .filter(w -> w.getMessage().getString().equals("Close videos"))
                      .findFirst()
                      .orElseThrow());
          case "CLOSED" -> {
            if (ticks - changed < 40) return;
            if (mc.gui.screen() instanceof DialogScreen<?>)
              throw new IllegalStateException("Closed video menu reopened");
          }
          default -> hit(mc, step);
        }
      advance();
    } catch (Exception e) {
      fail(mc, e);
    }
  }

  private void advance() {
    stage++;
    changed = ticks;
    System.out.println("VIDEO_TEST_STAGE " + stage);
  }

  private void fail(Minecraft mc, Exception e) {
    System.err.println("VIDEO_TEST_FAILED stage=" + stage);
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

  private static String fingerprint() throws Exception {
    return HexFormat.of()
        .formatHex(
            java.security.MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(PACK)));
  }

  private static JsonObject layout() throws Exception {
    return JsonParser.parseString(Files.readString(LAYOUT)).getAsJsonObject();
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
    int actual =
        mc.font.split(w.getMessage(), width + 4).stream()
            .mapToInt(mc.font::width)
            .max()
            .orElseThrow();
    if (actual != width + 2 || w.getHeight() != height + 8)
      throw new IllegalStateException("Text flow " + actual + "x" + w.getHeight());
    if (x < 0
        || x + width > mc.getWindow().getGuiScaledWidth()
        || w.getY() + w.getPadding() < 33
        || w.getY() + w.getPadding() + height > mc.getWindow().getGuiScaledHeight() - 33)
      throw new IllegalStateException("Canvas clipped");
    if (widgets(mc.gui.screen()).stream().anyMatch(it -> it instanceof ItemDisplayWidget))
      throw new IllegalStateException("Unexpected native item carriers");
  }

  private static void hit(Minecraft mc, String id) throws Exception {
    validate(mc);
    var data = layout();
    JsonObject hit = null;
    for (var element : data.getAsJsonArray("hits")) {
      var h = element.getAsJsonObject();
      if (h.get("id").getAsString().equals(id)) hit = h;
    }
    if (hit == null) throw new IllegalStateException("Missing hit " + id);
    var w = canvas(mc);
    clickAt(
        mc,
        w.getX()
            + (w.getWidth() - data.get("width").getAsInt() - 2) / 2.0
            + hit.get("x").getAsInt()
            + hit.get("width").getAsInt() / 2.0,
        w.getY() + w.getPadding() + hit.get("y").getAsInt() + hit.get("height").getAsInt() / 2.0);
  }

  private static void click(Minecraft mc, AbstractWidget w) {
    clickAt(mc, w.getX() + w.getWidth() / 2.0, w.getY() + w.getHeight() / 2.0);
  }

  private static void clickAt(Minecraft mc, double x, double y) {
    RealClientHarness.clickAt(mc, x, y);
  }

  private static void snapshot(Minecraft mc, String name) throws Exception {
    validate(mc);
    var data = layout();
    var w = canvas(mc);
    var meta = new JsonObject();
    meta.addProperty("canvasX", w.getX() + (w.getWidth() - data.get("width").getAsInt() - 2) / 2.0);
    meta.addProperty("canvasY", w.getY() + w.getPadding());
    meta.addProperty("scale", mc.getWindow().getGuiScale());
    meta.addProperty("guiSetting", mc.options.guiScale().get());
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
