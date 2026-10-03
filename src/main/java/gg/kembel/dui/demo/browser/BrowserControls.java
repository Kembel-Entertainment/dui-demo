package gg.kembel.dui.demo.browser;

import gg.kembel.dui.core.video.SurfaceInput;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Consumer-owned logical pointer and navigation policy. Coordinates are viewport pixels. */
public final class BrowserControls {
  public record Cursor(int x, int y) {}
  private final int width, height;
  private double x, y;
  private SurfaceInput held = SurfaceInput.released();
  public BrowserControls(int width, int height) {
    if (width < 1 || height < 1) throw new IllegalArgumentException("Cursor viewport");
    this.width = width; this.height = height; x = width / 2d; y = height / 2d;
  }
  public boolean accept(SurfaceInput input) {
    if (input.type() != SurfaceInput.Type.STATE) return false;
    boolean click = input.jump() && !held.jump(); held = input; return click;
  }
  public Cursor advance(double seconds) {
    if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Cursor time");
    double speed = held.sprint() ? 65 : 260;
    double dx = (held.right() ? 1 : 0) - (held.left() ? 1 : 0);
    double dy = (held.backward() ? 1 : 0) - (held.forward() ? 1 : 0);
    double scale = dx != 0 && dy != 0 ? Math.sqrt(.5) : 1;
    x = Math.clamp(x + dx * scale * speed * Math.min(.1, seconds), 0, width - 1);
    y = Math.clamp(y + dy * scale * speed * Math.min(.1, seconds), 0, height - 1);
    return cursor();
  }
  public Cursor cursor() { return new Cursor((int) Math.round(x), (int) Math.round(y)); }
  public Cursor mouse(double yaw, double pitch, double pixelsPerDegree) {
    if (!Double.isFinite(yaw) || !Double.isFinite(pitch) || !Double.isFinite(pixelsPerDegree) || pixelsPerDegree <= 0)
      throw new IllegalArgumentException("Mouse mapping");
    x = Math.clamp(x + yaw * pixelsPerDegree, 0, width - 1);
    y = Math.clamp(y + pitch * pixelsPerDegree, 0, height - 1);
    return cursor();
  }
  public void release() { held = SurfaceInput.released(); }
  public static int zoom(int percent) {
    if (percent < 50 || percent > 250) throw new IllegalArgumentException("Browser zoom 50..250 percent");
    return percent;
  }
  public static String address(String value) {
    String input = value.strip();
    if (input.isEmpty() || input.length() > 2048) throw new IllegalArgumentException("Enter an address or YouTube search");
    if (!input.contains("://") && !input.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*")) {
      if (input.contains(".") && !input.contains(" ")) input = "https://" + input;
      else return "https://www.youtube.com/results?search_query=" + URLEncoder.encode(input, StandardCharsets.UTF_8);
    }
    URI uri = URI.create(input);
    if (!java.util.Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
      throw new IllegalArgumentException("Use an HTTP(S) address without credentials");
    return uri.toASCIIString();
  }
}
