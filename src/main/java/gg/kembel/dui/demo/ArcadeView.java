package gg.kembel.dui.demo;

import java.util.*;

/** Data projection only; geometry and visual hierarchy are authored in the HTML templates. */
final class ArcadeView {
  private ArcadeView() {}

  static Map<String, Object> data(ArcadeMenu.Game game, ArcadeGame s) {
    boolean c = s.compact;
    var m = new LinkedHashMap<String, Object>();
    m.put("compact", c);
    m.put("wide", !c);
    m.put("width", c ? 320 : 480);
    m.put("height", c ? 171 : 288);
    m.put("startedAt", s.startedAt);
    m.put("motion", s.motion);
    m.put("title", game.title);
    m.put("busy", s.pending);
    m.put("help", s.help);
    m.put("playground", !s.help);
    m.put("credits", "CREDITS " + s.credits);
    m.put("wager", "BET " + s.wager());
    m.put("returned", s.returned > 0 ? "RETURN +" + s.returned : "ROUND " + s.rounds);
    m.put("status", s.message);
    m.put("motionLabel", s.motion ? "Motion" : "Still");
    m.put("sizeLabel", c ? "Wide" : "Small");
    m.put("helpLabel", s.help ? "Back" : "Rules");
    m.put(
        "playLabel",
        s.pending
            ? "In motion..."
            : s.freeRound()
                ? "FREE SPIN"
                : switch (game) {
                  case HORSES -> "START RACE";
                  case WHEEL -> "SPIN THE WHEEL";
                  case COIN -> "FLIP COIN";
                  case BOOK -> "SPIN / 10 LINES";
                });
    m.put("playLocked", s.pending || !s.freeRound() && s.credits < s.wager());
    m.put("betLocked", s.pending || s.freeRound());
    m.put("lessLocked", s.pending || s.freeRound() || s.betIndex == 0);
    m.put("moreLocked", s.pending || s.freeRound() || s.betIndex == ArcadeGame.BETS.length - 1);
    m.put("duration", s.pending ? s.duration() : 0);
    m.put("b", s.duration() | (s.pending ? 512 : 0));
    String[] theme =
        switch (game) {
          case HORSES -> new String[] {"#101D26", "#20343C", "#5AF2AF", "#D8E7E8"};
          case WHEEL -> new String[] {"#19162E", "#322746", "#FFCE66", "#F2EBFD"};
          case COIN -> new String[] {"#211D19", "#3B3025", "#F9D486", "#F2EBDA"};
          case BOOK -> new String[] {"#101D29", "#263544", "#E8C779", "#F4EBD8"};
        };
    m.put("background", theme[0]);
    m.put("panel", theme[1]);
    m.put("accent", theme[2]);
    m.put("ink", theme[3]);
    m.put(
        "subtitle",
        switch (game) {
          case HORSES -> "FOUR RUNNERS / ONE FINISH LINE";
          case WHEEL -> "EIGHT SLICES / BIG COLOUR ENERGY";
          case COIN -> "TWO SIDES / ONE GOLDEN MOMENT";
          case BOOK -> "FIVE REELS / A NEW ADVENTURE";
        });
    m.put("rules", rules(game));
    if (s instanceof HorseRaceGame race) {
      m.put("a", race.winner | (race.seed << 2) | (race.selected << 10));
      m.put("b", race.duration() | (race.pending ? 512 : 0) | (race.rounds > 0 ? 1024 : 0));
      var runners = new ArrayList<Map<String, Object>>();
      String[] colours = {"#5AF2AF", "#FFC047", "#59B5FF", "#FF69B5"};
      for (int i = 0; i < 4; i++)
        runners.add(
            Map.of(
                "id",
                i,
                "x",
                (c ? 8 : 12) + i * (c ? 76 : 114),
                "width",
                c ? 74 : 110,
                "label",
                c
                    ? (i + 1) + " / " + HorseRaceGame.RETURNS[i] + "x"
                    : HorseRaceGame.NAMES[i] + " " + HorseRaceGame.RETURNS[i] + "x",
                "detail",
                HorseRaceGame.RETURNS[i] + "x RETURN",
                "colour",
                colours[i],
                "active",
                race.selected == i,
                "tooltip",
                HorseRaceGame.NAMES[i] + " / " + HorseRaceGame.RETURNS[i] + "x total return"));
      m.put("runners", runners);
    } else if (s instanceof PrizeWheelGame wheel) {
      m.put("a", wheel.sector | (wheel.previous << 3));
      m.put(
          "outcome",
          wheel.pending
              ? "SPINNING"
              : wheel.rounds == 0
                  ? "MAKE IT SPIN"
                  : PrizeWheelGame.RETURNS[wheel.sector] + "x RETURN");
      var prizes = new ArrayList<Map<String, Object>>();
      String[] colours = {
        "#55457D", "#34B3BF", "#EE5994", "#38416B", "#8B5FE3", "#26A887", "#FF9E38", "#FFD157"
      };
      for (int i = 0; i < 8; i++)
        prizes.add(
            Map.of(
                "id",
                i,
                "x",
                (c ? 180 : 252) + (i % 2) * (c ? 64 : 104),
                "y",
                (c ? 45 : 81) + (i / 2) * (c ? 18 : 27),
                "width",
                c ? 58 : 96,
                "label",
                PrizeWheelGame.RETURNS[i] + "x",
                "colour",
                colours[i]));
      m.put("prizes", prizes);
    } else if (s instanceof CoinflipGame coin) {
      m.put("a", coin.face);
      m.put("heads", coin.selected == 0);
      m.put("tails", coin.selected == 1);
      m.put(
          "outcome",
          coin.pending
              ? "IN THE AIR"
              : coin.rounds == 0
                  ? "CALL YOUR SIDE"
                  : coin.face == 0 ? "HEADS / SUN" : "TAILS / MOON");
    } else if (s instanceof TempleSlotsGame book) {
      var reels = new ArrayList<Map<String, Object>>();
      for (int i = 0; i < 5; i++) {
        int a = i << 12, b = 90 + i * 10 | (book.pending ? 512 : 0) | (book.expanded[i] ? 1024 : 0);
        for (int row = 0; row < 3; row++)
          a |= (book.expanded[i] ? book.expanding : book.grid[i][row]) << (row * 4);
        if (!book.pending)
          for (int line : book.winningLines) b |= 1 << (11 + TempleSlotsGame.LINES[line][i]);
        reels.add(
            Map.of(
                "id",
                i,
                "x",
                (c ? 10 : 18) + i * (c ? 60 : 90),
                "width",
                c ? 56 : 84,
                "a",
                a,
                "b",
                b));
      }
      m.put("reels", reels);
      m.put(
          "bonus",
          book.freeSpins > 0
              ? "FREE SPINS " + book.freeSpins + " / " + TempleSlotsGame.SYMBOLS[book.expanding]
              : "BOOK = WILD + SCATTER");
      m.put("hasLines", !book.pending && !book.winningLines.isEmpty());
    }
    return m;
  }

  private static List<Map<String, Object>> rules(ArcadeMenu.Game game) {
    List<String> lines =
        switch (game) {
          case HORSES ->
              List.of(
                  "Choose a horse, then start the race.",
                  "Comet: 40% chance / 2x return",
                  "Gold: 30% / 3x; Blue: 20% / 4x",
                  "Thunder: 10% chance / 8x return",
                  "Return includes your original wager.",
                  "Demo credits. No inventory or real money.");
          case WHEEL ->
              List.of(
                  "Each of eight slices is equally likely.",
                  "Slices: 0x, 1x, 2x, 0x, 3x, 1x, 5x, 10x",
                  "Return = wager x landed multiplier.",
                  "The wheel is generous in this playground.",
                  "Animation shows the server-selected result.",
                  "Demo credits. No inventory or real money.");
          case COIN ->
              List.of(
                  "Call Heads (sun) or Tails (moon).",
                  "Both sides have a 50% chance.",
                  "Correct call returns 2x your wager.",
                  "Wrong call returns zero.",
                  "The result is selected before the animation.",
                  "Demo credits. No inventory or real money.");
          case BOOK ->
              List.of(
                  "10 lines / total wager split across all 10.",
                  "Match 3+ from the left. Book is wild.",
                  "A/K/Q/J/10: 5 / 10 / 25 per line bet",
                  "Lotus: 10/40/100; Scarab: 15/60/150",
                  "Explorer: 20/100/300; Book: 20/100/500",
                  "3/4/5+ Books anywhere: 2/10/50x total",
                  "3+ Books: +10 free spins, also retriggers.",
                  "Bonus symbol in 3+ reels expands & pays.");
        };
    var result = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < lines.size(); i++)
      result.add(
          Map.of("id", i, "text", lines.get(i), "wideY", 63 + i * 18, "compactY", 36 + i * 9));
    return result;
  }
}
