package gg.kembel.dui.demo;

import gg.kembel.dui.core.world.*;
import gg.kembel.dui.pack.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

/** Consumer-owned geometry and textures; runtime state references only stable layer IDs. */
public final class WorldMapAssets {
  enum DemoLayerKind {
    MAP,
    BACKGROUND,
    PULSE
  }

  public record Target(
      String id,
      String name,
      double x,
      double y,
      double width,
      double height,
      boolean unlocked,
      String record) {}

  public static final List<Target> TARGETS =
      List.of(
          new Target("sylvan", "Sylvan Reach", 400, 400, 144, 72, true, "00:42"),
          new Target("cloud", "Cloud Harbor", 560, 240, 144, 72, true, "01:18"),
          new Target("ember", "Ember Vault", 720, 400, 144, 72, false, "--:--"));
  public static final List<String> MODES = List.of("solo", "public", "party", "back");

  private WorldMapAssets() {}

  public static WorldMapDefinition definition() {
    var layers = new ArrayList<WorldMapDefinition.Layer>();
    layers.add(layer("background", "background", 560, 320, 1120, 640, DemoLayerKind.BACKGROUND));
    for (int i = 0; i < 20; i++)
      layers.add(
          layer(
              "tile_" + i,
              "tile_" + i,
              (i % 5 + .5) * 224,
              (i / 5 + .5) * 160,
              224,
              160,
              DemoLayerKind.MAP));
    for (var t : TARGETS)
      layers.add(layer(t.id(), t.id(), t.x(), t.y(), t.width(), t.height(), DemoLayerKind.MAP));
    for (var t : TARGETS)
      layers.add(layer("hover_" + t.id(), "hover", t.x(), t.y(), 152, 80, DemoLayerKind.PULSE));
    double[][] offsets = {{0, -100}, {-140, 0}, {140, 0}, {0, 100}};
    for (var t : TARGETS)
      for (int i = 0; i < 4; i++)
        layers.add(
            layer(
                t.id() + "_" + MODES.get(i),
                MODES.get(i),
                t.x() + offsets[i][0],
                t.y() + offsets[i][1],
                96,
                44,
                DemoLayerKind.MAP));
    layers.add(layer("calibration", "calibration", 560, 320, 144, 72, DemoLayerKind.MAP));
    return new WorldMapDefinition(
        "demo:elsewhere",
        1120,
        640,
        8,
        200,
        15,
        12,
        31,
        layers,
        new WorldMapDefinition.Opening(18, .88, 0, 1, gg.kembel.dui.core.Motion.Easing.EASE_OUT));
  }

  private static WorldMapDefinition.Layer layer(
      String id, String image, double x, double y, double w, double h, DemoLayerKind kind) {
    return new WorldMapDefinition.Layer(
        id,
        image,
        kind == DemoLayerKind.BACKGROUND ? .5 : x,
        kind == DemoLayerKind.BACKGROUND ? .5 : y,
        kind == DemoLayerKind.BACKGROUND ? 1 : w,
        kind == DemoLayerKind.BACKGROUND ? 1 : h,
        kind == DemoLayerKind.BACKGROUND
            ? WorldMapDefinition.Space.SCREEN
            : WorldMapDefinition.Space.MAP,
        kind == DemoLayerKind.BACKGROUND ? .80 : .9,
        kind == DemoLayerKind.PULSE ? .6 : 1,
        1,
        kind == DemoLayerKind.PULSE ? .25 : 0);
  }

  public static PackContribution contribution() throws IOException {
    var tiles = WorldMapPack.tiles(WorldMapArt.atlas(), 224, 160);
    var images = new TreeMap<String, BufferedImage>(tiles.images());
    images.put("background", WorldMapArt.solid(0x0C1826));
    images.put("hover", WorldMapArt.hover());
    images.put("calibration", WorldMapArt.calibration());
    for (int i = 0; i < TARGETS.size(); i++)
      images.put(TARGETS.get(i).id(), WorldMapArt.marker(TARGETS.get(i), i));
    for (var mode : MODES) images.put(mode, WorldMapArt.button(mode.toUpperCase(Locale.ROOT)));
    return WorldMapPack.contribution("demo", definition(), images);
  }
}
