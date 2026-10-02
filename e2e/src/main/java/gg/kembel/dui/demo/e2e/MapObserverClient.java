package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** A second vanilla-rendered viewer verifies other players' map entities and flags stay private. */
final class MapObserverClient {
  private int ticks, stage, checks;
  private final Set<Integer> privateIds = new HashSet<>();
  private boolean detached;

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    mc.options.pauseOnLostFocus = false;
    if (!detached) {
      long window = mc.getWindow().handle();
      org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(window, null);
      org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback(window, null);
      org.lwjgl.glfw.GLFW.glfwSetScrollCallback(window, null);
      org.lwjgl.glfw.GLFW.glfwSetKeyCallback(window, null);
      detached = true;
    }
    ticks++;
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    try {
      if (ticks > 4500) throw new IllegalStateException("Map observer timeout");
      if (mc.gui.overlay() != null) return;
      if (stage == 0 && ticks > 70) {
        mc.getWindow().setWindowed(640, 480);
        mc.options.guiScale().set(1);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var server = new ServerData("Private map observer", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        stage = 1;
        return;
      }
      if (stage == 1 && mc.player != null && mc.gui.screen() instanceof DialogScreen<?>) {
        mc.player.connection.sendCommand("worldmap still");
        System.out.println("MAP_OBSERVER_OPEN");
        stage = 2;
        return;
      }
      if (stage != 2 || mc.player == null || mc.gui.screen() != null) return;
      var dataPath = Paths.plugin().resolve("layouts/MapTest-map.json");
      if (Files.exists(dataPath)) {
        var data = JsonParser.parseString(Files.readString(dataPath)).getAsJsonObject();
        if (data.has("age") && data.get("age").getAsInt() >= 40) {
          for (String role : List.of("seat", "input", "display"))
            if (data.has(role) && !data.get(role).isJsonNull()) {
              int id = data.get(role).getAsInt();
              if (mc.level.getEntity(id) != null)
                throw new IllegalStateException("Other viewer's " + role + " leaked: " + id);
              privateIds.add(id);
            }
          for (var other : mc.level.players())
            if (other.getName().getString().equals("MapTest") && other.isInvisible())
              throw new IllegalStateException("Self-only flag masking leaked to observer");
          checks++;
        }
      }
      if (Files.exists(Paths.output().getParent().resolve("map/client-result.json"))) {
        if (checks < 20 || privateIds.size() < 3)
          throw new IllegalStateException("Insufficient concurrent map observations");
        Files.writeString(
            Paths.output().resolve("client-result.json"),
            new Gson()
                .toJson(
                    Map.of(
                        "passed",
                        true,
                        "checks",
                        checks,
                        "privateIds",
                        privateIds.size(),
                        "muted",
                        true)));
        System.out.println(
            "MAP_OBSERVER_TEST_COMPLETE checks=" + checks + " ids=" + privateIds.size());
        mc.stop();
        stage = 3;
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("MAP_OBSERVER_TEST_FAILED");
      mc.stop();
      stage = 3;
    }
  }
}
