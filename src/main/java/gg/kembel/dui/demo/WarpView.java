package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Geometry and bindings only. Decoration, controls and transport stay in the template/library. */
public final class WarpView {
  private WarpView() {}

  public static Map<String, Object> data(WarpState s) {
    var d = new HashMap<String, Object>();
    int width = LayoutProfile.choose(s.compact).width(), height = s.compact ? 108 : 252;
    int size = s.compact ? 63 : 126, stride = s.compact ? 66 : 108;
    int center = s.compact ? 129 : 174, y = s.compact ? 18 : 54;
    int clipX = s.compact ? 68 : 82, clipW = s.compact ? 185 : 316;
    d.put("width", width);
    d.put("height", height);
    d.put("inner", width - 24);
    d.put("wide", !s.compact);
    d.put("compact", s.compact);
    d.put("motion", s.motion);
    d.put("moving", s.moving);
    d.put("tick", Math.max(0, s.startedAt));
    d.put("duration", WarpState.SLIDE_TICKS);
    d.put("distance", s.moving ? s.direction * stride : 0);
    d.put("viewportX", clipX);
    d.put("viewportY", y);
    d.put("viewportW", clipW);
    d.put("viewportH", size);
    d.put("selected", s.selected);
    d.put("name", s.destination().name());
    d.put(
        "subtitle",
        s.arrived
            ? "Demo portal ready. Your controller decides what happens next."
            : s.destination().subtitle());
    d.put("accent", "#%06X".formatted(s.destination().accent()));
    d.put("counter", (s.selected + 1) + " / 4");
    d.put("sizeLabel", s.compact ? "Wide" : "Compact");
    d.put("sizeX", width - (s.compact ? 72 : 90));
    d.put("sizeW", s.compact ? 60 : 78);
    d.put("motionX", width - (s.compact ? 87 : 111));
    d.put("motionW", s.compact ? 75 : 99);
    d.put("motionLabel", s.motion ? "Motion on" : "Motion off");
    d.put("navX", s.compact ? 12 : 24);
    d.put("navR", s.compact ? 281 : 420);
    d.put("navY", s.compact ? 36 : 99);
    d.put("navW", s.compact ? 27 : 36);
    d.put("footer", s.compact ? 90 : 225);
    d.put("travelW", s.compact ? 153 : 180);
    d.put("travelLabel", s.arrived ? "Portal ready!" : "Enter portal >");
    d.put("nameY", s.compact ? 81 : 189);
    d.put("textW", s.compact ? 296 : 408);
    d.put("titleW", s.compact ? 224 : 330);
    d.put("titleY", s.compact ? 0 : 9);
    d.put("title", s.compact ? "WAYFARER ATLAS" : "WAYFARER / ATLAS");
    d.put("paperY", s.compact ? 18 : 45);
    d.put("paperH", s.compact ? 63 : 135);
    d.put("arrived", s.arrived);
    var cards = new ArrayList<Map<String, Object>>();
    for (var slotInfo :
        Carousel.window(
            WarpState.DESTINATIONS.size(),
            s.selected,
            1,
            center,
            stride,
            s.moving ? s.direction : 0)) {
      int slot = slotInfo.offset(), index = slotInfo.index();
      var card = new HashMap<String, Object>();
      card.put("id", "card_" + index);
      card.put("index", index);
      card.put("x", slotInfo.x());
      card.put("y", y);
      card.put("size", size);
      card.put("active", slot == 0);
      card.put(
          "transition",
          s.moving ? "slide" : s.arrived && s.startedAt >= 0 && slot == 0 ? "bounce" : "");
      cards.add(card);
    }
    d.put("cards", cards);
    var cardHits = new ArrayList<Map<String, Object>>();
    for (int slot = -1; slot <= 1; slot++) {
      int index = Math.floorMod(s.selected + slot, 4);
      cardHits.add(
          Map.of(
              "slot",
              slot,
              "x",
              center + slot * stride + size / 8,
              "w",
              size * 3 / 4,
              "name",
              WarpState.DESTINATIONS.get(index).name()));
    }
    d.put("cardHits", cardHits);
    var tabs = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < 4; i++) {
      var dest = WarpState.DESTINATIONS.get(i);
      var tab = new HashMap<String, Object>();
      tab.put("index", i);
      tab.put("x", 210 + i * 30);
      tab.put("label", Integer.toString(i + 1));
      tab.put("fill", i == s.selected ? "#EAC477" : "#604138");
      tab.put("color", i == s.selected ? "#35262D" : "#F9E7C8");
      tab.put("name", dest.name());
      tabs.add(tab);
    }
    d.put("tabs", tabs);
    var planks = new ArrayList<Map<String, Object>>();
    for (int row = 0; row < height; row += 18)
      planks.add(Map.of("y", row, "w", width - 4, "color", row % 36 == 0 ? "#4A3035" : "#51353A"));
    d.put("planks", planks);
    return d;
  }

  public static Canvas render(WarpState s) {
    try (var in = WarpView.class.getResourceAsStream("/ui/warps.html")) {
      return MenuTemplate.parse(
              new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8),
              new gg.kembel.dui.core.GlyphFont(),
              gg.kembel.dui.components.VisualComponents.registry(),
              "demo template")
          .render(data(s));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
