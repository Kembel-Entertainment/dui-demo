package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Declarative public bindings. Hole card and shoe never enter the canvas or transport. */
public final class BlackjackView {
  private BlackjackView() {}

  public static Map<String, Object> data(BlackjackState s) {
    boolean c = s.compact;
    var g = s.game;
    int w = c ? 320 : 480;
    var d = new HashMap<String, Object>();
    d.put("width", w);
    d.put("height", c ? 153 : 324);
    d.put("art", c ? "compact" : "wide");
    d.put("pixel", c ? 2 : 4);
    d.put("compact", c);
    d.put("wide", !c);
    d.put("tick", s.startedAt);
    d.put("motion", s.motion);
    d.put("motionX", w - 118);
    d.put("sizeX", w - 60);
    d.put("motionLabel", s.motion ? "Motion" : "Still");
    d.put("sizeLabel", c ? "Wide" : "Compact");
    d.put("inner", w - 32);
    d.put("balance", "CREDITS " + BlackjackGame.credits(g.balanceHalf));
    d.put("balanceX", c ? 8 : 154);
    d.put("balanceY", c ? 9 : 18);
    d.put("balanceW", c ? 186 : 200);
    d.put("dealerY", c ? 18 : 45);
    d.put("heroY", c ? 63 : 153);
    d.put("statusY", c ? 117 : 270);
    d.put("tabsY", c ? 108 : 243);
    d.put("controlY", c ? 135 : 288);
    d.put("controlH", c ? 18 : 27);
    d.put("controlW", c ? 67 : 107);
    d.put("betLocked", s.busy());
    String dealer =
        "DEALER"
            + (g.hands.isEmpty()
                ? ""
                : " / "
                    + g.dealerTotal()
                    + (g.revealed() ? g.dealerSoft() ? " SOFT" : "" : " + ?"));
    d.put("dealerLabel", dealer);
    d.put(
        "heroLabel",
        g.hands.isEmpty()
            ? "YOUR SEAT / THE NEXT HAND IS YOURS"
            : "HAND "
                + (s.focus + 1)
                + " / "
                + g.hands.get(s.focus).total()
                + (BlackjackGame.soft(g.hands.get(s.focus).cards) ? " SOFT" : "")
                + " / BET "
                + BlackjackGame.credits(g.hands.get(s.focus).stakeHalf));
    String status =
        s.busy()
            ? switch (s.event) {
              case "deal" -> "CARDS IN MOTION";
              case "split" -> "TWO HANDS. TWO CHANCES.";
              case "reveal" -> "THE REVEAL";
              case "dealer" -> "DEALER DRAWS";
              case "payout" -> "SETTLING THE TABLE";
              default -> "ONE MORE CARD";
            }
            : switch (g.phase) {
              case BETTING -> "TAKE A SEAT. CHOOSE YOUR STAKE.";
              case PLAYER -> "YOUR MOVE / HIT, STAND, DOUBLE OR SPLIT";
              case RESULT ->
                  "NET "
                      + (g.netHalf >= 0 ? "+" : "")
                      + BlackjackGame.credits(g.netHalf)
                      + " / "
                      + g.hands.get(s.focus).outcome;
              default -> "TWENTY ONE";
            };
    d.put("status", status);
    d.put(
        "statusColor",
        g.phase == BlackjackGame.Phase.RESULT
            ? (g.netHalf > 0 ? "#256A57" : g.netHalf < 0 ? "#9D4842" : "#765E36")
            : "#5E5340");
    boolean between =
        g.phase == BlackjackGame.Phase.BETTING || g.phase == BlackjackGame.Phase.RESULT;
    d.put("between", between);
    d.put("playing", !between);
    d.put("dealLocked", s.busy() || !g.canDeal(s.bet));
    d.put("locked", s.busy() || g.phase != BlackjackGame.Phase.PLAYER);
    d.put("doubleLocked", s.busy() || !g.canDouble());
    d.put("splitLocked", s.busy() || !g.canSplit());
    d.put("betLabel", "BET " + s.bet);
    d.put("betY", c ? 126 : 279);
    d.put("previewLabel", c ? "Demo" : "Demo deal");
    d.put(
        "demoTooltip",
        "Explicit scripted preview: "
            + new String[] {
                  "Split eights",
                  "Natural blackjack",
                  "Soft seventeen",
                  "Seventeen push",
                  "Hit or hold"
                }
                [s.preview % 5]
            + ". Ordinary Deal uses a shuffled shoe.");
    var chips = new ArrayList<Map<String, Object>>();
    String[] colors = {"#518F8D", "#B46760", "#627CAB", "#8671A0"};
    for (int i = 0; i < 4; i++)
      chips.add(
          Map.of(
              "n",
              BlackjackGame.BETS[i],
              "x",
              16 + i * (c ? 39 : 46),
              "w",
              c ? 36 : 40,
              "fill",
              s.bet == BlackjackGame.BETS[i] ? "#D5B776" : colors[i],
              "color",
              s.bet == BlackjackGame.BETS[i] ? "#192D3C" : "#FFF0D4"));
    d.put("chips", chips);
    d.put("dealX", c ? 178 : 300);
    d.put("dealW", c ? 60 : 78);
    d.put("demoX", c ? 242 : 386);
    d.put("demoW", c ? 62 : 78);
    var tabs = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < g.hands.size(); i++) {
      var h = g.hands.get(i);
      tabs.add(
          Map.of(
              "i",
              i,
              "x",
              16 + i * (c ? 73 : 112),
              "w",
              c ? 70 : 108,
              "label",
              "H" + (i + 1) + " / " + h.total() + (h.done ? " *" : ""),
              "fill",
              i == s.focus ? "#D8B976" : "#203D50",
              "color",
              i == s.focus ? "#142C3D" : "#C5C4B1",
              "locked",
              s.busy() || g.phase == BlackjackGame.Phase.PLAYER));
    }
    d.put("tabs", tabs);
    var cards = new ArrayList<Map<String, Object>>();
    var dealerCards = g.visibleDealer();
    int dp = Math.min(s.dealerPage, Math.max(0, dealerCards.size() - 3));
    addCards(cards, s, dealerCards, dp, 3, c ? 27 : 54, true, 0);
    if (!g.hands.isEmpty()) {
      if (s.event.equals("split")) {
        addCards(cards, s, g.hands.get(s.splitFocus).cards, 0, 2, c ? 72 : 174, false, 1);
        addCards(cards, s, g.hands.get(s.splitFocus + 1).cards, 0, 2, c ? 72 : 174, false, 2);
      } else {
        var hc = g.hands.get(s.focus).cards;
        int hp = Math.min(s.heroPage, Math.max(0, hc.size() - 4));
        addCards(cards, s, hc, hp, 4, c ? 72 : 174, false, 0);
      }
    }
    d.put("cards", cards);
    d.put("payout", s.event.equals("payout"));
    d.put("chipCount", Math.min(7, Math.max(1, Math.abs(g.netHalf) / 50)));
    d.put("chipPalette", g.netHalf < 0 ? "coral" : "gold");
    d.put("chipFrom", g.netHalf < 0 ? "bottom" : "top");
    d.put("chipTo", g.netHalf < 0 ? "top" : "bottom");
    d.put("chipX", c ? 85 : 130);
    d.put("chipY", c ? 45 : 81);
    d.put("chipW", c ? 150 : 220);
    d.put("chipH", c ? 60 : 153);
    var pages = new ArrayList<Map<String, Object>>();
    if (dealerCards.size() > 3) {
      pages.add(page("dealer", -1, 16, c ? 36 : 81, dp == 0));
      pages.add(page("dealer", 1, w - 40, c ? 36 : 81, dp + 3 >= dealerCards.size()));
    }
    if (!g.hands.isEmpty() && g.hands.get(s.focus).cards.size() > 4 && !s.event.equals("split")) {
      pages.add(page("hero", -1, 16, c ? 81 : 198, s.heroPage == 0));
      pages.add(
          page(
              "hero",
              1,
              w - 40,
              c ? 81 : 198,
              s.heroPage + 4 >= g.hands.get(s.focus).cards.size()));
    }
    d.put("pages", pages);
    return d;
  }

  private static Map<String, Object> page(String group, int dir, int x, int y, boolean locked) {
    return Map.of(
        "id",
        group + (dir < 0 ? "_prev" : "_next"),
        "group",
        group,
        "dir",
        dir,
        "label",
        dir < 0 ? "<" : ">",
        "x",
        x,
        "y",
        y,
        "locked",
        locked);
  }

  private static void addCards(
      List<Map<String, Object>> out,
      BlackjackState s,
      List<Integer> list,
      int offset,
      int limit,
      int y,
      boolean dealer,
      int side) {
    boolean c = s.compact;
    int width = c ? 24 : 40,
        height = c ? 36 : 60,
        gap = c ? 28 : 46,
        n = Math.min(limit, list.size() - offset),
        start = ((c ? 320 : 480) - n * gap + (gap - width)) / 2;
    if (side > 0) start = (side == 1 ? (c ? 68 : 125) : (c ? 196 : 263));
    for (int j = 0; j < n; j++) {
      int i = j + offset, x = start + j * gap;
      String mode = "static";
      int delay = 0;
      if (s.event.equals("deal")) {
        mode = "fly";
        delay = dealer ? 8 + i * 16 : i * 16;
      } else if ((s.event.equals("hit") && !dealer && i == list.size() - 1)
          || (s.event.equals("dealer") && dealer && i == list.size() - 1)) {
        mode = "fly";
      } else if (s.event.equals("split") && !dealer) {
        mode = "fly";
        delay = (side - 1) * 8 + j * 8;
      } else if (s.event.equals("reveal") && dealer && i == 1) mode = "flip";
      int ex = x, ey = y, ew = width, eh = height;
      int lift = 0;
      if (mode.equals("flip")) {
        lift = 6;
        ey -= lift;
        eh += lift;
      }
      if (mode.equals("fly")) {
        ey = c ? 20 : 44;
        ew = (c ? 300 : 452) - x;
        eh = y + height - ey;
      }
      var a = new HashMap<String, Object>();
      a.put("id", (dealer ? "dealer_" : "hero_" + side + "_") + i);
      a.put("x", ex);
      a.put("y", ey);
      a.put("w", ew);
      a.put("h", eh);
      a.put("faceH", c ? 30 : 54);
      a.put("lift", lift);
      a.put("value", list.get(i));
      a.put("down", list.get(i) < 0);
      a.put("active", !dealer && s.game.phase == BlackjackGame.Phase.PLAYER);
      a.put("mode", mode);
      a.put("delay", delay);
      out.add(a);
    }
  }

  public static Canvas render(BlackjackState s) {
    try (var in = BlackjackView.class.getResourceAsStream("/ui/blackjack.html")) {
      return MenuTemplate.parse(
              new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8))
          .render(data(s), BlackjackArt.images());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
