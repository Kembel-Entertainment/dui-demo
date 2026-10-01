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

/** Real clicks, item carriers and QR screenshots. Never accepts the external browser prompt. */
final class ShopClient {
  private static final Path OUT = Paths.output();
  private static final String[] STEPS = {
    "SHOT:store-empty",
    "shop_checkout",
    "STATE:checkout:false",
    "shop_add_light",
    "shop_add_library",
    "TOTAL:3000",
    "SHOT:store-cart",
    "shop_add_light",
    "TOTAL:4200",
    "shop_remove_light",
    "TOTAL:3000",
    "shop_checkout",
    "STATE:checkout:true",
    "SHOT:checkout",
    "shop_link",
    "LINK_PROMPT",
    "CANCEL_LINK",
    "shop_back",
    "TOTAL:3000",
    "shop_kits",
    "SHOT:kits",
    "shop_add_garden",
    "shop_objects",
    "shop_add_lantern",
    "shop_add_pot",
    "shop_add_case",
    "shop_cart_next",
    "SHOT:cart-page-2",
    "shop_remove_pot",
    "REMOVE_FIRST",
    "REMOVE_FIRST",
    "REMOVE_FIRST",
    "REMOVE_FIRST",
    "REMOVE_FIRST",
    "TOTAL:0",
    "SHOT:cart-cleared",
    "SMALL_WINDOW",
    "SHOT:compact-store",
    "shop_add_lantern",
    "shop_next",
    "shop_add_pot",
    "shop_next",
    "shop_add_light",
    "shop_cart_next",
    "SHOT:compact-cart-page-2",
    "shop_checkout",
    "SHOT:compact-checkout",
    "shop_link",
    "LINK_PROMPT",
    "CANCEL_LINK",
    "shop_back",
    "AUTO_WINDOW",
    "SHOT:auto-store",
    "shop_checkout",
    "SHOT:auto-checkout",
    "shop_link",
    "LINK_PROMPT",
    "CANCEL_LINK",
    "CLOSE"
  };
  private int ticks, stage, changed;
  private String before;

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    if (ticks > 6000 || stage > 0 && ticks - changed > 700) {
      fail(mc, new IllegalStateException("Timeout stage=" + stage));
      return;
    }
    if (mc.gui.overlay() != null || ticks - changed < 12) return;
    try {
      if (stage == 0 && ticks > 80) {
        Files.createDirectories(OUT.resolve("screenshots"));
        mc.options.guiScale().set(Paths.referenceScale(mc));
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var server = new ServerData("Shop fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        advance();
        return;
      }
      if (stage == 1) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        before = mc.player.getInventory().getNonEquipmentItems().toString();
        mc.getConnection().sendCommand("uishop spacious");
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
                        true)));
        System.out.println("SHOP_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2) return;
      String step = STEPS[stage - 2];
      if (step.startsWith("SHOT:") && ticks - changed < 40) return;
      if (step.startsWith("SHOT:")) snapshot(mc, step.substring(5));
      else if (step.startsWith("STATE:")) {
        var parts = step.split(":", 3);
        if (!layout().getAsJsonObject("state").get(parts[1]).getAsString().equals(parts[2]))
          throw new IllegalStateException(step);
      } else if (step.startsWith("TOTAL:")) {
        if (layout().get("total").getAsInt() != Integer.parseInt(step.substring(6)))
          throw new IllegalStateException(step);
      } else
        switch (step) {
          case "LINK_PROMPT" -> {
            if (!(mc.gui.screen() instanceof ConfirmLinkScreen))
              throw new IllegalStateException(
                  "Expected vanilla link confirmation: " + mc.gui.screen());
          }
          case "CANCEL_LINK" ->
              click(
                  mc,
                  widgets(mc.gui.screen()).stream()
                      .filter(w -> List.of("Cancel", "No").contains(w.getMessage().getString()))
                      .findFirst()
                      .orElseThrow());
          case "SMALL_WINDOW" -> {
            mc.getWindow().setWindowed(640, 480);
            mc.options.guiScale().set(Paths.referenceScale(mc));
            mc.resizeGui();
            mc.getConnection().sendCommand("uishop compact");
          }
          case "AUTO_WINDOW" -> {
            mc.getWindow().setWindowed(1440, 1000);
            mc.options.guiScale().set(0);
            mc.resizeGui();
          }
          case "REMOVE_FIRST" -> {
            String id = null;
            for (var hit : layout().getAsJsonArray("hits"))
              if (hit.getAsJsonObject().get("action").getAsString().equals("shop_remove")) {
                id = hit.getAsJsonObject().get("id").getAsString();
                break;
              }
            if (id == null) throw new IllegalStateException("No removable entry");
            hit(mc, id);
          }
          case "CLOSE" ->
              click(
                  mc,
                  widgets(mc.gui.screen()).stream()
                      .filter(w -> w.getMessage().getString().equals("Close store"))
                      .findFirst()
                      .orElseThrow());
          default -> hit(mc, step);
        }
      advance();
    } catch (Exception e) {
      fail(mc, e);
    }
  }

  private void fail(Minecraft mc, Exception e) {
    System.err.println(
        "SHOP_TEST_FAILED stage="
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
    System.out.println("SHOP_TEST_STAGE " + stage);
  }

  private static JsonObject layout() throws Exception {
    return JsonParser.parseString(Files.readString(Paths.plugin().resolve("layouts/ShopTest.json")))
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
