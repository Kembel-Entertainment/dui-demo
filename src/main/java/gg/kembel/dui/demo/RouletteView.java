package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Public bindings for an ordinary template consumer. No bets, randomness or payouts in the view.
 */
public final class RouletteView {
  private RouletteView() {}

  public static Map<String, Object> data(RouletteGame s) {
    boolean c = s.compact;
    int width = c ? 320 : 480, height = c ? 153 : 324;
    var d = new HashMap<String, Object>();
    d.put("width", width);
    d.put("height", height);
    d.put("compact", c);
    d.put("wide", !c);
    d.put("table", c ? "table_compact" : "table_wide");
    d.put("pixel", c ? 3 : 5);
    d.put("tick", s.startedAt);
    d.put("motion", s.motion);
    d.put("locked", s.locked());
    d.put("motionLabel", s.motion ? "Motion" : "Still");
    d.put("motionX", width - 118);
    d.put("sizeX", width - 60);
    d.put("sizeLabel", c ? "Wide" : "Compact");
    d.put("wheelX", c ? 6 : 8);
    d.put("wheelY", c ? 18 : 36);
    d.put("wheelSize", c ? 108 : 216);
    d.put("wheelValue", s.wheelValue());
    d.put("previous", s.previousWheel);
    d.put("spinMode", s.phase == RouletteGame.Phase.SPINNING ? "spin" : "static");
    d.put("status", s.message);
    d.put("balance", s.balance);
    d.put("stake", s.stake());
    d.put("balanceLabel", "CREDITS " + s.balance);
    d.put("stakeLabel", "ON TABLE " + s.stake());
    d.put(
        "spinLabel",
        s.phase == RouletteGame.Phase.SPINNING
            ? "Spinning..."
            : s.phase == RouletteGame.Phase.PAYOUT ? "Settling..." : "Spin the wheel");
    d.put("spinLocked", s.locked() || s.bets.isEmpty());
    d.put("undoLocked", s.locked() || s.bets.isEmpty());
    d.put("repeatLocked", s.locked() || !s.bets.isEmpty() || s.lastBets.isEmpty());
    d.put("phase", s.phase);
    d.put("busy", s.locked());
    d.put(
        "result",
        s.phase == RouletteGame.Phase.SPINNING
            ? "BALL IN PLAY"
            : s.lastResult < 0
                ? "SINGLE ZERO"
                : "RESULT "
                    + s.lastResult
                    + " / "
                    + (s.lastResult == 0
                        ? "GREEN"
                        : RouletteGame.red(s.lastResult) ? "RED" : "BLACK"));
    d.put(
        "returnLabel",
        s.lastResult < 0
            ? "EUROPEAN / 37 POCKETS"
            : "RETURN " + s.lastReturn + " / NET " + (s.lastReturn - s.lastStake));
    d.put(
        "resultColor",
        s.lastResult == 0
            ? "#357660"
            : s.lastResult > 0 && RouletteGame.red(s.lastResult) ? "#A73B3A" : "#263D35");
    d.put("historyX", c ? 128 : 234);
    d.put("historyY", c ? 18 : 45);
    var history = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < s.history.size() && i < (c ? 7 : 9); i++) {
      int n = s.history.get(i);
      history.add(
          Map.of(
              "n",
              n,
              "x",
              (c ? 128 : 234) + i * (c ? 24 : 26),
              "y",
              c ? 18 : 45,
              "w",
              c ? 21 : 24,
              "h",
              c ? 9 : 18,
              "fill",
              fill(n),
              "border",
              i == 0 ? "#D6B264" : fill(n)));
    }
    d.put("history", history);
    var cells = new ArrayList<Map<String, Object>>();
    int x = c ? 128 : 248, y = c ? 27 : 72, cw = c ? 12 : 16, ch = c ? 18 : 27;
    cells.add(cell(s, "n:0", "0", x, y, cw, ch * 3, fill(0)));
    for (int col = 0; col < 12; col++)
      for (int row = 0; row < 3; row++) {
        int n = (col + 1) * 3 - row;
        cells.add(cell(s, "n:" + n, "" + n, x + cw * (col + 1), y + row * ch, cw, ch, fill(n)));
      }
    for (int row = 0; row < 3; row++)
      cells.add(
          cell(
              s,
              "c:" + (3 - row),
              c ? "C" + (3 - row) : "2:1",
              x + cw * 13,
              y + row * ch,
              c ? 22 : 24,
              ch,
              "#285242"));
    for (int i = 0; i < 3; i++)
      cells.add(
          cell(
              s,
              "d:" + (i + 1),
              (i + 1 == 1 ? "1st" : i + 1 == 2 ? "2nd" : "3rd") + " 12",
              x + cw + i * cw * 4,
              y + ch * 3,
              cw * 4,
              c ? 9 : 18,
              "#285242"));
    String[] keys = {"low", "even", "red", "black", "odd", "high"};
    String[] labels =
        c
            ? new String[] {"LO", "EV", "R", "B", "OD", "HI"}
            : new String[] {"1-18", "EVEN", "RED", "BLACK", "ODD", "19-36"};
    for (int i = 0; i < 6; i++)
      cells.add(
          cell(
              s,
              keys[i],
              labels[i],
              x + cw + i * cw * 2,
              y + ch * 3 + (c ? 9 : 18),
              cw * 2,
              c ? 9 : 18,
              i == 2 ? "#9E3938" : i == 3 ? "#1F302A" : "#285242"));
    d.put("cells", cells);
    var chips = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < 4; i++) {
      int n = RouletteGame.CHIPS[i];
      var item =
          new HashMap<String, Object>(
              Map.of(
                  "value",
                  n,
                  "x",
                  (c ? 128 : 234) + i * (c ? 45 : 58),
                  "y",
                  c ? 99 : 207,
                  "w",
                  c ? 42 : 52,
                  "h",
                  c ? 18 : 27,
                  "image",
                  "chip_" + n + (s.denomination == n ? "_selected" : ""),
                  "selected",
                  s.denomination == n,
                  "fill",
                  s.denomination == n ? "#E1CCA0" : "#EAE2CF",
                  "border",
                  s.denomination == n ? "#A07B39" : "#BEB195"));
      item.put("iconX", (c ? 130 : 238) + i * (c ? 45 : 58));
      item.put("iconY", c ? 102 : 213);
      item.put("iconSize", c ? 9 : 14);
      item.put("labelX", (c ? 141 : 256) + i * (c ? 45 : 58));
      item.put("labelY", c ? 102 : 216);
      item.put("labelW", c ? 27 : 26);
      chips.add(item);
    }
    d.put("chips", chips);
    boolean chip = s.motion && s.animated() && s.event.equals("chip") && !s.chipBet.isBlank();
    var target =
        cells.stream()
            .filter(v -> v.get("key").equals(s.chipBet))
            .findFirst()
            .orElse(cells.getFirst());
    d.put("chipFlight", chip);
    // The shared transfer's top anchor is (width * .5, height * .14).
    // Land on the actual marker, including short-row and zero-field placements.
    double markerX = (int) target.get("chipX") + (int) target.get("chipSize") / 2.0;
    double markerY = (int) target.get("chipY") + (int) target.get("chipSize") / 2.0;
    d.put("chipX", (int) Math.round(markerX - 10));
    d.put("chipY", (int) Math.round(markerY - 36 * .14));
    d.put("payout", s.phase == RouletteGame.Phase.PAYOUT && s.lastReturn > 0);
    d.put("payoutX", c ? 7 : 22);
    d.put("payoutY", c ? 103 : 245);
    d.put("payoutW", c ? 108 : 207);
    d.put("payoutH", c ? 43 : 72);
    d.put("spinX", c ? 128 : 234);
    d.put("spinY", c ? 135 : 270);
    d.put("spinW", c ? 183 : 234);
    d.put("controlsY", c ? 117 : 297);
    d.put("controlX", c ? 128 : 234);
    d.put("controlW", c ? 58 : 74);
    d.put("controlGap", c ? 4 : 6);
    d.put("controlH", c ? 9 : 18);
    d.put("creditsX", c ? 6 : 234);
    d.put("creditsY", c ? 135 : 243);
    d.put("creditsW", c ? 114 : 120);
    d.put("stakeX", c ? 128 : 354);
    d.put("stakeY", c ? 117 : 243);
    d.put("stakeW", c ? 183 : 114);
    d.put("resultY", c ? 126 : 270);
    d.put("resultW", c ? 116 : 220);
    return d;
  }

  private static String fill(int n) {
    return n == 0 ? "#357660" : RouletteGame.red(n) ? "#A73B3A" : "#23352E";
  }

  private static Map<String, Object> cell(
      RouletteGame s, String key, String label, int x, int y, int w, int h, String fill) {
    var d = new HashMap<String, Object>();
    boolean c = s.compact;
    int amount = (s.phase == RouletteGame.Phase.PAYOUT ? s.lastBets : s.bets).getOrDefault(key, 0);
    boolean win =
        s.lastResult >= 0
            && s.phase != RouletteGame.Phase.SPINNING
            && RouletteGame.wins(key, s.lastResult);
    d.put("key", key);
    d.put("id", "bet_" + key.replace(':', '_'));
    d.put("label", label);
    d.put("x", x);
    d.put("y", y);
    d.put("w", w);
    d.put("h", h);
    d.put("fill", fill);
    d.put(
        "chipImage",
        fill.equals("#A73B3A") || fill.equals("#9E3938")
            ? "chip_bet_red"
            : fill.equals("#23352E") || fill.equals("#1F302A")
                ? "chip_bet_black"
                : fill.equals("#357660") ? "chip_bet_green" : "chip_bet_outside");
    d.put("border", win ? "#F3D68F" : "#A7AC83");
    d.put("hasChip", amount > 0);
    // Tall cells reserve a centred label/marker stack. Nine-pixel Compact rows
    // place the marker beside a concise label, with both inside the felt border.
    int chipSize = c ? 5 : h == 18 ? 7 : 9;
    boolean inline = h == 9;
    int gap = c ? 2 : h == 18 ? 0 : 3;
    int blockY = y + (h - 9 - gap - chipSize) / 2;
    d.put("textX", inline ? x + 1 : x);
    d.put("textW", inline ? w - chipSize - 5 : w);
    d.put("textY", inline ? y : blockY);
    d.put("chipX", inline ? x + w - chipSize - 2 : x + (w - chipSize) / 2);
    d.put("chipY", inline ? y + (h - chipSize) / 2 : blockY + 9 + gap);
    d.put("chipSize", chipSize);
    d.put(
        "tooltip",
        RouletteGame.label(key)
            + " / pays "
            + (RouletteGame.multiplier(key) - 1)
            + ":1 / placed "
            + amount
            + " credits");
    return d;
  }

  public static Canvas render(RouletteGame s) {
    try (var in = RouletteView.class.getResourceAsStream("/ui/roulette.html")) {
      return MenuTemplate.parse(
              new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8),
              new gg.kembel.dui.core.GlyphFont(),
              gg.kembel.dui.components.VisualComponents.registry(),
              "demo template")
          .render(data(s), RouletteArt.images());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
