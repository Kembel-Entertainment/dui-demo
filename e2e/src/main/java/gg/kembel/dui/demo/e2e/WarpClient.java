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
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Real mouse callbacks, cyclic selection, shared-shader slides and fixed viewport clipping. */
final class WarpClient {
  private static final Path OUT = Paths.output();
  private static final String[] STEPS = {
    "SHOT:atlas",
    "warp_next",
    "FAST:left-early",
    "FAST:left-late",
    "SAME_SCREEN",
    "WAIT_SETTLED",
    "EXPECT:1",
    "SHOT:bloom",
    "warp_next",
    "WAIT_SETTLED",
    "EXPECT:2",
    "SHOT:ember",
    "warp_card_1",
    "WAIT_SETTLED",
    "EXPECT:3",
    "SHOT:astral",
    "warp_next",
    "WAIT_SETTLED",
    "EXPECT:0",
    "SHOT:wrap",
    "warp_previous",
    "FAST:right-early",
    "FAST:right-late",
    "SAME_SCREEN",
    "WAIT_SETTLED",
    "EXPECT:3",
    "warp_tab_1",
    "SHOT:direct-select",
    "EXPECT:1",
    "warp_travel",
    "SHOT:portal",
    "ARRIVED",
    "warp_motion",
    "warp_next",
    "EXPECT:2",
    "SHOT:motion-off",
    "SHOT:still-frame",
    "SAME_SCREEN",
    "SMALL_WINDOW",
    "SHOT:compact",
    "warp_card_1",
    "EXPECT:3",
    "SHOT:compact-astral",
    "AUTO_WINDOW",
    "SHOT:auto",
    "warp_motion",
    "warp_next",
    "FAST:auto-early",
    "FAST:auto-late",
    "SAME_SCREEN",
    "WAIT_SETTLED",
    "EXPECT:0",
    "SHOT:auto-rest",
    "warp_next",
    "ESCAPE",
    "WAIT_CLOSED",
    "REOPEN",
    "SHOT:after-close",
    "warp_next",
    "CLOSE",
    "WAIT_CLOSED"
  };
  private int ticks, stage, changed;
  private String inventory;
  private Screen previous;
  private final Set<Integer> visited = new HashSet<>();

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
    if (mc.gui.overlay() != null || ticks - changed < 4) return;
    try {
      if (stage == 0 && ticks > 80) {
        Files.createDirectories(OUT.resolve("screenshots"));
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.options.guiScale().set(2);
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var server = new ServerData("Warp fixture", "127.0.0.1:25584", ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        advance();
        return;
      }
      if (stage == 1) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        inventory = mc.player.getInventory().getNonEquipmentItems().toString();
        mc.getConnection().sendCommand("dui warps spacious");
        advance();
        return;
      }
      if (stage == 2 && ticks - changed < 360) return;
      if (stage == STEPS.length + 2) {
        if (mc.gui.screen() instanceof DialogScreen<?>
            || !inventory.equals(mc.player.getInventory().getNonEquipmentItems().toString()))
          throw new IllegalStateException("Inventory or close regression");
        if (visited.size() != 4) throw new IllegalStateException("Not all realms visited");
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
                        "realmsVisited",
                        visited.size())));
        System.out.println("WARP_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2) return;
      String step = STEPS[stage - 2];
      if ((step.startsWith("SHOT:") || step.equals("WAIT_CLOSED")) && ticks - changed < 40) return;
      if (step.startsWith("FAST:")) {
        if (ticks - changed < 6 || !received(mc)) return;
        snapshot(mc, step.substring(5));
        if (step.endsWith("early")) previous = mc.gui.screen();
      } else if (step.startsWith("SHOT:")) {
        if (!received(mc)) return;
        snapshot(mc, step.substring(5));
        if (step.equals("SHOT:motion-off")) previous = mc.gui.screen();
      } else if (step.startsWith("EXPECT:")) {
        int expected = Integer.parseInt(step.substring(7));
        if (layout().get("selected").getAsInt() != expected)
          throw new IllegalStateException("Wrong realm expected=" + expected);
        visited.add(expected);
      } else
        switch (step) {
          case "WAIT_SETTLED" -> {
            if (layout().get("moving").getAsBoolean() || !received(mc)) return;
          }
          case "SAME_SCREEN" -> {
            if (previous != mc.gui.screen())
              throw new IllegalStateException("Slide replaced the dialog per frame");
          }
          case "ARRIVED" -> {
            if (!layout().get("arrived").getAsBoolean())
              throw new IllegalStateException("Preview not confirmed");
          }
          case "SMALL_WINDOW" -> {
            mc.getWindow().setWindowed(640, 480);
            mc.options.guiScale().set(2);
            mc.resizeGui();
            mc.getConnection().sendCommand("dui warps compact");
          }
          case "AUTO_WINDOW" -> {
            mc.getWindow().setWindowed(1440, 1000);
            mc.options.guiScale().set(0);
            mc.resizeGui();
          }
          case "REOPEN" -> mc.getConnection().sendCommand("dui warps compact");
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
                      .filter(w -> w.getMessage().getString().equals("Close atlas"))
                      .findFirst()
                      .orElseThrow());
          case "WAIT_CLOSED" -> {
            if (mc.gui.screen() instanceof DialogScreen<?>)
              throw new IllegalStateException("Stale animation reopened closed atlas");
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
    System.out.println("WARP_TEST_STAGE " + stage);
  }

  private void fail(Minecraft mc, Exception e) {
    System.err.println("WARP_TEST_FAILED stage=" + stage);
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
    return JsonParser.parseString(Files.readString(Paths.plugin().resolve("layouts/WarpTest.json")))
        .getAsJsonObject();
  }

  private static List<AbstractWidget> widgets(GuiEventListener parent) {
    var list = new ArrayList<AbstractWidget>();
    if (parent instanceof AbstractWidget w) list.add(w);
    if (parent instanceof ContainerEventHandler c)
      for (var child : c.children()) list.addAll(widgets(child));
    return list;
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
    var w = mc.getWindow();
    ((FixtureMouseAccess) mc.mouseHandler)
        .dui$move(
            w.handle(),
            x * w.getScreenWidth() / w.getGuiScaledWidth(),
            y * w.getScreenHeight() / w.getGuiScaledHeight());
  }

  private static void clickAt(Minecraft mc, double x, double y) {
    move(mc, x, y);
    var mouse = (FixtureMouseAccess) mc.mouseHandler;
    mouse.dui$button(mc.getWindow().handle(), new MouseButtonInfo(0, 0), 1);
    mouse.dui$button(mc.getWindow().handle(), new MouseButtonInfo(0, 0), 0);
    move(mc, 5, 5);
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
