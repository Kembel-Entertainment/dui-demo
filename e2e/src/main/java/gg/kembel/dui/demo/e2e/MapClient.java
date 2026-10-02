package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import gg.kembel.dui.demo.e2e.mixin.FixtureMouseAccess;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.sounds.SoundSource;

/** Exercises vanilla rendering and hit testing; the mod adds no map rendering or input protocol. */
final class MapClient {
  private record Step(String label, int delay, java.util.function.Consumer<Minecraft> action) {}

  private final List<Step> steps = new ArrayList<>();
  private Integer serverInventory;
  private int ticks, stage, changed;
  private float reference;
  private double teleportX;
  private boolean physicalInputDetached;
  private String originalHud;
  private int hudSeat, hudInput, hudDisplay;
  private String hudPack;
  private final Set<Integer> lastEntityIds = new HashSet<>();

  void initialize() {

    steps.add(
        new Step(
            "open_rectangle",
            10,
            mc -> {
              requireAppearance(mc, false);
              reference = 175;
              mc.player.getInventory().setSelectedSlot(0);
              mc.gameMode.tick();
              mc.player.setYRot(reference);
              mc.player.setXRot(0);
              mc.getConnection()
                  .send(
                      new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(
                          reference, 0, mc.player.onGround(), false));
              command(mc, "worldmap debug");
            }));
    steps.add(
        new Step(
            "rectangle_center",
            160,
            mc -> {
              requireView("CALIBRATION");
              requireAppearance(mc, true);
              shot(mc, "01-rectangle-center");
            }));
    steps.add(new Step("pan_rectangle", 20, mc -> aim(mc, 12, -8)));
    steps.add(new Step("rectangle_pan", 30, mc -> shot(mc, "02-rectangle-pan")));
    steps.add(
        new Step(
            "rectangle_fov30",
            20,
            mc -> {
              aim(mc, 0, 0);
              mc.options.fov().set(30);
            }));
    steps.add(new Step("rectangle_fov30_shot", 30, mc -> shot(mc, "03-rectangle-fov30")));
    steps.add(new Step("rectangle_fov110", 20, mc -> mc.options.fov().set(110)));
    steps.add(new Step("rectangle_fov110_shot", 30, mc -> shot(mc, "04-rectangle-fov110")));
    steps.add(
        new Step(
            "rectangle_gui1",
            20,
            mc -> {
              mc.options.guiScale().set(1);
              mc.resizeGui();
            }));
    steps.add(new Step("rectangle_gui1_shot", 30, mc -> shot(mc, "05-rectangle-gui1")));
    steps.add(
        new Step(
            "rectangle_auto",
            20,
            mc -> {
              mc.options.guiScale().set(0);
              mc.resizeGui();
            }));
    steps.add(new Step("rectangle_auto_shot", 30, mc -> shot(mc, "06-rectangle-auto")));
    steps.add(
        new Step(
            "reload_custom_hud",
            10,
            mc -> {
              try {
                var path = Paths.plugin().resolve("ui/worldmap-hud.html");
                originalHud = Files.readString(path);
                var state = lastState();
                hudSeat = state.get("seat").getAsInt();
                hudInput = state.get("input").getAsInt();
                hudDisplay = state.get("display").getAsInt();
                hudPack = Files.readString(Paths.plugin().resolve("pack/dui.json"));
                Files.writeString(
                    path,
                    """
                    <dui-hud native-hud-color="#101D28">
                      <dui-surface anchor-x="left" anchor-y="top" offset-x="12" offset-y="12"><dui-menu width="120" height="36"><dui-layer>
                        <dui-rect width="fill" height="fill" fill="#FF5EAD"/><dui-text x="6" y="9" width="108" height="18" label="CUSTOM HUD" color="#FFFFFF"/>
                      </dui-layer></dui-menu></dui-surface>
                      <dui-surface anchor-x="center" anchor-y="center"><dui-menu width="120" height="27"><dui-layer>
                        <dui-rect width="fill" height="fill" fill="#3FB4FF"/><dui-text x="6" y="9" width="108" height="9" label="{{zoom}} / LIVE" color="#FFFFFF"/>
                      </dui-layer></dui-menu></dui-surface>
                      <dui-surface anchor-x="right" anchor-y="bottom" offset-x="-12" offset-y="-12"><dui-menu width="180" height="126"><dui-layer>
                        <dui-rect width="fill" height="fill" fill="#A855F7"/><dui-column x="6" y="0" width="168" height="126">
                          <dui-text label="CONTROL ONE" color="#FFFFFF"/><dui-text label="CONTROL TWO" color="#FFFFFF"/>
                          <dui-text label="CONTROL THREE" color="#FFFFFF"/><dui-text label="CONTROL FOUR" color="#FFFFFF"/>
                          <dui-text label="CONTROL FIVE" color="#FFFFFF"/><dui-text label="CONTROL SIX" color="#FFFFFF"/>
                          <dui-text label="CONTROL SEVEN" color="#FFFFFF"/>
                        </dui-column>
                      </dui-layer></dui-menu></dui-surface>
                    </dui-hud>
                    """);
                command(mc, "dui reload");
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
            }));
    steps.add(
        new Step(
            "custom_hud_applied",
            30,
            mc -> {
              requireSameHudTransport();
              mc.gui.hud.getChat().clearMessages(false);
            }));
    steps.add(
        new Step(
            "custom_hud_screenshot",
            4,
            mc -> {
              shot(mc, "08-hud-template-custom");
              try {
                Files.writeString(
                    Paths.plugin().resolve("ui/worldmap-hud.html"), "<dui-hud invalid='true'/>");
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
              command(mc, "dui reload");
            }));
    steps.add(
        new Step(
            "invalid_hud_keeps_previous",
            30,
            mc -> {
              requireSameHudTransport();
              mc.gui.hud.getChat().clearMessages(false);
            }));
    steps.add(
        new Step(
            "invalid_hud_screenshot",
            4,
            mc -> {
              shot(mc, "09-hud-template-invalid-retained");
              try {
                Files.writeString(Paths.plugin().resolve("ui/worldmap-hud.html"), originalHud);
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
              command(mc, "dui reload");
            }));
    steps.add(
        new Step(
            "hud_template_restored",
            30,
            mc -> {
              if (lastState().get("hudSurfaces").getAsInt() != 4)
                throw new IllegalStateException("Original HUD did not restore");
              mc.gui.hud.getChat().clearMessages(false);
            }));
    steps.add(
        new Step(
            "restored_hud_screenshot",
            4,
            mc -> {
              shot(mc, "09-hud-template-restored");
            }));
    steps.add(
        new Step(
            "close_rectangle",
            20,
            mc -> {
              command(mc, "worldmap close");
              mc.options.fov().set(70);
              mc.options.guiScale().set(2);
              mc.resizeGui();
            }));
    steps.add(
        new Step(
            "appearance_restored",
            20,
            mc -> {
              requireAppearance(mc, false);
              shot(mc, "07-appearance-restored");
            }));
    {
      steps.add(
          new Step(
              "open_map",
              35,
              mc -> {
                reference = mc.player.getYRot();
                command(mc, "worldmap");
              }));
      steps.add(
          new Step(
              "map_overview",
              100,
              mc -> {
                requireAppearance(mc, true);
                shot(mc, "10-overview");
              }));
      steps.add(new Step("hover_locked", 20, mc -> aim(mc, 20, 10)));
      steps.add(new Step("locked_click", 8, mc -> button(mc, false)));
      steps.add(
          new Step(
              "locked_stays_overview",
              30,
              mc -> {
                requireView("OVERVIEW");
                shot(mc, "11-locked");
              }));
      steps.add(new Step("aim_sylvan", 20, mc -> aim(mc, -20, 10)));
      steps.add(
          new Step(
              "dwell_detail",
              25,
              mc -> {
                requireView("DETAIL");
                shot(mc, "12-detail-dwell");
              }));
      steps.add(new Step("right_back", 10, mc -> button(mc, true)));
      steps.add(new Step("back_requires_reentry", 30, mc -> requireView("OVERVIEW")));
      steps.add(new Step("leave_target", 5, mc -> aim(mc, 0, 0)));
      steps.add(new Step("aim_cloud", 8, mc -> aim(mc, 0, -10)));
      steps.add(new Step("click_cloud", 2, mc -> button(mc, false)));
      steps.add(
          new Step(
              "click_detail",
              10,
              mc -> {
                requireView("DETAIL");
                shot(mc, "13-detail-click");
              }));
      steps.add(new Step("right_back_again", 5, mc -> button(mc, true)));
      steps.add(
          new Step(
              "scroll_wrap_0_to_8",
              20,
              mc -> {
                aim(mc, 0, 0);
                mc.player.getInventory().setSelectedSlot(0);
                mc.gameMode.tick();
              }));
      steps.add(
          new Step(
              "scroll_wrap",
              5,
              mc ->
                  ((FixtureMouseAccess) mc.mouseHandler)
                      .dui$scroll(mc.getWindow().handle(), 0, 1)));
      steps.add(
          new Step(
              "wrap_result",
              10,
              mc -> {
                var s = lastState();
                if (s.get("zoom").getAsInt() != 11)
                  throw new IllegalStateException("0→8 should zoom -1: " + s);
                shot(mc, "14-zoom");
              }));
      steps.add(
          new Step(
              "scroll_restore",
              5,
              mc ->
                  ((FixtureMouseAccess) mc.mouseHandler)
                      .dui$scroll(mc.getWindow().handle(), 0, -1)));
      steps.add(
          new Step(
              "scroll_fast",
              5,
              mc -> {
                for (int i = 0; i < 3; i++)
                  ((FixtureMouseAccess) mc.mouseHandler).dui$scroll(mc.getWindow().handle(), 0, -1);
              }));
      steps.add(
          new Step(
              "rapid_zoom",
              10,
              mc -> {
                if (lastState().get("zoom").getAsInt() != 15)
                  throw new IllegalStateException("Fast wheel did not accumulate");
              }));
      steps.add(new Step("aim_cloud_for_mode", 5, mc -> aim(mc, 0, -10)));
      steps.add(new Step("detail_for_mode", 25, mc -> requireView("DETAIL")));
      steps.add(new Step("aim_solo", 5, mc -> aim(mc, 0, -22.5f)));
      steps.add(new Step("choose_solo", 10, mc -> button(mc, false)));
      steps.add(
          new Step(
              "mode_closes",
              20,
              mc -> {
                if (mc.player.getVehicle() != null)
                  throw new IllegalStateException("Mode selection did not close");
                command(mc, "worldmap");
              }));
      steps.add(new Step("wait_reopen", 80, mc -> requireView("OVERVIEW")));
      steps.add(new Step("right_close", 5, mc -> button(mc, true)));
      steps.add(
          new Step(
              "closed_unmounted",
              25,
              mc -> {
                if (mc.player.getVehicle() != null) throw new IllegalStateException("Seat leaked");
                requireAppearance(mc, false);
              }));
      steps.add(new Step("reopen_for_dismount", 5, mc -> command(mc, "worldmap still")));
      steps.add(new Step("shift_dismount", 80, mc -> mc.options.keyShift.setDown(true)));
      steps.add(
          new Step(
              "dismounted",
              20,
              mc -> {
                mc.options.keyShift.setDown(false);
                if (mc.player.getVehicle() != null)
                  throw new IllegalStateException("Dismount failed");
                requireAppearance(mc, false);
              }));
      steps.add(new Step("external_teleport_open", 5, mc -> command(mc, "worldmap")));
      steps.add(
          new Step(
              "external_teleport",
              80,
              mc -> {
                teleportX = mc.player.getX() + 3;
                command(mc, "worldmap fixtureteleport");
              }));
      steps.add(
          new Step(
              "external_teleport_no_snapback",
              25,
              mc -> {
                if (mc.player.getVehicle() != null || Math.abs(mc.player.getX() - teleportX) > .2)
                  throw new IllegalStateException("External teleport cleanup failed");
                requireAppearance(mc, false);
              }));
      steps.add(new Step("map_for_death", 5, mc -> command(mc, "worldmap still")));
      steps.add(new Step("fixture_death", 80, mc -> command(mc, "worldmap fixturedeath")));
      steps.add(
          new Step(
              "death_cleanup",
              20,
              mc -> {
                if (!lastState().get("closed").getAsBoolean() || mc.player.getVehicle() != null)
                  throw new IllegalStateException("Death did not clean map");
                mc.player.respawn();
              }));
      steps.add(new Step("respawn_restored", 40, mc -> requireAppearance(mc, false)));
      steps.add(new Step("map_to_dialog", 5, mc -> command(mc, "worldmap")));
      steps.add(new Step("open_dialog", 80, mc -> command(mc, "dui acceptance")));
      steps.add(
          new Step(
              "dialog_visible",
              30,
              mc -> {
                if (!(mc.gui.screen()
                    instanceof net.minecraft.client.gui.screens.dialog.DialogScreen<?>))
                  throw new IllegalStateException("Dialog did not replace map");
                requireAppearance(mc, false);
                if (mc.player.getVehicle() != null)
                  throw new IllegalStateException("Map seat leaked on dialog switch");
                try {
                  var data =
                      JsonParser.parseString(
                              Files.readString(Paths.plugin().resolve("layouts/MapTest.json")))
                          .getAsJsonObject();
                  RealClientHarness.snapshot(mc, Paths.output(), "16-dialog-after-map", data);
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }
              }));
      steps.add(new Step("reopen_for_cleanup", 5, mc -> command(mc, "worldmap")));
      steps.add(new Step("sync_masked_inventory", 70, mc -> command(mc, "worldmap fixturesync")));
      steps.add(new Step("sync_stays_masked", 10, mc -> requireAppearance(mc, true)));
      steps.add(
          new Step(
              "disable_plugin",
              10,
              mc -> {
                var state = lastState();
                for (String role : List.of("seat", "input", "display"))
                  lastEntityIds.add(state.get(role).getAsInt());
                command(mc, "worldmap fixturedisable");
              }));
      steps.add(
          new Step(
              "plugin_cleanup",
              30,
              mc -> {
                if (mc.player.isInvisible() || mc.player.getVehicle() != null)
                  throw new IllegalStateException(
                      "Plugin shutdown did not restore client appearance");
                for (int id : lastEntityIds)
                  if (mc.level.getEntity(id) != null)
                    throw new IllegalStateException("Plugin shutdown leaked map entity " + id);
                if (!lastState().get("closed").getAsBoolean())
                  throw new IllegalStateException("Plugin shutdown did not close the demo session");
              }));
      steps.add(new Step("disconnect", 70, mc -> mc.disconnect(new TitleScreen(), false)));
    }
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    if (!physicalInputDetached) {
      long window = mc.getWindow().handle();
      org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(window, null);
      org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback(window, null);
      org.lwjgl.glfw.GLFW.glfwSetScrollCallback(window, null);
      org.lwjgl.glfw.GLFW.glfwSetKeyCallback(window, null);
      physicalInputDetached = true;
    }
    mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
    mc.options.pauseOnLostFocus = false;
    ticks++;
    if (ticks > 5000) {
      System.err.println("MAP_TEST_FAILED timeout stage=" + stage);
      mc.stop();
      return;
    }
    if (mc.gui.overlay() != null) return;
    if (stage == 0) {
      if (ticks < 70) return;
      var data = new ServerData("dui world-map", Paths.server(), ServerData.Type.OTHER);
      data.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
      ConnectScreen.startConnecting(
          new TitleScreen(), mc, ServerAddress.parseString(data.ip), data, false, null);
      mc.getWindow().setWindowed(1280, 900);
      mc.options.guiScale().set(2);
      mc.resizeGui();
      org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
      stage++;
      changed = ticks;
      return;
    }
    if (stage > steps.size()) {
      if (ticks - changed < 60) return;
      try {
        Files.writeString(
            Paths.output().resolve("client-result.json"),
            "{\"passed\":true,\"inventoryUnchanged\":true,\"muted\":true,\"steps\":"
                + steps.size()
                + "}");
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
      System.out.println("MAP_TEST_COMPLETE");
      mc.stop();
      return;
    }
    Step step = steps.get(stage - 1);
    if (ticks - changed < step.delay
        || mc.player == null
        || mc.gui.screen() != null
            && !(mc.gui.screen() instanceof net.minecraft.client.gui.screens.dialog.DialogScreen<?>)
            && !step.label.equals("death_cleanup")) return;
    try {
      System.out.println("MAP_TEST_STEP " + step.label);
      step.action.accept(mc);
      stage++;
      changed = ticks;
    } catch (Exception e) {
      System.err.println("MAP_TEST_FAILED stage=" + stage + " " + step.label);
      e.printStackTrace();
      mc.stop();
    }
  }

  private void aim(Minecraft mc, float yaw, float pitch) {
    mc.player.setYRot(reference + yaw);
    mc.player.setXRot(pitch);
  }

  private static void command(Minecraft mc, String command) {
    mc.getConnection().sendCommand(command);
  }

  private static void button(Minecraft mc, boolean right) {
    System.out.println("MAP_TEST_HIT " + mc.hitResult);
    var mouse = (FixtureMouseAccess) mc.mouseHandler;
    var button =
        new MouseButtonInfo(
            right ? InputConstants.MOUSE_BUTTON_RIGHT : InputConstants.MOUSE_BUTTON_LEFT, 0);
    mouse.dui$button(mc.getWindow().handle(), button, 1);
    mouse.dui$button(mc.getWindow().handle(), button, 0);
  }

  private void shot(Minecraft mc, String name) {
    try {
      Path folder = Paths.output().resolve("screenshots");
      Files.createDirectories(folder);
      var state = lastState();
      if (state.has("inventory")) {
        int current = state.get("inventory").getAsInt();
        if (serverInventory == null) serverInventory = current;
        else if (serverInventory != current)
          throw new IllegalStateException("Server inventory changed while map was open");
      }
      if (state.has("realInvisible") && state.get("realInvisible").getAsBoolean())
        throw new IllegalStateException("Server invisibility was changed");
      var meta = new JsonObject();
      meta.add("layout", state);
      state.addProperty(
          "packSha1",
          JsonParser.parseString(Files.readString(Paths.plugin().resolve("pack/dui.json")))
              .getAsJsonObject()
              .get("sha1")
              .getAsString());
      meta.addProperty("scale", mc.getWindow().getGuiScale());
      meta.addProperty("yaw", ((mc.player.getYRot() - reference + 180) % 360 + 360) % 360 - 180);
      meta.addProperty("pitch", mc.player.getXRot());
      meta.addProperty("zoom", state.has("zoom") ? state.get("zoom").getAsInt() : 12);
      Files.writeString(folder.resolve(name + ".json"), meta.toString());
      Screenshot.takeScreenshot(
          mc.gameRenderer.mainRenderTarget(),
          img -> {
            try (img) {
              img.writeToFile(folder.resolve(name + ".png"));
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          });
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private void requireSameHudTransport() {
    var state = lastState();
    if (state.get("hudSurfaces").getAsInt() != 3
        || state.get("seat").getAsInt() != hudSeat
        || state.get("input").getAsInt() != hudInput
        || state.get("display").getAsInt() != hudDisplay)
      throw new IllegalStateException("HUD reload changed transport or failed to replace layout");
    try {
      if (!Files.readString(Paths.plugin().resolve("pack/dui.json")).equals(hudPack))
        throw new IllegalStateException("HUD reload rebuilt resource pack");
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  private static JsonObject lastState() {
    try {
      return JsonParser.parseString(
              Files.readString(Paths.plugin().resolve("layouts/MapTest-map.json")))
          .getAsJsonObject();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static void requireView(String view) {
    if (!lastState().get("view").getAsString().startsWith(view))
      throw new IllegalStateException("Expected " + view + " got " + lastState());
  }

  private static void requireAppearance(Minecraft mc, boolean hidden) {
    var inv = mc.player.getInventory();
    if (hidden) {
      if (!mc.player.isInvisible()
          || !inv.getItem(0).isEmpty()
          || !inv.getItem(8).isEmpty()
          || !mc.player.getOffhandItem().isEmpty())
        throw new IllegalStateException("Hand/inventory mask missing");
    } else if (mc.player.isInvisible()
        || !inv.getItem(0).is(net.minecraft.world.item.Items.COMPASS)
        || !inv.getItem(8).is(net.minecraft.world.item.Items.DIAMOND_SWORD)
        || !mc.player.getOffhandItem().is(net.minecraft.world.item.Items.CARROT))
      throw new IllegalStateException("Actual client appearance/inventory not restored");
    System.out.println("MAP_APPEARANCE_CHECK " + (hidden ? "masked" : "restored"));
  }
}
