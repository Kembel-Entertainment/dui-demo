package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import gg.kembel.dui.demo.e2e.mixin.FixtureMouseAccess;
import net.minecraft.client.input.MouseButtonInfo;
import java.nio.file.Files;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Vanilla rendering and real logical inputs/native dialog callbacks; no custom browser renderer. */
final class BrowserClient {
  private int ticks, stage, changed, originalSlot;
  private String inventory;
  private final String fixture = System.getProperty("dui.e2e.browser.fixture");
  void initialize() { ClientTickEvents.END_CLIENT_TICK.register(this::tick); }
  private void step() { stage++; changed = ticks; System.out.println("BROWSER_TEST_STAGE " + stage); }
  private JsonObject stats(Minecraft mc) throws Exception {
    return JsonParser.parseString(Files.readString(Paths.plugin().resolve("layouts/" + mc.player.getUUID() + "-browser.json"))).getAsJsonObject();
  }
  private String title(Minecraft mc) throws Exception { return stats(mc).getAsJsonObject("state").get("title").getAsString(); }
  private static String inventory(Minecraft mc) {
    var result = new StringBuilder();
    for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) result.append(mc.player.getInventory().getItem(i)).append(';');
    return result.toString();
  }
  private void shot(Minecraft mc, String name) {
    Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
      try (image) { image.writeToFile(Paths.output().resolve(name + ".png")); }
      catch (Exception e) { throw new RuntimeException(e); }
    });
  }
  private void label(Minecraft mc, String label) {
    var widget = RealClientHarness.widgets(mc.gui.screen()).stream().filter(w -> w.getMessage().getString().equals(label)).findFirst().orElseThrow();
    RealClientHarness.clickAt(mc, widget.getX() + widget.getWidth() / 2d, widget.getY() + widget.getHeight() / 2d);
  }
  private static void button(Minecraft mc, int number) {
    var mouse = (FixtureMouseAccess) mc.mouseHandler;
    var info = new MouseButtonInfo(number, 0);
    mouse.dui$button(mc.getWindow().handle(), info, 1);
    mouse.dui$button(mc.getWindow().handle(), info, 0);
  }
  private static void menuHit(Minecraft mc, int x, int y) {
    var canvas = RealClientHarness.canvas(mc);
    RealClientHarness.clickAt(mc, canvas.getX() + (canvas.getWidth() - 342 - 2) / 2d + x,
        canvas.getY() + canvas.getPadding() + y);
  }
  private void tick(Minecraft mc) {
    ticks++; mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    try {
      if (ticks > 1800) throw new IllegalStateException("Browser timeout at stage " + stage);
      if (mc.gui.overlay() != null || ticks - changed < 20) return;
      if (stage == 0 && ticks > 60) {
        mc.options.pauseOnLostFocus = false; mc.options.framerateLimit().set(60); mc.options.guiScale().set(2); mc.resizeGui();
        var server = new ServerData("Browser fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null); step();
      } else if (stage == 1 && mc.player != null && mc.gui.screen() instanceof DialogScreen<?>) {
        inventory = inventory(mc); originalSlot = mc.player.getInventory().getSelectedSlot();
        if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        mc.player.connection.sendCommand("browser"); step();
      } else if (stage == 2 && mc.player.isPassenger() && mc.gui.screen() == null && title(mc).equals("First")) {
        if (stats(mc).getAsJsonObject("source").get("frames").getAsLong() < 5) return;
        shot(mc, "browser"); button(mc, 0); step();
      } else if (stage == 3 && title(mc).equals("Clicked")) {
        // Inject the ordinary GLFW focus callback: unattended macOS test windows can be inactive.
        ((gg.kembel.dui.demo.e2e.mixin.FixtureWindowAccess) (Object) mc.getWindow())
            .dui$focus(mc.getWindow().handle(), true);
        var mouse = (FixtureMouseAccess) mc.mouseHandler;
        double x = mc.mouseHandler.xpos(), y = mc.mouseHandler.ypos();
        mouse.dui$move(mc.getWindow().handle(), x, y); // Prime Vanilla's first-move suppression after grab.
        mouse.dui$move(mc.getWindow().handle(), x + 80, y + 20);
        mc.mouseHandler.handleAccumulatedMovement();
        System.out.println("BROWSER_MOUSE_START yaw=" + mc.player.getYRot() + " pitch=" + mc.player.getXRot()); step();
      } else if (stage == 4 && ticks - changed >= 20) {
        if (stats(mc).getAsJsonObject("cursor").get("x").getAsInt() < 680) throw new IllegalStateException("Cursor did not move: " + stats(mc).getAsJsonObject("cursor") + " yaw=" + mc.player.getYRot() + " pitch=" + mc.player.getXRot());
        button(mc, 1); step();
      } else if (stage == 5 && mc.gui.screen() instanceof DialogScreen<?>) {
        if (mc.player.isPassenger()) throw new IllegalStateException("Seat leaked into browser menu");
        if (mc.gui.hud.isHidden()) mc.gui.hud.toggle(); shot(mc, "controls");
        var address = RealClientHarness.widgets(mc.gui.screen()).stream().filter(w -> w instanceof EditBox e && e.getValue().startsWith("http"))
            .map(w -> (EditBox) w).findFirst().orElseThrow();
        address.setValue(fixture + "second"); label(mc, "Open / Search"); step();
      } else if (stage == 6 && mc.player.isPassenger() && mc.gui.screen() == null && title(mc).equals("Second")) {
        if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); shot(mc, "second");
        button(mc, 1); step();
      } else if (stage == 7 && mc.gui.screen() instanceof DialogScreen<?>) {
        menuHit(mc, 64, 90); step();
      } else if (stage == 8 && mc.player.isPassenger() && stats(mc).getAsJsonObject("state").get("url").getAsString().equals(fixture)) {
        button(mc, 1); step();
      } else if (stage == 9 && mc.gui.screen() instanceof DialogScreen<?>) {
        menuHit(mc, 170, 90); step();
      } else if (stage == 10 && mc.player.isPassenger() && mc.gui.screen() == null && title(mc).equals("Second")) {
        ((FixtureMouseAccess) mc.mouseHandler).dui$scroll(mc.getWindow().handle(), 0, -1); step();
      } else if (stage == 11 && title(mc).startsWith("Scrolled:")) {
        shot(mc, "scrolled"); button(mc, 1); step();
      } else if (stage == 12 && mc.gui.screen() instanceof DialogScreen<?>) {
        menuHit(mc, 277, 171); step();
      } else if (stage == 13 && mc.player.isPassenger() && mc.gui.screen() == null
          && stats(mc).getAsJsonObject("state").get("zoomPercent").getAsInt() == 125) {
        shot(mc, "zoomed"); button(mc, 1); step();
      } else if (stage == 14 && mc.gui.screen() instanceof DialogScreen<?>) {
        label(mc, "Close browser"); step();
      } else if (stage == 15 && !mc.player.isPassenger() && stats(mc).get("closed").getAsBoolean()) {
        if (!inventory.equals(inventory(mc)) || mc.getCameraEntity() != mc.player || originalSlot != mc.player.getInventory().getSelectedSlot())
          throw new IllegalStateException("Player state was not restored");
        var result = new JsonObject(); result.addProperty("passed", true); result.addProperty("muted", true);
        result.addProperty("inventoryUnchanged", true); result.addProperty("cursorVerified", true); result.addProperty("clickVerified", true);
        result.addProperty("menuNavigationVerified", true); result.addProperty("historyVerified", true); result.addProperty("scrollVerified", true);
        result.addProperty("cleanupVerified", true); result.addProperty("steps", 15);
        result.addProperty("mouseMotionVerified", true); result.addProperty("mouseClickVerified", true);
        result.addProperty("rightClickMenuVerified", true); result.addProperty("mouseWheelVerified", true); result.addProperty("zoomVerified", true);
        result.addProperty("hdViewportVerified", stats(mc).getAsJsonObject("state").get("width").getAsInt() == 1280);
        Files.writeString(Paths.output().resolve("client-result.json"), result.toString());
        System.out.println("BROWSER_TEST_COMPLETE " + result); stage = 16; mc.stop();
      }
    } catch (Exception e) {
      try { Files.writeString(Paths.output().resolve("failure.txt"), e.toString()); } catch (Exception ignored) {}
      e.printStackTrace(); System.out.println("BROWSER_TEST_FAILED " + e); mc.stop(); stage = 16;
    }
  }
}
