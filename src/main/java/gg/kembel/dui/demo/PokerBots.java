package gg.kembel.dui.demo;

import java.util.*;

/** Small stochastic opponents using only their own cards and the public board. */
public final class PokerBots {
  private PokerBots() {}

  public static void act(HoldemGame g, Random random, boolean showcase) {
    int s = g.actor;
    if (s <= 0) throw new IllegalArgumentException("Expected bot turn");
    if (showcase) {
      g.act(s, HoldemGame.Move.CHECK_CALL, 0);
      return;
    }
    int a = g.hole(s, 0), b = g.hole(s, 1), ra = PokerHand.rank(a), rb = PokerHand.rank(b);
    double strength =
        (ra + rb - 4) / 24.0 * .5
            + (ra == rb ? .32 : 0)
            + (PokerHand.suit(a) == PokerHand.suit(b) ? .10 : 0)
            + (Math.abs(ra - rb) <= 2 ? .07 : 0);
    if (g.board.size() >= 3) {
      var visible = new ArrayList<>(g.board);
      visible.add(a);
      visible.add(b);
      var hand = PokerHand.evaluate(visible);
      strength = .18 + hand.category() * .13 + hand.high() / 100.0;
    }
    double cost = (double) g.owed(s) / Math.max(1, g.pot() + g.owed(s));
    double temperament = s == 1 ? .12 : s == 2 ? -.08 : .02;
    if (g.owed(s) > 0 && strength + temperament + random.nextDouble() * .25 < cost + .17) {
      g.act(s, HoldemGame.Move.FOLD, 0);
      return;
    }
    if (g.canRaise(s)
        && random.nextDouble() < Math.min(.42, Math.max(.07, strength * .32 + temperament))) {
      int target =
          Math.min(
              g.maximumRaise(s),
              Math.max(g.minimumRaise(), g.currentBet + Math.max(20, g.pot() / 2)));
      g.act(s, HoldemGame.Move.RAISE, target);
    } else g.act(s, HoldemGame.Move.CHECK_CALL, 0);
  }
}
