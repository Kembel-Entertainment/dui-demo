package gg.kembel.dui.demo.media;

import com.google.gson.Gson;
import gg.kembel.dui.core.*;
import gg.kembel.dui.core.video.*;
import gg.kembel.dui.paper.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** File selection/decoding is consumer policy; the existing DUI surface transports the pictures. */
public final class CinemaDemo implements Listener, AutoCloseable {
  private final JavaPlugin plugin;
  private final Dui dui;
  private final Path directory;
  private final MediaFiles files;
  private final MenuTemplate menu;
  private final VideoSurfaceTemplate screen;
  private final Map<UUID, Session> sessions = new HashMap<>();
  private final ExecutorService launches = Executors.newFixedThreadPool(2, r -> {
    var t = new Thread(r, "dui-demo-media-launch"); t.setDaemon(true); return t;
  });
  private final BukkitTask ticker;
  private volatile boolean active = true;
  private static final Gson JSON = new Gson();
  private static final class Session {
    final Player player;
    final Path file;
    volatile FrameSource source;
    volatile VideoSurfaceSession surface;
    volatile VideoFrame lastFrame;
    volatile boolean closed;
    DialogSession menu;
    FfmpegFrameSource.Info info;
    Map<String, Object> data;
    VideoSurfaceSpec spec;
    final java.util.concurrent.atomic.AtomicLong sequence = new java.util.concurrent.atomic.AtomicLong();
    final PlaybackControls controls = new PlaybackControls();
    double speed = 1, requestedFps;
    boolean changing, started;
    Session(Player player, Path file) { this.player = player; this.file = file; }
  }
  public CinemaDemo(JavaPlugin plugin, Dui dui, Path directory) throws Exception {
    this.plugin = plugin; this.dui = dui; this.directory = directory;
    files = new MediaFiles(directory.resolve("media/files"));
    for (String name : List.of("cinema", "cinema-screen")) {
      Path template = directory.resolve("ui/" + name + ".html");
      if (!Files.exists(template)) try (var input = plugin.getResource("ui/" + name + ".html")) {
        Files.copy(Objects.requireNonNull(input), template);
      }
    }
    menu = dui.compile("ui/cinema.html", gg.kembel.dui.demo.DemoTemplates.normalize(Files.readString(directory.resolve("ui/cinema.html"))),
        ComponentRegistry.EMPTY, gg.kembel.dui.demo.DemoTemplates.environment(gg.kembel.dui.demo.DemoTemplates.font()));
    screen = VideoSurfaceTemplate.parse(Files.readString(directory.resolve("ui/cinema-screen.html")),
        gg.kembel.dui.demo.DemoTemplates.environment(gg.kembel.dui.demo.DemoTemplates.font()),
        ComponentRegistry.EMPTY, "ui/cinema-screen.html");
    screen.render(data("Cinema", 384, 216, 30, 25, 16777216, "READY", "", 0, PixelFormat.RGB888));
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 10);
  }
  public boolean command(Player player, List<String> arguments) {
    try {
      String action = arguments.isEmpty() ? "menu" : arguments.getFirst().toLowerCase(Locale.ROOT);
      var session = sessions.get(player.getUniqueId());
      switch (action) {
        case "play" -> {
          if (arguments.size() < 2) showMenu(player);
          else play(player, arguments.get(1), arguments.size() > 2 ? Integer.parseInt(arguments.get(2)) : plugin.getConfig().getInt("media.width", 384),
              arguments.size() > 3 ? Double.parseDouble(arguments.get(3)) : plugin.getConfig().getDouble("media.fps", 30),
              arguments.size() > 4 ? Double.parseDouble(arguments.get(4)) : 0,
              PixelFormat.valueOf((arguments.size() > 5 ? arguments.get(5) : plugin.getConfig().getString("media.format", "RGB888")).toUpperCase(Locale.ROOT)));
        }
        case "resume" -> { if (session == null || session.source == null) showMenu(player); else showScreen(session); }
        case "pause" -> { if (session != null && session.source != null) session.source.pause(!session.source.paused()); }
        case "restart" -> { if (session != null && session.source != null) replaceSource(session, 0, session.speed); }
        case "seek" -> { if (session != null && session.spec != null && arguments.size() > 1) {
          double position = Double.parseDouble(arguments.get(1));
          if (!Double.isFinite(position) || position < 0 || position >= session.info.duration())
            throw new IllegalArgumentException("Seek within the video duration");
          replaceSource(session, position, session.speed);
        } }
        case "speed" -> { if (session != null && session.source != null && arguments.size() > 1)
          replaceSource(session, session.source.statistics().seconds(), Double.parseDouble(arguments.get(1))); }
        case "close" -> stop(player.getUniqueId());
        case "stats" -> {
          if (session == null || session.source == null) player.sendMessage("No video playing.");
          else {
            player.sendMessage("Video: " + session.info.width() + "x" + session.info.height() + " @ "
                + String.format(Locale.ROOT, "%.2f", session.info.fps()) + " -> "
                + session.spec.width() + "x" + session.spec.height() + " @ " + session.spec.maximumFps()
                + " / " + session.source.statistics());
            if (session.surface != null) player.sendMessage("Transport: " + session.surface.statistics());
            export(session);
          }
        }
        default -> showMenu(player);
      }
    } catch (Exception e) { player.sendMessage("Cinema: " + e.getMessage()); }
    return true;
  }
  private void showMenu(Player player) throws Exception {
    Session session = sessions.get(player.getUniqueId());
    if (session != null) {
      session.changing = true;
      if (session.source != null) session.source.pause(true);
      if (session.surface != null) { session.surface.close(); session.surface = null; }
      if (session.menu != null) session.menu.close();
    }
    var entries = new ArrayList<Map<String, Object>>(); int y = 72;
    var catalogue = files.list();
    for (Path file : catalogue.stream().limit(5).toList()) {
      entries.add(Map.of("id", file.getFileName().toString(), "label", title(file), "y", y)); y += 27;
    }
    var view = dui.open(player, menu, ViewModel.data(Map.of("files", entries, "running", session != null && session.source != null,
        "status", session != null && session.source == null ? "Preparing the decoder..." : catalogue.isEmpty()
            ? "Add a local video to media/files/." : "Local video / real-time pixels / audio off")),
        DialogOptions.notice("DUI / Cinema", "Close", "close"), event -> {
          if (event.action().equals("play")) command(player, List.of("play", event.value()));
          else if (event.action().equals("resume")) command(player, List.of("resume"));
          else event.session().close();
        });
    if (session != null) {
      session.menu = view; session.changing = false;
      view.onClose(() -> { if (!session.changing) stop(player.getUniqueId()); });
    }
  }
  private void play(Player player, String alias, int width, double fps, double startSeconds, PixelFormat format) throws Exception {
    if (width < 128 || width > 1024 || !Double.isFinite(fps) || fps < 1 || fps > 60)
      throw new IllegalArgumentException("Output width: 128..1024; FPS: 1..60");
    if (!Double.isFinite(startSeconds) || startSeconds < 0) throw new IllegalArgumentException("Video start time");
    Path file = files.find(alias);
    stop(player.getUniqueId());
    if (sessions.size() >= plugin.getConfig().getInt("media.maximum-sessions", 2))
      throw new IllegalArgumentException("All video slots are occupied");
    Session session = new Session(player, file);
    session.requestedFps = fps;
    sessions.put(player.getUniqueId(), session);
    try { showMenu(player); }
    catch (Exception error) { stop(player.getUniqueId()); throw error; }
    String ffprobe = plugin.getConfig().getString("media.ffprobe", "ffprobe");
    String ffmpeg = plugin.getConfig().getString("media.ffmpeg", "ffmpeg");
    long bytes = plugin.getConfig().getLong("media.bytes-per-second", 16777216);
    launches.execute(() -> {
      try {
        var info = FfmpegFrameSource.probe(ffprobe, file);
        if (startSeconds >= info.duration()) throw new IllegalArgumentException("Start time exceeds video duration");
        int outputWidth = Math.min(width, info.width());
        int height = Math.max(1, (int) Math.round((double) outputWidth * info.height() / info.width()));
        int tileWidth = 128 / format.symbols;
        int tiles = 1 + ((outputWidth + tileWidth - 1) / tileWidth) * ((height + 126) / 127);
        var requested = screen.render(data(title(file), outputWidth, height, fps, tiles, bytes,
            "PLAYING / AUDIO OFF", "", 0, format)).specification();
        double effectiveFps = requested.sustainableFps();
        var bindings = data(title(file), outputWidth, height, effectiveFps, tiles, bytes,
            "PLAYING / AUDIO OFF", sourceDescription(info, outputWidth, height, effectiveFps, format), startSeconds / info.duration(), format);
        var spec = screen.render(bindings).specification();
        var source = new FfmpegFrameSource(ffmpeg, file, spec, startSeconds, session.speed, session.sequence::incrementAndGet);
        main(() -> {
          if (session.closed) { source.close(); return; }
          session.info = info; session.spec = spec; session.data = bindings; session.source = source;
          if (effectiveFps < fps - .01) player.sendMessage(String.format(Locale.ROOT,
              "Cinema: %.1f FPS at %dx%d (requested %.1f); %.1f MiB/s stream budget. Use a smaller output for more FPS.",
              effectiveFps, outputWidth, height, fps, bytes / 1048576d));
          showScreen(session);
        });
      } catch (Exception e) { main(() -> {
        if (!session.closed) { player.sendMessage("Video could not start: " + e.getMessage()); stop(player.getUniqueId()); }
      }); }
    });
  }
  /** Replace only the decoder: the seat, private textures, surface and monotonic sequence remain. */
  private void replaceSource(Session session, double position, double speed) {
    if (!Double.isFinite(speed) || speed < .25 || speed > 4) throw new IllegalArgumentException("Speed: 0.25..4");
    position = Math.max(0, Math.min(session.info.duration() - .001, position));
    FrameSource previous = session.source;
    var source = new FfmpegFrameSource(plugin.getConfig().getString("media.ffmpeg", "ffmpeg"),
        session.file, session.spec, position, speed, session.sequence::incrementAndGet);
    source.pause(previous.paused());
    synchronized (session) {
      session.source = source; session.speed = speed; session.started = false;
    }
    previous.close();
    startWhenReady(session); export(session);
  }
  private void main(Runnable action) {
    if (active) try { plugin.getServer().getScheduler().runTask(plugin, () -> { if (active) action.run(); }); }
    catch (IllegalStateException ignored) {}
  }
  private void showScreen(Session session) {
    if (session.closed || session.source == null) return;
    session.changing = true;
    if (session.menu != null) { session.menu.close(); session.menu = null; }
    if (session.surface != null) session.surface.close();
    session.source.pause(false);
    var presentation = screen.render(session.data);
    session.surface = dui.openVideoSurface(session.player, presentation.specification(), new VideoSurfaceOptions(8, false), input -> {
      if (input.type() == SurfaceInput.Type.SLOT) {
        if (input.slot() == 0) session.source.pause(!session.source.paused());
        else if (input.slot() == 1) { command(session.player, List.of("restart")); return; }
        else if (input.slot() == 7) { command(session.player, List.of("menu")); return; }
      } else if (input.sneak()) { stop(session.player.getUniqueId()); return; }
      var adjustment = session.controls.accept(input);
      if (adjustment.seekSeconds() != 0 || adjustment.speedSteps() != 0) {
        replaceSource(session, session.source.statistics().seconds() + adjustment.seekSeconds(),
            PlaybackControls.speed(session.speed, adjustment.speedSteps()));
      }
      session.source.input(input);
    }).onClose(() -> { if (!session.changing) stop(session.player.getUniqueId()); });
    session.surface.hud(presentation.hud()); session.changing = false;
    if (!session.surface.isActive()) { stop(session.player.getUniqueId()); return; }
    if (session.lastFrame != null) session.surface.submit(session.lastFrame);
    startWhenReady(session);
  }
  private void startWhenReady(Session session) {
    if (session.started || session.source == null || session.surface == null || !session.surface.isStarted()) return;
    session.started = true;
    FrameSource source = session.source;
    source.start(frame -> {
      synchronized (session) {
        if (session.closed || session.source != source) return;
        session.lastFrame = frame;
        var target = session.surface;
        if (target != null && target.isActive()) target.submit(frame);
      }
    }, error -> main(() -> {
      if (!session.closed && session.source == source) {
        session.player.sendMessage("Video decoder stopped: " + error.getMessage());
        plugin.getLogger().log(java.util.logging.Level.WARNING, "Video decoding failed", error);
        stop(session.player.getUniqueId());
      }
    }), () -> main(() -> { if (!session.closed && session.source == source) export(session); }));
  }
  private void tick() {
    for (Session session : List.copyOf(sessions.values())) {
      if (session.source == null || session.surface == null) continue;
      startWhenReady(session);
      var state = session.source.statistics();
      var bindings = new HashMap<>(session.data);
      bindings.put("status", (state.ended() ? "FINISHED / 2 TO REPLAY" : state.paused() ? "PAUSED" : "PLAYING / AUDIO OFF")
          + String.format(Locale.ROOT, " / %.2fx", session.speed));
      bindings.put("progress", Math.min(1, state.seconds() / session.info.duration()));
      bindings.put("time", time(state.seconds()) + " / " + time(session.info.duration()));
      session.surface.hud(screen.render(bindings).hud());
      export(session);
    }
  }
  private void export(Session session) {
    try {
      var result = new LinkedHashMap<String, Object>();
      result.put("file", session.file.getFileName().toString()); result.put("input", session.info);
      result.put("output", session.spec); result.put("source", session.source.statistics());
      result.put("requestedFps", session.requestedFps);
      result.put("maximumFrameBytes", session.spec.maximumFrameBytes());
      result.put("started", session.surface != null && session.surface.isStarted());
      if (session.surface != null) result.put("transport", session.surface.statistics());
      result.put("sampledAtNanos", System.nanoTime());
      Path target = directory.resolve("layouts/" + session.player.getUniqueId() + "-cinema.json");
      Path temporary = target.resolveSibling(target.getFileName() + ".pending");
      Files.writeString(temporary, JSON.toJson(result));
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (Exception e) { plugin.getLogger().fine("Cinema stats: " + e.getMessage()); }
  }
  private void stop(UUID id) {
    Session session = sessions.remove(id);
    if (session == null) return;
    session.closed = true;
    if (session.source != null) session.source.close();
    if (session.surface != null) session.surface.close();
    if (session.menu != null) session.menu.close();
  }
  private static String title(Path file) { return MediaFiles.stem(file).replace('-', ' ').replace('_', ' '); }
  private static String time(double seconds) {
    int total = Math.max(0, (int) seconds);
    return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60);
  }
  private static String sourceDescription(FfmpegFrameSource.Info info, int w, int h, double fps, PixelFormat format) {
    return String.format(Locale.ROOT, "%dx%d -> %dx%d / %.1f FPS / %d-bit color", info.width(), info.height(), w, h, fps,
        format == PixelFormat.BGR555 ? 15 : 24);
  }
  private static Map<String, Object> data(String title, int width, int height, double fps, int tiles,
      long bytes, String status, String source, double progress, PixelFormat format) {
    var result = new HashMap<String, Object>(Map.of("title", title, "width", width, "height", height, "fps", fps, "tiles", tiles,
        "bytes", bytes, "status", status, "source", source, "progress", progress, "time", "0:00"));
    result.put("format", format.name()); return result;
  }
  @EventHandler public void quit(PlayerQuitEvent event) { stop(event.getPlayer().getUniqueId()); }
  @Override public void close() {
    for (UUID id : List.copyOf(sessions.keySet())) stop(id);
    active = false; ticker.cancel(); launches.shutdownNow(); HandlerList.unregisterAll(this);
  }
}
