package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class AdventView {
  private AdventView() {}

  // Packed shelf cells: four wide boxes, four tall boxes and sixteen regular parcels.
  private static final int[][] CELLS = {
    {0, 0, 2, 1}, {2, 0, 1, 2}, {3, 0, 1, 1}, {4, 0, 1, 1}, {5, 0, 2, 1}, {7, 0, 1, 2},
    {0, 1, 1, 1}, {1, 1, 1, 1}, {3, 1, 1, 2}, {4, 1, 2, 1}, {6, 1, 1, 1}, {0, 2, 1, 1},
    {1, 2, 1, 2}, {2, 2, 1, 1}, {4, 2, 1, 1}, {5, 2, 1, 1}, {6, 2, 2, 1}, {0, 3, 1, 1},
    {2, 3, 1, 1}, {3, 3, 1, 1}, {4, 3, 1, 1}, {5, 3, 1, 1}, {6, 3, 1, 1}, {7, 3, 1, 1}
  };
  private static final int[] DAYS = {
    5, 13, 20, 8, 18, 2, 15, 1, 9, 24, 10, 11, 7, 23, 16, 4, 19, 14, 6, 3, 22, 12, 21, 17
  };
  private static final Map<String, RasterImage> WIDE = images(false), COMPACT = images(true);

  private static List<Map<String, Object>> parcels(boolean compact) {
    var result = new ArrayList<Map<String, Object>>();
    int stride = compact ? 36 : 54,
        row = compact ? 27 : 54,
        origin = compact ? 16 : 27,
        top = compact ? 36 : 99;
    for (int i = 0; i < 24; i++) {
      int[] cell = CELLS[i];
      int w = cell[2] * stride - 6, h = cell[3] * row - (compact ? 0 : 9);
      int day = DAYS[i];
      var d = new HashMap<String, Object>();
      d.put("day", day);
      d.put("x", origin + cell[0] * stride);
      d.put("y", top + cell[1] * row);
      d.put("w", w);
      d.put("h", h);
      d.put("ix", origin + cell[0] * stride + 3);
      d.put("iy", top + cell[1] * row + 2);
      d.put("iw", w - 6);
      d.put("ih", h - 12);
      d.put("ny", top + cell[1] * row + h - 9);
      d.put("color", hex(AdventArt.COLORS[day % 6]));
      d.put("key", "gift_" + day);
      result.add(d);
    }
    return List.copyOf(result);
  }

  private static Map<String, RasterImage> images(boolean compact) {
    var images = new HashMap<String, RasterImage>();
    images.put("title", AdventArt.wordmark());
    images.put("roof", AdventArt.roof(compact ? 74 : 114, compact ? 3 : 7));
    for (var d : parcels(compact))
      images.put(
          (String) d.get("key"),
          AdventArt.parcel(
              ((int) d.get("iw") + 1) / 2, ((int) d.get("ih") + 1) / 2, (int) d.get("day")));
    return Map.copyOf(images);
  }

  public static Map<String, RasterImage> images(AdventState s) {
    return s.compact ? COMPACT : WIDE;
  }

  private static String hex(int c) {
    return "#%06X".formatted(c);
  }

  public static Map<String, Object> data(AdventState s, long tick) {
    boolean board = s.phase == AdventState.Phase.BOARD,
        shown = s.phase == AdventState.Phase.REVEALED;
    int width = s.compact ? 320 : 480,
        height = board ? (s.compact ? 180 : 360) : (s.compact ? 135 : 288);
    var d = new HashMap<String, Object>();
    d.put("width", width);
    d.put("height", height);
    d.put("inner", width - 18);
    d.put("board", board);
    d.put("scene", !board);
    d.put("revealed", shown);
    d.put("opening", !board && !shown);
    d.put("motion", s.motion && (board || !s.effectsFinished));
    d.put("tick", board ? tick : s.startedAt);
    d.put("rewardTick", shown ? s.rewardAt : tick);
    d.put("footer", height - 27);
    d.put("parcels", parcels(s.compact));
    d.put("titleX", s.compact ? 12 : 27);
    d.put("titleY", s.compact ? 0 : 9);
    d.put("titleW", s.compact ? 192 : 240);
    d.put("titleH", s.compact ? 27 : 36);
    d.put("subtitleY", s.compact ? 45 : 54);
    d.put("shelfY", s.compact ? 36 : 90);
    d.put("sceneY", s.compact ? 36 : 63);
    d.put("wide", !s.compact);
    d.put("parcelVisible", !s.compact || !shown);
    d.put("roofY", s.compact ? 27 : 63);
    d.put("roofH", s.compact ? 9 : 27);
    d.put("shelfH", board ? (s.compact ? 117 : 225) : height - (s.compact ? 63 : 99));
    d.put("lightsY", s.compact ? 27 : 90);
    d.put("lightsW", width - 42);
    d.put("lightsCount", s.compact ? 14 : 22);
    d.put("motionX", width - 111);
    d.put("motionLabel", s.motion ? "Motion on" : "Motion off");
    d.put("sizeLabel", s.compact ? "Wide" : "Compact");
    d.put("selected", s.selected);
    d.put("name", board ? "" : s.gift().name());
    d.put("caption", board ? "" : s.gift().caption());
    d.put("accent", hex(AdventArt.COLORS[s.selected % 6]));
    int itemSize = s.compact ? 81 : 126, itemX = s.compact ? 6 : 24, itemY = s.compact ? 27 : 90;
    d.put("itemX", itemX);
    d.put("itemY", itemY);
    d.put("itemSize", itemSize);
    d.put("lift", s.compact ? 24 : 42);
    d.put("rewardX", itemX + itemSize / 4);
    d.put("rewardY", itemY + itemSize / 4);
    d.put("rewardSize", s.compact ? 36 : 54);
    d.put("textX", s.compact ? 105 : 180);
    d.put("textY", s.compact ? 45 : 117);
    d.put("textW", s.compact ? 198 : 279);
    d.put("copyY", s.compact ? 63 : 153);
    d.put("tagY", s.compact ? 81 : 198);
    d.put("buttonW", s.compact ? 93 : 153);
    d.put("againX", s.compact ? 111 : 180);
    d.put("originX", itemX + itemSize / 2);
    d.put("originY", itemY + itemSize / 2);
    return d;
  }

  public static Canvas render(AdventState s, long tick) {
    try (var in = AdventView.class.getResourceAsStream("/ui/advent.html")) {
      return MenuTemplate.parse(
              new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8))
          .render(data(s, tick), images(s));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
