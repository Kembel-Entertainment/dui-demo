package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import gg.kembel.dui.demo.e2e.mixin.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.input.MouseButtonInfo;

final class RealClientHarness {
  private RealClientHarness() {}
  static void detachHardware() {
    for (int type : new int[] {org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_MOTION,
        org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN, org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP,
        org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_WHEEL, org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_DOWN,
        org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_UP, org.lwjgl.sdl.SDLEvents.SDL_EVENT_TEXT_INPUT})
      org.lwjgl.sdl.SDLEvents.SDL_SetEventEnabled(type, false);
  }

  static List<AbstractWidget> widgets(GuiEventListener parent) {
    var list = new ArrayList<AbstractWidget>();
    if (parent instanceof AbstractWidget w) list.add(w);
    if (parent instanceof ContainerEventHandler c)
      for (var child : c.children()) list.addAll(widgets(child));
    return list;
  }

  static FocusableTextWidget canvas(Minecraft mc) {
    return widgets(mc.gui.screen()).stream()
        .filter(w -> w instanceof FocusableTextWidget)
        .map(w -> (FocusableTextWidget) w)
        .max(Comparator.comparingInt(AbstractWidget::getHeight))
        .orElseThrow();
  }

  static void move(Minecraft mc, double x, double y) {
    var w = mc.getWindow();
    double px = x * w.getScreenWidth() / w.getGuiScaledWidth();
    double py = y * w.getScreenHeight() / w.getGuiScaledHeight();
    ((FixtureMouseAccess) mc.mouseHandler)
        .dui$move(
            w.handle(),
            px, py, px - mc.mouseHandler.xpos(), py - mc.mouseHandler.ypos());
  }

  static void clickAt(Minecraft mc, double x, double y) {
    move(mc, x, y);
    var mouse = (FixtureMouseAccess) mc.mouseHandler;
    var button = new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0);
    mouse.dui$button(mc.getWindow().handle(), button, 1);
    mouse.dui$button(mc.getWindow().handle(), button, 0);
    move(mc, 5, 5);
  }

  static void hit(Minecraft mc, JsonObject data, String id) {
    var w = canvas(mc);
    var hit =
        data.getAsJsonArray("hits").asList().stream()
            .map(JsonElement::getAsJsonObject)
            .filter(h -> h.get("id").getAsString().equals(id))
            .findFirst()
            .orElseThrow();
    clickAt(
        mc,
        w.getX()
            + (w.getWidth() - data.get("width").getAsInt() - 2) / 2.0
            + hit.get("x").getAsInt()
            + hit.get("width").getAsInt() / 2.0,
        w.getY() + w.getPadding() + hit.get("y").getAsInt() + hit.get("height").getAsInt() / 2.0);
  }

  static void snapshot(Minecraft mc, Path out, String name, JsonObject data) throws Exception {
    var w = canvas(mc);
    var meta = new JsonObject();
    meta.addProperty("screenWidth", mc.getWindow().getGuiScaledWidth());
    meta.addProperty("screenHeight", mc.getWindow().getGuiScaledHeight());
    meta.addProperty("scale", mc.getWindow().getGuiScale());
    meta.addProperty("guiSetting", mc.options.guiScale().get());
    meta.addProperty("canvasX", w.getX() + (w.getWidth() - data.get("width").getAsInt() - 2) / 2.0);
    meta.addProperty("canvasY", w.getY() + w.getPadding());
    meta.add("layout", data);
    Files.createDirectories(out.resolve("screenshots"));
    Files.writeString(out.resolve("screenshots/" + name + ".json"), meta.toString());
    Screenshot.takeScreenshot(
        mc.gameRenderer.mainRenderTarget(),
        img -> {
          try (img) {
            img.writeToFile(out.resolve("screenshots/" + name + ".png"));
          } catch (Exception ex) {
            throw new IllegalStateException(ex);
          }
        });
  }
}
