package gg.kembel.dui.demo;

import com.google.gson.*;
import gg.kembel.dui.core.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.*;
import javax.imageio.ImageIO;

/**
 * Original procedural parcel illustrations. Only decorative art is rasterized, never reward items.
 */
public final class RewardArt {
  public static final List<String> MODELS =
      List.of("reward/idle", "reward/party", "reward/idle_still", "reward/party_still");
  private static final Color INK = new Color(0x322647);

  private RewardArt() {}

  public static JsonObject definition(String id) {
    return JsonParser.parseString(
            "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"dui_demo:" + id + "\"}}")
        .getAsJsonObject();
  }

  public static void write(ZipOutputStream zip) throws IOException {
    for (String id : MODELS) {
      boolean party = id.contains("party"), still = id.endsWith("still");
      int frames = still ? 1 : 32;
      var sheet = new BufferedImage(96, 96 * frames, BufferedImage.TYPE_INT_ARGB);
      var target = sheet.createGraphics();
      for (int f = 0; f < frames; f++)
        target.drawImage(frame(party, still ? 0 : f), 0, f * 96, null);
      target.dispose();
      var out = new ByteArrayOutputStream();
      ImageIO.write(sheet, "png", out);
      put(zip, "assets/dui_demo/textures/item/" + id + ".png", out.toByteArray());
      if (!still)
        put(
            zip,
            "assets/dui_demo/textures/item/" + id + ".png.mcmeta",
            "{\"animation\":{\"frametime\":2,\"width\":96,\"height\":96,\"interpolate\":false}}");
      // A single front-lit quad avoids extruding every animated pixel as generated-item geometry.
      put(
          zip,
          "assets/dui_demo/models/" + id + ".json",
          "{\"gui_light\":\"front\",\"textures\":{\"art\":\"dui_demo:item/"
              + id
              + "\",\"particle\":\"dui_demo:item/"
              + id
              + "\"},\"elements\":[{\"from\":[0,0,8],\"to\":[16,16,8],\"shade\":false,\"light_emission\":15,\"faces\":{\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#art\"}}}]}");
    }
  }

  public static BufferedImage frame(boolean party, int frame) {
    var image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
    var g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    double t = frame / 32.0 * Math.PI * 2, bob = Math.sin(t) * 3;
    g.setColor(new Color(0x32264733, true));
    g.fill(new Ellipse2D.Double(23, 84, 50, 6));
    if (party) {
      int[] colors = {0xFF799D, 0xFFD45E, 0x63D5B2, 0x8ECFFF, 0xB69BFF};
      for (int i = 0; i < 18; i++) {
        double p = (frame / 32.0 + i * 0.137) % 1.0;
        double x = 7 + (i * 37 % 82) + Math.sin(t + i) * 4, y = 4 + p * 82;
        var old = g.getTransform();
        g.translate(x, y);
        g.rotate(t + i);
        g.setColor(new Color(colors[i % colors.length]));
        g.fillRoundRect(-2, -3, 4, 6, 1, 1);
        g.setTransform(old);
      }
    } else {
      star(g, 13, 27, 4 + Math.sin(t) * 1.2, 0xFFD45E);
      star(g, 83, 47, 4 - Math.sin(t) * 1.2, 0x9F88F5);
      star(g, 78, 14, 2.5, 0xFF8DB6);
    }
    g.translate(0, bob);
    // Chunky purple parcel, warm yellow ribbon and a friendly face.
    shape(g, new RoundRectangle2D.Double(23, 43, 50, 37, 8, 8), 0x9874E9, 3);
    g.setColor(new Color(0x7356B8));
    g.fill(new RoundRectangle2D.Double(25, 69, 46, 9, 4, 4));
    shape(g, new Rectangle2D.Double(43, 44, 11, 35), 0xFFD45E, 2);
    g.setColor(new Color(0xFFF0B1));
    g.fillRect(46, 46, 3, 29);
    var old = g.getTransform();
    if (party) {
      g.translate(0, -18);
      g.rotate(-.12 + Math.sin(t) * .08, 48, 35);
    } else g.rotate(Math.sin(t) * .025, 48, 41);
    shape(g, new RoundRectangle2D.Double(18, 33, 60, 14, 5, 5), 0xBBA1FA, 3);
    shape(g, new Rectangle2D.Double(43, 34, 11, 12), 0xFFD45E, 2);
    var bow = new Path2D.Double();
    bow.moveTo(47, 33);
    bow.curveTo(25, 35, 24, 10, 38, 20);
    bow.lineTo(48, 31);
    bow.curveTo(73, 4, 79, 35, 51, 33);
    bow.closePath();
    shape(g, bow, 0xFFD45E, 3);
    shape(g, new Ellipse2D.Double(43, 27, 11, 9), 0xFFE99A, 2);
    g.setTransform(old);
    g.setColor(INK);
    g.setStroke(new BasicStroke(2.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    if (party) {
      g.draw(new Arc2D.Double(28, 55, 9, 8, 15, 150, Arc2D.OPEN));
      g.draw(new Arc2D.Double(59, 55, 9, 8, 15, 150, Arc2D.OPEN));
    } else if (frame == 14 || frame == 15) {
      g.drawLine(29, 60, 35, 60);
      g.drawLine(61, 60, 67, 60);
    } else {
      g.fill(new Ellipse2D.Double(31, 54, 4, 7));
      g.fill(new Ellipse2D.Double(61, 54, 4, 7));
    }
    g.setColor(new Color(0xFF93B6));
    g.fill(new Ellipse2D.Double(27, 63, 9, 4));
    g.fill(new Ellipse2D.Double(60, 63, 9, 4));
    g.setColor(INK);
    g.draw(new Arc2D.Double(41, 61, 15, 9, 195, 150, Arc2D.OPEN));
    if (party) star(g, 48, 36 + Math.sin(t) * 4, 9, 0xFFD45E);
    g.dispose();
    return image;
  }

  private static void shape(Graphics2D g, Shape shape, int fill, float stroke) {
    g.setColor(new Color(fill));
    g.fill(shape);
    g.setColor(INK);
    g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.draw(shape);
  }

  private static void star(Graphics2D g, double x, double y, double size, int color) {
    var p = new Path2D.Double();
    for (int i = 0; i < 10; i++) {
      double a = -Math.PI / 2 + i * Math.PI / 5, r = i % 2 == 0 ? size : size * .45;
      double xx = x + Math.cos(a) * r, yy = y + Math.sin(a) * r;
      if (i == 0) p.moveTo(xx, yy);
      else p.lineTo(xx, yy);
    }
    p.closePath();
    shape(g, p, color, 1.5f);
  }

  private static void put(ZipOutputStream zip, String name, String value) throws IOException {
    put(zip, name, value.getBytes(StandardCharsets.UTF_8));
  }

  private static void put(ZipOutputStream zip, String name, byte[] value) throws IOException {
    var e = new ZipEntry(name);
    e.setTime(0);
    zip.putNextEntry(e);
    zip.write(value);
    zip.closeEntry();
  }
}
