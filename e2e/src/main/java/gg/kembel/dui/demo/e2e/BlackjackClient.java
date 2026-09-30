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
 * Real clicks and GPU card flight/flip checks; scripted deals are explicitly labelled demo actions.
 */
final class BlackjackClient {
  private static final Path OUT = Paths.output();
  private static final String[] STEPS = {
    "SHOT:lobby",
    "blackjack_demo",
    "EVENT:deal:deal-early:8",
    "EVENT:deal:deal-late:35",
    "WAIT_PLAYER",
    "SHOT:player",
    "HOLE",
    "blackjack_split",
    "EVENT:split:split-early:7",
    "EVENT:split:split-late:28",
    "WAIT_PLAYER",
    "SHOT:split",
    "blackjack_double",
    "EVENT:hit:double-early:7",
    "EVENT:hit:double-late:14",
    "WAIT_PLAYER",
    "blackjack_hit",
    "EVENT:hit:hit-early:7",
    "EVENT:hit:hit-late:14",
    "WAIT_PLAYER",
    "SHOT:second-hand",
    "blackjack_stand",
    "EVENT:reveal:flip-early:7",
    "EVENT:reveal:flip-late:14",
    "EVENT:dealer:dealer-early:7",
    "EVENT:dealer:dealer-late:14",
    "EVENT:payout:payout-early:7",
    "EVENT:payout:payout-late:14",
    "WAIT_RESULT",
    "LEDGER:10150",
    "SHOT:result",
    "blackjack_demo",
    "WAIT_RESULT",
    "LEDGER:10225",
    "SHOT:natural",
    "blackjack_motion",
    "SHOT:motion-off",
    "SHOT:still-frame",
    "SAME_SCREEN",
    "blackjack_demo",
    "WAIT_PLAYER",
    "blackjack_stand",
    "WAIT_RESULT",
    "LEDGER:10275",
    "SHOT:soft17",
    "blackjack_deal",
    "AUTO_PLAY",
    "SHOT:shuffled",
    "SMALL_WINDOW",
    "SHOT:compact",
    "blackjack_motion",
    "blackjack_demo",
    "EVENT:deal:compact-early:7",
    "EVENT:deal:compact-late:34",
    "WAIT_PLAYER",
    "blackjack_stand",
    "EVENT:reveal:close-flip:7",
    "ESCAPE",
    "WAIT_CLOSED",
    "REOPEN",
    "SHOT:after-close",
    "AUTO_WINDOW",
    "SHOT:auto",
    "CLOSE",
    "WAIT_CLOSED"
  };
  private int ticks, stage, changed, eventAt, receivedAt;
  private String eventName = "", inventory;
  private Screen previous, receivedScreen;

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    if (ticks > 6500 || stage > 0 && ticks - changed > 1000) {
      fail(
          mc,
          new IllegalStateException(
              "Timeout stage="
                  + stage
                  + " step="
                  + (stage >= 2 && stage < STEPS.length + 2 ? STEPS[stage - 2] : "startup")));
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
        var server = new ServerData("Blackjack fixture", "127.0.0.1:25584", ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        advance();
        return;
      }
      if (stage == 1) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        inventory = mc.player.getInventory().getNonEquipmentItems().toString();
        mc.getConnection().sendCommand("dui blackjack spacious");
        advance();
        return;
      }
      if (stage == 2 && ticks - changed < 180) return;
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
                        "exactLedgerVerified",
                        true,
                        "holeCardPrivate",
                        true)));
        System.out.println("BLACKJACK_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2) return;
      String step = STEPS[stage - 2];
      if (step.startsWith("SHOT:")) {
        if (ticks - changed < 35 || !received(mc)) return;
        snapshot(mc, step.substring(5));
        if (step.equals("SHOT:motion-off")) previous = mc.gui.screen();
      } else if (step.startsWith("EVENT:")) {
        if (!received(mc)) return;
        var parts = step.split(":");
        String now = layout().get("event").getAsString();
        if (!now.equals(parts[1])) return;
        if (!eventName.equals(now)) {
          eventName = now;
          eventAt = ticks;
        }
        if (ticks - eventAt < Integer.parseInt(parts[3])) return;
        snapshot(mc, parts[2]);
        if (parts[2].endsWith("early")) previous = mc.gui.screen();
        else if (parts[2].endsWith("late") && previous != mc.gui.screen())
          throw new IllegalStateException("Animation replaced dialog per frame");
      } else if (step.startsWith("LEDGER:")) {
        if (!received(mc)) return;
        if (layout().get("balanceHalf").getAsInt() != Integer.parseInt(step.substring(7)))
          throw new IllegalStateException("Incorrect blackjack ledger: " + layout());
      } else
        switch (step) {
          case "WAIT_PLAYER" -> {
            if (!received(mc)
                || !layout().get("phase").getAsString().equals("PLAYER")
                || layout().get("busy").getAsBoolean()) return;
          }
          case "WAIT_RESULT" -> {
            if (!received(mc)
                || !layout().get("phase").getAsString().equals("RESULT")
                || layout().get("busy").getAsBoolean()) return;
          }
          case "AUTO_PLAY" -> {
            if (!received(mc) || layout().get("busy").getAsBoolean()) return;
            String phase = layout().get("phase").getAsString();
            if (phase.equals("PLAYER")) {
              hit(mc, "blackjack_stand");
              changed = ticks;
              return;
            }
            if (!phase.equals("RESULT")) return;
            if (layout().get("demo").getAsBoolean())
              throw new IllegalStateException("Ordinary deal is scripted");
          }
          case "HOLE" -> {
            var data = layout();
            if (data.getAsJsonArray("dealer").get(1).getAsInt() != -1)
              throw new IllegalStateException("Hole card leaked");
            var es = data.getAsJsonArray("effects");
            boolean found = false;
            for (var e : es)
              if (e.getAsJsonObject().get("id").getAsString().equals("dealer_1")) {
                found = true;
                if ((e.getAsJsonObject().get("parameter0").getAsInt() & 63) != 63)
                  throw new IllegalStateException("Transport leaks private card");
              }
            if (!found) throw new IllegalStateException("Hole card missing");
          }
          case "SAME_SCREEN" -> {
            if (previous != mc.gui.screen())
              throw new IllegalStateException("Dialog replaced per frame");
          }
          case "SMALL_WINDOW" -> {
            mc.getWindow().setWindowed(640, 480);
            mc.options.guiScale().set(2);
            mc.resizeGui();
            mc.getConnection().sendCommand("dui blackjack compact");
          }
          case "AUTO_WINDOW" -> {
            mc.getWindow().setWindowed(1440, 1000);
            mc.options.guiScale().set(0);
            mc.resizeGui();
          }
          case "REOPEN" -> mc.getConnection().sendCommand("dui blackjack compact");
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
            if (ticks - changed < 150) return;
            if (mc.gui.screen() instanceof DialogScreen<?>)
              throw new IllegalStateException("Timer reopened a closed table");
          }
          default -> {
            if (!received(mc)) return;
            hit(mc, step);
            eventName = "";
            eventAt = ticks;
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
    System.out.println("BLACKJACK_TEST_STAGE " + stage);
  }

  private void fail(Minecraft mc, Exception e) {
    System.err.println("BLACKJACK_TEST_FAILED stage=" + stage);
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
            Files.readString(Paths.plugin().resolve("layouts/BlackjackTest.json")))
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
    validate(mc);
    var data = layout();
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
