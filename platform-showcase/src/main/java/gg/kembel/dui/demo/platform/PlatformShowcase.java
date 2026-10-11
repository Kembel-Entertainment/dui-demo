package gg.kembel.dui.demo.platform;

import gg.kembel.dui.core.*;
import gg.kembel.dui.core.video.*;
import gg.kembel.dui.core.world.*;
import gg.kembel.dui.runtime.*;
import java.util.*;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;

/** One consumer presentation used unchanged by Paper and Minestom. */
public final class PlatformShowcase {
  private final Dui dui;
  private final MenuTemplate template;
  public PlatformShowcase(Dui dui) throws Exception {
    this.dui = dui;
    template = dui.compile("platform.html", """
        <dui-menu width="360" height="180" background="none">
          <dui-layer height="fill">
            <dui-text x="12" y="9" width="240" height="18" label="DUI / TWO PLATFORMS" color="#EED3A0" />
            <dui-text x="12" y="36" width="210" height="18" label="Count: {{count}}" color="#FFFFFF" />
            <dui-text x="12" y="63" width="210" height="18" label="{{text}}" color="#FFFFFF" />
            <dui-head x="12" y="90" width="9" height="9" player="self" />
          </dui-layer>
        </dui-menu>
        """);
  }
  private static final class State { int count; String text = "Shared presentation"; boolean enabled = true; }
  public MenuController<?> open(UiViewer viewer, NativeItem item, PlayerAppearance appearance,
                                Consumer<Map<String, Object>> report) {
    var state = new State();
    var definition = new MenuDefinition<State>("platform", s -> {
      var model = new ViewModel(Map.of("count", s.count, "text", s.text), Map.of(), Map.of("sample", item),
          Map.of(), Map.of("viewer", appearance));
      var canvas = template.render(model.data(), model.images());
      canvas.item("sample", 36, 90, 18);
      canvas.playerModel("viewer", "viewer", 252, 27, 96, 144, 0, true, true);
      canvas.rect(12, 126, 180, 27, 0x30495A);
      canvas.text(24, 135, 144, "Add one", 0xFFFFFF);
      canvas.hit(new Canvas.Hit("increment", "increment", "", "Add one", 12, 126, 180, 27));
      var options = new DialogOptions(Component.text("DUI / portable demo"),
          List.of(new DialogInput.Text("text", 240, Component.text("Message"), true, s.text, 64, null),
              new DialogInput.Boolean("enabled", Component.text("Enabled"), s.enabled, "true", "false")),
          List.of(new DialogOptions.Button("Apply", "apply")), new DialogOptions.Button("Close", "close"), 1, false);
      return new MenuView(canvas, model, options);
    }, (s, action) -> {
      if (action.action().equals("increment")) s.count++;
      if (action.action().equals("apply")) {
        var text = action.response().text("text"); if (text != null) s.text = text;
        var enabled = action.response().bool("enabled"); if (enabled != null) s.enabled = enabled;
      }
    }, s -> report.accept(Map.of("closed", true, "count", s.count, "text", s.text, "enabled", s.enabled)));
    var controller = new MenuController<>(dui, viewer, state, definition).onPresented(session -> {
      var data = new LinkedHashMap<String, Object>();
      data.put("closed", false); data.put("count", state.count); data.put("text", state.text); data.put("enabled", state.enabled);
      data.put("width", session.canvas().width); data.put("height", session.canvas().height); data.put("hits", session.canvas().hits);
      data.put("packSha1", dui.metadata().sha1()); data.put("bodyCount", session.bodyCount());
      report.accept(data);
    });
    controller.refresh(); return controller;
  }
  public WorldMapSession map(UiViewer viewer) {
    var metadata = dui.metadata().worldMaps().get("demo:elsewhere");
    if (metadata == null) throw new IllegalStateException("Build the demo pack with the atlas");
    var layers = metadata.definition().layers().stream().map(WorldMapDefinition.Layer::id)
        .filter(id -> id.equals("background") || id.startsWith("tile_")).toList();
    return dui.spatial().openWorldMap(viewer, metadata.definition().id(),
        new WorldMapFrame("platform", layers, List.of(), WorldHud.EMPTY), WorldMapOptions.still(), input -> {});
  }
  public VideoSurfaceSession video(UiViewer viewer) {
    var spec = new VideoSurfaceSpec(128, 96, PixelFormat.RGB888, 10,
        VideoSurfaceSpec.Viewport.FULL, new VideoSurfaceSpec.Budget(16, 1_000_000), 0x101820, true);
    var session = dui.spatial().openVideoSurface(viewer, spec, VideoSurfaceOptions.DEFAULT, input -> {});
    produce(viewer, session, 0); return session;
  }
  private void produce(UiViewer viewer, VideoSurfaceSession session, long sequence) {
    if (!session.isActive()) return;
    var pixels = new int[128 * 96];
    for (int y = 0; y < 96; y++) for (int x = 0; x < 128; x++)
      pixels[y * 128 + x] = (((x + (int)sequence * 8) & 255) << 16) | ((y * 2) << 8) | 0x80;
    session.submit(new VideoFrame(128, 96, PixelFormat.RGB888, sequence, pixels));
    viewer.scheduler().later(2, () -> produce(viewer, session, sequence + 1));
  }
}
