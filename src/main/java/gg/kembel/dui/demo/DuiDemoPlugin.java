package gg.kembel.dui.demo;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** An ordinary consumer of dui. Business rules, files and background work live here. */
public final class DuiDemoPlugin extends JavaPlugin implements Listener {
  private static final Gson JSON = new Gson();
  private final Map<UUID, DemoSession> sessions = new HashMap<>();
  private final SecureRandom random = new SecureRandom();
  private gg.kembel.dui.core.world.WorldHudTemplate mapHudTemplate;
  private final Map<String, MenuTemplate> templates = new HashMap<>();
  private Dui dui;
  private gg.kembel.dui.demo.gba.GbaDemo gba;
  private MapTestFixture mapFixture;
  private final Map<UUID, WorldMapDemo> maps = new HashMap<>();
  private HttpServer http;
  private PackMetadata metadata;
  private Path directory;
  private VideoProvider videos;
  private MenuCatalogue<DemoEntry> catalogue;

  private void configureMenus() {
    var entries = new ArrayList<MenuCatalogue.Definition<DemoEntry>>();
    for (var factory : DemoMenus.factories()) {
      var menu = factory.apply(services(null, null));
      entries.add(
          new MenuCatalogue.Definition<>(
              menu.id(),
              menu.aliases(),
              menu.templates(),
              new DemoEntry.Dialogue(factory),
              () -> {
                menu.validate(false);
                menu.validate(true);
              }));
    }
    entries.add(
        new MenuCatalogue.Definition<>(
            "map",
            Set.of("worldmap"),
            List.of(),
            new DemoEntry.Atlas(),
            () -> {
              new WorldMapState(false).project(12, 0).validate(WorldMapAssets.definition());
            }));
    catalogue = new MenuCatalogue<>(entries);
  }

  @Override
  public void onEnable() {
    try {
      saveDefaultConfig();
      directory = getDataFolder().toPath();
      for (String name : List.of("layouts", "display", "rewards", "slots", "ui"))
        Files.createDirectories(directory.resolve(name));
      configureMenus();
      for (String name :
          java.util.stream.Stream.concat(
                  catalogue.templates().stream(), java.util.stream.Stream.of("worldmap-hud"))
              .toList())
        if (!Files.exists(directory.resolve("ui/" + name + ".html")))
          try (var in = getResource("ui/" + name + ".html")) {
            Files.copy(Objects.requireNonNull(in), directory.resolve("ui/" + name + ".html"));
          }
      var packDirectory = directory.resolve("pack");
      metadata = PackMetadata.read(packDirectory.resolve("dui.json"));
      byte[] pack = Files.readAllBytes(packDirectory.resolve("dui.zip"));
      if (!HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-1").digest(pack))
          .equals(metadata.sha1()))
        throw new IllegalStateException("Pack file and metadata do not match");
      http =
          HttpServer.create(
              new InetSocketAddress(
                  getConfig().getString("pack.bind-address", "127.0.0.1"),
                  getConfig().getInt("pack.port", 25585)),
              0);
      http.createContext(
          "/dui.zip",
          exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, pack.length);
            try (var out = exchange.getResponseBody()) {
              out.write(pack);
            }
          });
      http.start();
      dui =
          Dui.create(
              this,
              PackDescriptor.of(URI.create(getConfig().getString("pack.public-url")), metadata),
              metadata);
      dui.registerBackend(
          "proof:note",
          new BodyBackend() {
            public Set<String> requiredPackCapabilities() {
              return Set.of("proof:note");
            }

            public Set<String> geometryCapabilities() {
              return Set.of();
            }

            public List<io.papermc.paper.registry.data.dialog.body.DialogBody> render(
                RenderPrimitive primitive, ViewModel model) {
              return List.of(
                  io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(
                      net.kyori.adventure.text.Component.text(
                          primitive.data().get("text").toString()),
                      360));
            }
          });
      reloadTemplates();
      gba = new gg.kembel.dui.demo.gba.GbaDemo(this, dui, directory);
      videos =
          getConfig().getBoolean("videos.live", true)
              ? new VideoService(directory.resolve("videos-cache"))
              : new FixtureVideos();
      videos.refresh(false);
      getServer().getPluginManager().registerEvents(this, this);
      if (getServer().getIp().equals("127.0.0.1")
          && getConfig().getBoolean("testing.map-fixtures", false))
        mapFixture = new MapTestFixture(this);
      var commands = new HashSet<String>();
      for (var entry : catalogue.definitions()) {
        commands.addAll(entry.aliases());
        if (getCommand(entry.id()) != null) commands.add(entry.id());
      }
      commands.add("gba");
      for (String command : commands) Objects.requireNonNull(getCommand(command)).setExecutor(this);
      getLogger().info("DUI_DEMO_READY pack=" + metadata.sha1() + " minecraft=26.2");
    } catch (Exception e) {
      getLogger()
          .log(
              java.util.logging.Level.SEVERE,
              "dui-demo startup failed. Run the pack build and install tasks first.",
              e);
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (gba != null) gba.close();
    if (videos != null) videos.close();
    for (var entry : sessions.entrySet()) finishSlots(entry.getKey(), entry.getValue());
    for (var map : List.copyOf(maps.values())) map.close();
    maps.clear();
    if (dui != null) dui.close();
    if (mapFixture != null) mapFixture.close();
    if (http != null) http.stop(0);
    sessions.clear();
  }

  private void reloadTemplates() throws Exception {
    var next = new HashMap<String, MenuTemplate>();
    for (String name : catalogue.templates())
      next.put(
          name,
          dui.compile(
              "ui/" + name + ".html",
              DemoTemplates.normalize(Files.readString(directory.resolve("ui/" + name + ".html"))),
              CasinoComponents.registry(),
              DemoTemplates.environment(DemoTemplates.font())));
    var nextHud =
        dui.compileWorldHud(
            "ui/worldmap-hud.html",
            Files.readString(directory.resolve("ui/worldmap-hud.html")),
            CasinoComponents.registry(),
            DemoTemplates.environment(DemoTemplates.font()));
    nextHud.render(
        Map.of(
            "title",
            "HUD validation",
            "progress",
            0.5,
            "controls",
            List.of(),
            "zoom",
            12,
            "detail",
            false));
    for (var map : maps.values()) map.validateHud(nextHud);
    var oldHud = mapHudTemplate;
    var old = new HashMap<>(templates);
    templates.clear();
    templates.putAll(next);
    try {
      catalogue.validate();
      mapHudTemplate = nextHud;
      for (var map : maps.values()) map.refreshHud();
    } catch (Exception e) {
      mapHudTemplate = oldHud;
      templates.clear();
      templates.putAll(old);
      throw e;
    }
  }

  private DemoSession session(Player player) {
    return sessions.computeIfAbsent(
        player.getUniqueId(),
        id -> {
          var s = new DemoSession();
          s.display = read("display", id, DisplayPreferences.class, s.display);
          if (!s.display.valid()) s.display = new DisplayPreferences();
          s.kit.layout = s.display.layout;
          s.rewards = read("rewards", id, RewardState.class, s.rewards);
          if (!s.rewards.valid()) s.rewards = new RewardState();
          s.slots = read("slots", id, SlotState.class, s.slots);
          if (!s.slots.valid()) throw new IllegalStateException("Invalid demo chip ledger");
          if (s.slots.settle()) save("slots", id, s.slots);
          return s;
        });
  }

  private <T> T read(String folder, UUID id, Class<T> type, T fallback) {
    try {
      var file = directory.resolve(folder + "/" + id + ".json");
      if (!Files.exists(file)) return fallback;
      T value = JSON.fromJson(Files.readString(file), type);
      if (value == null) throw new IllegalStateException("Empty saved state");
      return value;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read demo state " + folder, e);
    }
  }

  private void save(String folder, UUID id, Object value) {
    try {
      var file = directory.resolve(folder + "/" + id + ".json");
      var temp = file.resolveSibling(file.getFileName() + ".tmp");
      Files.writeString(temp, JSON.toJson(value));
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot save demo state " + folder, e);
    }
  }

  private DemoServices services(Player player, DemoSession state) {
    return new DemoServices() {
      public MenuTemplate template(String name) {
        return Objects.requireNonNull(templates.get(name), name);
      }

      public EquipmentPort equipment() {
        return new PaperEquipment(player);
      }

      public java.util.concurrent.CompletionStage<PlayerAppearance> resolveAppearance(
          PlayerAppearance captured) {
        var profile = captured.profile();
        if (profile == null || !captured.fallback())
          return java.util.concurrent.CompletableFuture.completedFuture(captured);
        return profile.update().thenApply(p -> new PlayerAppearance(p, captured.armor()));
      }

      public String viewerName() {
        return player.getName();
      }

      public long tick() {
        return player.getWorld().getGameTime();
      }

      public LocalDate date() {
        return LocalDate.now(ZoneOffset.UTC);
      }

      public java.util.random.RandomGenerator random() {
        return random;
      }

      public Map<String, ItemStack> viewerItems(
          java.util.function.Function<Player, Map<String, ItemStack>> factory) {
        return factory.apply(player);
      }

      public void message(String text) {
        player.sendMessage(text);
      }

      public void save(String collection, Object value) {
        DuiDemoPlugin.this.save(collection, player.getUniqueId(), value);
      }

      public SlotState copySlots(SlotState value) {
        return DuiDemoPlugin.this.copySlots(value);
      }

      public void settleAfter(long ticks) {
        if (state.settlement != null) state.settlement.cancel();
        state.settlement =
            getServer()
                .getScheduler()
                .runTaskLater(
                    DuiDemoPlugin.this,
                    () -> {
                      settle(player.getUniqueId(), state);
                      state.settlement = null;
                      if (visible(player, state, "slots")) show(player, state);
                    },
                    ticks);
      }

      public VideoProvider videos() {
        return videos;
      }

      public TaskScope viewTasks() {
        return state.controller.viewTasks();
      }

      public TaskScope tasks() {
        return state.controller.tasks();
      }

      public void refresh() {
        if (visible(player, state, state.section)) show(player, state);
      }
    };
  }

  private void show(Player player, DemoSession state) {
    if (state.section.equals("map")) return;
    if (state.menu == null) {
      state.menu =
          ((DemoEntry.Dialogue) catalogue.find(state.section).orElseThrow().factory())
              .factory()
              .apply(services(player, state));
      state.menu.prepare(state, state.display.layout.equals("compact"));
    }
    var menu = state.menu;
    menu.capture(state);
    if (state.controller == null || !state.controller.active()) {
      state.controller =
          new MenuController<>(
                  dui,
                  player,
                  state,
                  new MenuDefinition<>(
                      menu.id(),
                      menu::project,
                      (s, ctx) -> {
                        s.tick = player.getWorld().getGameTime();
                        s.date = LocalDate.now(ZoneOffset.UTC);
                        s.reopenRequested = false;
                        menu.dispatch(s, ctx.action(), ctx.value(), ctx.response());
                        audit(player, s, ctx.action());
                        if (s.reopenRequested && !ctx.session().isActive()) {
                          s.controller = null;
                          show(player, s);
                        } else if (ctx.session().isActive()) menu.capture(s);
                      },
                      menu::closed))
              .onPresented(
                  view -> {
                    state.ui = view;
                    export(player, state, view.canvas(), menu.report(state, view.canvas()));
                    menu.presented(state, view.canvas());
                  });
    }
    state.controller.refresh();
  }

  private SlotState copySlots(SlotState value) {
    var next = JSON.fromJson(JSON.toJson(value), SlotState.class);
    next.compact = value.compact;
    next.paytable = value.paytable;
    next.startedAt = value.startedAt;
    return next;
  }

  private void settle(UUID id, DemoSession s) {
    var next = copySlots(s.slots);
    if (next.settle()) {
      save("slots", id, next);
      s.slots = next;
    }
  }

  private void finishSlots(UUID id, DemoSession s) {
    if (s.settlement != null) s.settlement.cancel();
    settle(id, s);
  }

  private boolean visible(Player p, DemoSession s, String section) {
    return p.isOnline()
        && sessions.get(p.getUniqueId()) == s
        && s.section.equals(section)
        && s.ui != null
        && s.ui.isActive()
        && dui.packLoaded(p);
  }

  private void export(Player p, DemoSession s, Canvas c, Map<String, Object> extra) {
    c = c.renderPlan();
    try {
      var data = new LinkedHashMap<String, Object>(extra);
      data.put("renderReport", RenderReport.of(c));
      data.put("renderNanos", s.ui.renderNanos());
      data.put("bodyCount", s.ui.bodyCount());
      data.put("width", c.width);
      data.put("height", c.height);
      data.put("hits", c.hits);
      data.put("items", c.items);
      data.put("motions", c.motions);
      data.put("motions", c.motions);
      data.put("effectMotions", c.effectMotions);
      data.put("coverage", c.coverage);
      data.put("heads", c.heads);
      data.put("playerModels", c.playerModels);
      data.put("paints", c.paints);
      data.put("effects", c.effects);
      data.put("packSha1", metadata.sha1());
      var images = new ArrayList<Map<String, Object>>();
      for (var i : c.images) {
        var rgb = new ArrayList<Integer>();
        for (int y = 0; y < i.raster().height; y++)
          for (int x = 0; x < i.raster().width; x++) rgb.add(i.raster().rgb(x, y));
        images.add(
            new LinkedHashMap<>(
                Map.of(
                    "id",
                    i.id(),
                    "x",
                    i.x(),
                    "y",
                    i.y(),
                    "width",
                    i.width(),
                    "height",
                    i.height(),
                    "pixelSize",
                    i.pixelSize(),
                    "columns",
                    i.raster().width,
                    "rows",
                    i.raster().height,
                    "rgb",
                    rgb)));
        images.getLast().put("background", i.background());
        images.getLast().put("argb", i.raster().pixels());
      }
      data.put("images", images);
      if (s.ui.component() != null) {
        String plain =
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(s.ui.component());
        data.put(
            "componentTextHash",
            HexFormat.of()
                .formatHex(
                    MessageDigest.getInstance("SHA-256")
                        .digest(plain.getBytes(StandardCharsets.UTF_8))));
        data.put(
            "componentBytes",
            net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
                .serialize(s.ui.component())
                .getBytes(StandardCharsets.UTF_8)
                .length);
      }
      var target = directory.resolve("layouts/" + p.getName() + ".json");
      var staging = target.resolveSibling(target.getFileName() + ".tmp");
      Files.writeString(staging, JSON.toJson(data));
      Files.move(
          staging, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      getLogger().info("DUI_DEMO_SHOW player=" + p.getName() + " section=" + extra.get("section"));
    } catch (Exception e) {
      getLogger().warning("Demo layout export failed: " + e.getMessage());
    }
  }

  private void audit(Player p, DemoSession s, String action) {
    try {
      Files.writeString(
          directory.resolve("actions.jsonl"),
          JSON.toJson(
                  auditState(
                      s,
                      Map.of(
                          "time",
                          Instant.now().toString(),
                          "player",
                          p.getName(),
                          "action",
                          action,
                          "display",
                          s.display,
                          "showcase",
                          s.kit,
                          "shop",
                          s.shop,
                          "rewards",
                          s.rewards,
                          "advent",
                          s.advent,
                          "warps",
                          s.warps,
                          "slots",
                          s.slots)))
              + "\n",
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (Exception e) {
      getLogger().warning("Demo action report failed: " + e.getMessage());
    }
  }

  private Map<String, Object> auditState(DemoSession s, Map<String, Object> base) {
    var result = new LinkedHashMap<>(base);
    result.put("poker", s.poker);
    result.put("blackjack", s.blackjack.publicState());
    result.put(
        "roulette",
        Map.of(
            "balance",
            s.roulette.balance,
            "phase",
            s.roulette.phase,
            "stake",
            s.roulette.stake(),
            "rounds",
            s.roulette.rounds));
    return result;
  }

  @EventHandler
  public void join(PlayerJoinEvent e) {
    getServer()
        .getScheduler()
        .runTaskLater(
            this,
            () -> {
              if (e.getPlayer().isOnline()) show(e.getPlayer(), session(e.getPlayer()));
            },
            20);
  }

  @EventHandler
  public void pack(PlayerResourcePackStatusEvent e) {
    if (e.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED
        && dui.packLoaded(e.getPlayer())) {
      getLogger().info("DUI_PACK_LOADED " + e.getPlayer().getName());
      if (!maps.containsKey(e.getPlayer().getUniqueId()))
        show(e.getPlayer(), session(e.getPlayer()));
    }
  }

  @EventHandler
  public void quit(PlayerQuitEvent e) {
    maps.remove(e.getPlayer().getUniqueId());
    var s = sessions.remove(e.getPlayer().getUniqueId());
    if (s != null) {
      if (s.controller != null) s.controller.close();
      finishSlots(e.getPlayer().getUniqueId(), s);
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String name = command.getName();
    String section = name.equals("gba") ? "gba" : catalogue.find(name).map(MenuCatalogue.Definition::id).orElse("components");
    var arguments = new ArrayList<>(List.of(args));
    if (name.equals("dui") && !arguments.isEmpty())
      section = arguments.removeFirst().toLowerCase(Locale.ROOT);
    if (section.equals("reload")) {
      if (!sender.hasPermission("dui-demo.reload")) {
        sender.sendMessage("You cannot reload the demo templates.");
        return true;
      }
      try {
        reloadTemplates();
        sender.sendMessage("dui / Templates reloaded.");
        for (var p : getServer().getOnlinePlayers()) {
          var s = sessions.get(p.getUniqueId());
          if (s != null && s.ui != null && s.ui.isActive()) show(p, s);
        }
      } catch (Exception e) {
        sender.sendMessage("Templates unchanged: " + e.getMessage());
      }
      return true;
    }
    Player p =
        sender instanceof Player player
            ? player
            : sender instanceof ProxiedCommandSender proxy
                    && proxy.getCallee() instanceof Player callee
                ? callee
                : null;
    if (p == null) {
      sender.sendMessage("Run /dui as a player.");
      return true;
    }
    if (section.equals("gba")) return gba.command(p, arguments);
    if (!section.equals("setup") && catalogue.find(section).isEmpty()) {
      if (section.equals("extensions")) {
        var s = session(p);
        if (s.controller != null) s.controller.close();
        s.controller = null;
        var map = maps.remove(p.getUniqueId());
        if (map != null) map.close();
        s.section = "extensions";
        var lab =
            new ExtensionLab(
                dui,
                metadata,
                p,
                (canvas, extra) -> {
                  if (s.ui != null) export(p, s, canvas, extra);
                });
        s.ui = lab.session();
        export(p, s, s.ui.canvas(), Map.of("section", "extensions", "age", 0, "clicks", 0));
        if (!arguments.isEmpty() && arguments.getFirst().equals("play")) lab.play();
        return true;
      }
      p.sendMessage(
          "/dui ["
              + String.join(
                  "|", catalogue.definitions().stream().map(MenuCatalogue.Definition::id).toList())
              + "|setup|reload]");
      return true;
    }
    if (section.equals("character")
        && !arguments.isEmpty()
        && arguments.getFirst().equalsIgnoreCase("kit")) {
      PaperEquipment.kit(p);
      return true;
    }
    var s = session(p);
    if (section.equals("map")) {
      String mode = arguments.isEmpty() ? "open" : arguments.getFirst().toLowerCase(Locale.ROOT);
      if (mapFixture != null && mapFixture.command(p, mode)) return true;
      if (mode.equals("dynamic")) {
        var old = maps.remove(p.getUniqueId());
        if (old != null) old.close();
        if (s.controller != null) s.controller.close();
        s.controller = null;
        s.ui = null;
        s.section = "map";
        new ExtensionMapLab(dui, p, directory);
        return true;
      }
      var previous = maps.get(p.getUniqueId());
      if (mode.equals("state")) {
        p.sendMessage(
            previous == null || !previous.session().isActive()
                ? "MAP_STATE closed"
                : "MAP_STATE " + JSON.toJson(previous.session().snapshot()));
        return true;
      }
      if (mode.equals("close")) {
        if (previous != null) previous.close();
        maps.remove(p.getUniqueId());
        return true;
      }
      if (!Set.of("open", "debug", "still").contains(mode)) {
        p.sendMessage("/worldmap [debug|still|state|close]");
        return true;
      }
      if (s.controller != null) s.controller.close();
      s.controller = null;
      s.ui = null;
      s.menu = null;
      if (previous != null) previous.close();
      s.section = "map";
      maps.put(
          p.getUniqueId(),
          new WorldMapDemo(
              dui, p, directory, mode.equals("debug"), mode.equals("still"), () -> mapHudTemplate));
      return true;
    }
    var previousMap = maps.remove(p.getUniqueId());
    if (previousMap != null) previousMap.close();
    if (s.controller != null) s.controller.close();
    s.section = section.equals("setup") ? "components" : section;
    s.preview = null;
    String option = arguments.isEmpty() ? "" : arguments.getFirst().toLowerCase(Locale.ROOT);
    boolean compact =
        option.isEmpty() ? s.display.layout.equals("compact") : option.equals("compact");
    if (section.equals("setup") || name.equals("uikit") && option.equals("setup")) {
      s.menu = new ShowcaseMenu(services(p, s));
      ((ShowcaseMenu) s.menu).setup(s);
      show(p, s);
      return true;
    }
    s.menu =
        ((DemoEntry.Dialogue) catalogue.find(s.section).orElseThrow().factory())
            .factory()
            .apply(services(p, s));
    s.videoForce = section.equals("videos") && option.equals("refresh");
    s.menu.prepare(s, compact);
    if (section.equals("components")) {
      s.kit.page = option.isEmpty() ? "basics" : option;
      if (ShowcaseState.PAGES.stream().noneMatch(page -> page.id().equals(s.kit.page)))
        s.kit.page = "basics";
    }
    show(p, s);
    return true;
  }
}
