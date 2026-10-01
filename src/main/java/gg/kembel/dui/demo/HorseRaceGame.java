package gg.kembel.dui.demo;

import java.util.random.RandomGenerator;

final class HorseRaceGame extends ArcadeGame {
  static final String[] NAMES = {"NEON COMET", "GOLD RUSH", "BLUE HOUR", "PINK THUNDER"};
  static final int[] RETURNS = {2, 3, 4, 8};
  int selected, winner;

  int duration() {
    return 180;
  }

  void select(int horse) {
    if (horse < 0 || horse >= 4) throw new IllegalArgumentException("Horse");
    if (!pending) selected = horse;
  }

  void draw(RandomGenerator random) {
    int ticket = random.nextInt(10);
    winner = ticket < 4 ? 0 : ticket < 7 ? 1 : ticket < 9 ? 2 : 3;
  }

  long payout(int wager) {
    return selected == winner ? (long) wager * RETURNS[winner] : 0;
  }

  String resultLabel() {
    return NAMES[winner] + " wins";
  }
}
