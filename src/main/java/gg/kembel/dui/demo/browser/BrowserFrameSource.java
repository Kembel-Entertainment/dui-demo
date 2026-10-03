package gg.kembel.dui.demo.browser;

import com.google.gson.*;
import gg.kembel.dui.core.video.*;
import gg.kembel.dui.demo.media.FrameSource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

/** External browser producer. This process adapter has no dependency on DUI internals. */
public final class BrowserFrameSource implements FrameSource {
  public record State(String url, String title, boolean loading, String error, String lastAction, long actionId, int width, int height, int zoomPercent) {}
  private static final Gson JSON = new Gson();
  private final Process process;
  private final VideoSurfaceSpec spec;
  private final ArrayBlockingQueue<String> commands = new ArrayBlockingQueue<>(64);
  private final AtomicBoolean closed = new AtomicBoolean(), started = new AtomicBoolean();
  private final AtomicLong actions = new AtomicLong(), frames = new AtomicLong();
  private volatile BrowserControls.Cursor cursor;
  private volatile boolean paused;
  private volatile double preparationMillis;
  private volatile String error = "", stderr = "";
  private volatile State state;
  private long beginning;
  public BrowserFrameSource(String node, Path bridge, Path runtime, String url, VideoSurfaceSpec spec) throws IOException {
    this(node, bridge, runtime, url, spec, 100);
  }
  public BrowserFrameSource(String node, Path bridge, Path runtime, String url, VideoSurfaceSpec spec, int zoomPercent) throws IOException {
    BrowserControls.zoom(zoomPercent); this.spec = spec;
    state = new State(BrowserControls.address(url), "Starting Chromium...", true, "", "", 0, spec.width(), spec.height(), zoomPercent);
    cursor = new BrowserControls.Cursor(spec.width() / 2, spec.height() / 2);
    process = new ProcessBuilder(node, bridge.toAbsolutePath().toString(), "--runtime", runtime.toAbsolutePath().toString(),
        "--url", state.url(), "--width", "" + spec.width(), "--height", "" + spec.height(), "--fps", "" + spec.maximumFps(), "--zoom", "" + zoomPercent)
        .directory(runtime.toFile()).start();
    Thread.ofVirtual().name("dui-browser-errors").start(() -> {
      try (var reader = process.errorReader(StandardCharsets.UTF_8)) {
        String line; while ((line = reader.readLine()) != null) stderr = (stderr + line + '\n').substring(0, Math.min(4096, stderr.length() + line.length() + 1));
      } catch (IOException ignored) {}
    });
  }
  public State state() { return state; }
  public long command(String action, Map<String, Object> values) {
    if (closed.get()) return -1;
    var message = new HashMap<String, Object>(values);
    long id = actions.incrementAndGet(); message.put("action", action); message.put("id", id);
    if (!commands.offer(JSON.toJson(message))) throw new IllegalStateException("Browser is busy; try again shortly");
    return id;
  }
  public void pointer(BrowserControls.Cursor next) {
    if (next.equals(cursor)) return;
    cursor = next;
    // Mouse state is replaceable. Discrete clicks/navigation retain their ordering.
    commands.removeIf(value -> value.contains("\"action\":\"pointer\""));
    command("pointer", Map.of("x", next.x(), "y", next.y()));
  }
  public void click() { var at = cursor; command("click", Map.of("x", at.x(), "y", at.y())); }
  @Override public void start(Consumer<VideoFrame> sink, Consumer<Throwable> failure, Runnable ended) {
    if (!started.compareAndSet(false, true)) throw new IllegalStateException("Browser source already started");
    beginning = System.nanoTime();
    Thread.ofVirtual().name("dui-browser-controls").start(() -> {
      try (var output = process.outputWriter(StandardCharsets.UTF_8)) {
        while (!closed.get()) {
          String command = commands.poll(100, TimeUnit.MILLISECONDS);
          if (command != null) { output.write(command); output.newLine(); output.flush(); }
        }
      } catch (Exception e) { if (!closed.get()) failure.accept(e); }
    });
    Thread.ofVirtual().name("dui-browser-pixels").start(() -> {
      try (var input = new DataInputStream(process.getInputStream())) {
        while (!closed.get()) {
          int type = input.readUnsignedByte();
          byte[] payload = readPayload(input);
          if (type == 2) {
            var next = JSON.fromJson(new String(payload, StandardCharsets.UTF_8), State.class);
            if (next == null || next.url() == null || next.title() == null || next.error() == null
                || next.width() != spec.width() || next.height() != spec.height() || next.zoomPercent() < 50 || next.zoomPercent() > 250)
              throw new IOException("Invalid browser state");
            state = next;
          } else if (type == 1) {
            if (paused) continue;
            long begin = System.nanoTime();
            var picture = ImageIO.read(new ByteArrayInputStream(payload));
            if (picture == null || picture.getWidth() < spec.width() || picture.getHeight() < spec.height()
                || picture.getWidth() > spec.width() + 2 || picture.getHeight() > spec.height() + 2)
              throw new IOException("Browser picture does not match the viewport: " + (picture == null ? "invalid PNG"
                  : picture.getWidth() + "x" + picture.getHeight()) + " expected " + spec.width() + "x" + spec.height());
            // Integer CSS clips can add up to two device pixels at fractional DPR (max zoom 2.5).
            // Crop only that rounded edge; never rescale the page or shift pointer coordinates.
            if (picture.getWidth() != spec.width() || picture.getHeight() != spec.height())
              picture = picture.getSubimage(0, 0, spec.width(), spec.height());
            var frame = frame(picture, spec.format(), frames.incrementAndGet(), cursor);
            preparationMillis = (System.nanoTime() - begin) / 1e6;
            sink.accept(frame);
          } else throw new IOException("Unknown browser stream message");
        }
      } catch (Exception e) {
        if (!closed.get()) {
          error = "Headless browser stopped. Run scripts/browser_runtime.py; " + (stderr.isBlank() ? e.getMessage() : stderr);
          failure.accept(new IOException(error, e));
        }
      } finally { if (!closed.get()) ended.run(); }
    });
  }
  static byte[] readPayload(DataInputStream input) throws IOException {
    int length = input.readInt();
    if (length < 1 || length > 8 * 1024 * 1024) throw new IOException("Invalid browser payload length");
    byte[] data = new byte[length]; input.readFully(data); return data;
  }
  static VideoFrame frame(BufferedImage image, PixelFormat format, long sequence, BrowserControls.Cursor cursor) {
    // Cursor is consumer artwork painted into the live frame, never a pack asset or page modification.
    if (cursor != null) {
      Graphics2D g = image.createGraphics();
      try {
        int x = cursor.x(), y = cursor.y();
        var triangle = new Polygon(new int[] {x, x, x + 11}, new int[] {y, y + 16, y + 11}, 3);
        g.setColor(Color.WHITE); g.fillPolygon(triangle); g.setColor(new Color(0x101722)); g.drawPolygon(triangle);
      } finally { g.dispose(); }
    }
    int w = image.getWidth(), h = image.getHeight();
    int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
    for (int i = 0; i < pixels.length; i++) {
      int rgb = pixels[i];
      pixels[i] = format == PixelFormat.RGB888 ? rgb & 0xffffff
          : (rgb >> 19 & 31) | (rgb >> 11 & 31) << 5 | (rgb >> 3 & 31) << 10;
    }
    return new VideoFrame(w, h, format, sequence, pixels);
  }
  @Override public void pause(boolean value) { paused = value; command("pause", Map.of("paused", value)); }
  @Override public boolean paused() { return paused; }
  @Override public Statistics statistics() {
    return new Statistics(frames.get(), beginning == 0 ? 0 : (System.nanoTime() - beginning) / 1e9,
        paused, closed.get(), preparationMillis, error, 1);
  }
  public boolean alive() { return process.isAlive(); }
  @Override public void close() {
    if (!closed.compareAndSet(false, true)) return;
    var descendants = process.descendants().toList();
    process.destroy(); // Node handles SIGTERM by closing Chromium and its temporary context.
    Thread.ofVirtual().name("dui-browser-close").start(() -> {
      try { if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly(); }
      catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      for (var child : descendants) if (child.isAlive()) child.destroyForcibly();
    });
  }
}
