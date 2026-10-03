package gg.kembel.dui.demo.media;

import com.google.gson.JsonParser;
import gg.kembel.dui.core.video.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** A silent, paced FFmpeg pipe. The OS pipe and DUI's latest-frame mailbox bound buffering. */
public final class FfmpegFrameSource implements FrameSource {
  public record Info(int width, int height, double fps, double duration) {
    public Info {
      if (width < 1 || height < 1 || !Double.isFinite(fps) || fps <= 0
          || !Double.isFinite(duration) || duration <= 0) throw new IllegalArgumentException("Video metadata");
    }
  }
  private final List<String> command;
  private final VideoSurfaceSpec spec;
  private final double startSeconds;
  private final double speed;
  private final LongSupplier sequence;
  private final Object clock = new Object();
  private final AtomicBoolean launched = new AtomicBoolean();
  private final AtomicLong frames = new AtomicLong(), conversionNanos = new AtomicLong();
  private volatile boolean closed, paused, ended;
  private volatile Process process;
  private volatile String error = "";
  private boolean resetClock;
  private Thread reader;

  public static Info probe(String executable, Path file) throws Exception {
    var p = new ProcessBuilder(executable, "-v", "error", "-select_streams", "v:0",
        "-show_entries", "stream=width,height,avg_frame_rate:format=duration", "-of", "json",
        file.toAbsolutePath().toString()).redirectErrorStream(true).start();
    try {
      if (!p.waitFor(10, TimeUnit.SECONDS)) throw new IOException("ffprobe timed out");
      String output = new String(p.getInputStream().readNBytes(16384), StandardCharsets.UTF_8);
      if (p.exitValue() != 0) throw new IOException("ffprobe: " + output.strip());
      var json = JsonParser.parseString(output).getAsJsonObject();
      var stream = json.getAsJsonArray("streams").get(0).getAsJsonObject();
      String[] fps = stream.get("avg_frame_rate").getAsString().split("/");
      return new Info(stream.get("width").getAsInt(), stream.get("height").getAsInt(),
          Double.parseDouble(fps[0]) / Double.parseDouble(fps[1]),
          json.getAsJsonObject("format").get("duration").getAsDouble());
    } finally { if (p.isAlive()) p.destroyForcibly(); }
  }
  public FfmpegFrameSource(String executable, Path file, VideoSurfaceSpec spec) {
    this(executable, file, spec, 0);
  }
  public FfmpegFrameSource(String executable, Path file, VideoSurfaceSpec spec, double startSeconds) {
    this(executable, file, spec, startSeconds, 1, null);
  }
  public FfmpegFrameSource(String executable, Path file, VideoSurfaceSpec spec, double startSeconds,
      double speed, LongSupplier sequence) {
    if (!Double.isFinite(startSeconds) || startSeconds < 0) throw new IllegalArgumentException("Video start time");
    if (!Double.isFinite(speed) || speed < .25 || speed > 4) throw new IllegalArgumentException("Speed: 0.25..4");
    this.spec = spec;
    this.startSeconds = startSeconds;
    this.speed = speed; this.sequence = sequence;
    command = List.of(executable, "-hide_banner", "-loglevel", "error", "-nostdin",
        "-threads", "2", "-filter_threads", "1", "-ss", Double.toString(startSeconds), "-i", file.toAbsolutePath().toString(),
        "-map", "0:v:0", "-an", "-sn", "-dn", "-vf",
        "setpts=(PTS-STARTPTS)/" + speed + ",fps="
            + String.format(Locale.ROOT, "%.6f", spec.maximumFps())
            + ",scale=" + spec.width() + ":" + spec.height() + ":flags=lanczos",
        "-pix_fmt", "rgb24", "-threads:v", "1", "-f", "rawvideo", "pipe:1");
  }
  List<String> command() { return command; }
  @Override public void start(Consumer<VideoFrame> sink, Consumer<Throwable> failure, Runnable finished) {
    if (!launched.compareAndSet(false, true)) throw new IllegalStateException("Source already started");
    reader = new Thread(() -> decode(sink, failure, finished), "dui-demo-media-decode");
    reader.setDaemon(true); reader.start();
  }
  private void decode(Consumer<VideoFrame> sink, Consumer<Throwable> failure, Runnable finished) {
    var errors = new StringBuilder();
    Thread diagnostics = null;
    try {
      synchronized (clock) {
        if (closed) return;
        process = new ProcessBuilder(command).start();
      }
      Process decoder = process;
      diagnostics = new Thread(() -> {
        try (var text = decoder.errorReader(StandardCharsets.UTF_8)) {
          char[] buffer = new char[512]; int n;
          while ((n = text.read(buffer)) >= 0) synchronized (errors) {
            errors.append(buffer, 0, n);
            if (errors.length() > 4096) errors.delete(0, errors.length() - 4096);
          }
        } catch (IOException ignored) {}
      }, "dui-demo-media-errors");
      diagnostics.setDaemon(true); diagnostics.start();
      byte[] raw = new byte[Math.multiplyExact(Math.multiplyExact(spec.width(), spec.height()), 3)];
      int[] pixels = new int[spec.width() * spec.height()];
      long next = System.nanoTime(), interval = Math.round(1_000_000_000d / spec.maximumFps());
      try (var input = decoder.getInputStream()) {
        while (!closed) {
          synchronized (clock) {
            while (!closed) {
              if (paused && frames.get() > 0) { clock.wait(); continue; }
              if (resetClock) { next = System.nanoTime(); resetClock = false; }
              long remaining = next - System.nanoTime();
              if (remaining <= 0) break;
              clock.wait(remaining / 1_000_000, (int) (remaining % 1_000_000));
            }
          }
          if (closed) break;
          if (!readFrame(input, raw)) break;
          long begin = System.nanoTime();
          rgb(raw, pixels, spec.format()); conversionNanos.addAndGet(System.nanoTime() - begin);
          synchronized (clock) {
            while (paused && frames.get() > 0 && !closed) clock.wait();
            if (closed) break;
          }
          long count = frames.incrementAndGet();
          sink.accept(new VideoFrame(spec.width(), spec.height(), spec.format(),
              sequence == null ? count : sequence.getAsLong(), pixels));
          // Preserve timing without releasing a burst after slow decoding or a long pause.
          next = Math.max(next + interval, System.nanoTime());
        }
      }
      if (!closed) {
        if (!decoder.waitFor(5, TimeUnit.SECONDS)) throw new IOException("FFmpeg did not finish");
        if (diagnostics != null) diagnostics.join(1000);
        if (decoder.exitValue() != 0) {
          synchronized (errors) { throw new IOException("FFmpeg: " + errors.toString().strip()); }
        }
        ended = true; finished.run();
      }
    } catch (Throwable e) {
      if (!closed) { error = e.toString(); failure.accept(e); }
    } finally {
      Process decoder = process;
      if (decoder != null && decoder.isAlive()) {
        decoder.destroy();
        try { if (!decoder.waitFor(2, TimeUnit.SECONDS)) decoder.destroyForcibly(); }
        catch (InterruptedException ignored) { decoder.destroyForcibly(); Thread.currentThread().interrupt(); }
      }
    }
  }
  static boolean readFrame(InputStream input, byte[] target) throws IOException {
    int offset = 0;
    while (offset < target.length) {
      int n = input.read(target, offset, target.length - offset);
      if (n < 0) {
        if (offset == 0) return false;
        throw new EOFException("Truncated RGB frame: " + offset + "/" + target.length);
      }
      if (n == 0) continue;
      offset += n;
    }
    return true;
  }
  static void rgb(byte[] raw, int[] pixels) {
    rgb(raw, pixels, PixelFormat.RGB888);
  }
  static void rgb(byte[] raw, int[] pixels, PixelFormat format) {
    if (raw.length != pixels.length * 3) throw new IllegalArgumentException("RGB frame size");
    if (format == PixelFormat.BGR555) {
      for (int i = 0, at = 0; i < pixels.length; i++, at += 3)
        pixels[i] = (Byte.toUnsignedInt(raw[at]) >> 3)
            | ((Byte.toUnsignedInt(raw[at + 1]) >> 3) << 5) | ((Byte.toUnsignedInt(raw[at + 2]) >> 3) << 10);
    } else {
      for (int i = 0, at = 0; i < pixels.length; i++, at += 3)
        pixels[i] = (Byte.toUnsignedInt(raw[at]) << 16)
            | (Byte.toUnsignedInt(raw[at + 1]) << 8) | Byte.toUnsignedInt(raw[at + 2]);
    }
  }
  @Override public Statistics statistics() {
    long count = frames.get();
    return new Statistics(count, startSeconds + count * speed / spec.maximumFps(), paused, ended,
        count == 0 ? 0 : conversionNanos.get() / 1_000_000d / count, error, speed);
  }
  @Override public void pause(boolean value) {
    synchronized (clock) { paused = value; resetClock = true; clock.notifyAll(); }
  }
  @Override public boolean paused() { return paused; }
  @Override public void close() {
    synchronized (clock) { closed = true; clock.notifyAll(); }
    Process decoder = process;
    if (decoder != null) {
      decoder.destroy();
      CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
        if (decoder.isAlive()) decoder.destroyForcibly();
      });
    }
    if (reader != null) reader.interrupt();
  }
}
