package gg.kembel.dui.demo.browser;

import com.google.gson.Gson;
import gg.kembel.dui.core.*;
import gg.kembel.dui.core.video.*;
import gg.kembel.dui.demo.DemoTemplates;
import gg.kembel.dui.paper.*;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Browser process, navigation and UI policy belong to this consumer, not the library. */
public final class BrowserDemo implements Listener, AutoCloseable {
  private static final Gson JSON = new Gson();
  private final JavaPlugin plugin;
  private final Dui dui;
  private final Path directory, bridge, runtime;
  private final MenuTemplate menu;
  private final VideoSurfaceTemplate screen;
  private final Map<UUID, Session> sessions = new HashMap<>();
  private final ExecutorService launches = Executors.newFixedThreadPool(2, r -> {
    var thread = new Thread(r, "dui-demo-browser-launch"); thread.setDaemon(true); return thread;
  });
  private final BukkitTask ticker;
  private volatile boolean active = true;
  private int ticks;
  private static final class Session {
    final Player player;
    final VideoSurfaceSpec spec;
    final BrowserControls controls;
    volatile BrowserFrameSource source;
    volatile VideoFrame lastFrame;
    volatile VideoSurfaceSession surface;
    DialogSession menu;
    boolean changing, started;
    int zoomPercent;
    volatile boolean closed;
    Session(Player player, VideoSurfaceSpec spec, int zoomPercent) {
      this.player = player; this.spec = spec; this.zoomPercent = zoomPercent; controls = new BrowserControls(spec.width(), spec.height());
    }
  }
  public BrowserDemo(JavaPlugin plugin, Dui dui, Path directory) throws Exception {
    this.plugin = plugin; this.dui = dui; this.directory = directory;
    bridge = directory.resolve("browser/bridge.mjs");
    runtime = Path.of(plugin.getConfig().getString("browser.runtime", directory.resolve("browser/runtime").toString())).toAbsolutePath();
    for (String resource : List.of("ui/browser.html", "ui/browser-screen.html", "browser/bridge.mjs")) {
      Path file = directory.resolve(resource); Files.createDirectories(file.getParent());
      if (!Files.exists(file)) try (var input = plugin.getResource(resource)) { Files.copy(Objects.requireNonNull(input), file); }
    }
    menu = dui.compile("ui/browser.html", DemoTemplates.normalize(Files.readString(directory.resolve("ui/browser.html"))),
        ComponentRegistry.EMPTY, DemoTemplates.environment(DemoTemplates.font()));
    screen = VideoSurfaceTemplate.parse(Files.readString(directory.resolve("ui/browser-screen.html")),
        DemoTemplates.environment(DemoTemplates.font()), ComponentRegistry.EMPTY, "ui/browser-screen.html");
    screen.render(data(768, 432, 12, 49, 16777216, PixelFormat.BGR555, "Browser", "", "READY"));
    ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }
  private String home() { return BrowserControls.address(plugin.getConfig().getString("browser.home", "https://www.youtube.com/")); }
  public boolean command(Player player, List<String> args) {
    if (!player.hasPermission("dui-demo.browser")) { player.sendMessage("Browser access requires dui-demo.browser."); return true; }
    try {
      Session session = sessions.get(player.getUniqueId());
      String action = args.isEmpty() ? "open" : args.getFirst().toLowerCase(Locale.ROOT);
      if (action.equals("close")) { stop(player.getUniqueId()); return true; }
      if (action.equals("open")) {
        if (args.size() > 1) open(player, BrowserControls.address(String.join(" ", args.subList(1, args.size()))));
        else if (session != null && session.source != null) showScreen(session);
        else open(player, home());
        return true;
      }
      if (session == null) { open(player, home()); return true; }
      if (action.equals("menu")) { showMenu(session); return true; }
      if (action.equals("resume")) { showScreen(session); return true; }
      if (session.source == null) { player.sendMessage("Chromium is starting..."); return true; }
      switch (action) {
        case "back", "forward", "reload" -> session.source.command(action, Map.of());
        case "home" -> session.source.command("navigate", Map.of("url", home()));
        case "scroll" -> session.source.command("scroll", Map.of("dy", args.size() > 1 ? Integer.parseInt(args.get(1)) : 400));
        case "click" -> session.source.click();
        case "zoom" -> zoom(session, args.size() > 1 ? Integer.parseInt(args.get(1)) : 100);
        case "type" -> session.source.command("type", Map.of("text", String.join(" ", args.subList(1, args.size())), "enter", false));
        case "key" -> session.source.command("key", Map.of("key", args.size() > 1 ? args.get(1) : "Enter"));
        case "stats" -> { export(session); player.sendMessage("Browser: " + session.source.state() + " / " + session.source.statistics()); }
        default -> showMenu(session);
      }
    } catch (Exception e) { player.sendMessage("Browser: " + e.getMessage()); }
    return true;
  }
  private void open(Player player, String url) throws Exception {
    if (!Files.isRegularFile(runtime.resolve("node_modules/playwright/package.json")))
      throw new IllegalStateException("Install the server browser first: python3 scripts/browser_runtime.py");
    stop(player.getUniqueId());
    if (sessions.size() >= plugin.getConfig().getInt("browser.maximum-sessions", 2)) throw new IllegalStateException("All browser slots are occupied");
    int width = plugin.getConfig().getInt("browser.width", 1280), height = plugin.getConfig().getInt("browser.height", 720);
    double fps = plugin.getConfig().getDouble("browser.fps", 12);
    if (width < 128 || width > 2048 || height < 64 || height > 1152 || !Double.isFinite(fps) || fps < 1 || fps > 30)
      throw new IllegalArgumentException("Browser viewport 128..2048 x 64..1152; capture FPS 1..30");
    var format = PixelFormat.valueOf(plugin.getConfig().getString("browser.format", "BGR555").toUpperCase(Locale.ROOT));
    int tileWidth = 128 / format.symbols;
    int tiles = 1 + ((width + tileWidth - 1) / tileWidth) * ((height + 126) / 127);
    long bytes = plugin.getConfig().getLong("browser.bytes-per-second", 67108864);
    var requested = screen.render(data(width, height, fps, tiles, bytes, format, "Browser", url, "STARTING")).specification();
    var spec = screen.render(data(width, height, requested.sustainableFps(), tiles, bytes, format, "Browser", url, "STARTING")).specification();
    int initialZoom = BrowserControls.zoom(plugin.getConfig().getInt("browser.zoom", 100));
    var session = new Session(player, spec, initialZoom); sessions.put(player.getUniqueId(), session);
    try { showMenu(session); } catch (Exception e) { stop(player.getUniqueId()); throw e; }
    String node = plugin.getConfig().getString("browser.node", "node");
    launches.execute(() -> {
      try {
        var source = new BrowserFrameSource(node, bridge, runtime, url, spec, initialZoom);
        main(() -> { if (session.closed) { source.close(); return; } session.source = source; showScreen(session); }, source::close);
      } catch (Exception e) { main(() -> { if (!session.closed) { player.sendMessage("Browser: " + e.getMessage()); stop(player.getUniqueId()); } }); }
    });
  }
  private void showMenu(Session session) throws Exception {
    if (session.closed) return;
    session.changing = true; session.controls.release();
    if (session.source != null) session.source.pause(true);
    if (session.surface != null) { session.surface.close(); session.surface = null; }
    if (session.menu != null) session.menu.close();
    var state = session.source == null ? null : session.source.state();
    var options = new DialogOptions(Component.text("DUI / Browser controls"),
        List.of(DialogInput.text("address", Component.text("Address or YouTube search"))
            .initial(state == null ? home() : state.url()).maxLength(512).width(320).build(),
            DialogInput.text("typed", Component.text("Text for the focused browser field"))
                .initial("").maxLength(512).width(320).build()),
        List.of(new DialogOptions.Button("Open / Search", "address", 104),
            new DialogOptions.Button("Type", "type", 104), new DialogOptions.Button("Type + Enter", "submit", 104)),
        new DialogOptions.Button("Close browser", "close", 320), 3, false);
    session.menu = dui.open(session.player, menu,
        ViewModel.data(Map.of("title", state == null ? "Starting Chromium..." : trim(state.title(), 45),
            "status", state == null ? "Preparing a private browser session" : state.error().isBlank() ? "Session stays open while you use this menu" : trim(state.error(), 65),
            "running", session.source != null, "zoom", session.zoomPercent + "%")), options, event -> {
          try {
            String action = event.action();
            if (action.equals("close")) { stop(session.player.getUniqueId()); return; }
            if (action.equals("address")) {
              String target = BrowserControls.address(event.response().getText("address"));
              if (session.source == null) { open(session.player, target); return; }
              session.source.command("navigate", Map.of("url", target));
            } else if (session.source != null) {
              switch (action) {
                case "type", "submit" -> session.source.command("type", Map.of("text", event.response().getText("typed"), "enter", action.equals("submit")));
                case "up", "down" -> session.source.command("scroll", Map.of("dy", action.equals("up") ? -400 : 400));
                case "home" -> session.source.command("navigate", Map.of("url", home()));
                case "enter", "escape", "space", "tab" -> session.source.command("key", Map.of("key", switch (action) { case "escape" -> "Escape"; case "space" -> "Space"; case "tab" -> "Tab"; default -> "Enter"; }));
                case "back", "forward", "reload" -> session.source.command(action, Map.of());
                case "click" -> session.source.click();
                case "zoom_out" -> zoom(session, Math.max(50, session.zoomPercent - 25));
                case "zoom_in" -> zoom(session, Math.min(250, session.zoomPercent + 25));
                case "zoom_reset" -> zoom(session, 100);
              }
            }
            showScreen(session);
          } catch (Exception e) { session.player.sendMessage("Browser: " + e.getMessage()); }
        });
    session.changing = false;
    session.menu.onClose(() -> { if (!session.changing) stop(session.player.getUniqueId()); });
    export(session);
  }
  private void zoom(Session session, int percent) {
    BrowserControls.zoom(percent);
    session.source.command("zoom", Map.of("percent", percent));
    session.zoomPercent = percent;
  }
  private void showScreen(Session session) {
    if (session.closed || session.source == null) return;
    session.changing = true;
    if (session.menu != null) { session.menu.close(); session.menu = null; }
    if (session.surface != null) { session.surface.close(); session.surface = null; }
    session.surface = dui.openVideoSurface(session.player, session.spec, new VideoSurfaceOptions(8, true), input -> {
      if (session.closed || session.changing) return;
      if (input.type() == SurfaceInput.Type.SLOT) {
        switch (input.slot()) {
          case 0 -> session.source.click();
          case 1, 2, 5 -> session.source.command(input.slot() == 1 ? "back" : input.slot() == 2 ? "forward" : "reload", Map.of());
          case 3, 4 -> session.source.command("scroll", Map.of("dy", input.slot() == 3 ? -400 : 400));
          case 6 -> session.source.command("navigate", Map.of("url", home()));
          case 7 -> { try { showMenu(session); } catch (Exception e) { session.player.sendMessage(e.getMessage()); } }
        }
      } else if (session.controls.accept(input)) session.source.click();
    });
    session.surface.onClose(() -> { if (!session.changing) stop(session.player.getUniqueId()); });
    if (plugin.getConfig().getBoolean("browser.mouse-controls", true)) session.surface.pointerInput(input -> {
      if (session.closed || session.changing) return;
      switch (input.type()) {
        case LOOK -> session.source.pointer(session.controls.mouse(input.deltaYaw(), input.deltaPitch(),
            plugin.getConfig().getDouble("browser.pixels-per-degree", session.spec.width() / 90d)));
        case PRIMARY -> session.source.click();
        case SECONDARY -> { try { showMenu(session); } catch (Exception e) { session.player.sendMessage(e.getMessage()); } }
        case SCROLL -> session.source.command("scroll", Map.of("dy", input.scrollSteps() * 180));
      }
    });
    session.changing = false;
    if (!session.surface.isActive()) { stop(session.player.getUniqueId()); return; }
    session.source.pause(false);
    if (session.lastFrame != null) session.surface.submit(session.lastFrame);
    start(session);
  }
  private void start(Session session) {
    if (session.started || session.source == null || session.surface == null || !session.surface.isStarted()) return;
    session.started = true;
    session.source.start(frame -> {
      if (session.closed) return; session.lastFrame = frame;
      var target = session.surface; if (target != null && target.isActive()) target.submit(frame);
    }, error -> main(() -> {
      if (!session.closed) { session.player.sendMessage(trim(error.getMessage(), 220)); plugin.getLogger().warning(error.getMessage()); stop(session.player.getUniqueId()); }
    }), () -> {});
  }
  private void tick() {
    ticks++;
    for (var session : List.copyOf(sessions.values())) {
      if (session.source == null) continue;
      start(session);
      if (session.surface != null && session.surface.isStarted()) session.source.pointer(session.controls.advance(.05));
      if (ticks % 10 != 0) continue;
      if (session.surface != null) {
        var state = session.source.state(); var spec = session.spec;
        int tiles = spec.budget().maximumTiles();
        session.surface.hud(screen.render(data(spec.width(), spec.height(), spec.maximumFps(), tiles, spec.budget().bytesPerSecond(),
            spec.format(), trim(state.title(), 45), trim(state.url(), 65), state.error().isBlank() ? "LIVE / " + state.zoomPercent() + "% / AUDIO OFF" : trim(state.error(), 60))).hud());
      }
      export(session);
    }
  }
  private void export(Session session) {
    try {
      var data = new LinkedHashMap<String, Object>();
      data.put("state", session.source == null ? null : session.source.state()); data.put("output", session.spec);
      data.put("source", session.source == null ? null : session.source.statistics());
      data.put("transport", session.surface == null ? null : session.surface.statistics());
      data.put("cursor", session.controls.cursor()); data.put("closed", session.closed);
      data.put("sampledAtNanos", System.nanoTime());
      Path file = directory.resolve("layouts/" + session.player.getUniqueId() + "-browser.json");
      Path temporary = file.resolveSibling(file.getFileName() + ".pending");
      Files.writeString(temporary, JSON.toJson(data)); Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (Exception e) { plugin.getLogger().fine("Browser stats: " + e.getMessage()); }
  }
  private void stop(UUID id) {
    var session = sessions.remove(id); if (session == null) return;
    session.closed = true; export(session);
    if (session.source != null) session.source.close();
    if (session.surface != null) session.surface.close();
    if (session.menu != null) session.menu.close();
  }
  private void main(Runnable work) {
    main(work, () -> {});
  }
  private void main(Runnable work, Runnable discarded) {
    if (!active) { discarded.run(); return; }
    try { plugin.getServer().getScheduler().runTask(plugin, () -> { if (active) work.run(); else discarded.run(); }); }
    catch (org.bukkit.plugin.IllegalPluginAccessException e) { discarded.run(); }
  }
  private static String trim(String value, int length) { return value == null ? "" : value.length() <= length ? value : value.substring(0, length - 3) + "..."; }
  static Map<String, Object> data(int width, int height, double fps, int tiles, long bytes, PixelFormat format, String title, String url, String status) {
    return Map.of("width", width, "height", height, "fps", fps, "tiles", tiles, "bytes", bytes,
        "format", format.name(), "title", title, "url", url, "status", status);
  }
  @EventHandler public void quit(PlayerQuitEvent event) { stop(event.getPlayer().getUniqueId()); }
  @Override public void close() {
    for (UUID id : List.copyOf(sessions.keySet())) stop(id);
    active = false; ticker.cancel(); launches.shutdownNow(); HandlerList.unregisterAll(this);
  }
}
