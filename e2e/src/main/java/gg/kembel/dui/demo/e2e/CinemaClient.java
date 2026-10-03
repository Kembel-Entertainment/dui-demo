package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import com.mojang.blaze3d.opengl.GlTexture;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.lwjgl.opengl.*;

/** Only observes vanilla pixels and drives vanilla hotbar/command inputs; no client video renderer. */
public final class CinemaClient {
  public static CinemaClient instance;
  private int ticks, stage, changed, fbo, samples, visible, last, benchmark;
  private long start, pausedFrames, lastChange, maximumGap, pausedSequence;
  private double pausedPosition;
  private int seat;
  private String inventory, measurement;
  private int originalSlot;
  private final ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(8 * 8 * 4);
  private final JsonObject measurements = new JsonObject();
  private final boolean benchmarkOnly = Boolean.getBoolean("dui.e2e.media.benchmark-only");
  private JsonObject initialStatistics;
  private final List<Double> gaps = new ArrayList<>();
  private final List<String> benchmarks = Arrays.stream(System.getProperty("dui.e2e.media", "").split(","))
      .filter(s -> !s.isBlank()).toList();
  void initialize() { instance = this; ClientTickEvents.END_CLIENT_TICK.register(this::tick); }
  public void rendered(Minecraft mc) {
    if (measurement == null || mc.player == null || !mc.player.isPassenger() || mc.gui.screen() != null) return;
    var target = mc.gameRenderer.mainRenderTarget();
    if (!(target.getColorTexture() instanceof GlTexture texture)) return;
    int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    int packBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
    int[] packing = {GL11.GL_PACK_ALIGNMENT, GL11.GL_PACK_ROW_LENGTH, GL11.GL_PACK_SKIP_ROWS, GL11.GL_PACK_SKIP_PIXELS};
    int[] savedPacking = Arrays.stream(packing).map(GL11::glGetInteger).toArray();
    if (fbo == 0) fbo = GL30.glGenFramebuffers();
    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
    GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture.glId(), 0);
    int signature = 1;
    try {
      GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
      for (int value : packing) GL11.glPixelStorei(value, value == GL11.GL_PACK_ALIGNMENT ? 1 : 0);
      for (double y : new double[] {.25, .5, .75}) for (double x : new double[] {.2, .5, .8}) {
        pixels.clear(); GL11.glReadPixels((int) (target.width * x), (int) (target.height * y), 8, 8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        for (int at = 0; at < pixels.capacity(); at++) signature = 31 * signature + Byte.toUnsignedInt(pixels.get(at));
      }
    } finally {
      for (int i = 0; i < packing.length; i++) GL11.glPixelStorei(packing[i], savedPacking[i]);
      GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, packBuffer);
      GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
    }
    samples++; if (signature != last) {
      long now = System.nanoTime();
      if (lastChange != 0) { maximumGap = Math.max(maximumGap, now - lastChange); gaps.add((now - lastChange) / 1e9); }
      lastChange = now; visible++; last = signature;
    }
    if (start == 0) start = System.nanoTime();
  }
  private void begin(String label) throws Exception {
    measurement = label; start = lastChange = maximumGap = 0; samples = visible = 0; last = -1;
    initialStatistics = stats(); gaps.clear();
  }
  private JsonObject end() throws Exception {
    var data = new JsonObject(); data.addProperty("visibleChanges", visible); data.addProperty("renderSamples", samples);
    data.addProperty("seconds", (System.nanoTime() - start) / 1_000_000_000d);
    data.addProperty("visibleFps", visible / ((System.nanoTime() - start) / 1_000_000_000d));
    data.addProperty("maximumFrameGapSeconds", maximumGap / 1_000_000_000d);
    double seconds = (System.nanoTime() - start) / 1e9;
    data.addProperty("renderFps", samples / seconds);
    var sortedGaps = gaps.stream().sorted().toList();
    data.addProperty("p95FrameGapSeconds", sortedGaps.isEmpty() ? 0 : sortedGaps.get(Math.min(sortedGaps.size()-1, (int) (sortedGaps.size() * .95))));
    var current = stats();
    double statisticsSeconds = (current.get("sampledAtNanos").getAsLong() - initialStatistics.get("sampledAtNanos").getAsLong()) / 1e9;
    if (statisticsSeconds <= 0) statisticsSeconds = seconds;
    long encoded = current.getAsJsonObject("transport").get("sent").getAsLong() - initialStatistics.getAsJsonObject("transport").get("sent").getAsLong();
    data.addProperty("sourceFps", (current.getAsJsonObject("source").get("frames").getAsLong() - initialStatistics.getAsJsonObject("source").get("frames").getAsLong()) / statisticsSeconds);
    data.addProperty("sentFps", encoded / statisticsSeconds);
    data.addProperty("mapMiBPerSecond", (current.getAsJsonObject("transport").get("bytes").getAsLong() - initialStatistics.getAsJsonObject("transport").get("bytes").getAsLong()) / statisticsSeconds / 1048576d);
    data.addProperty("encodeMillisPerFrame", encoded == 0 ? 0 : (current.getAsJsonObject("transport").get("encodeNanos").getAsLong() - initialStatistics.getAsJsonObject("transport").get("encodeNanos").getAsLong()) / 1e6 / encoded);
    data.add("server", current); measurements.add(measurement, data); measurement = null; return data;
  }
  private void step() { stage++; changed = ticks; System.out.println("CINEMA_TEST_STAGE " + stage); }
  private JsonObject stats() throws Exception {
    var mc = Minecraft.getInstance();
    return JsonParser.parseString(Files.readString(Paths.plugin().resolve("layouts/" + mc.player.getUUID() + "-cinema.json"))).getAsJsonObject();
  }
  private void shot(Minecraft mc, String name) {
    Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
      try (image) { image.writeToFile(Paths.output().resolve(name + ".png")); }
      catch (Exception e) { throw new RuntimeException(e); }
    });
  }
  private static String inventory(Minecraft mc) {
    var text = new StringBuilder();
    for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) text.append(mc.player.getInventory().getItem(i)).append(';');
    return text.toString();
  }
  private static void slot(Minecraft mc, int slot) { mc.player.getInventory().setSelectedSlot(slot); }
  private void tick(Minecraft mc) {
    ticks++; mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    try {
      if (ticks > 2400) throw new IllegalStateException("Cinema timeout at stage " + stage);
      if (mc.gui.overlay() != null || ticks - changed < 15) return;
      if (stage == 0 && ticks > 60) {
        mc.options.pauseOnLostFocus = false; mc.options.framerateLimit().set(120); mc.options.enableVsync().set(false);
        String[] window = System.getProperty("dui.e2e.media.window", "1280x900").split("x");
        mc.getWindow().setWindowed(Integer.parseInt(window[0]), Integer.parseInt(window[1])); mc.options.guiScale().set(2); mc.resizeGui();
        var server = new ServerData("Cinema fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null); step();
      } else if (stage == 1 && mc.player != null && mc.gui.screen() instanceof DialogScreen<?>) {
        inventory = inventory(mc); originalSlot = mc.player.getInventory().getSelectedSlot();
        if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (benchmarkOnly) { stage = 9; changed = ticks; }
        else { mc.player.connection.sendCommand("cinema play dui-color-fixture"); step(); }
      } else if (stage == 2 && mc.player.isPassenger() && mc.gui.screen() == null && ticks - changed > 40) {
        if (measurement == null) begin("fixture");
        if (start != 0 && System.nanoTime() - start > 6_000_000_000L) {
          var result = end(); if (result.get("visibleFps").getAsDouble() < 18) throw new IllegalStateException("No smooth video: " + result);
          shot(mc, "moving"); slot(mc, 0); step();
        }
      } else if (stage == 3 && stats().getAsJsonObject("source").get("paused").getAsBoolean() && ticks - changed > 30) {
        pausedFrames = stats().getAsJsonObject("source").get("frames").getAsLong(); begin("paused"); step();
      } else if (stage == 4 && start != 0 && System.nanoTime() - start > 1_500_000_000L) {
        var result = end();
        if (result.get("visibleChanges").getAsInt() > 1 || stats().getAsJsonObject("source").get("frames").getAsLong() != pausedFrames)
          throw new IllegalStateException("Pause advanced: " + result);
        shot(mc, "paused");
        pausedPosition = stats().getAsJsonObject("source").get("seconds").getAsDouble();
        pausedSequence = stats().getAsJsonObject("transport").get("sequence").getAsLong();
        seat = mc.player.getVehicle().getId();
        mc.options.keyRight.setDown(true); stage = 20; changed = ticks;
      } else if (stage == 20 && ticks - changed > 30) {
        if (!stats().getAsJsonObject("source").get("paused").getAsBoolean()
            || stats().getAsJsonObject("source").get("seconds").getAsDouble() < pausedPosition + 4.9
            || stats().getAsJsonObject("transport").get("sequence").getAsLong() <= pausedSequence
            || mc.player.getVehicle().getId() != seat)
          throw new IllegalStateException("D did not seek with a paused preview on the same surface: " + stats());
        shot(mc, "seek-forward");
        mc.options.keyRight.setDown(false); mc.options.keyLeft.setDown(true); stage = 21; changed = ticks;
      } else if (stage == 21 && ticks - changed > 30) {
        if (Math.abs(stats().getAsJsonObject("source").get("seconds").getAsDouble() - pausedPosition) > .15)
          throw new IllegalStateException("A did not seek back: " + stats());
        mc.options.keyLeft.setDown(false); mc.options.keyUp.setDown(true); stage = 22; changed = ticks;
      } else if (stage == 22 && ticks - changed > 30) {
        if (stats().getAsJsonObject("source").get("speed").getAsDouble() != 1.25
            || !stats().getAsJsonObject("source").get("paused").getAsBoolean()
            || mc.player.getVehicle().getId() != seat)
          throw new IllegalStateException("W did not change speed once while held: " + stats());
        mc.options.keyUp.setDown(false); mc.options.keyDown.setDown(true); stage = 23; changed = ticks;
      } else if (stage == 23 && ticks - changed > 30) {
        if (stats().getAsJsonObject("source").get("speed").getAsDouble() != 1)
          throw new IllegalStateException("S did not restore normal speed: " + stats());
        mc.options.keyDown.setDown(false);
        pausedFrames = stats().getAsJsonObject("source").get("frames").getAsLong();
        slot(mc, 0); stage = 5; changed = ticks;
      } else if (stage == 5 && !stats().getAsJsonObject("source").get("paused").getAsBoolean() && ticks - changed > 30) {
        if (stats().getAsJsonObject("source").get("frames").getAsLong() <= pausedFrames) throw new IllegalStateException("Resume did not advance");
        mc.player.setYRot(mc.player.getYRot() + 180); mc.player.setXRot(70); shot(mc, "camera-turned");
        mc.player.connection.sendCommand("cinema menu"); step();
      } else if (stage == 6 && mc.gui.screen() instanceof DialogScreen<?>) {
        if (mc.player.isPassenger()) throw new IllegalStateException("Seat leaked into selection");
        if (mc.gui.hud.isHidden()) mc.gui.hud.toggle(); shot(mc, "selection");
        mc.player.connection.sendCommand("cinema resume"); step();
      } else if (stage == 7 && mc.player.isPassenger() && mc.gui.screen() == null) {
        if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (stats().getAsJsonObject("source").get("ended").getAsBoolean()) {
          mc.options.guiScale().set(0); mc.resizeGui(); shot(mc, "finished");
          mc.player.connection.sendCommand("cinema restart"); step();
        }
      } else if (stage == 8 && mc.player.isPassenger() && !stats().getAsJsonObject("source").get("ended").getAsBoolean()) {
        if (stats().getAsJsonObject("source").get("frames").getAsLong() > 70) return;
        shot(mc, "restarted"); mc.player.connection.sendCommand("cinema close"); step();
      } else if (stage == 9 && !mc.player.isPassenger() && ticks - changed > 25) {
        if (!inventory.equals(inventory(mc)) || mc.getCameraEntity() != mc.player || mc.player.getInventory().getSelectedSlot() != originalSlot)
          throw new IllegalStateException("Player restoration failed");
        if (benchmark < benchmarks.size()) {
          mc.options.guiScale().set(2); mc.resizeGui();
          String[] clip = benchmarks.get(benchmark).split(":");
          mc.player.connection.sendCommand("cinema play " + clip[0] + " " + (clip.length > 1 ? clip[1] : "384")
              + " " + (clip.length > 2 ? clip[2] : "30") + " 20" + (clip.length > 3 ? " " + clip[3] : "")); stage = 10; changed = ticks;
        } else {
          var result = new JsonObject(); result.addProperty("passed", true); result.addProperty("muted", true);
          result.addProperty("steps", 11 + benchmark); result.addProperty("inventoryUnchanged", true);
          result.addProperty("benchmarkOnly", benchmarkOnly);
          result.addProperty("pauseStable", !benchmarkOnly); result.addProperty("eofVerified", !benchmarkOnly); result.add("measurements", measurements);
          result.addProperty("movementControlsVerified", !benchmarkOnly);
          Files.writeString(Paths.output().resolve("client-result.json"), result.toString());
          if (fbo != 0) GL30.glDeleteFramebuffers(fbo); System.out.println("CINEMA_TEST_COMPLETE " + result); mc.stop(); stage = 11;
        }
      } else if (stage == 10 && mc.player.isPassenger() && mc.gui.screen() == null && ticks - changed > 40) {
        if (measurement == null) begin(benchmarks.get(benchmark));
        if (start != 0 && System.nanoTime() - start > Long.getLong("dui.e2e.media.seconds", 6) * 1_000_000_000L) {
          var result = end();
          if (!benchmarkOnly && benchmarks.get(benchmark).contains(":1024:60")
              && (result.get("maximumFrameGapSeconds").getAsDouble() > .65 || result.get("visibleFps").getAsDouble() < 3))
            throw new IllegalStateException("High-resolution periodic stall: " + result);
          shot(mc, benchmarks.get(benchmark).replace(':', '-')); benchmark++;
          mc.player.connection.sendCommand("cinema close"); stage = 9; changed = ticks;
        }
      }
    } catch (Exception e) {
      try { Files.writeString(Paths.output().resolve("failure.txt"), e.toString()); } catch (Exception ignored) {}
      e.printStackTrace(); System.out.println("CINEMA_TEST_FAILED " + e); mc.stop(); stage = 11;
    }
  }
}
