package gg.kembel.dui.demo;

import java.util.random.RandomGenerator;

final class CoinflipGame extends ArcadeGame {
  int selected, face;

  int duration() {
    return 100;
  }

  void select(int side) {
    if (side < 0 || side > 1) throw new IllegalArgumentException("Coin side");
    if (!pending) selected = side;
  }

  void draw(RandomGenerator random) {
    face = random.nextInt(2);
  }

  long payout(int wager) {
    return face == selected ? (long) wager * 2 : 0;
  }

  String resultLabel() {
    return face == 0 ? "HEADS / the sun" : "TAILS / the moon";
  }
}
