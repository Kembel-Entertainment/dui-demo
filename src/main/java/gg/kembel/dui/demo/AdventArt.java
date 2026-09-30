package gg.kembel.dui.demo;

import gg.kembel.dui.core.RasterImage;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Own procedural holiday artwork; native Minecraft rewards are never rasterized. */
public final class AdventArt {
  public static final int INK = 0x142F38, BACK = 0x203E47;
  public static final int[] COLORS = {0xFF679A, 0x77EAC5, 0xFDCB65, 0xAE9DF1, 0x7DCFFF, 0xFFF1D0};

  private AdventArt() {}

  public static RasterImage parcel(int width, int height, int variant) {
    var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics();
    g.setColor(new Color(BACK));
    g.fillRect(0, 0, width, height);
    int left = Math.max(2, width / 10),
        top = Math.max(3, height / 5),
        w = width - left * 2,
        h = height - top - 2;
    int fill = COLORS[variant % 6], ribbon = COLORS[(variant + 2) % 6];
    g.setColor(new Color(INK));
    g.fillRoundRect(left - 1, top - 1, w + 2, h + 2, 3, 3);
    g.setColor(new Color(fill));
    g.fillRoundRect(left, top, w, h, 2, 2);
    g.setColor(new Color(fill).darker());
    g.fillRect(left, top + h - 3, w, 3);
    g.setColor(new Color(ribbon));
    g.fillRect(width / 2 - 1, top, 3, h);
    g.fillRect(left, top + Math.max(2, h / 3), w, 2);
    g.setColor(new Color(0xFFFAE8));
    g.fillRect(left, top, Math.max(2, w / 3), 1);
    g.setColor(new Color(ribbon));
    g.drawOval(width / 2 - 4, top - 4, 4, 3);
    g.drawOval(width / 2, top - 4, 4, 3);
    if (w > 16 && h > 10) {
      g.setColor(new Color(INK));
      g.fillRect(left + 4, top + h / 2 + 2, 1, 2);
      g.fillRect(left + w - 5, top + h / 2 + 2, 1, 2);
    }
    g.dispose();
    return raster(image);
  }

  public static RasterImage roof(int width, int height) {
    var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics();
    g.setColor(new Color(INK));
    g.fillRect(0, 0, width, height);
    for (int x = 0; x < width; x++) {
      int y =
          height == 3
              ? 0
              : Math.min(
                  height - 3, (int) (Math.abs(x - width / 2.0) / (width / 2.0) * (height - 3)));
      g.setColor(new Color(0xB7D7D4));
      g.fillRect(x, y + 1, 1, 2);
      g.setColor(new Color(0xFFF5DA));
      g.fillRect(x, y, 1, 2);
    }
    g.dispose();
    return raster(image);
  }

  public static RasterImage wordmark() {
    // Pixel lettering is authored here, independent of installed system fonts.
    String[] letters = {
      "11111/10000/10111/10001/11111",
      "111/010/010/010/111",
      "11111/10000/11110/10000/10000",
      "11111/00100/00100/00100/00100",
      "11110/10001/10001/10001/11110",
      "11110/10001/11110/10100/10010",
      "01110/10001/10001/10001/01110",
      "11110/10001/11110/10000/10000"
    };
    var image = new BufferedImage(120, 18, BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics();
    g.setColor(new Color(INK));
    g.fillRect(0, 0, 120, 18);
    int x = 2;
    for (int l = 0; l < letters.length; l++) {
      if (l == 4) x += 6;
      String[] rows = letters[l].split("/");
      for (int y = 0; y < 5; y++)
        for (int k = 0; k < rows[y].length(); k++)
          if (rows[y].charAt(k) == '1') {
            g.setColor(new Color(0x0A202A));
            g.fillRect(x + k * 2 + 1, y * 2 + 4, 2, 3);
            g.setColor(new Color(l < 4 ? COLORS[1] : COLORS[0]));
            g.fillRect(x + k * 2, y * 2 + 2, 2, 2);
            if (y == 0) {
              g.setColor(new Color(0xFFF5DA));
              g.fillRect(x + k * 2, y * 2 + 2, 2, 1);
            }
          }
      x += rows[0].length() * 2 + 3;
    }
    g.dispose();
    return raster(image);
  }

  private static RasterImage raster(BufferedImage image) {
    int[] rgb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    for (int i = 0; i < rgb.length; i++) rgb[i] &= 0xFFFFFF;
    return new RasterImage(image.getWidth(), image.getHeight(), rgb);
  }

  public static void write(ZipOutputStream zip) throws IOException {
    for (int v = 0; v < 6; v++)
      for (String part : new String[] {"body", "lid"}) {
        String id = "advent/" + part + "_" + v;
        var image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = new Color(COLORS[v]), ribbon = new Color(COLORS[(v + 2) % 6]);
        if (part.equals("body")) {
          shape(g, new RoundRectangle2D.Double(17, 43, 62, 38, 7, 7), fill, 3);
          g.setColor(fill.darker());
          g.fillRoundRect(19, 70, 58, 8, 4, 4);
          shape(g, new Rectangle2D.Double(42, 44, 12, 35), ribbon, 2);
          g.setColor(new Color(INK));
          g.fillOval(27, 55, 4, 7);
          g.fillOval(66, 55, 4, 7);
          g.setStroke(new BasicStroke(2.5f));
          g.draw(new Arc2D.Double(42, 61, 13, 8, 195, 150, Arc2D.OPEN));
          g.setColor(new Color(0xFFAAC4));
          g.fillOval(23, 63, 10, 4);
          g.fillOval(63, 63, 10, 4);
        } else {
          shape(g, new RoundRectangle2D.Double(12, 33, 72, 14, 5, 5), fill.brighter(), 3);
          shape(g, new Rectangle2D.Double(42, 34, 12, 12), ribbon, 2);
          shape(g, new Ellipse2D.Double(25, 19, 24, 15), ribbon, 3);
          shape(g, new Ellipse2D.Double(47, 19, 24, 15), ribbon, 3);
          shape(g, new Ellipse2D.Double(43, 26, 10, 9), ribbon.brighter(), 2);
        }
        g.dispose();
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        put(zip, "assets/dui_demo/textures/item/" + id + ".png", bytes.toByteArray());
        put(
            zip,
            "assets/dui_demo/models/" + id + ".json",
            ("{\"gui_light\":\"front\",\"textures\":{\"art\":\"dui_demo:item/"
                    + id
                    + "\",\"particle\":\"dui_demo:item/"
                    + id
                    + "\"},\"elements\":[{\"from\":[0,0,8],\"to\":[16,16,8],\"shade\":false,\"light_emission\":15,\"faces\":{\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#art\"}}}]}")
                .getBytes(StandardCharsets.UTF_8));
        put(
            zip,
            "assets/dui_demo/items/" + id + ".json",
            RewardArt.definition(id).toString().getBytes(StandardCharsets.UTF_8));
      }
  }

  private static void shape(Graphics2D g, Shape s, Color c, float stroke) {
    g.setColor(c);
    g.fill(s);
    g.setColor(new Color(INK));
    g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.draw(s);
  }

  private static void put(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
    var entry = new ZipEntry(path);
    entry.setTime(0);
    zip.putNextEntry(entry);
    zip.write(bytes);
    zip.closeEntry();
  }
}
