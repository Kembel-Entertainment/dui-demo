package gg.kembel.dui.demo;

import com.google.gson.Gson;
import gg.kembel.dui.core.world.*;
import gg.kembel.dui.paper.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import org.bukkit.entity.Player;

/**
 * Ordinary library consumer; no entities, shaders, resource-pack status listener or packet code.
 */
final class WorldMapDemo implements AutoCloseable {
  private final Dui dui;
  private final Player player;
  private final Path diagnostics;
  private final WorldMapState state;
  private WorldMapSession session;
  private final Supplier<WorldHudTemplate> hudTemplate;

  WorldMapDemo(
      Dui dui,
      Player player,
      Path directory,
      boolean debug,
      boolean still,
      Supplier<WorldHudTemplate> hudTemplate) {
    this.dui = dui;
    this.hudTemplate = hudTemplate;
    this.player = player;
    this.diagnostics = directory.resolve("layouts/" + player.getName() + "-map.json");
    state = new WorldMapState(debug);
    session =
        dui.openWorldMap(
            player,
            "demo:elsewhere",
            project(12, System.nanoTime()),
            new WorldMapOptions(still),
            input -> {
              state.accept(input);
              if (state.close) {
                if (state.chosen != null)
                  player.sendMessage(
                      "Demo selection: " + state.chosen + ". No game or teleport is started.");
                session.close();
              } else {
                session.update(project(input.zoom(), input.nanoTime()));
                report();
              }
            });
    if (!session.isActive()) return;
    session.onClose(
        () -> {
          try {
            write("{\"closed\":true}");
          } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
          }
        });
    poll();
  }

  private WorldMapFrame project(int zoom, long now) {
    return project(zoom, now, hudTemplate.get());
  }

  private WorldMapFrame project(int zoom, long now, WorldHudTemplate template) {
    var frame = state.project(zoom, now);
    var controls =
        List.of(
            Map.<String, Object>of("icon", "demo:move", "label", "Pan", "hint", "MOVE MOUSE"),
            Map.<String, Object>of("icon", "demo:wheel", "label", "Zoom", "hint", "SCROLL"),
            Map.<String, Object>of(
                "icon",
                "demo:primary",
                "label",
                state.selected != null ? "Select" : "Explore",
                "hint",
                "LEFT CLICK"),
            Map.<String, Object>of(
                "icon",
                "demo:secondary",
                "label",
                state.selected != null ? "Back" : "Close",
                "hint",
                "RIGHT CLICK"));
    var presentation =
        template.render(
            Map.of(
                "title",
                state.hudTitle,
                "progress",
                state.hudProgress,
                "controls",
                controls,
                "zoom",
                zoom,
                "detail",
                state.selected != null));
    return new WorldMapFrame(frame.viewId(), frame.visibleLayers(), frame.regions(), presentation);
  }

  void validateHud(WorldHudTemplate template) {
    if (session.isActive()) project(session.zoom(), System.nanoTime(), template);
  }

  void refreshHud() {
    if (session.isActive()) session.update(project(session.zoom(), System.nanoTime()));
  }

  private void poll() {
    if (!session.isActive()) return;
    report();
    session.tasks().later("diagnostics", 5, this::poll);
  }

  private void report() {
    try {
      var data = new LinkedHashMap<String, Object>(session.snapshot());
      data.put("hover", state.hover);
      data.put("selected", state.selected);
      data.put("player", player.getName());
      data.put("realInvisible", player.isInvisible());
      data.put("inventory", Arrays.hashCode(player.getInventory().getContents()));
      write(new Gson().toJson(data));
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  private void write(String data) throws java.io.IOException {
    var temporary = diagnostics.resolveSibling(diagnostics.getFileName() + ".tmp");
    Files.writeString(temporary, data);
    Files.move(
        temporary,
        diagnostics,
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE);
  }

  WorldMapSession session() {
    return session;
  }

  @Override
  public void close() {
    if (session != null && session.isActive()) session.close();
  }
}
