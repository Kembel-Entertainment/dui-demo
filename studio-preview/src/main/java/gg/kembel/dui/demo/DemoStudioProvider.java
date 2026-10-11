package gg.kembel.dui.demo;

import com.google.gson.Gson;
import gg.kembel.dui.core.*;
import gg.kembel.dui.core.video.*;
import gg.kembel.dui.core.world.*;
import gg.kembel.dui.pack.*;
import gg.kembel.dui.studio.sdk.StudioProjectProvider;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipFile;

/**
 * Consumer-side presentation fixtures. No Paper server, inventory mutations or settlements run
 * here.
 */
public final class DemoStudioProvider implements StudioProjectProvider {
  private Context context;
  private List<PackContribution> packs;
  private Path skin;

  public void initialize(Context context) throws Exception {
    this.context = context;
    if (context.minecraftJar() == null)
      throw new IllegalArgumentException(
          "Select a local Minecraft 26.3 client JAR to preview dui-demo's native assets.");
    packs = new ArrayList<>(DemoPackGenerator.contributions(context.minecraftJar()));
    Path additions = context.root().resolve("build/pack-additions");
    if (Files.isDirectory(additions.resolve("assets/dui_demo"))) {
      var resources = new TreeMap<String, byte[]>();
      try (var files = Files.walk(additions.resolve("assets/dui_demo"))) {
        for (var file : files.filter(Files::isRegularFile).toList())
          resources.put(
              additions.relativize(file).toString().replace('\\', '/'), Files.readAllBytes(file));
      }
      packs.add(new PackContribution("dui_demo", resources, List.of()));
    }
    skin = context.root().resolve("build/studio/preview-skin.png");
    try (var zip = new ZipFile(context.minecraftJar().toFile());
        var input =
            zip.getInputStream(
                zip.getEntry("assets/minecraft/textures/entity/player/wide/steve.png"))) {
      Files.createDirectories(skin.getParent());
      Files.write(skin, input.readAllBytes());
    }
  }

  public List<SourceFile> sourceFiles() {
    var result = new ArrayList<SourceFile>();
    result.add(
        new SourceFile(
            "src/main/resources/casino/builtin.glsl",
            "module",
            "demo:builtin",
            "",
            "src/main/resources/ui/slots.html"));
    result.add(
        new SourceFile(
            "src/main/resources/casino/casino-primitives.glsl",
            "module",
            "demo:casino",
            "",
            "src/main/resources/ui/horses.html"));
    result.add(
        new SourceFile(
            "src/main/resources/casino/gift-confetti.glsl",
            "shader",
            "demo:gift-confetti",
            "",
            "src/main/resources/ui/advent.html"));
    for (var name : List.of("race", "prize-wheel", "coin", "temple-reel")) {
      String function =
          switch (name) {
            case "race" -> "demoRace";
            case "prize-wheel" -> "demoPrizeWheel";
            case "coin" -> "demoCoin";
            default -> "demoTempleReel";
          };
      String template =
          switch (name) {
            case "race" -> "horses";
            case "prize-wheel" -> "wheel";
            case "coin" -> "coinflip";
            default -> "bookofra";
          };
      String suffix =
          "\nvec4 "
              + function
              + "Typed(vec2 q,vec2 size,DUI_PARAMETERS p,float t,bool live){return "
              + function
              + "(q,size,p.a,p.b,t,live);}";
      result.add(
          new SourceFile(
              "src/main/resources/casino/" + name + ".glsl",
              "shader",
              "demo:" + name,
              suffix,
              "src/main/resources/ui/" + template + ".html"));
    }
    return result;
  }

  public RenderEnvironment environment() {
    return DemoTemplates.environment(DemoTemplates.font());
  }

  public ComponentRegistry components() {
    return CasinoComponents.registry();
  }

  public List<PackContribution> contributions() {
    return packs;
  }

  public String normalize(String source) {
    return source.contains("<dui-menu") ? DemoTemplates.normalize(source) : source;
  }

  public List<Fixture> fixtures(String document) {
    String name = Path.of(document).getFileName().toString().replace(".html", "");
    var out = new ArrayList<Fixture>();
    for (boolean compact : new boolean[] {false, true}) {
      String size = compact ? "compact" : "wide";
      switch (name) {
        case "shop" -> {
          for (boolean checkout : new boolean[] {false, true}) {
            var s = new ShopState();
            s.compact = compact;
            s.checkout = checkout;
            s.cart.put(s.products().getFirst().id(), 1);
            out.add(
                f(
                    size + (checkout ? "-checkout" : ""),
                    ShopView.data(s, "Studio explorer"),
                    ShopView.images(s),
                    shopItems()));
          }
        }
        case "showcase", "showcase-compact" -> {
          if (compact != (name.equals("showcase-compact"))) continue;
          for (var page : ShowcaseState.PAGES) {
            var s = new ShowcaseState();
            s.layout = compact ? "compact" : "spacious";
            s.page = page.id();
            for (int i = 0; i < s.partCount(); i++) {
              s.part = i;
              var data = s.data();
              out.add(
                  f(
                      size + "-" + page.id() + "-" + i,
                      data,
                      Map.of(),
                      Map.of(
                          "kit_grass",
                          "minecraft:grass_block",
                          "kit_head",
                          "skin:viewer",
                          "kit_tool",
                          "minecraft:diamond_pickaxe",
                          "kit_banner",
                          "minecraft:blue_banner")));
            }
          }
        }
        case "slots" -> {
          var s = new SlotState();
          s.compact = compact;
          s.modeIndex = 1;
          s.spin(0, new Random(1));
          out.add(f(size, SlotView.data(s), Map.of(), Map.of()));
        }
        case "roulette" -> {
          var s = new RouletteGame();
          s.compact = compact;
          out.add(f(size, RouletteView.data(s), RouletteArt.images(), Map.of()));
        }
        case "poker" -> {
          var s = new PokerState();
          s.compact = compact;
          s.deal(0, true);
          out.add(f(size, PokerView.data(s), PokerArt.images(s.game), Map.of()));
        }
        case "blackjack" -> {
          var s = new BlackjackState();
          s.compact = compact;
          s.deal(0, true, new Random(1));
          out.add(f(size, BlackjackView.data(s), BlackjackArt.images(), Map.of()));
        }
        case "advent" -> {
          for (var phase : AdventState.Phase.values()) {
            var s = new AdventState();
            s.compact = compact;
            s.open(5, 0);
            s.phase = phase;
            s.rewardAt = 24;
            out.add(
                f(
                    size + "-" + phase.name().toLowerCase(Locale.ROOT),
                    AdventView.data(s, 30),
                    AdventView.images(s),
                    Map.of(
                        "parcel_body",
                        "dui_demo:advent/body_5",
                        "parcel_lid",
                        "dui_demo:advent/lid_5",
                        "advent_reward",
                        "minecraft:gold_ingot")));
          }
        }
        case "warps" -> {
          var s = new WarpState();
          s.compact = compact;
          var items = new HashMap<String, String>();
          for (int i = 0; i < 4; i++)
            items.put(
                "card_" + i,
                "dui_demo:warps/"
                    + WarpState.DESTINATIONS.get(i).id()
                    + (i == s.selected ? "_selected" : ""));
          out.add(f(size, WarpView.data(s), Map.of(), items));
        }
        case "rewards" -> {
          var s = new RewardState();
          s.compact = compact;
          out.add(
              f(
                  size,
                  RewardView.data(s, LocalDate.of(2026, 10, 3)),
                  Map.of(),
                  Map.of(
                      "reward_mascot",
                      "dui_demo:reward/idle",
                      "reward_preview",
                      "minecraft:honey_bottle")));
        }
        case "character" -> {
          var s = new CharacterState();
          s.compact = compact;
          out.add(
              f(
                  size,
                  CharacterView.data(s, "Studio adventurer"),
                  Map.of("character_background", CharacterArt.background(compact)),
                  Map.of()));
        }
        case "horses", "wheel", "coinflip", "bookofra" -> {
          var game =
              Arrays.stream(ArcadeMenu.Game.values())
                  .filter(v -> v.id.equals(name))
                  .findFirst()
                  .orElseThrow();
          var s = game.fresh();
          s.compact = compact;
          s.play(new Random(1), 0);
          out.add(f(size, ArcadeView.data(game, s), Map.of(), Map.of()));
        }
        case "acceptance" ->
            out.add(f(size, AcceptanceView.data(0, compact), AcceptanceView.images(), Map.of()));
        case "videos" -> {
          var feed =
              new YouTubeFeed.Feed(
                  "Studio video library",
                  List.of(),
                  java.time.Instant.parse("2026-10-03T12:00:00Z"));
          var model = VideoView.model(feed, 0, compact, false, "", Map.of());
          out.add(f(size, model.data(), model.images(), Map.of()));
        }
        case "confirm", "form", "setup" ->
            out.add(
                f(
                    size,
                    Map.of("width", compact ? 284 : 344, "theme", "studio_dark"),
                    Map.of(),
                    Map.of()));
        case "cinema" ->
            out.add(
                f(
                    size,
                    Map.of(
                        "status",
                        "Local media / Studio fixture",
                        "files",
                        List.of(
                            Map.of("id", "sample", "y", 72, "label", "Add your own local media")),
                        "running",
                        true),
                    Map.of(),
                    Map.of()));
        case "browser" ->
            out.add(
                f(
                    size,
                    Map.of(
                        "title",
                        "A project-owned browser source",
                        "status",
                        "Preview fixture / no browser process started",
                        "running",
                        true,
                        "zoom",
                        "100%"),
                    Map.of(),
                    Map.of()));
        case "gba" ->
            out.add(
                f(
                    size,
                    Map.of(
                        "status",
                        "Local cartridges stay private",
                        "roms",
                        List.of(
                            Map.of(
                                "id",
                                "sample",
                                "y",
                                63,
                                "title",
                                "Bring your own local cartridge")),
                        "running",
                        false),
                    Map.of(),
                    Map.of()));
        case "cinema-screen", "browser-screen", "gba-screen" -> {
          var data = new HashMap<String, Object>();
          data.put("width", name.equals("gba-screen") ? 240 : 320);
          data.put("height", name.equals("gba-screen") ? 160 : 180);
          data.put("format", "RGB888");
          data.put("fps", 30);
          data.put("tiles", 64);
          data.put("bytes", 8_000_000);
          data.put("title", "STUDIO / LIVE PIXEL SURFACE");
          data.put("url", "Local frame source");
          data.put("status", "Sampled frame");
          data.put("source", "Procedural fixture; no external media");
          data.put("progress", .4);
          data.put("time", "00:12 / 00:30");
          out.add(f(size, data, Map.of(), Map.of()));
        }
        default -> {
          if (!compact) out.add(f("default", Map.of(), Map.of(), Map.of()));
        }
      }
    }
    return out;
  }

  private Map<String, String> shopItems() {
    var items = new HashMap<String, String>();
    for (var p : ShopState.PRODUCTS) items.put("product_" + p.id(), "minecraft:" + p.model());
    items.put("shop_profile", "skin:viewer");
    return items;
  }

  private Fixture f(
      String id,
      Map<String, Object> data,
      Map<String, RasterImage> images,
      Map<String, String> items) {
    var appearance =
        new Appearance(
            skin,
            false,
            Map.of(
                "head",
                "minecraft:diamond",
                "chest",
                "minecraft:diamond",
                "legs",
                "minecraft:diamond",
                "feet",
                "minecraft:diamond"));
    return new Fixture(
        id,
        id.replace('-', ' '),
        data,
        images,
        items,
        Map.of("viewer", appearance, "self", appearance));
  }

  public Map<String, Object> sample(
      String document, Map<String, Object> data, double seconds, boolean playing) {
    var result = new HashMap<>(data);
    String name = Path.of(document).getFileName().toString();
    if (name.equals("advent.html") && !Boolean.TRUE.equals(data.get("board"))) {
      var s = new AdventState();
      s.compact = ((Number) data.get("width")).intValue() < 400;
      s.open(5, 0);
      long tick = (long) (seconds * 20);
      if (tick >= AdventState.OPEN_TICKS) {
        s.reveal(s.generation, tick);
        s.rewardAt = AdventState.OPEN_TICKS;
      }
      result.putAll(AdventView.data(s, tick));
    }
    return result;
  }

  public Optional<MapPreview> map(String document, Map<String, Object> data) throws Exception {
    if (!document.endsWith("atlas.map.json") && !document.endsWith("worldmap-hud.html"))
      return Optional.empty();
    var definition =
        new Gson()
            .fromJson(
                Files.readString(
                    context.root().resolve("src/main/resources/worldmap/atlas.map.json")),
                WorldMapDefinition.class);
    var images = new TreeMap<String, RasterImage>();
    var tiles = WorldMapPack.tiles(WorldMapArt.atlas(), 224, 160);
    tiles.images().forEach((id, image) -> images.put(id, raster(image)));
    images.put("background", raster(WorldMapArt.solid(0x0C1826)));
    images.put("hover", raster(WorldMapArt.hover()));
    images.put("calibration", raster(WorldMapArt.calibration()));
    for (int i = 0; i < WorldMapAssets.TARGETS.size(); i++) {
      var target = WorldMapAssets.TARGETS.get(i);
      images.put(target.id(), raster(WorldMapArt.marker(target, i)));
    }
    for (var mode : WorldMapAssets.MODES)
      images.put(mode, raster(WorldMapArt.button(mode.toUpperCase(Locale.ROOT))));
    var hudData = new HashMap<String, Object>();
    hudData.put("title", "ELSEWHERE / WORLD ATLAS");
    hudData.put("progress", 0);
    hudData.put(
        "controls",
        List.of(
            Map.of("icon", "demo:move", "label", "LOOK", "hint", "Pan the atlas"),
            Map.of("icon", "demo:wheel", "label", "SCROLL", "hint", "Zoom"),
            Map.of("icon", "demo:primary", "label", "LEFT CLICK", "hint", "Explore"),
            Map.of("icon", "demo:secondary", "label", "RIGHT CLICK", "hint", "Close")));
    var source =
        Files.readString(context.root().resolve("src/main/resources/ui/worldmap-hud.html"));
    var hud =
        WorldHudTemplate.parse(source, environment(), components(), "map HUD").render(hudData);
    var visible =
        definition.layers().stream()
            .filter(
                l ->
                    l.id().equals("background")
                        || l.id().startsWith("tile_")
                        || WorldMapAssets.TARGETS.stream().anyMatch(t -> t.id().equals(l.id())))
            .map(WorldMapDefinition.Layer::id)
            .toList();
    var regions =
        WorldMapAssets.TARGETS.stream()
            .map(t -> new WorldMapFrame.Region(t.id(), t.id(), "select", t.id(), t.unlocked()))
            .toList();
    return Optional.of(
        new MapPreview(definition, new WorldMapFrame("overview", visible, regions, hud), images));
  }

  public Optional<VideoFrame> frame(String document, double seconds, boolean live) {
    if (!document.endsWith("-screen.html")) return Optional.empty();
    int w = document.endsWith("gba-screen.html") ? 240 : 320, h = w == 240 ? 160 : 180;
    int[] pixels = new int[w * h];
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++) {
        int glow = (int) (127 + 127 * Math.sin(x * .028 + seconds * 2));
        pixels[y * w + x] = (x * 255 / w) << 16 | (y * 255 / h) << 8 | glow;
      }
    return Optional.of(new VideoFrame(w, h, PixelFormat.RGB888, (long) (seconds * 30), pixels));
  }

  private static RasterImage raster(BufferedImage image) {
    return RasterImage.argb(
        image.getWidth(),
        image.getHeight(),
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()));
  }
}
