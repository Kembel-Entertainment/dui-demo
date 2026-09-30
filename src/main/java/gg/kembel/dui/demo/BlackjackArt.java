package gg.kembel.dui.demo;

import gg.kembel.dui.core.RasterImage;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Map;

/** Original vector illustration, rasterized at runtime; no table or card assets in the pack. */
public final class BlackjackArt {
  private BlackjackArt() {}

  private static final Map<String, RasterImage> IMAGES =
      Map.of("wide", table(false), "compact", table(true), "wordmark", wordmark());

  public static Map<String, RasterImage> images() {
    return IMAGES;
  }

  private static RasterImage wordmark() {
    var im = new BufferedImage(128, 27, BufferedImage.TYPE_INT_RGB);
    var g = im.createGraphics();
    try {
      g.setColor(new Color(0xEFE5D0));
      g.fillRect(0, 0, 128, 27);
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(new Color(0x263C4B));
      g.setFont(new Font(Font.SERIF, Font.BOLD, 20));
      g.drawString("MONARCH", 0, 23);
    } finally {
      g.dispose();
    }
    return new RasterImage(128, 27, im.getRGB(0, 0, 128, 27, null, 0, 128));
  }

  private static RasterImage table(boolean compact) {
    int w = compact ? 320 : 480, h = compact ? 153 : 324;
    var im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    var g = im.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(new Color(0xEFE5D0));
      g.fillRect(0, 0, w, h);
      g.setColor(new Color(0xB29B72));
      g.draw(new RoundRectangle2D.Double(2, 2, w - 5, h - 5, 10, 10));
      int top = compact ? 18 : 36, bottom = compact ? 115 : 266;
      g.setColor(new Color(0x715036));
      g.fill(
          new RoundRectangle2D.Double(
              7, top, w - 14, bottom - top, compact ? 18 : 52, compact ? 18 : 52));
      g.setColor(new Color(0xD6B570));
      g.setStroke(new BasicStroke(compact ? 1.5f : 2.5f));
      g.draw(
          new RoundRectangle2D.Double(
              10, top + 3, w - 20, bottom - top - 6, compact ? 16 : 48, compact ? 16 : 48));
      g.setPaint(new GradientPaint(0, top, new Color(0x1D4155), 0, bottom, new Color(0x0D2538)));
      g.fill(
          new RoundRectangle2D.Double(
              14, top + 7, w - 28, bottom - top - 14, compact ? 14 : 44, compact ? 14 : 44));
      g.setColor(new Color(0x486877));
      g.setStroke(new BasicStroke(.8f));
      g.draw(
          new RoundRectangle2D.Double(
              18, top + 11, w - 36, bottom - top - 22, compact ? 12 : 40, compact ? 12 : 40));
      if (!compact) {
        g.setColor(new Color(0xA78E5B));
        g.setStroke(new BasicStroke(.7f));
        g.draw(new Arc2D.Double(105, 147, 270, 98, 182, 176, Arc2D.OPEN));
        g.draw(new Arc2D.Double(99, 144, 282, 104, 182, 176, Arc2D.OPEN));
        // Geometric crown crest, champagne inlay and restrained symmetric linework.
        var crown = new Path2D.Double();
        crown.moveTo(225, 116);
        crown.lineTo(220, 102);
        crown.lineTo(230, 107);
        crown.lineTo(240, 96);
        crown.lineTo(250, 107);
        crown.lineTo(260, 102);
        crown.lineTo(255, 116);
        crown.closePath();
        g.setColor(new Color(0xD8B973));
        g.fill(crown);
        g.fillRoundRect(225, 118, 30, 3, 2, 2);
        g.drawLine(74, 118, 204, 118);
        g.drawLine(276, 118, 406, 118);
        g.setColor(new Color(0xCBBDA0));
        g.setFont(new Font(Font.SERIF, Font.BOLD, 21));
        g.drawString("MONARCH", 16, 25);
        // Chip rack has its own illustrator-style metallic edge and four colored stacks.
        g.setColor(new Color(0x1A2630));
        g.fillRoundRect(31, 49, 62, 32, 6, 6);
        g.setColor(new Color(0x8B7958));
        g.drawRoundRect(31, 49, 62, 32, 6, 6);
        int[] colors = {0xD7B469, 0x79BCAA, 0xCA7873, 0x927DB3};
        for (int j = 0; j < 4; j++)
          for (int i = 0; i < 4; i++) {
            g.setColor(new Color(colors[j]).darker());
            g.fillOval(36 + j * 13, 69 - i * 4, 10, 4);
            g.setColor(new Color(colors[j]));
            g.fillOval(36 + j * 13, 68 - i * 4, 10, 4);
          }
      }
      int sx = compact ? 279 : 421,
          sy = compact ? 22 : 45,
          sw = compact ? 20 : 30,
          sh = compact ? 24 : 38;
      g.setColor(new Color(0x091827));
      g.fillRoundRect(sx + 2, sy + 3, sw, sh, 4, 4);
      g.setColor(new Color(0xB79256));
      g.fillRoundRect(sx, sy, sw, sh, 4, 4);
      g.setColor(new Color(0x203448));
      g.fillRoundRect(sx + 2, sy + 2, sw - 4, sh - 5, 3, 3);
      g.setColor(new Color(0xF2E2C1));
      for (int i = 0; i < 4; i++) g.drawLine(sx + 4, sy + 4 + i * 2, sx + sw - 4, sy + 4 + i * 2);
      g.setColor(new Color(0x8A6C42));
      g.fillRoundRect(sx + 3, sy + sh - 9, sw - 6, 7, 2, 2);
      if (!compact) {
        g.setColor(new Color(0x9A855D));
        g.drawLine(16, 280, 464, 280);
      }
    } finally {
      g.dispose();
    }
    return new RasterImage(w, h, im.getRGB(0, 0, w, h, null, 0, w));
  }
}
