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

/**
 * Exercises actual lever clicks, sequential reels, previews, payout, auto GUI and closing mid-spin.
 */
final class SlotClient {
  private static final Path OUT = Paths.output();
  private static final String[] STEPS = {
    "NORMALIZE",
    "FOCUS",
    "SHOT:wide-focused",
    "BLUR",
    "SHOT:wide-idle",
    "slot_mode",
    "SPIN",
    "FRAME:wide-pull:5",
    "FRAME:wide-spin:23",
    "FRAME:wide-stop-one:48",
    "FRAME:wide-stop-two:59",
    "WIN:wide-coins:82",
    "SHOT:wide-more-coins",
    "FINISH:wide-finished",
    "slot_mode",
    "SPIN",
    "WIN:wide-pair:85",
    "FINISH:wide-pair-finished",
    "slot_mode",
    "SPIN",
    "WIN:wide-miss:85",
    "SMALL_WINDOW",
    "slot_mode",
    "slot_mode",
    "SPIN",
    "FRAME:compact-spin:25",
    "WIN:compact-coins:82",
    "FINISH:compact-finished",
    "FOCUS",
    "SHOT:compact-focused",
    "BLUR",
    "slot_paytable",
    "SHOT:compact-payouts",
    "slot_paytable",
    "slot_motion",
    "SPIN",
    "STILL:compact-no-motion",
    "SHOT:compact-still",
    "slot_motion",
    "slot_mode",
    "slot_mode",
    "slot_mode",
    "RANDOM",
    "ESCAPE",
    "WAIT_CLOSED",
    "REOPEN",
    "LEDGER",
    "slot_bet_more",
    "BET",
    "CLOSE",
    "WAIT_CLOSED",
    "WIDE_REOPEN",
    "NORMALIZE",
    "slot_mode",
    "MOVE_TEMPLATE",
    "WAIT_LAYOUT",
    "SPIN",
    "WIN:dynamic-coins:82",
    "FINISH:dynamic-finished",
    "RESTORE_TEMPLATE",
    "WAIT_RESTORED",
    "NATIVE_FOCUS_TEMPLATE",
    "WAIT_NATIVE_FOCUS",
    "FOCUS",
    "SHOT:wide-native-focused",
    "BLUR",
    "RESTORE_TEMPLATE",
    "WAIT_RESTORED",
    "CLOSE",
    "WAIT_CLOSED"
  };
  private int ticks, stage, changed, claimedAt;
  private String before;
  private Screen burstScreen;
  private long balance;
  private String originalTemplate, packFingerprint;
  private static final Path TEMPLATE = Paths.plugin().resolve("ui/slots.html"),
      PACK = Paths.plugin().resolve("pack/dui.zip");

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    if (ticks > 6000 || stage > 0 && ticks - changed > 600) {
      fail(mc, new IllegalStateException("Timeout stage=" + stage));
      return;
    }
    if (mc.gui.overlay() != null || ticks - changed < 5) return;
    try {
      if (stage == 0 && ticks > 80) {
        Files.createDirectories(OUT.resolve("screenshots"));
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.options.guiScale().set(2);
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var server = new ServerData("Slots fixture", "127.0.0.1:25584", ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        advance();
        return;
      }
      if (stage == 1) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        before = mc.player.getInventory().getNonEquipmentItems().toString();
        mc.getConnection().sendCommand("slots spacious");
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
                        true,
                        "templateReload",
                        true,
                        "packUnchanged",
                        packFingerprint.equals(fingerprint()))));
        System.out.println("SLOT_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2) return;
      String step = STEPS[stage - 2];
      if (step.startsWith("FRAME:") || step.startsWith("WIN:")) {
        var parts = step.split(":");
        if (ticks - claimedAt < Integer.parseInt(parts[2])) return;
        if (step.startsWith("FRAME:")
            && ticks - claimedAt > 12
            && burstScreen != null
            && burstScreen != mc.gui.screen())
          throw new IllegalStateException("Per-frame dialog replacement");
        burstScreen = mc.gui.screen();
        if (step.startsWith("WIN:")) {
          if (layout().getAsJsonObject("state").get("pending").getAsBoolean()) return;
          if (layout().getAsJsonObject("state").get("chips").getAsLong() != balance)
            throw new IllegalStateException("Preview changed balance");
        }
        snapshot(mc, parts[1]);
      } else if (step.startsWith("FINISH:")) {
        if (ticks - claimedAt < 205) return;
        if (layout().getAsJsonObject("effect").get("startedAt").getAsLong() != -1)
          throw new IllegalStateException("Effect not retired");
        snapshot(mc, step.substring(7));
      } else if (step.startsWith("SHOT:") || step.startsWith("STILL:")) {
        if (ticks - changed < 24) return;
        snapshot(mc, step.substring(step.indexOf(':') + 1));
      } else
        switch (step) {
          case "FOCUS" -> canvas(mc).setFocused(true);
          case "BLUR" -> canvas(mc).setFocused(false);
          case "NORMALIZE" -> {
            var state = layout().getAsJsonObject("state");
            if (!state.get("motion").getAsBoolean()) {
              hit(mc, "slot_motion");
              changed = ticks;
              return;
            }
            if (state.get("modeIndex").getAsInt() != 0) {
              hit(mc, "slot_mode");
              changed = ticks;
              return;
            }
            balance = state.get("chips").getAsLong();
          }
          case "SPIN" -> {
            hit(mc, "slot_lever");
            claimedAt = ticks;
            burstScreen = null;
          }
          case "RANDOM" -> {
            balance = layout().getAsJsonObject("state").get("chips").getAsLong();
            hit(mc, "slot_spin");
            claimedAt = ticks;
          }
          case "SMALL_WINDOW" -> {
            mc.getWindow().setWindowed(640, 480);
            mc.options.guiScale().set(0);
            mc.resizeGui();
            mc.getConnection().sendCommand("slots compact");
          }
          case "WIDE_REOPEN" -> {
            mc.getWindow().setWindowed(1280, 900);
            mc.options.guiScale().set(2);
            mc.resizeGui();
            mc.getConnection().sendCommand("slots spacious");
          }
          case "MOVE_TEMPLATE" -> {
            originalTemplate = Files.readString(TEMPLATE);
            packFingerprint = fingerprint();
            String next =
                originalTemplate
                    .replace(
                        "x=\"30\" y=\"81\" width=\"240\" height=\"90\" gap=\"12\"",
                        "x=\"39\" y=\"90\" width=\"216\" height=\"72\" gap=\"18\"")
                    .replace("width=\"72\" symbols=", "width=\"60\" symbols=")
                    .replace("x=\"288\" y=\"63\" width=\"39\"", "x=\"294\" y=\"63\" width=\"33\"");
            if (next.equals(originalTemplate)) throw new IllegalStateException("No template edit");
            Files.writeString(TEMPLATE, next);
            mc.getConnection().sendCommand("dui reload");
          }
          case "WAIT_LAYOUT" -> {
            var reel = layout().getAsJsonArray("effects").get(0).getAsJsonObject();
            if (reel.get("x").getAsInt() != 39) return;
            if (reel.get("width").getAsInt() != 60 || reel.get("height").getAsInt() != 72)
              throw new IllegalStateException("Template geometry");
            if (!packFingerprint.equals(fingerprint()))
              throw new IllegalStateException("Template required resource-pack rebuild");
          }
          case "RESTORE_TEMPLATE" -> {
            Files.writeString(TEMPLATE, originalTemplate);
            originalTemplate = null;
            mc.getConnection().sendCommand("dui reload");
          }
          case "WAIT_RESTORED" -> {
            var data = layout();
            if (data.getAsJsonArray("effects").get(0).getAsJsonObject().get("x").getAsInt() != 30
                || !data.get("focusOutlineHidden").getAsBoolean()) return;
          }
          case "NATIVE_FOCUS_TEMPLATE" -> {
            originalTemplate = Files.readString(TEMPLATE);
            Files.writeString(
                TEMPLATE,
                originalTemplate.replace("<dui-menu ", "<dui-menu focus-outline=\"native\" "));
            mc.getConnection().sendCommand("dui reload");
          }
          case "WAIT_NATIVE_FOCUS" -> {
            if (layout().get("focusOutlineHidden").getAsBoolean()) return;
          }
          case "REOPEN" -> mc.getConnection().sendCommand("slots compact");
          case "LEDGER" -> {
            var state = layout().getAsJsonObject("state");
            if (state.get("pending").getAsBoolean()
                || state.get("chips").getAsLong()
                    != balance - state.get("wager").getAsInt() + state.get("payout").getAsInt())
              throw new IllegalStateException("Closed spin settlement");
          }
          case "BET" -> {
            if (layout().getAsJsonObject("state").get("betIndex").getAsInt() < 0)
              throw new IllegalStateException("Bet");
          }
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
                      .filter(w -> w.getMessage().getString().equals("Leave arcade"))
                      .findFirst()
                      .orElseThrow());
          case "WAIT_CLOSED" -> {
            if (ticks - changed < 205) return;
            if (mc.gui.screen() instanceof DialogScreen<?>)
              throw new IllegalStateException("Slot task reopened the dialog");
          }
          default -> hit(mc, step);
        }
      advance();
    } catch (Exception e) {
      fail(mc, e);
    }
  }

  private void fail(Minecraft mc, Exception e) {
    try {
      if (originalTemplate != null) {
        Files.writeString(TEMPLATE, originalTemplate);
        if (mc.getConnection() != null) mc.getConnection().sendCommand("dui reload");
      }
    } catch (Exception restore) {
      e.addSuppressed(restore);
    }
    System.err.println(
        "SLOT_TEST_FAILED stage="
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
    System.out.println("SLOT_TEST_STAGE " + stage);
  }

  private static String fingerprint() throws Exception {
    return HexFormat.of()
        .formatHex(
            java.security.MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(PACK)));
  }

  private static JsonObject layout() throws Exception {
    return JsonParser.parseString(Files.readString(Paths.plugin().resolve("layouts/SlotTest.json")))
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

  private static void validate(Minecraft mc) throws Exception {
    var data = layout();
    var w = canvas(mc);
    int width = data.get("width").getAsInt(), height = data.get("height").getAsInt();
    double x = w.getX() + (w.getWidth() - width - 2) / 2.0;
    int textWidth =
        mc.font.split(w.getMessage(), width + 4).stream()
            .mapToInt(mc.font::width)
            .max()
            .orElseThrow();
    if (textWidth != width + 2 || w.getHeight() != height + 8)
      throw new IllegalStateException(
          "Text flow mismatch: "
              + textWidth
              + "x"
              + w.getHeight()
              + ", expected "
              + (width + 2)
              + "x"
              + (height + 8));
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
    meta.addProperty("focused", w.isFocused());
    meta.addProperty("widgetX", w.getX());
    meta.addProperty("widgetY", w.getY());
    meta.addProperty("widgetWidth", w.getWidth());
    meta.addProperty("widgetHeight", w.getHeight());
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
