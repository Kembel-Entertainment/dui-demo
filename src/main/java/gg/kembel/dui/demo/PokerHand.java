package gg.kembel.dui.demo;

import java.util.*;

/** Best five of up to seven cards. IDs: suit * 13 + rank - 2; C, D, H, S. */
public final class PokerHand {
  private PokerHand() {}

  public record Result(long score, int category, int high, List<Integer> cards)
      implements Comparable<Result> {
    @Override
    public int compareTo(Result other) {
      return Long.compare(score, other.score);
    }

    public String name() {
      return category == 8 && high == 14
          ? "Royal flush"
          : switch (category) {
            case 8 -> "Straight flush";
            case 7 -> "Four of a kind";
            case 6 -> "Full house";
            case 5 -> "Flush";
            case 4 -> "Straight";
            case 3 -> "Three of a kind";
            case 2 -> "Two pair";
            case 1 -> "One pair";
            default -> "High card";
          };
    }
  }

  public static int card(int rank, int suit) {
    if (rank < 2 || rank > 14 || suit < 0 || suit > 3)
      throw new IllegalArgumentException("Invalid card");
    return suit * 13 + rank - 2;
  }

  public static int rank(int card) {
    return card % 13 + 2;
  }

  public static int suit(int card) {
    return card / 13;
  }

  public static String label(int card) {
    int r = rank(card);
    return (r < 10
            ? "" + r
            : switch (r) {
              case 10 -> "10";
              case 11 -> "J";
              case 12 -> "Q";
              case 13 -> "K";
              default -> "A";
            })
        + "CDHS".charAt(suit(card));
  }

  public static Result evaluate(List<Integer> cards) {
    if (cards.size() < 5
        || cards.size() > 7
        || new HashSet<>(cards).size() != cards.size()
        || cards.stream().anyMatch(c -> c < 0 || c >= 52))
      throw new IllegalArgumentException("Expected 5..7 unique cards");
    Result best = null;
    int n = cards.size();
    for (int a = 0; a < n - 4; a++)
      for (int b = a + 1; b < n - 3; b++)
        for (int c = b + 1; c < n - 2; c++)
          for (int d = c + 1; d < n - 1; d++)
            for (int e = d + 1; e < n; e++) {
              var value =
                  five(
                      List.of(
                          cards.get(a), cards.get(b), cards.get(c), cards.get(d), cards.get(e)));
              if (best == null || value.compareTo(best) > 0) best = value;
            }
    return best;
  }

  private static Result five(List<Integer> cards) {
    int[] counts = new int[15];
    for (int c : cards) counts[rank(c)]++;
    var ranks = new ArrayList<Integer>();
    for (int r = 14; r >= 2; r--) if (counts[r] > 0) ranks.add(r);
    boolean flush = cards.stream().map(PokerHand::suit).distinct().count() == 1;
    int straight = 0;
    if (ranks.size() == 5) {
      if (ranks.getFirst() - ranks.getLast() == 4) straight = ranks.getFirst();
      else if (ranks.equals(List.of(14, 5, 4, 3, 2))) straight = 5;
    }
    var groups = new ArrayList<>(ranks);
    groups.sort(
        Comparator.<Integer>comparingInt(r -> counts[r])
            .reversed()
            .thenComparing(Comparator.reverseOrder()));
    int cat;
    List<Integer> keys;
    if (flush && straight > 0) {
      cat = 8;
      keys = List.of(straight);
    } else if (counts[groups.get(0)] == 4) {
      cat = 7;
      keys = groups;
    } else if (counts[groups.get(0)] == 3 && counts[groups.get(1)] == 2) {
      cat = 6;
      keys = groups;
    } else if (flush) {
      cat = 5;
      keys = ranks;
    } else if (straight > 0) {
      cat = 4;
      keys = List.of(straight);
    } else if (counts[groups.get(0)] == 3) {
      cat = 3;
      keys = groups;
    } else if (counts[groups.get(0)] == 2 && counts[groups.get(1)] == 2) {
      cat = 2;
      keys = groups;
    } else if (counts[groups.get(0)] == 2) {
      cat = 1;
      keys = groups;
    } else {
      cat = 0;
      keys = ranks;
    }
    long score = cat;
    for (int i = 0; i < 5; i++) score = score * 15 + (i < keys.size() ? keys.get(i) : 0);
    return new Result(score, cat, keys.getFirst(), List.copyOf(cards));
  }
}
