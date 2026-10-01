package gg.kembel.dui.demo;

import java.util.random.RandomGenerator;

final class PrizeWheelGame extends ArcadeGame {
  static final int[] RETURNS = {0, 1, 2, 0, 3, 1, 5, 10};
  int sector, previous;

  int duration() {
    return 140;
  }

  void draw(RandomGenerator random) {
    previous = sector;
    sector = random.nextInt(8);
  }

  long payout(int wager) {
    return (long) wager * RETURNS[sector];
  }

  String resultLabel() {
    return "Wheel lands on " + RETURNS[sector] + "x";
  }
}
