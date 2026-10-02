package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Presentation data only: all layout, colours and shader geometry live in slots.html. */
public final class SlotView {
  private static final MenuTemplate DEFAULT = load();

  private SlotView() {}

  private static MenuTemplate load() {
    try (var in = SlotView.class.getResourceAsStream("/ui/slots.html")) {
      return DemoTemplates.parse(
          new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8),
          DemoTemplates.font(),
          DemoVisualComponents.registry(),
          "demo template");
    } catch (Exception e) {
      throw new IllegalStateException("Invalid bundled arcade template", e);
    }
  }

  public static Canvas render(SlotState s) {
    return DEFAULT.render(data(s));
  }

  public static Map<String, Object> data(SlotState s) {
    var data = new LinkedHashMap<String, Object>();
    data.put("compact", s.compact);
    data.put("spacious", !s.compact);
    data.put("paytable", s.paytable);
    data.put("machine", !s.paytable);
    data.put("chips", s.chips);
    data.put("bet", s.bet());
    data.put("message", s.message);
    data.put(
        "compactMessage",
        s.pending ? (s.preview ? "PREVIEW / Spinning..." : "Spinning...") : s.message);
    data.put("statusColor", s.preview ? "#7CE3D0" : "#FFF0C9");
    data.put("controlsLocked", s.pending);
    data.put("spinLocked", !s.canSpin());
    data.put("refillLocked", s.pending || s.chips >= 1000);
    data.put("motion", s.motion);
    data.put("startedAt", s.startedAt);
    data.put("motionLabel", s.motion ? "FX ON" : "FX OFF");
    data.put("modeLabel", SlotState.MODES.get(s.modeIndex));
    data.put(
        "spinLabel",
        s.pending ? "SPINNING..." : s.modeIndex == 0 ? "SPIN / " + s.bet() : "PLAY PREVIEW");
    var reels = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < s.reels.length; i++)
      reels.add(Map.of("index", i, "value", s.reels[i], "previous", s.previous[i]));
    data.put("reels", reels);
    var rows = new ArrayList<Map<String, Object>>();
    for (int i : new int[] {0, 1, 2, 5, 3, 4})
      rows.add(
          Map.of(
              "label",
              String.format(
                  Locale.ROOT,
                  "%-10s %2dx",
                  i == 0 ? "777" : SlotState.SYMBOLS.get(i),
                  SlotState.TRIPLES[i])));
    rows.add(Map.of("label", String.format(Locale.ROOT, "%-10s %2dx", "Any pair", 1)));
    data.put("payouts", rows);
    data.put(
        "coinCount",
        SlotState.multiplier(s.reels) == 25 ? 42 : SlotState.multiplier(s.reels) > 0 ? 20 : 0);
    return data;
  }
}
