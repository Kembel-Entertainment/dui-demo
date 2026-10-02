package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import gg.kembel.dui.demo.e2e.mixin.FixtureMouseAccess;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Real vanilla inputs/rendering; Fabric only automates and records assertions. */
final class DynamicClient {
  private int ticks, stage, changed;
  private float reference;
  private String inventory;
  private JsonObject original;
  private boolean detached;
  private String renderedHash = "";
  private int hashChanged;
  private final java.util.Set<Integer> mapEntities = new java.util.HashSet<>();

  private void next() {
    stage++;
    changed = ticks;
    System.out.println("DYNAMIC_TEST_STAGE " + stage);
  }

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private JsonObject read(String file) throws Exception {
    return JsonParser.parseString(
            Files.readString(Paths.plugin().resolve("layouts/DynamicTest" + file + ".json")))
        .getAsJsonObject();
  }

  private static String inventory(Minecraft mc) {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++)
      s.append(mc.player.getInventory().getItem(i)).append(';');
    return s.toString();
  }

  private static void command(Minecraft mc, String cmd) {
    mc.getConnection().sendCommand(cmd);
  }

  private static void button(Minecraft mc, int number) {
    var mouse = (FixtureMouseAccess) mc.mouseHandler;
    var info = new MouseButtonInfo(number, 0);
    mouse.dui$button(mc.getWindow().handle(), info, 1);
    mouse.dui$button(mc.getWindow().handle(), info, 0);
  }

  private void mapShot(Minecraft mc, String name, JsonObject d) throws Exception {
    var meta = new JsonObject();
    d.add(
        "packSha1",
        JsonParser.parseString(Files.readString(Paths.plugin().resolve("pack/dui.json")))
            .getAsJsonObject()
            .get("sha1"));
    meta.add("layout", d);
    meta.addProperty("yaw", mc.player.getYRot() - reference);
    meta.addProperty("pitch", mc.player.getXRot());
    meta.addProperty("scale", mc.getWindow().getGuiScale());
    Files.createDirectories(Paths.output().resolve("screenshots"));
    Files.writeString(Paths.output().resolve("screenshots/" + name + ".json"), meta.toString());
    Screenshot.takeScreenshot(
        mc.gameRenderer.mainRenderTarget(),
        img -> {
          try (img) {
            img.writeToFile(Paths.output().resolve("screenshots/" + name + ".png"));
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }

  private void tick(Minecraft mc) {
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    mc.options.pauseOnLostFocus = false;
    if (!detached) {
      long window = mc.getWindow().handle();
      org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(window, null);
      org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback(window, null);
      org.lwjgl.glfw.GLFW.glfwSetScrollCallback(window, null);
      org.lwjgl.glfw.GLFW.glfwSetKeyCallback(window, null);
      detached = true;
    }
    try {
      if (stage == 99 || mc.gui.overlay() != null || ticks - changed < 10) return;
      if (ticks > 3000) throw new IllegalStateException("Dynamic scenario timeout: " + stage);
      if (stage == 0 && ticks > 80) {
        mc.options.guiScale().set(Paths.referenceScale(mc));
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var server = new ServerData("Dynamic fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        next();
        return;
      }
      if (mc.player == null) return;
      if (stage == 1 && mc.gui.screen() instanceof DialogScreen<?>) {
        inventory = inventory(mc);
        command(mc, "dui extensions");
        next();
        return;
      }
      if (stage >= 2 && stage <= 7) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        var d = read("");
        if (!d.get("section").getAsString().equals("extensions")) return;
        String hash =
            java.util.HexFormat.of()
                .formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                        .digest(
                            RealClientHarness.canvas(mc)
                                .getMessage()
                                .getString()
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        if (!hash.equals(d.get("componentTextHash").getAsString())) return;
        if (!hash.equals(renderedHash)) {
          renderedHash = hash;
          hashChanged = ticks;
          return;
        }
        // End-tick runs before drawing: require two ticks so the screenshot is this received frame.
        if (ticks - hashChanged < 2) return;
        if (stage == 2) {
          if (ticks - changed < 40) return;
          original = d;
          RealClientHarness.snapshot(mc, Paths.output(), "01-initial", d);
          RealClientHarness.hit(mc, d, "animate");
          next();
        } else if (stage == 3) {
          if (d.get("age").getAsInt() < 24) return;
          RealClientHarness.snapshot(mc, Paths.output(), "02-moving", d);
          var id =
              d.getAsJsonArray("hits").asList().stream()
                  .map(JsonElement::getAsJsonObject)
                  .filter(h -> h.get("action").getAsString().equals("choose"))
                  .findFirst()
                  .orElseThrow()
                  .get("id")
                  .getAsString();
          RealClientHarness.hit(mc, d, id);
          next();
        } else if (stage == 4) {
          if (d.get("age").getAsInt() != 80) return;
          if (d.get("clicks").getAsInt() != 1)
            throw new IllegalStateException("Transformed hit not received");
          RealClientHarness.snapshot(mc, Paths.output(), "03-complete", d);
          command(mc, "dui extensions play");
          next();
        } else if (stage == 5) {
          if (d.get("age").getAsInt() < 16) return;
          command(mc, "dui extensions");
          next();
        } else if (stage == 6) {
          if (d.get("age").getAsInt() != 0) return;
          next();
        } else {
          if (ticks - changed < 35) return;
          if (d.get("age").getAsInt() != 0)
            throw new IllegalStateException("Old animation survived replacement");
          RealClientHarness.snapshot(mc, Paths.output(), "04-interrupted", d);
          reference = mc.player.getYRot();
          mc.player.setXRot(0);
          command(mc, "worldmap dynamic");
          next();
        }
        return;
      }
      if (stage >= 8) {
        var d = read("-dynamic-map");
        if (stage == 8) {
          if (ticks - changed < 100) return;
          if (d.getAsJsonArray("dynamicDisplays").size() != 1)
            throw new IllegalStateException("No runtime map entity");
          mapShot(mc, "05-map-initial", d);
          button(mc, 0);
          next();
        } else if (stage == 9) {
          if (d.get("selected").getAsInt() != 1)
            throw new IllegalStateException("Initial map target not selected");
          mapShot(mc, "06-map-moved", d);
          mc.player.setYRot(reference + 10);
          mc.player.setXRot(4);
          next();
        } else if (stage == 10) {
          if (!d.get("hover").getAsString().equals("moving"))
            throw new IllegalStateException("Moved hit target mismatch");
          mapShot(mc, "07-map-aimed", d);
          button(mc, 0);
          next();
        } else if (stage == 11) {
          if (d.get("selected").getAsInt() != 2)
            throw new IllegalStateException("Moved target click lost");
          mapShot(mc, "08-map-transparent", d);
          ((FixtureMouseAccess) mc.mouseHandler).dui$scroll(mc.getWindow().handle(), 0, -1);
          next();
        } else if (stage == 12) {
          if (d.get("zoom").getAsInt() != 13)
            throw new IllegalStateException("Dynamic zoom mismatch");
          mapShot(mc, "09-map-zoom", d);
          for (String role : java.util.List.of("seat", "input", "display"))
            mapEntities.add(d.get(role).getAsInt());
          for (var id : d.getAsJsonArray("dynamicDisplays")) mapEntities.add(id.getAsInt());
          button(mc, 1);
          next();
        } else if (stage == 13) {
          if (!d.get("closed").getAsBoolean() || mc.player.getVehicle() != null)
            throw new IllegalStateException("Dynamic map leaked session");
          for (int id : mapEntities)
            if (mc.level.getEntity(id) != null)
              throw new IllegalStateException("Map entity leaked: " + id);
          if (!inventory.equals(inventory(mc)))
            throw new IllegalStateException("Inventory changed");
          var result = new JsonObject();
          result.addProperty("passed", true);
          result.addProperty("inventoryUnchanged", true);
          result.addProperty("muted", true);
          result.addProperty("steps", 14);
          result.addProperty("interruptedAnimationCancelled", true);
          result.addProperty("sharedHitClicked", true);
          Files.writeString(Paths.output().resolve("client-result.json"), result.toString());
          System.out.println("DYNAMIC_TEST_COMPLETE");
          stage = 99;
          mc.stop();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("DYNAMIC_TEST_FAILED");
      try {
        Files.writeString(Paths.output().resolve("failure.txt"), e.toString());
      } catch (Exception ignored) {
      }
      stage = 99;
      mc.stop();
    }
  }
}
