package gg.kembel.dui.demo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Original pixel landscapes and brass frames. Art remains a consumer-owned pack addition. */
public final class WarpArt {
  private WarpArt() {}

  public static BufferedImage card(int index, boolean selected) {
    var image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
    var g = image.createGraphics();
    g.scale(2, 2);
    rect(g, 10, 5, 46, 59, 0x35262D);
    rect(g, 8, 4, 48, 57, 0x33262D);
    rect(g, 7, 6, 50, 53, 0x33262D);
    rect(g, 9, 3, 46, 58, selected ? 0xFFF2CE : 0xA87957);
    rect(g, 8, 6, 48, 52, selected ? 0xFFF2CE : 0xA87957);
    rect(g, 10, 5, 44, 54, 0x5B3F3C);
    rect(g, 11, 6, 42, 51, 0x8AB8C0);
    g.setClip(11, 6, 42, 51);
    switch (index) {
      case 0 -> willow(g);
      case 1 -> bloom(g);
      case 2 -> ember(g);
      case 3 -> astral(g);
      default -> throw new IllegalArgumentException("Unknown landscape");
    }
    g.setClip(null);
    if (!selected) {
      g.setColor(new Color(0x1C142B));
      g.setComposite(AlphaComposite.SrcOver.derive(.20f));
      g.fillRect(11, 6, 42, 51);
      g.setComposite(AlphaComposite.SrcOver);
    }
    rect(g, 11, 5, 42, 1, selected ? 0xEAC477 : 0x755346);
    rect(g, 11, 57, 42, 2, 0x392A30);
    // Four hand-pixelled realm seals, independent of Minecraft item rendering.
    rect(g, 40, 45, 11, 12, 0x2F2638);
    rect(g, 41, 46, 9, 10, 0xFFF1CE);
    int accent = WarpState.DESTINATIONS.get(index).accent();
    rect(g, 42, 47, 7, 8, accent);
    if (index == 0) {
      rect(g, 45, 48, 2, 6, 0x4C7544);
      rect(g, 43, 49, 5, 3, 0x376348);
    }
    if (index == 1) {
      rect(g, 45, 50, 1, 5, 0x795044);
      rect(g, 43, 48, 5, 3, 0xD45B99);
      rect(g, 44, 47, 3, 5, 0xEF78B5);
    }
    if (index == 2) {
      rect(g, 44, 48, 3, 6, 0x743450);
      rect(g, 45, 49, 1, 4, 0xD28AFF);
    }
    if (index == 3) {
      rect(g, 44, 48, 3, 6, 0x4A618A);
      rect(g, 43, 49, 5, 4, 0x4A618A);
      rect(g, 44, 49, 2, 2, 0xD5F0E4);
    }
    for (int k = 0; k <= index; k++) rect(g, 15 + k * 3, 8, 1, 3, 0xFFF1CE);
    g.dispose();
    return image;
  }

  private static void willow(Graphics2D g) {
    rect(g, 11, 6, 42, 51, 0x75BCCE);
    rect(g, 13, 12, 11, 2, 0xD9EFE0);
    rect(g, 17, 10, 9, 2, 0xD9EFE0);
    poly(g, 0x729A88, 11, 29, 18, 20, 29, 25, 36, 18, 53, 25, 53, 57, 11, 57);
    poly(g, 0x477D69, 11, 34, 24, 26, 39, 29, 47, 24, 53, 31, 53, 57, 11, 57);
    rect(g, 11, 38, 42, 19, 0x447C48);
    poly(g, 0x78B3BC, 25, 28, 32, 28, 30, 37, 38, 45, 37, 57, 25, 57, 27, 46, 23, 40);
    poly(g, 0xAEDBD1, 28, 29, 30, 29, 27, 40, 34, 46, 32, 57, 29, 57, 30, 46, 25, 40);
    rect(g, 37, 24, 4, 25, 0x8E5B47);
    rect(g, 39, 25, 2, 21, 0xB48152);
    rect(g, 33, 14, 17, 15, 0x315D46);
    rect(g, 36, 11, 12, 19, 0x315D46);
    rect(g, 30, 18, 19, 8, 0x477E4D);
    rect(g, 35, 13, 13, 15, 0x5A9654);
    rect(g, 37, 15, 3, 4, 0x89BC62);
    rect(g, 42, 21, 4, 3, 0x78AC58);
    rect(g, 33, 28, 2, 8, 0x477E4D);
    rect(g, 46, 27, 2, 9, 0x5A9654);
    // A tiny abandoned stone tower on the river bank.
    rect(g, 14, 32, 8, 9, 0x8BA391);
    rect(g, 14, 30, 2, 3, 0xD0C8A1);
    rect(g, 17, 29, 2, 4, 0xD0C8A1);
    rect(g, 20, 30, 2, 3, 0xD0C8A1);
    rect(g, 17, 35, 2, 6, 0x365B58);
    rect(g, 14, 39, 8, 2, 0x577561);
    scatter(g, 0, 0x82AC5C, 11, 40, 42, 17, 50);
  }

  private static void bloom(Graphics2D g) {
    rect(g, 11, 6, 42, 51, 0xB8B8DA);
    rect(g, 13, 10, 10, 2, 0xF9D5CF);
    rect(g, 17, 8, 8, 2, 0xF9D5CF);
    poly(g, 0xAAA2BF, 11, 27, 24, 19, 33, 26, 42, 18, 53, 25, 53, 57, 11, 57);
    rect(g, 11, 34, 42, 23, 0x729779);
    poly(g, 0xDDCBAA, 29, 38, 34, 38, 38, 57, 25, 57);
    rect(g, 22, 27, 19, 16, 0xE7C4A0);
    rect(g, 22, 36, 19, 7, 0xB78474);
    poly(g, 0x703F6A, 19, 29, 31, 18, 43, 29);
    poly(g, 0xAA5E8E, 19, 27, 31, 18, 43, 27);
    rect(g, 24, 30, 5, 5, 0x696998);
    rect(g, 35, 30, 4, 5, 0x696998);
    rect(g, 25, 31, 2, 2, 0xFFDDAC);
    rect(g, 36, 31, 2, 2, 0xFFDDAC);
    rect(g, 31, 34, 4, 9, 0x76546A);
    rect(g, 31, 39, 1, 1, 0xFFDDAC);
    rect(g, 45, 14, 3, 26, 0x876076);
    rect(g, 39, 8, 14, 13, 0xD88AA9);
    rect(g, 43, 6, 10, 17, 0xD88AA9);
    rect(g, 41, 8, 10, 9, 0xF3AEBD);
    rect(g, 39, 11, 5, 5, 0xFFD2C8);
    rect(g, 14, 20, 2, 22, 0x876076);
    rect(g, 11, 12, 11, 13, 0xCF88AB);
    rect(g, 12, 13, 9, 8, 0xF3AEBD);
    rect(g, 16, 13, 4, 3, 0xFFD2C8);
    scatter(g, 1, 0xF5A7C2, 11, 36, 42, 21, 46);
    scatter(g, 13, 0xFFF2CE, 11, 36, 42, 21, 15);
  }

  private static void ember(Graphics2D g) {
    rect(g, 11, 6, 42, 51, 0x492E50);
    poly(g, 0x713E4D, 11, 28, 19, 12, 27, 24, 37, 9, 53, 19, 53, 57, 11, 57);
    poly(g, 0xA35149, 11, 37, 22, 20, 32, 34, 46, 18, 53, 28, 53, 57, 11, 57);
    rect(g, 11, 43, 42, 14, 0xF18A4D);
    rect(g, 13, 46, 14, 2, 0xFFD371);
    rect(g, 27, 49, 23, 2, 0xFFD371);
    rect(g, 15, 54, 25, 2, 0xFFD371);
    rect(g, 43, 44, 8, 2, 0xFFD371);
    // Obsidian gate and a winding bridge, rather than the reference's landscape.
    poly(g, 0x8A6662, 19, 43, 22, 40, 41, 49, 38, 53);
    poly(g, 0xC88966, 19, 43, 22, 42, 41, 51, 39, 52);
    rect(g, 21, 22, 17, 20, 0x352B43);
    rect(g, 20, 20, 19, 4, 0x48374E);
    rect(g, 21, 17, 3, 7, 0x5B3F50);
    rect(g, 28, 16, 3, 8, 0x5B3F50);
    rect(g, 35, 17, 3, 7, 0x5B3F50);
    rect(g, 26, 27, 7, 15, 0x8655A7);
    rect(g, 27, 28, 5, 13, 0xC376BC);
    rect(g, 29, 30, 2, 10, 0xE6A1D1);
    scatter(g, 2, 0xFFC17D, 11, 8, 42, 35, 17);
  }

  private static void astral(Graphics2D g) {
    rect(g, 11, 6, 42, 51, 0x353652);
    scatter(g, 3, 0xAFA3CD, 11, 6, 42, 51, 60);
    rect(g, 39, 9, 6, 7, 0xEBDFC0);
    rect(g, 42, 9, 4, 5, 0x353652);
    island(g, 13, 20, 17, 0xC7CFB4);
    island(g, 33, 34, 18, 0xC7CFB4);
    island(g, 12, 49, 20, 0xA6B6AC);
    rect(g, 20, 10, 3, 10, 0x66738A);
    rect(g, 19, 11, 5, 6, 0x8499A6);
    rect(g, 21, 10, 1, 5, 0xCBDCD4);
    rect(g, 40, 25, 3, 9, 0x66738A);
    rect(g, 39, 26, 5, 5, 0x8B8BB6);
    rect(g, 41, 25, 1, 4, 0xD5C7EC);
    rect(g, 20, 34, 2, 2, 0xA5BFCE);
    rect(g, 23, 37, 2, 2, 0xA5BFCE);
    rect(g, 26, 40, 2, 2, 0xA5BFCE);
    rect(g, 29, 42, 2, 2, 0xA5BFCE);
    rect(g, 34, 11, 1, 5, 0xBDB1DD);
    rect(g, 32, 13, 5, 1, 0xBDB1DD);
  }

  private static void island(Graphics2D g, int x, int y, int w, int color) {
    poly(g, 0x64647A, x, y + 2, x + w, y + 2, x + w / 2, y + 12);
    rect(g, x, y, w, 3, color);
    rect(g, x + 3, y - 2, w - 5, 3, color);
    rect(g, x + 4, y + 3, 2, 4, 0x929889);
  }

  private static void scatter(
      Graphics2D g, int seed, int color, int x, int y, int w, int h, int count) {
    var rng = new Random(9147L + seed);
    for (int i = 0; i < count; i++) rect(g, x + rng.nextInt(w), y + rng.nextInt(h), 1, 1, color);
  }

  private static void rect(Graphics2D g, int x, int y, int w, int h, int rgb) {
    g.setColor(new Color(rgb));
    g.fillRect(x, y, w, h);
  }

  private static void poly(Graphics2D g, int rgb, int... xy) {
    int n = xy.length / 2;
    int[] x = new int[n], y = new int[n];
    for (int i = 0; i < n; i++) {
      x[i] = xy[i * 2];
      y[i] = xy[i * 2 + 1];
    }
    g.setColor(new Color(rgb));
    g.fillPolygon(x, y, n);
  }

  public static void write(ZipOutputStream zip) throws IOException {
    for (int index = 0; index < 4; index++)
      for (boolean selected : new boolean[] {false, true}) {
        String id =
            "warps/" + WarpState.DESTINATIONS.get(index).id() + (selected ? "_selected" : "");
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(card(index, selected), "png", bytes);
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

  private static void put(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
    var e = new ZipEntry(path);
    e.setTime(0);
    zip.putNextEntry(e);
    zip.write(bytes);
    zip.closeEntry();
  }
}
