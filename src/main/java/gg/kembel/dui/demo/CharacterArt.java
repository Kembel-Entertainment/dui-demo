package gg.kembel.dui.demo;

import gg.kembel.dui.core.RasterImage;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/** Original illustrated metal, gilt frame and celestial backdrop. No reference artwork used. */
final class CharacterArt {
  private static final RasterImage WIDE = draw(false), COMPACT = draw(true);

  static RasterImage background(boolean compact) {
    return compact ? COMPACT : WIDE;
  }

  private static RasterImage draw(boolean compact) {
    int w = compact ? 160 : 120, h = compact ? 90 : 79;
    var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setPaint(new GradientPaint(0, 0, new Color(0x292F32), w, h, new Color(0x0C1118)));
    g.fillRect(0, 0, w, h);
    g.setColor(new Color(0xAB8A55));
    g.drawRect(1, 1, w - 3, h - 3);
    g.setColor(new Color(0x4A4539));
    g.drawRect(3, 3, w - 7, h - 7);
    g.setPaint(new GradientPaint(0, 0, new Color(0x4A4131), w, 16, new Color(0x232D33)));
    g.fillRect(4, 4, w - 8, compact ? 11 : 8);
    double cx = compact ? 51 : 39, cy = compact ? 45 : 43, r = compact ? 24 : 23;
    g.setPaint(
        new RadialGradientPaint(
            new Point2D.Double(cx, cy),
            (float) r,
            new float[] {0, 1},
            new Color[] {new Color(0x304552), new Color(0x131C28)}));
    g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    g.setColor(new Color(0x665740));
    for (double radius : new double[] {r - 1, r - 4})
      g.draw(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
    for (int n = 0; n < 12; n++) {
      double a = n * Math.PI / 6;
      int x = (int) (cx + Math.cos(a) * (r - 2)), y = (int) (cy + Math.sin(a) * (r - 2));
      g.setColor(new Color(0xC7A568));
      g.fillRect(x, y, 1, 1);
    }
    g.setColor(new Color(0x393D3C));
    int sx = compact ? 98 : 74;
    g.fillRect(sx, compact ? 18 : 17, w - sx - 5, h - (compact ? 28 : 25));
    g.setColor(new Color(0x8D744D));
    g.drawLine(sx, compact ? 18 : 17, sx, h - 10);
    for (int corner = 0; corner < 4; corner++) {
      int x = corner % 2 == 0 ? 4 : w - 5, y = corner < 2 ? 4 : h - 5;
      g.setColor(new Color(0xE8C78C));
      g.fill(new Polygon(new int[] {x - 1, x, x + 1, x}, new int[] {y, y - 1, y, y + 1}, 4));
    }
    g.dispose();
    int[] colors = image.getRGB(0, 0, w, h, null, 0, w);
    for (int n = 0; n < colors.length; n++) colors[n] &= 0xFFFFFF;
    return new RasterImage(w, h, colors);
  }
}
