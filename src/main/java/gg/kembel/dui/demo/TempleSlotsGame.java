package gg.kembel.dui.demo;

import java.util.*;
import java.util.random.RandomGenerator;

/** Original ten-line, five-reel book adventure. All rules are consumer-owned. */
final class TempleSlotsGame extends ArcadeGame {
  static final String[] SYMBOLS = {"A", "K", "Q", "J", "10", "Lotus", "Scarab", "Explorer", "Book"};
  static final int[][] LINES = {
    {1, 1, 1, 1, 1}, {0, 0, 0, 0, 0}, {2, 2, 2, 2, 2}, {0, 1, 2, 1, 0}, {2, 1, 0, 1, 2},
    {0, 0, 1, 2, 2}, {2, 2, 1, 0, 0}, {1, 0, 0, 0, 1}, {1, 2, 2, 2, 1}, {0, 1, 1, 1, 0}
  };
  static final int[] WEIGHTS = {18, 18, 16, 16, 14, 8, 7, 4, 3};
  static final int[][] PAY = {
    {0, 0, 0, 5, 10, 25}, {0, 0, 0, 5, 10, 25}, {0, 0, 0, 5, 10, 25},
    {0, 0, 0, 5, 10, 25}, {0, 0, 0, 5, 10, 25}, {0, 0, 0, 10, 40, 100},
    {0, 0, 0, 15, 60, 150}, {0, 0, 0, 20, 100, 300}, {0, 0, 0, 20, 100, 500}
  };
  int[][] grid = {{0, 5, 1}, {2, 6, 3}, {4, 8, 0}, {1, 7, 2}, {3, 5, 4}};
  int freeSpins, expanding = -1, bonusWager, books;
  boolean bonusRound, awardedBonus;
  final List<Integer> winningLines = new ArrayList<>();
  boolean[] expanded = new boolean[5];

  int duration() {
    return 140;
  }

  @Override
  int wager() {
    return freeRound() && bonusWager > 0 ? bonusWager : super.wager();
  }

  boolean freeRound() {
    return freeSpins > 0;
  }

  void consumeRound() {
    if (bonusRound) freeSpins--;
  }

  void draw(RandomGenerator random) {
    bonusRound = freeRound();
    awardedBonus = false;
    books = 0;
    winningLines.clear();
    Arrays.fill(expanded, false);
    for (int reel = 0; reel < 5; reel++)
      for (int row = 0; row < 3; row++) {
        int ticket = random.nextInt(Arrays.stream(WEIGHTS).sum()), symbol = 0;
        while (ticket >= WEIGHTS[symbol]) ticket -= WEIGHTS[symbol++];
        grid[reel][row] = symbol;
        if (symbol == 8) books++;
      }
    if (bonusRound) {
      for (int reel = 0; reel < 5; reel++)
        for (int row = 0; row < 3; row++) if (grid[reel][row] == expanding) expanded[reel] = true;
      if (countExpanded() < 3) Arrays.fill(expanded, false);
    }
    if (books >= 3 && !bonusRound) {
      bonusWager = super.wager();
      expanding = random.nextInt(8);
    }
  }

  static int lineMultiplier(int[] symbols) {
    if (symbols.length != 5) throw new IllegalArgumentException("Five symbols per line");
    for (int symbol : symbols)
      if (symbol < 0 || symbol > 8) throw new IllegalArgumentException("Symbol");
    int best = 0;
    for (int symbol = 0; symbol < 9; symbol++) {
      int count = 0;
      for (int value : symbols) {
        if (value != symbol && (value != 8 || symbol == 8)) break;
        count++;
      }
      best = Math.max(best, PAY[symbol][count]);
    }
    return best;
  }

  int countExpanded() {
    int count = 0;
    for (boolean value : expanded) if (value) count++;
    return count;
  }

  void demoBonus() {
    if (pending) return;
    freeSpins = 10;
    expanding = 6;
    bonusWager = super.wager();
    help = false;
    message = "Bonus demo / Scarab expands / 10 free spins";
  }

  long payout(int wager) {
    winningLines.clear();
    long total = 0;
    for (int i = 0; i < LINES.length; i++) {
      int[] symbols = new int[5];
      for (int reel = 0; reel < 5; reel++) symbols[reel] = grid[reel][LINES[i][reel]];
      int multiplier = lineMultiplier(symbols);
      if (multiplier > 0) {
        total += (long) (wager / 10) * multiplier;
        winningLines.add(i);
      }
    }
    if (books >= 3) total += (long) wager * (books == 3 ? 2 : books == 4 ? 10 : 50);
    // Bonus expansion pays each of the ten lines, even across non-adjacent matching reels.
    if (countExpanded() >= 3) {
      total += (long) wager * PAY[expanding][countExpanded()];
      for (int i = 0; i < 10; i++) if (!winningLines.contains(i)) winningLines.add(i);
    }
    return total;
  }

  void settled() {
    if (books >= 3) {
      freeSpins += 10;
      awardedBonus = true;
    }
  }

  String resultLabel() {
    if (awardedBonus) return "+10 FREE SPINS / " + SYMBOLS[expanding] + " expands";
    if (bonusRound) return "Free spin / " + winningLines.size() + " winning lines";
    return winningLines.isEmpty()
        ? "The temple keeps its secrets"
        : winningLines.size() + " winning lines";
  }

  void resetFeature() {
    freeSpins = 0;
    expanding = -1;
    bonusWager = 0;
    books = 0;
    bonusRound = false;
    awardedBonus = false;
    winningLines.clear();
    Arrays.fill(expanded, false);
  }
}
