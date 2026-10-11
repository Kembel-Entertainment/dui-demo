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
      List.of(DemoPresentation.read("/worldmap/atlas.targets.json", Target[].class));
  public static final List<String> MODES = List.of("solo", "public", "party", "back");

  private WorldMapAssets() {}

  public static WorldMapDefinition definition() {
    return DemoPresentation.read("/worldmap/atlas.map.json", WorldMapDefinition.class);
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
