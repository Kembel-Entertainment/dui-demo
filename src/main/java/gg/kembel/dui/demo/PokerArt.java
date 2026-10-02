package gg.kembel.dui.demo;

import gg.kembel.dui.core.RasterImage;
import java.util.*;

/**
 * Original table illustration and small runtime portraits. Cards and chip motion use dui
 * primitives.
 */
public final class PokerArt {
  private PokerArt() {}

  private static final RasterImage TABLE = table();

  private static RasterImage table() {
    try (var in = PokerArt.class.getResourceAsStream("/art/holdem-table.png")) {
      return RasterImage.decode(Objects.requireNonNull(in).readAllBytes(), 0x16171D);
    } catch (Exception e) {
      throw new IllegalStateException("Missing original poker table artwork", e);
    }
  }

  public static Map<String, RasterImage> images(HoldemGame game) {
    var images = new HashMap<String, RasterImage>();
    images.put("table", TABLE);
    images.put("wordmark", wordmark());
    for (int seat = 1; seat < 4; seat++) {
      images.put("portrait_" + seat, portrait(seat));
      images.put("hole_" + seat, miniCards(game, seat));
    }
    return images;
  }

  private static RasterImage wordmark() {
    int w = 72, h = 18;
    int[] pixels = new int[w * h];
    Arrays.fill(pixels, 0x111E2A);
    String[] glyphs = {
      "101101101101010",
      "111100110100111",
      "100100100100111",
      "101101101101010",
      "111100110100111",
      "111010010010010"
    };
    for (int i = 0; i < 6; i++)
      for (int y = 0; y < 5; y++)
        for (int x = 0; x < 3; x++)
          if (glyphs[i].charAt(y * 3 + x) == '1')
            for (int dy = 0; dy < 3; dy++)
              for (int dx = 0; dx < 3; dx++) {
                int px = i * 12 + x * 3 + dx, py = y * 3 + dy + 1;
                pixels[(py + 2) * w + px + 1] = 0x513F2E;
              }
    for (int i = 0; i < 6; i++)
      for (int y = 0; y < 5; y++)
        for (int x = 0; x < 3; x++)
          if (glyphs[i].charAt(y * 3 + x) == '1')
            for (int dy = 0; dy < 3; dy++)
              for (int dx = 0; dx < 3; dx++)
                pixels[(y * 3 + dy + 1) * w + i * 12 + x * 3 + dx] =
                    y < 2 ? 0xF5D990 : y < 4 ? 0xDFB56F : 0xC09052;
    return new RasterImage(w, h, pixels);
  }

  private static RasterImage portrait(int seat) {
    int w = 18;
    int[] pixels = new int[w * w];
    Arrays.fill(pixels, 0x182A35);
    int accent = seat == 1 ? 0xAB89DB : seat == 2 ? 0x84C69C : 0xF39695;
    for (int y = 1; y < 17; y++)
      for (int x = 1; x < 17; x++) {
        int c = 0x182A35;
        if (y > 11 && Math.abs(x - 9) < 6) c = accent;
        if (y >= 5 && y <= 12 && x >= 5 && x <= 12) c = 0xE9B59A;
        if (y < 7 && y >= 3 && x >= 4 && x <= 13)
          c = seat == 2 ? 0x35453B : seat == 1 ? 0x554370 : 0x763E4D;
        if (y == 9 && (x == 6 || x == 11)) c = 0x17222A;
        if (y == 12 && x >= 7 && x <= 10) c = 0x965C59;
        if (y == 1 || y == 16 || x == 1 || x == 16) c = accent;
        pixels[y * w + x] = c;
      }
    return new RasterImage(w, w, pixels);
  }

  private static boolean rankPixel(int rank, int x, int y) {
    String[] glyphs = {
      "111001111100111",
      "111001111001111",
      "101101111001001",
      "111100111001111",
      "111100111101111",
      "111001001010010",
      "111101111101111",
      "111101111001111",
      "111010010010010",
      "001001001101111",
      "111101101110011",
      "101101110101101",
      "010101111101101"
    };
    return glyphs[rank - 2].charAt(y * 3 + x) == '1';
  }

  private static RasterImage miniCards(HoldemGame g, int seat) {
    int w = 28, h = 18;
    int[] rgb = new int[w * h];
    Arrays.fill(rgb, 0x172D32);
    for (int k = 0; k < 2; k++)
      for (int y = 1; y < 17; y++)
        for (int x = 1; x < 12; x++) {
          int card = g.hole(seat, k),
              ink = PokerHand.suit(card) == 1 || PokerHand.suit(card) == 2 ? 0xC54B59 : 0x233746;
          boolean show = g.ended() && !g.folded[seat] && g.board.size() == 5;
          int c = show ? 0xFCF2DF : seat == 1 ? 0x775998 : seat == 2 ? 0x347D6D : 0xAE626D;
          if (x == 1 || x == 11 || y == 1 || y == 16) c = 0xD3B174;
          if (!show && x > 3 && x < 9 && y > 5 && y < 12 && (x + y) % 3 == 0) c = 0xE9C888;
          if (show
              && x >= 4
              && x <= 8
              && y >= 7
              && y <= 11
              && Math.abs(x - 6) + Math.abs(y - 9) < 3) c = ink;
          if (show
              && x >= 2
              && x < 5
              && y >= 3
              && y < 8
              && rankPixel(PokerHand.rank(card), x - 2, y - 3)) c = ink;
          rgb[y * w + k * 14 + x] = c;
        }
    return new RasterImage(w, h, rgb);
  }
}
