package gg.kembel.dui.demo;

import gg.kembel.dui.core.RasterImage;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Original code-native vector illustration. No photographs, borrowed assets or per-menu pack art.
 */
public final class RouletteArt {
  private RouletteArt() {}

  private static final Map<String, RasterImage> IMAGES = build();

  public static Map<String, RasterImage> images() {
    return IMAGES;
  }

  private static Map<String, RasterImage> build() {
    var m = new HashMap<String, RasterImage>();
    m.put("table_wide", table(false));
    m.put("table_compact", table(true));
    int[] colors = {0x40837A, 0xB96059, 0x526FA0, 0x78629C};
    for (int i = 0; i < 4; i++) {
      m.put("chip_" + RouletteGame.CHIPS[i], chip(colors[i], 0xEAE2CF));
      m.put("chip_" + RouletteGame.CHIPS[i] + "_selected", chip(colors[i], 0xE1CCA0));
    }
    m.put("chip_bet_red", chip(0xD6B264, 0xA73B3A));
    m.put("chip_bet_black", chip(0xD6B264, 0x23352E));
    m.put("chip_bet_green", chip(0xD6B264, 0x357660));
    m.put("chip_bet_outside", chip(0xD6B264, 0x285242));
    m.put("wordmark", wordmark());
    return Map.copyOf(m);
  }

  private static RasterImage raster(BufferedImage image) {
    return new RasterImage(
        image.getWidth(),
        image.getHeight(),
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()));
  }

  private static RasterImage table(boolean compact) {
    int w = compact ? 320 : 480, h = compact ? 153 : 324;
    var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setPaint(new GradientPaint(0, 0, new Color(0xF4EFE3), w, h, new Color(0xDED6C2)));
      g.fillRect(0, 0, w, h);
      g.setColor(new Color(0xA49573));
      g.setStroke(new BasicStroke(1));
      g.draw(new RoundRectangle2D.Double(2, 2, w - 5, h - 5, 10, 10));
      g.setColor(new Color(0xFFFAE9));
      g.draw(new RoundRectangle2D.Double(4, 4, w - 9, h - 9, 8, 8));
      int x = compact ? 122 : 228,
          y = compact ? 24 : 66,
          bw = compact ? 192 : 244,
          bh = compact ? 76 : 126;
      g.setPaint(new GradientPaint(x, y, new Color(0x42635A), x, y + bh, new Color(0x143C31)));
      g.fill(new RoundRectangle2D.Double(x, y, bw, bh, compact ? 8 : 16, compact ? 8 : 16));
      g.setColor(new Color(0xB8AA7F));
      g.draw(
          new RoundRectangle2D.Double(
              x + 2, y + 2, bw - 4, bh - 4, compact ? 6 : 14, compact ? 6 : 14));
      // Restrained felt linework: vector shading rather than noisy photographic textures.
      g.setColor(new Color(0xFFFFFF, true));
      for (int i = 0; i < 9; i++) {
        g.setColor(new Color(255, 255, 238, 7));
        g.draw(new Line2D.Double(x + 8, y + 8 + i * bh / 9.0, x + bw - 8, y + 8 + i * bh / 9.0));
      }
      int wx = compact ? 6 : 8, wy = compact ? 18 : 36, size = compact ? 108 : 216;
      g.setColor(new Color(0xC4BAA1));
      g.fill(new Ellipse2D.Double(wx + 5, wy + 7, size - 8, size - 8));
      g.setColor(new Color(0xF9F3E6));
      g.fill(new Ellipse2D.Double(wx + 1, wy + 1, size - 2, size - 2));
      int lineY = compact ? 19 : 32;
      g.setColor(new Color(0xC9BEA5));
      g.drawLine(10, lineY, w - 10, lineY);
      if (!compact) {
        g.setColor(new Color(0xD8CEB7));
        g.drawLine(232, 236, 468, 236);
        g.drawLine(16, 294, 230, 294);
      }
    } finally {
      g.dispose();
    }
    return raster(image);
  }

  private static RasterImage wordmark() {
    var image = new BufferedImage(126, 27, BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(new Color(0xF4EFE3));
      g.fillRect(0, 0, 126, 27);
      g.setColor(new Color(0x264B3D));
      g.setFont(new Font(Font.SERIF, Font.BOLD, 22));
      g.drawString("RIVIERA", 3, 21);
    } finally {
      g.dispose();
    }
    return raster(image);
  }

  private static RasterImage chip(int paint, int background) {
    var image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(new Color(background));
      g.fillRect(0, 0, 18, 18);
      g.setColor(new Color(0x142F28));
      g.fillOval(1, 2, 16, 16);
      g.setColor(new Color(paint));
      g.fillOval(1, 0, 16, 16);
      g.setColor(new Color(0xF9EED5));
      g.setStroke(new BasicStroke(1.7f));
      for (int i = 0; i < 6; i++)
        g.draw(new Arc2D.Double(2, 1, 14, 14, i * 60 + 10, 25, Arc2D.OPEN));
      g.setStroke(new BasicStroke(.8f));
      g.drawOval(5, 4, 8, 8);
      g.setColor(new Color(0xF8E9C1));
      g.fillOval(8, 7, 2, 2);
    } finally {
      g.dispose();
    }
    return raster(image);
  }
}
