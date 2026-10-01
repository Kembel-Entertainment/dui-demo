package gg.kembel.dui.demo;

import java.util.*;
import java.util.random.RandomGenerator;

/** Transient demo round: reserve once, reveal once, refund nothing by resizing. */
abstract class ArcadeGame {
  static final int[] BETS = {10, 50, 100, 250};
  long credits = 5000, startedAt = -1, returned;
  int betIndex = 1, rounds, seed;
  boolean pending, motion = true, compact, help;
  String message = "Choose your wager. Make your move.";

  abstract int duration();

  abstract void draw(RandomGenerator random);

  abstract long payout(int wager);

  abstract String resultLabel();

  int wager() {
    return BETS[betIndex];
  }

  boolean freeRound() {
    return false;
  }

  void consumeRound() {}

  void settled() {}

  void resetFeature() {}

  boolean play(RandomGenerator random, long tick) {
    if (pending || !freeRound() && credits < wager()) return false;
    boolean free = freeRound();
    draw(random);
    seed = random.nextInt(256);
    if (!free) credits -= wager();
    consumeRound();
    pending = true;
    startedAt = tick;
    returned = 0;
    message = free ? "Free spin in motion..." : "Good luck. The result is on its way...";
    if (!motion) finish(tick, true);
    return true;
  }

  boolean finish(long tick, boolean immediate) {
    if (!pending || !immediate && tick - startedAt < duration()) return false;
    returned = payout(wager());
    credits = Math.addExact(credits, returned);
    pending = false;
    rounds++;
    settled();
    message = resultLabel() + (returned > 0 ? " / +" + returned + " credits" : " / Try again");
    return true;
  }

  void bet(int direction) {
    if (direction != -1 && direction != 1) throw new IllegalArgumentException("Bet direction");
    if (!pending && !freeRound()) betIndex = Math.clamp(betIndex + direction, 0, BETS.length - 1);
  }

  void toggleMotion(long tick) {
    motion = !motion;
    if (!motion) finish(tick, true);
  }

  void reset() {
    if (pending) return;
    credits = 5000;
    returned = 0;
    rounds = 0;
    startedAt = -1;
    message = "Fresh bankroll. Let's play.";
    resetFeature();
  }
}
