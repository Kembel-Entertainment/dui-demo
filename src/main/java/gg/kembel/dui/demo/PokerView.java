package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Public geometry and bindings. Opponent hole IDs never enter the template before showdown. */
public final class PokerView {
  private PokerView() {}

  public static Map<String, Object> data(PokerState s) {
    var g = s.game;
    boolean c = s.compact, active = g.playing(), hero = active && g.actor == 0 && !s.busy();
    int width = c ? 320 : 480, height = c ? 153 : 324;
    var d = new HashMap<String, Object>();
    d.put("width", width);
    d.put("height", height);
    d.put("compact", c);
    d.put("wide", !c);
    d.put("motion", s.motion);
    d.put("tick", s.startedAt);
    d.put("pixel", c ? 3 : 4);
    d.put("inner", width - 16);
    d.put("headerW", width - 24);
    d.put("headerH", c ? 18 : 36);
    d.put("sizeX", width - 62);
    d.put("motionX", width - (c ? 127 : 149));
    d.put("motionW", c ? 60 : 81);
    d.put("motionLabel", s.motion ? "Motion" : "Still");
    d.put("sizeLabel", c ? "Wide" : "Compact");
    d.put("lobby", g.street == HoldemGame.Street.LOBBY);
    d.put("playing", active);
    d.put("ended", g.ended());
    d.put("canDeal", !active && !s.busy());
    d.put("noDeal", active || s.busy() || Arrays.stream(g.stacks).filter(v -> v > 0).count() < 2);
    d.put("heroLocked", !hero);
    d.put("raiseLocked", !hero || !g.canRaise(0));
    d.put("allinLocked", !hero || (g.maximumRaise(0) > g.currentBet && !g.canRaise(0)));
    d.put("busy", s.busy());
    d.put("heroStack", g.stacks[0]);
    d.put("raise", s.raiseTarget);
    d.put("call", g.owed(0) == 0 ? "Check" : "Call " + Math.min(g.owed(0), g.stacks[0]));
    d.put(
        "status",
        g.ended()
            ? g.message
            : active
                ? (s.busy()
                    ? g.message
                    : g.actor == 0
                        ? "YOUR MOVE / " + g.message
                        : HoldemGame.names(g.actor) + " is thinking...")
                : "A little velvet. A little nerve.");
    d.put(
        "phase",
        s.showcase
            ? "SCRIPTED / " + g.street
            : g.street == HoldemGame.Street.LOBBY
                ? "NO LIMIT / DEMO CHIPS"
                : g.street.toString() + " / HAND " + g.handNumber);
    d.put("pot", g.ended() ? g.awardedPot : g.pot());
    d.put("potLabel", g.ended() ? "AWARDED" : "IN THE MIDDLE");
    d.put("statusY", c ? 117 : 207);
    d.put("statusX", c ? 6 : 18);
    d.put("statusW", width - (c ? 12 : 36));
    d.put("boardY", c ? 33 : 108);
    d.put("boardW", c ? 27 : 40);
    d.put("boardH", c ? 54 : 96);
    d.put("boardLift", c ? 12 : 24);
    d.put("heroY", c ? 71 : 213);
    d.put("heroW", c ? 28 : 42);
    d.put("heroH", c ? 48 : 81);
    d.put("heroLift", c ? 9 : 15);
    d.put("potY", c ? 27 : 90);
    d.put("phaseY", c ? 18 : 54);
    d.put("phaseX", c ? 84 : 16);
    d.put("controlsY", c ? 126 : 297);
    d.put("controlsW", c ? 72 : 108);
    d.put("controlsGap", c ? 5 : 9);
    d.put("controlsX", c ? 8 : 10);
    d.put("raiseY", c ? 144 : 279);
    d.put("raiseX", c ? 78 : 280);
    d.put("raiseLabelX", c ? 104 : 306);
    d.put("raiseLabelW", c ? 86 : 120);
    d.put("plusX", c ? 195 : 432);
    d.put("heroNameX", c ? 88 : 132);
    d.put("heroNameY", c ? 90 : 243);
    d.put("heroNameW", c ? 38 : 60);
    d.put("stackY", c ? 99 : 261);
    d.put("bestX", c ? 192 : 284);
    d.put("bestY", c ? 99 : 243);
    d.put("bestW", c ? 116 : 168);
    var best = g.hands.get(0);
    String bestName = best == null ? "" : best.name();
    if (best == null && g.board.size() >= 3 && !g.folded[0]) {
      var cards = new ArrayList<>(g.board);
      cards.add(g.hole(0, 0));
      cards.add(g.hole(0, 1));
      bestName = PokerHand.evaluate(cards).name();
    }
    d.put("best", g.folded[0] ? "Folded" : bestName);
    d.put("winner", g.payouts[0] > 0);
    d.put("playerLabel", g.payouts[0] > 0 ? "YOU WIN" : "YOU");
    d.put("lobbyY", c ? 54 : 126);
    d.put("lobbyTitleY", c ? 63 : 153);
    d.put("lobbySubY", c ? 81 : 180);
    d.put("lobbyTitle", c ? "VELVET HOLD'EM" : "THE VELVET TABLE");
    var seats = new ArrayList<Map<String, Object>>();
    int[][] pos =
        c
            ? new int[][] {{0, 0}, {5, 63}, {253, 18}, {253, 72}}
            : new int[][] {{0, 0}, {20, 126}, {168, 45}, {348, 126}};
    for (int i = 1; i < 4; i++) {
      var seat = new HashMap<String, Object>();
      int x = pos[i][0], y = pos[i][1];
      seat.put("seat", i);
      seat.put("x", x);
      seat.put("y", y);
      seat.put("w", c ? 62 : 112);
      seat.put("h", c ? 45 : 72);
      seat.put("portraitX", x + (c ? 2 : 9));
      seat.put("portraitY", y + (c ? 2 : 9));
      seat.put("portraitSize", c ? 12 : 18);
      seat.put("nameX", x + (c ? 17 : 33));
      seat.put("nameY", y + (c ? 0 : 9));
      seat.put("nameW", c ? 43 : 72);
      seat.put("stackY", y + (c ? 9 : 27));
      seat.put("holeX", x + (c ? 18 : 42));
      seat.put("holeY", y + (c ? 18 : 45));
      seat.put("holeW", c ? 28 : 28);
      seat.put("holeH", 18);
      seat.put("name", HoldemGame.names(i) + (i == g.dealer ? " / D" : ""));
      seat.put("stack", g.stacks[i]);
      seat.put(
          "bet",
          g.ended() && !g.folded[i] && g.board.size() == 5
              ? PokerHand.label(g.hole(i, 0)) + " / " + PokerHand.label(g.hole(i, 1))
              : g.folded[i]
                  ? "FOLD"
                  : g.stacks[i] == 0 && active
                      ? "ALL-IN"
                      : g.bets[i] > 0 ? "BET " + g.bets[i] : "");
      seat.put("betY", y + (c ? 36 : 63));
      seat.put("portrait", "portrait_" + i);
      seat.put("holes", "hole_" + i);
      seat.put("fill", g.folded[i] ? "#18222C" : "#172D32");
      seat.put("border", g.actor == i ? "#E9BE70" : g.payouts[i] > 0 ? "#7DE5C4" : "#506164");
      if (!c && i == 2) {
        seat.put("w", 144);
        seat.put("h", 45);
        seat.put("nameY", y);
        seat.put("stackY", y + 18);
        seat.put("holeX", x + 108);
        seat.put("holeY", y + 9);
        seat.put("betY", y + 36);
      }
      seat.put(
          "color", g.folded[i] ? "#7C8A88" : i == 1 ? "#C6A5EC" : i == 2 ? "#A5D5B1" : "#F5AAA6");
      seats.add(seat);
    }
    d.put("seats", seats);
    var cards = new ArrayList<Map<String, Object>>();
    Set<Integer> winning = new HashSet<>();
    if (g.ended() && g.payouts[0] > 0 && best != null) winning.addAll(best.cards());
    for (int i = 0; i < 5; i++) {
      int value = i < g.board.size() ? g.board.get(i) : -1;
      cards.add(
          card(
              "board_" + i,
              (c ? 84 : 132) + i * (c ? 30 : 44),
              c ? 33 : 108,
              c ? 27 : 40,
              c ? 54 : 96,
              c ? 12 : 24,
              value,
              false,
              winning.contains(value),
              s.event.equals("flip") && i >= s.newBoardFrom && value >= 0 ? "flip" : "static",
              i >= s.newBoardFrom ? (i - s.newBoardFrom) * 4 : 0));
    }
    for (int i = 0; i < 2; i++)
      cards.add(
          card(
              "hero_" + i,
              (c ? 130 : 192) + i * (c ? 31 : 46),
              c ? 71 : 213,
              c ? 28 : 42,
              c ? 48 : 81,
              c ? 9 : 15,
              g.street == HoldemGame.Street.LOBBY ? -1 : g.hole(0, i),
              g.folded[0],
              winning.contains(g.hole(0, i)),
              s.event.equals("deal") ? "deal" : "static",
              i * 4));
    d.put("cards", cards);
    d.put("hasCards", g.street != HoldemGame.Street.LOBBY);
    d.put(
        "chipCount",
        g.street == HoldemGame.Street.LOBBY
            ? 0
            : Math.min(7, Math.max(1, (g.ended() ? g.awardedPot : g.pot()) / 20)));
    d.put(
        "chipMode",
        s.event.isEmpty() || s.event.equals("deal") || s.event.equals("pause")
            ? "static"
            : "transfer");
    boolean transfer = !s.event.isEmpty() && !s.event.equals("deal") && !s.event.equals("pause");
    d.put("chipX", transfer ? (c ? 78 : 126) : (width / 2 - 15));
    d.put("chipY", transfer ? (c ? 27 : 87) : (c ? 37 : 99));
    d.put("chipW", transfer ? (c ? 164 : 228) : 30);
    d.put("chipH", transfer ? (c ? 87 : 189) : (c ? 12 : 18));
    d.put("chipFrom", s.chipFrom);
    d.put("chipTo", s.chipTo);
    d.put("refillX", c ? 243 : 18);
    d.put("refillY", c ? 144 : 279);
    d.put("refillW", c ? 68 : 108);
    d.put("dealX", c ? 10 : 48);
    d.put("dealW", c ? 145 : 180);
    d.put("showcaseX", c ? 165 : 252);
    d.put("showcaseW", c ? 145 : 180);
    return d;
  }

  private static Map<String, Object> card(
      String id,
      int x,
      int y,
      int w,
      int h,
      int lift,
      int value,
      boolean down,
      boolean active,
      String mode,
      int delay) {
    var d = new HashMap<String, Object>();
    d.put("id", id);
    d.put("x", x);
    d.put("y", y);
    d.put("w", w);
    d.put("h", h);
    d.put("lift", lift);
    d.put("value", value);
    d.put("down", down);
    d.put("active", active);
    d.put("mode", mode);
    d.put("delay", delay);
    return d;
  }

  public static Canvas render(PokerState s) {
    try (var in = PokerView.class.getResourceAsStream("/ui/poker.html")) {
      return DemoTemplates.parse(
              new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8),
              DemoTemplates.font(),
              DemoVisualComponents.registry(),
              "demo template")
          .render(data(s), PokerArt.images(s.game));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
