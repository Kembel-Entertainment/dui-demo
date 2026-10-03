package gg.kembel.dui.demo.media;

import gg.kembel.dui.core.*;
import gg.kembel.dui.core.video.*;
import gg.kembel.dui.demo.DemoTemplates;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MediaTest {
  @TempDir Path directory;
  private static SurfaceInput held(boolean forward, boolean backward, boolean left, boolean right) {
    return new SurfaceInput(SurfaceInput.Type.STATE, forward, backward, left, right, false, false, false, -1, 0);
  }
  @Test void playbackKeysAreEdgesOpposingDirectionsCancelAndSpeedIsBounded() {
    var controls = new PlaybackControls();
    assertEquals(5, controls.accept(held(false, false, false, true)).seekSeconds());
    assertEquals(0, controls.accept(held(false, false, false, true)).seekSeconds());
    assertEquals(1, controls.accept(held(true, false, false, true)).speedSteps());
    assertEquals(0, controls.accept(held(true, false, false, true)).speedSteps());
    controls.accept(SurfaceInput.released());
    assertEquals(-5, controls.accept(held(false, false, true, false)).seekSeconds());
    controls.accept(SurfaceInput.released());
    assertEquals(new PlaybackControls.Adjustment(0, 0), controls.accept(held(true, true, true, true)));
    assertEquals(.25, PlaybackControls.speed(.25, -1)); assertEquals(4, PlaybackControls.speed(4, 1));
    assertEquals(1.25, PlaybackControls.speed(1, 1)); assertEquals(1, PlaybackControls.speed(1.25, -1));
  }
  @Test void inputPathsStayInTheLocalCatalogueAndAmbiguousAliasesFail() throws Exception {
    var files = new MediaFiles(directory.resolve("media"));
    Path video = directory.resolve("media/movie.mp4"); Files.write(video, new byte[] {1});
    Files.write(directory.resolve("outside.mp4"), new byte[] {2});
    Files.createSymbolicLink(directory.resolve("media/linked.mp4"), directory.resolve("outside.mp4"));
    Files.write(directory.resolve("media/ignored.txt"), new byte[] {3});
    assertEquals(List.of(video), files.list());
    assertEquals(video, files.find("movie"));
    assertThrows(IllegalArgumentException.class, () -> files.find("../outside.mp4"));
    assertThrows(IllegalArgumentException.class, () -> files.find("linked"));
    Files.write(directory.resolve("media/movie.webm"), new byte[] {4});
    assertThrows(IllegalArgumentException.class, () -> files.find("movie"));
    assertEquals(video, files.find("movie.mp4"));
  }
  @Test void fragmentedRgbPipePreservesColorsAndRejectsPartialFrames() throws Exception {
    byte[] picture = {(byte) 255, 0, 0, 0, (byte) 255, 0, 0, 0, (byte) 255};
    var fragmented = new ByteArrayInputStream(picture) {
      @Override public synchronized int read(byte[] b, int off, int len) { return super.read(b, off, Math.min(2, len)); }
    };
    byte[] raw = new byte[9]; int[] pixels = new int[3];
    assertTrue(FfmpegFrameSource.readFrame(fragmented, raw));
    FfmpegFrameSource.rgb(raw, pixels);
    assertArrayEquals(new int[] {0xff0000, 0x00ff00, 0x0000ff}, pixels);
    FfmpegFrameSource.rgb(raw, pixels, PixelFormat.BGR555);
    assertArrayEquals(new int[] {31, 31 << 5, 31 << 10}, pixels);
    assertEquals(0xff0000, PixelFormat.BGR555.rgb(pixels[0]));
    assertEquals(0x00ff00, PixelFormat.BGR555.rgb(pixels[1]));
    assertEquals(0x0000ff, PixelFormat.BGR555.rgb(pixels[2]));
    assertFalse(FfmpegFrameSource.readFrame(fragmented, raw));
    assertThrows(EOFException.class, () -> FfmpegFrameSource.readFrame(new ByteArrayInputStream(new byte[8]), raw));
  }
  @Test void cinemaUsesTheExistingGenericTemplateWithAnUnrelatedSourceResolution() throws Exception {
    String xml = Files.readString(Path.of("src/main/resources/ui/cinema-screen.html"));
    var screen = VideoSurfaceTemplate.parse(xml, DemoTemplates.environment(DemoTemplates.font()), ComponentRegistry.EMPTY, "cinema");
    var bindings = new HashMap<>(Map.<String, Object>of("title", "Example", "width", 320, "height", 240, "fps", 24,
        "tiles", 21, "bytes", 16777216, "status", "PAUSED", "source", "640x480 -> 320x240", "progress", .5, "time", "0:10 / 0:20"));
    bindings.put("format", "RGB888");
    var view = screen.render(bindings);
    assertEquals(320, view.specification().width()); assertEquals(240, view.specification().height());
    assertEquals(24, view.specification().maximumFps());
    assertFalse(view.hud().surfaces().isEmpty());
    var menu = DemoTemplates.parse(Files.readString(Path.of("src/main/resources/ui/cinema.html")));
    var canvas = menu.render(Map.of("running", true, "status", "Paused", "files",
        List.of(Map.of("id", "movie.mp4", "label", "Movie", "y", 72))));
    assertTrue(canvas.hits.stream().anyMatch(h -> h.action().equals("play") && h.value().equals("movie.mp4")));
  }
  @Test void realDecoderIsSilentPacedPausesAndStopsAtEnd() throws Exception {
    String ffmpeg = System.getenv().getOrDefault("FFMPEG", "ffmpeg");
    String ffprobe = System.getenv().getOrDefault("FFPROBE", "ffprobe");
    try { new ProcessBuilder(ffmpeg, "-version").redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor(); }
    catch (IOException e) { Assumptions.abort("Install FFmpeg to run decoder integration tests"); }
    Path clip = directory.resolve("original-fixture.mp4");
    var generate = new ProcessBuilder(ffmpeg, "-v", "error", "-f", "lavfi", "-i",
        "color=c=red:s=96x64:r=12:d=2", "-an", "-c:v", "libx264", "-pix_fmt", "yuv420p", clip.toString())
        .redirectError(ProcessBuilder.Redirect.INHERIT).start();
    assertTrue(generate.waitFor(10, TimeUnit.SECONDS)); assertEquals(0, generate.exitValue());
    var info = FfmpegFrameSource.probe(ffprobe, clip); assertEquals(96, info.width()); assertEquals(64, info.height());
    var spec = new VideoSurfaceSpec(96, 64, PixelFormat.RGB888, 12, VideoSurfaceSpec.Viewport.FULL,
        new VideoSurfaceSpec.Budget(4, 1_000_000), 0, false);
    var ended = new CountDownLatch(1); var first = new CountDownLatch(1);
    var failure = new AtomicReference<Throwable>(); var picture = new AtomicReference<VideoFrame>();
    try (var source = new FfmpegFrameSource(ffmpeg, clip, spec)) {
      assertTrue(source.command().contains("-an")); assertFalse(source.command().contains("-re"));
      source.start(frame -> { picture.set(frame); first.countDown(); }, failure::set, ended::countDown);
      assertTrue(first.await(5, TimeUnit.SECONDS));
      int color = picture.get().pixel(48, 32); assertTrue((color >> 16) > 240 && (color & 0xffff) < 1024, Integer.toHexString(color));
      source.pause(true); Thread.sleep(150);
      long before = source.statistics().frames(); Thread.sleep(250);
      assertEquals(before, source.statistics().frames(), "Pause must stop the playback clock");
      source.pause(false); assertTrue(ended.await(5, TimeUnit.SECONDS));
      assertNull(failure.get(), () -> String.valueOf(failure.get())); assertTrue(source.statistics().ended());
      assertEquals(24, source.statistics().frames());
    }
    // Seeking a paused source provides one preview; speeding up skips source frames without doubling output FPS.
    var sequences = new AtomicLong(100);
    var fastEnded = new CountDownLatch(1); var preview = new CountDownLatch(1);
    var previewFrame = new AtomicReference<VideoFrame>();
    var fastSpec = new VideoSurfaceSpec(96, 64, PixelFormat.BGR555, 12, VideoSurfaceSpec.Viewport.FULL,
        new VideoSurfaceSpec.Budget(3, 1_000_000), 0, false);
    try (var source = new FfmpegFrameSource(ffmpeg, clip, fastSpec, .5, 2, sequences::incrementAndGet)) {
      source.pause(true);
      assertTrue(source.command().stream().anyMatch(s -> s.startsWith("setpts=(PTS-STARTPTS)/2.0,fps=")
          && s.indexOf("fps=") < s.indexOf("scale=")));
      source.start(frame -> { previewFrame.set(frame); preview.countDown(); }, failure::set, fastEnded::countDown);
      assertTrue(preview.await(5, TimeUnit.SECONDS)); Thread.sleep(250);
      assertEquals(1, source.statistics().frames()); assertEquals(101, previewFrame.get().sequence());
      assertEquals(PixelFormat.BGR555, previewFrame.get().format());
      assertTrue((PixelFormat.BGR555.rgb(previewFrame.get().pixel(48, 32)) >> 16) > 240);
      assertEquals(2, source.statistics().speed());
      assertEquals(.5 + 2d / 12, source.statistics().seconds(), .001);
      source.pause(false); assertTrue(fastEnded.await(5, TimeUnit.SECONDS));
      assertNull(failure.get()); assertEquals(9, source.statistics().frames(), 1);
    }
  }
}
