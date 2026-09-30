package gg.kembel.dui.demo;

import java.util.*;
import java.util.random.RandomGenerator;

/** Six-deck S17 blackjack. Money is integer half-credits, including exact 3:2 returns. */
public final class BlackjackGame {
  public enum Phase {
    BETTING,
    DEALING,
    PLAYER,
    REVEAL,
    DEALER,
    RESULT
  }

  public static final int[] BETS = {5, 25, 100, 500};

  public static final class Hand {
    public final List<Integer> cards = new ArrayList<>();
    public int stakeHalf;
    public boolean split, aces, done, doubled;
    public String outcome = "";
    public int returnedHalf;

    Hand(int stake) {
      stakeHalf = stake;
    }

    public int total() {
      return score(cards);
    }

    public boolean natural() {
      return !split && cards.size() == 2 && total() == 21;
    }
  }

  public Phase phase = Phase.BETTING;
  public int balanceHalf = 10000, active, rounds, returnedHalf, netHalf;
  public final List<Hand> hands = new ArrayList<>();
  private final List<Integer> dealer = new ArrayList<>();
  private final List<Integer> shoe = new ArrayList<>();
  private int cursor;
  private boolean revealed, scripted;

  public static int rank(int card) {
    return card % 13 + 2;
  }

  public static int score(List<Integer> cards) {
    int value = 0, aces = 0;
    for (int card : cards) {
      int rank = rank(card);
      value += rank == 14 ? 11 : Math.min(rank, 10);
      if (rank == 14) aces++;
    }
    while (value > 21 && aces-- > 0) value -= 10;
    return value;
  }

  public static boolean soft(List<Integer> cards) {
    int low = cards.stream().mapToInt(c -> rank(c) == 14 ? 1 : Math.min(rank(c), 10)).sum();
    return score(cards) != low;
  }

  public static String credits(int half) {
    return (half < 0 ? "-" : "") + Math.abs(half) / 2 + (half % 2 == 0 ? "" : ".5");
  }

  public boolean canDeal(int bet) {
    return (phase == Phase.BETTING || phase == Phase.RESULT)
        && balanceHalf >= bet * 2
        && Arrays.stream(BETS).anyMatch(v -> v == bet);
  }

  public Hand current() {
    return hands.get(active);
  }

  public boolean canDouble() {
    return phase == Phase.PLAYER
        && current().cards.size() == 2
        && !current().aces
        && balanceHalf >= current().stakeHalf;
  }

  public boolean canSplit() {
    return phase == Phase.PLAYER
        && hands.size() < 4
        && current().cards.size() == 2
        && !current().aces
        && rank(current().cards.get(0)) == rank(current().cards.get(1))
        && balanceHalf >= current().stakeHalf;
  }

  public List<Integer> visibleDealer() {
    return dealer.isEmpty()
        ? List.of()
        : revealed ? List.copyOf(dealer) : List.of(dealer.getFirst(), -1);
  }

  public int dealerTotal() {
    return revealed ? score(dealer) : dealer.isEmpty() ? 0 : score(List.of(dealer.getFirst()));
  }

  public boolean dealerSoft() {
    return revealed && soft(dealer);
  }

  public boolean revealed() {
    return revealed;
  }

  public int remaining() {
    return shoe.size() - cursor;
  }

  public void deal(int bet, RandomGenerator rng) {
    deal(bet, rng, List.of());
  }

  void deal(int bet, RandomGenerator rng, List<Integer> prefix) {
    if (!canDeal(bet))
      throw new IllegalArgumentException("Choose an affordable bet between rounds");
    if (!prefix.isEmpty() || scripted || remaining() < 80) shuffle(rng, prefix);
    scripted = !prefix.isEmpty();
    hands.clear();
    dealer.clear();
    active = 0;
    revealed = false;
    returnedHalf = netHalf = 0;
    var h = new Hand(bet * 2);
    hands.add(h);
    balanceHalf -= h.stakeHalf;
    h.cards.add(draw());
    dealer.add(draw());
    h.cards.add(draw());
    dealer.add(draw());
    phase = Phase.DEALING;
  }

  private void shuffle(RandomGenerator rng, List<Integer> prefix) {
    var next = new ArrayList<Integer>();
    for (int i = 0; i < 6; i++) for (int card = 0; card < 52; card++) next.add(card);
    for (int card : prefix)
      if (card < 0 || card >= 52 || !next.remove(Integer.valueOf(card)))
        throw new IllegalArgumentException("Invalid scripted shoe");
    for (int i = next.size() - 1; i > 0; i--) Collections.swap(next, i, rng.nextInt(i + 1));
    shoe.clear();
    shoe.addAll(prefix);
    shoe.addAll(next);
    cursor = 0;
  }

  private int draw() {
    if (cursor >= shoe.size()) throw new IllegalStateException("Shoe exhausted");
    return shoe.get(cursor++);
  }

  public void finishDeal() {
    if (phase != Phase.DEALING) return;
    phase = current().natural() || score(dealer) == 21 ? Phase.REVEAL : Phase.PLAYER;
  }

  public void hit() {
    requirePlayer();
    current().cards.add(draw());
    if (current().total() >= 21) {
      current().done = true;
      advance();
    }
  }

  public void stand() {
    requirePlayer();
    current().done = true;
    advance();
  }

  public void doubleDown() {
    if (!canDouble()) throw new IllegalArgumentException("Double unavailable");
    var h = current();
    balanceHalf -= h.stakeHalf;
    h.stakeHalf *= 2;
    h.doubled = true;
    h.cards.add(draw());
    h.done = true;
    advance();
  }

  public void split() {
    if (!canSplit()) throw new IllegalArgumentException("Split unavailable");
    var h = current();
    var other = new Hand(h.stakeHalf);
    balanceHalf -= h.stakeHalf;
    other.cards.add(h.cards.removeLast());
    h.split = other.split = true;
    h.aces = other.aces = rank(h.cards.getFirst()) == 14;
    hands.add(active + 1, other);
    h.cards.add(draw());
    other.cards.add(draw());
    h.done = h.aces || h.total() == 21;
    other.done = other.aces || other.total() == 21;
    if (h.done) advance();
  }

  private void requirePlayer() {
    if (phase != Phase.PLAYER || current().done)
      throw new IllegalArgumentException("Wait for your turn");
  }

  private void advance() {
    while (active < hands.size() && hands.get(active).done) active++;
    if (active == hands.size()) {
      active = hands.size() - 1;
      phase = Phase.REVEAL;
    }
  }

  public void reveal() {
    if (phase == Phase.REVEAL) {
      revealed = true;
      phase = Phase.DEALER;
    }
  }

  /** One dealer card or one atomic settlement. Dealer stands on every 17. */
  public boolean dealerStep() {
    if (phase != Phase.DEALER) return false;
    boolean needsDealer = hands.stream().anyMatch(h -> h.total() <= 21 && !h.natural());
    if (needsDealer && score(dealer) < 17) {
      dealer.add(draw());
      return true;
    }
    int total = score(dealer);
    boolean natural = dealer.size() == 2 && total == 21;
    int reserved = 0;
    for (var h : hands) {
      reserved += h.stakeHalf;
      if (h.total() > 21) h.outcome = "BUST";
      else if (natural) {
        if (h.natural()) {
          h.returnedHalf = h.stakeHalf;
          h.outcome = "PUSH";
        } else h.outcome = "DEALER BLACKJACK";
      } else if (h.natural()) {
        h.returnedHalf = h.stakeHalf * 5 / 2;
        h.outcome = "BLACKJACK";
      } else if (total > 21 || h.total() > total) {
        h.returnedHalf = h.stakeHalf * 2;
        h.outcome = "WIN";
      } else if (h.total() == total) {
        h.returnedHalf = h.stakeHalf;
        h.outcome = "PUSH";
      } else h.outcome = "DEALER WINS";
      returnedHalf += h.returnedHalf;
    }
    balanceHalf += returnedHalf;
    netHalf = returnedHalf - reserved;
    phase = Phase.RESULT;
    rounds++;
    return false;
  }
}
