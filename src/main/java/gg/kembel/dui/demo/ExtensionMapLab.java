package gg.kembel.dui.demo;

import com.google.gson.Gson;
import gg.kembel.dui.core.world.*;
import gg.kembel.dui.paper.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.entity.Player;

/** Runtime geometry exercises the public map API; no entity or shader code in the consumer. */
final class ExtensionMapLab {
  private final Path report;
  private WorldMapSession session;
  private int selected;
  private String hover = "";

  ExtensionMapLab(Dui dui, Player player, Path directory) {
    report = directory.resolve("layouts/" + player.getName() + "-dynamic-map.json");
    session =
        dui.openWorldMap(
            player,
            "demo:elsewhere",
            frame(),
            WorldMapOptions.still(),
            input -> {
              hover = input.region() == null ? "" : input.region().id();
              if (input.type() == WorldMapInput.Type.SECONDARY) {
                session.close();
                return;
              }
              if (input.type() == WorldMapInput.Type.PRIMARY && input.region() != null) {
                selected++;
                session.update(frame());
              }
              write(false);
            });
    session.onClose(() -> write(true));
    poll();
  }

  private WorldMapFrame frame() {
    var geometry =
        selected == 0
            ? new WorldLayerState(560, 320, 144, 72, 1)
            : new WorldLayerState(640, 352, 192, 96, selected >= 2 ? .5 : 1);
    return new WorldMapFrame(
        "DYNAMIC",
        List.of("background", "calibration"),
        List.of(new WorldMapFrame.Region("moving", "calibration", "choose", "", true)),
        WorldHud.EMPTY,
        Map.of("calibration", geometry));
  }

  private void poll() {
    if (session.isActive()) {
      write(false);
      session.tasks().later("extension-report", 2, this::poll);
    }
  }

  private void write(boolean closed) {
    try {
      var state =
          closed
              ? new LinkedHashMap<String, Object>()
              : new LinkedHashMap<String, Object>(session.snapshot());
      state.put("closed", closed);
      state.put("selected", selected);
      state.put("hover", hover);
      var temporary = report.resolveSibling(report.getFileName() + ".tmp");
      Files.writeString(temporary, new Gson().toJson(state));
      Files.move(
          temporary, report, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }
}
