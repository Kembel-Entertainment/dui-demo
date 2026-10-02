package gg.kembel.dui.demo;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Original demo artwork. No server capture, font or texture is required. */
final class WorldMapArt {
  static BufferedImage image(int w, int h) {
    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
  }

  private static void quality(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }

  static BufferedImage solid(int color) {
    var a = image(16, 16);
    var g = a.createGraphics();
    g.setColor(new Color(color));
    g.fillRect(0, 0, 16, 16);
    g.dispose();
    return a;
  }

  static BufferedImage header() {
    var a = image(16, 48);
    var g = a.createGraphics();
    g.setColor(new Color(0x101D28));
    g.fillRect(0, 0, 16, 41);
    for (int y = 0; y < 7; y++) {
      g.setColor(new Color(16, 29, 40, (7 - y) * 32));
      g.fillRect(0, 41 + y, 16, 1);
    }
    g.setColor(new Color(0xBBA16B));
    g.fillRect(0, 40, 16, 1);
    g.dispose();
    return a;
  }

  static BufferedImage hover() {
    var a = image(152, 80);
    var g = a.createGraphics();
    g.setColor(new Color(0xFFF2B5));
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(1, 1, 149, 77, 15, 15);
    g.dispose();
    return a;
  }

  static BufferedImage calibration() {
    var a = image(144, 72);
    var g = a.createGraphics();
    int[] colors = {0x3388FF, 0xF06DC0, 0x50D5A0, 0xFFCA55};
    for (int i = 0; i < 4; i++) {
      g.setColor(new Color(colors[i]));
      g.fillRect(i % 2 * 72, i / 2 * 36, 72, 36);
    }
    g.setColor(Color.WHITE);
    g.drawRect(0, 0, 143, 71);
    g.dispose();
    return a;
  }

  static BufferedImage atlas() {
    BufferedImage a = image(1120, 640);
    Graphics2D g = a.createGraphics();
    quality(g);
    g.setPaint(new GradientPaint(0, 0, new Color(0x193348), 1120, 640, new Color(0x24606C)));
    g.fillRect(0, 0, 1120, 640);
    Random r = new Random(7183);
    g.setColor(new Color(255, 255, 255, 13));
    g.setStroke(new BasicStroke(1));
    for (int y = 0; y < 640; y += 32)
      for (int x = 0; x < 1120; x += 32) g.drawLine(x, y, x + 12, y);
    Path2D land = new Path2D.Double();
    land.moveTo(110, 510);
    land.curveTo(65, 295, 285, 110, 450, 130);
    land.curveTo(560, 85, 635, 110, 700, 210);
    land.curveTo(900, 105, 1060, 305, 950, 420);
    land.curveTo(1040, 565, 810, 605, 710, 525);
    land.curveTo(535, 675, 290, 570, 110, 510);
    land.closePath();
    g.setStroke(new BasicStroke(24));
    g.setColor(new Color(0x163742));
    g.draw(land);
    g.setStroke(new BasicStroke(12));
    g.setColor(new Color(0xC7B887));
    g.draw(land);
    g.setPaint(new GradientPaint(0, 150, new Color(0x588B6D), 900, 500, new Color(0x87A875)));
    g.fill(land);
    Shape clip = g.getClip();
    g.clip(land);
    for (int i = 0; i < 550; i++) {
      int x = r.nextInt(1120), y = r.nextInt(640);
      g.setColor(new Color(i % 2 == 0 ? 0x507B5C : 0x709571));
      g.fillOval(x, y, 4 + r.nextInt(16), 3 + r.nextInt(10));
    }
    Path2D river = new Path2D.Double();
    river.moveTo(470, 100);
    river.curveTo(625, 190, 270, 235, 435, 310);
    river.curveTo(560, 360, 390, 440, 560, 660);
    g.setStroke(new BasicStroke(23, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.setColor(new Color(0xBFD5B1));
    g.draw(river);
    g.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.setColor(new Color(0x438F9F));
    g.draw(river);
    for (int i = 0; i < 95; i++) {
      int x = 220 + r.nextInt(250), y = 290 + r.nextInt(230);
      g.setColor(new Color(0x2C614D));
      g.fillRect(x + 5, y + 14, 3, 12);
      g.fillPolygon(new int[] {x, x + 7, x + 14}, new int[] {y + 18, y, y + 18}, 3);
      g.setColor(new Color(0x3F7960));
      g.fillPolygon(new int[] {x + 2, x + 7, x + 12}, new int[] {y + 10, y - 2, y + 10}, 3);
    }
    for (int i = 0; i < 25; i++) {
      int x = 650 + r.nextInt(230), y = 210 + r.nextInt(290);
      g.setColor(new Color(0x6D7460));
      g.fillPolygon(new int[] {x - 20, x, x + 24}, new int[] {y + 30, y - 23, y + 30}, 3);
      g.setColor(new Color(0xDADCB9));
      g.fillPolygon(new int[] {x - 8, x, x + 10}, new int[] {y - 4, y - 23, y - 3}, 3);
    }
    g.setClip(clip);
    g.setColor(new Color(0xEACF91));
    g.setStroke(
        new BasicStroke(
            3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[] {7, 7}, 0));
    g.draw(new CubicCurve2D.Double(400, 400, 440, 300, 500, 275, 560, 240));
    g.draw(new CubicCurve2D.Double(560, 240, 630, 270, 660, 340, 720, 400));
    g.setColor(new Color(255, 255, 255, 110));
    for (int i = 0; i < 10; i++) {
      int x = 480 + r.nextInt(210), y = 125 + r.nextInt(85);
      g.fillRoundRect(x, y, 70, 20, 20, 20);
      g.fillOval(x + 15, y - 9, 28, 28);
    }
    g.setColor(new Color(0xDB925A));
    g.fillOval(700, 415, 55, 18);
    g.setColor(new Color(0x432F30));
    g.fillPolygon(new int[] {700, 730, 763}, new int[] {427, 370, 427}, 3);
    g.setColor(new Color(0xEFAB65));
    g.fillOval(722, 373, 16, 6);
    g.setFont(new Font(Font.SERIF, Font.BOLD, 34));
    g.setColor(new Color(0xF1E3B7));
    g.drawString("ATLAS OF ELSEWHERE", 55, 68);
    g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    g.setColor(new Color(0xB5D2C8));
    g.drawString("THREE DESTINATIONS / ONE SMALL ADVENTURE", 57, 94);
    g.setFont(new Font(Font.SERIF, Font.ITALIC, 18));
    g.setColor(new Color(0xD5E7D1));
    g.drawString("The Quiet Sea", 80, 235);
    g.drawString("Highlands", 860, 255);
    g.setStroke(new BasicStroke(2));
    g.setColor(new Color(0xD2C08E));
    g.drawOval(966, 475, 75, 75);
    g.drawLine(1003, 461, 1003, 562);
    g.drawLine(953, 512, 1054, 512);
    g.setFont(new Font(Font.SERIF, Font.BOLD, 15));
    g.drawString("N", 997, 454);
    g.drawString("E", 1060, 517);
    g.dispose();
    return a;
  }

  static BufferedImage marker(WorldMapAssets.Target t, int index) {
    BufferedImage a = image((int) t.width(), (int) t.height());
    Graphics2D g = a.createGraphics();
    quality(g);
    int w = a.getWidth(), h = a.getHeight();
    g.setColor(new Color(0, 0, 0, 90));
    g.fillRoundRect(3, 5, w - 5, h - 6, 16, 16);
    g.setColor(new Color(t.unlocked() ? 0x132C37 : 0x312D38));
    g.fillRoundRect(1, 1, w - 4, h - 5, 16, 16);
    g.setStroke(new BasicStroke(2));
    g.setColor(new Color(t.unlocked() ? 0xF2D494 : 0x887880));
    g.drawRoundRect(1, 1, w - 4, h - 5, 16, 16);
    g.setColor(new Color(new int[] {0x78D3A0, 0x91CAEA, 0xCF8E75}[index % 3]));
    g.fillOval(w / 2 - 10, 9, 20, 20);
    g.setColor(new Color(0x122B35));
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    g.drawString(t.unlocked() ? String.valueOf(index + 1) : "×", w / 2 - 4, 24);
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    g.setColor(new Color(0xF6EACF));
    g.drawString(t.name(), (w - g.getFontMetrics().stringWidth(t.name())) / 2, 46);
    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
    String sub = t.unlocked() ? "AIM TO EXPLORE" : "LOCKED / DEMO";
    g.setColor(new Color(0xA7BDB7));
    g.drawString(sub, (w - g.getFontMetrics().stringWidth(sub)) / 2, 61);
    g.dispose();
    return a;
  }

  static BufferedImage button(String text) {
    BufferedImage a = image(96, 44);
    Graphics2D g = a.createGraphics();
    quality(g);
    g.setColor(new Color(0x162D39));
    g.fillRoundRect(1, 1, 93, 41, 11, 11);
    g.setColor(new Color(0xE5C88A));
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(1, 1, 93, 41, 11, 11);
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    g.drawString(text, (96 - g.getFontMetrics().stringWidth(text)) / 2, 27);
    g.dispose();
    return a;
  }
}
