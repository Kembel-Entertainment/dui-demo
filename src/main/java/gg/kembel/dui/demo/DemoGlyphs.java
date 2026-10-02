package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.pack.*;
import java.awt.image.BufferedImage;
import java.util.*;

public final class DemoGlyphs {
  public static final List<String> ICONS =
      List.of(
          "settings",
          "grid",
          "diamond",
          "star",
          "users",
          "book",
          "lock",
          "check",
          "coin",
          "leaf",
          "sun",
          "moon",
          "previous",
          "next",
          "move",
          "wheel",
          "primary",
          "secondary");
  private static final String[] PIXELS = {
    "...###.../..#####../.##.#.##./##..#..##/#########/##..#..##/.##.#.##./..#####../...###...",
    "####.####/####.####/####.####/####.####/........./####.####/####.####/####.####/####.####",
    "...###.../..#####../.#######./#########/.#######./..#####../...###.../....#..../.........",
    "....#..../...###.../#########/..#####../...###.../..#####../.###.###./.##...##./.........",
    "..###..../..###..../..###.##./......##./.#####.../#######../#######../#######../.........",
    "########./##....###/##....#.#/##....#.#/##....#.#/##....#.#/##....#.#/#########/..#######",
    "..#####../.##...##./.##...##./.##...##./#########/####.####/####.####/#########/#########",
    "........./.......##/......##./.....##../##..##.../.####..../..##...../........./.........",
    "..#####../.#######./###...###/###.#####/###...###/#####.###/###...###/.#######./..#####..",
    ".......##/....#####/..#######/.#######./.###.###./.##.###../..####.../.##....../##.......",
    "....#..../.#.....#./...###.../..#####../#.#####.#/..#####../...###.../.#.....#./....#....",
    "...###.../..###..../.###...../.###...../.###...../.####..../..#####../...#####./....###..",
    "......#../.....#.../....#..../...#...../..#....../...#...../....#..../.....#.../......#..",
    "..#....../...#...../....#..../.....#.../......#../.....#.../....#..../...#...../..#......",
    "...###.../..#.#.#../.#..#..#./.#..#..#./.#######./.#.....#./.#.###.#./..#.#.#../...###...",
    "...###.../..#.#.#../.#.###.#./.#.###.#./.#######./.#.....#./.#.....#./..#...#../...###...",
    "...###.../..###.#../.####..#./.####..#./.#######./.#.....#./.#.....#./..#...#../...###...",
    "...###.../..#.###../.#..####./.#..####./.#######./.#.....#./.#.....#./..#...#../...###..."
  };

  static BufferedImage iconImage(String name) {
    int index = ICONS.indexOf(name);
    if (index < 0) throw new IllegalArgumentException("Unknown icon: " + name);
    var image = new BufferedImage(9, 9, BufferedImage.TYPE_INT_ARGB);
    String[] rows = PIXELS[index].split("/");
    for (int y = 0; y < 9; y++)
      for (int x = 0; x < 9; x++) if (rows[y].charAt(x) == '#') image.setRGB(x, y, 0xFFFFFFFF);
    return image;
  }

  private static byte[] png(BufferedImage image) throws java.io.IOException {
    var out = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", out);
    return out.toByteArray();
  }

  private static final List<String> SPRITES =
      List.of(
          "item/amethyst_shard",
          "item/armor_stand",
          "item/beetroot_seeds",
          "item/blaze_powder",
          "item/bone_meal",
          "item/book",
          "item/bread",
          "item/breeze_rod",
          "item/chest_minecart",
          "item/clock_00",
          "item/cod",
          "item/cod_bucket",
          "item/diamond",
          "item/emerald",
          "item/ender_eye",
          "item/feather",
          "item/fire_charge",
          "item/fishing_rod",
          "item/glistering_melon_slice",
          "item/glow_berries",
          "item/glowstone_dust",
          "item/gold_ingot",
          "item/gold_nugget",
          "item/golden_carrot",
          "item/heart_of_the_sea",
          "item/hopper",
          "item/iron_hoe",
          "item/iron_pickaxe",
          "item/lead",
          "item/mace",
          "item/minecart",
          "item/nautilus_shell",
          "item/nether_star",
          "item/pitcher_pod",
          "item/rabbit_foot",
          "item/raw_gold",
          "item/salmon",
          "item/shears",
          "item/spyglass",
          "item/stone_hoe",
          "item/string",
          "item/tnt_minecart",
          "item/trial_key",
          "item/tropical_fish",
          "item/wheat",
          "item/wheat_seeds");

  public static List<GlyphSpec> specifications() {
    var result = new ArrayList<GlyphSpec>();
    for (var name : ICONS) result.add(new GlyphSpec("demo:" + name, 9, 9));
    for (var name : SPRITES) result.add(new GlyphSpec("demo:" + name, 18, 18, false));
    return List.copyOf(result);
  }

  public static Map<String, GlyphBinding> bindings() {
    return GlyphRegistry.bind(specifications());
  }

  public static List<PackContribution.Glyph> contributions(java.nio.file.Path clientJar)
      throws java.io.IOException {
    var result = new ArrayList<PackContribution.Glyph>();
    try (var assets = new VanillaAssets(clientJar)) {
      for (var spec : specifications()) {
        String name = spec.id().substring(5);
        var image =
            name.startsWith("item/")
                ? javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(
                        assets.read("assets/minecraft/textures/" + name + ".png")))
                : iconImage(name);
        if (name.startsWith("item/")) {
          var resized = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
          var g = resized.createGraphics();
          g.drawImage(image, 0, 0, 18, 18, null);
          g.dispose();
          image = resized;
        }
        result.add(new PackContribution.Glyph(spec, png(image)));
      }
    }
    return List.copyOf(result);
  }
}
