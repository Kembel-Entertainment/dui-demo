package gg.kembel.dui.demo;

import java.util.*;

/**
 * Server-authoritative, finite-stack no-limit Hold'em. No Bukkit, rendering, or money integration.
 */
public final class HoldemGame {
  public enum Street {
    LOBBY,
    PREFLOP,
    FLOP,
    TURN,
    RIVER,
    SHOWDOWN
  }

  public enum Move {
    FOLD,
    CHECK_CALL,
    RAISE,
    ALL_IN
  }

  public record Pot(int amount, List<Integer> eligible, List<Integer> winners) {}

  public final int[] stacks, bets = new int[4], contributed = new int[4], payouts = new int[4];
  public final boolean[] folded = new boolean[4];
  private final boolean[] pending = new boolean[4], acted = new boolean[4];
  private final int[] lastActedBet = new int[4];
  private final transient List<Integer> deck = new ArrayList<>();
  private final transient int[][] holes = new int[4][2];
  public final List<Integer> board = new ArrayList<>();
  public final List<Pot> pots = new ArrayList<>();
  public final Map<Integer, PokerHand.Result> hands = new HashMap<>();
  public Street street = Street.LOBBY;
  public int dealer = -1,
      smallBlind,
      bigBlind,
      actor = -1,
      currentBet,
      lastFullRaise = 20,
      handNumber;
  public int awardedPot;
  public String message = "Four seats. One beautiful bluff.";
  public final int bankroll;
  private int cursor;

  public HoldemGame() {
    this(new int[] {1000, 1000, 1000, 1000});
  }

  public HoldemGame(int[] initial) {
    if (initial.length != 4 || Arrays.stream(initial).anyMatch(v -> v < 0))
      throw new IllegalArgumentException("Four nonnegative stacks required");
    stacks = initial.clone();
    bankroll = Arrays.stream(initial).sum();
  }

  public boolean ended() {
    return street == Street.SHOWDOWN;
  }

  public boolean playing() {
    return street != Street.LOBBY && !ended();
  }

  public int pot() {
    return Arrays.stream(contributed).sum();
  }

  public int hole(int seat, int index) {
    return holes[seat][index];
  }

  public int live() {
    int n = 0;
    for (boolean f : folded) if (!f) n++;
    return n;
  }

  private int ready() {
    int n = 0;
    for (int i = 0; i < 4; i++) if (!folded[i] && stacks[i] > 0) n++;
    return n;
  }

  private int nextFunded(int from) {
    for (int n = 1; n <= 4; n++) {
      int i = (from + n) % 4;
      if (stacks[i] > 0) return i;
    }
    return -1;
  }

  public void start(Random random, boolean showcase) {
    var shuffled = new ArrayList<Integer>();
    for (int c = 0; c < 52; c++) shuffled.add(c);
    Collections.shuffle(shuffled, random);
    if (showcase) {
      // Clearly labelled scripted demo, never the ordinary shuffled game.
      int[] slots = {3, 7, 9, 10, 11, 13, 15};
      int[] cards = {
        PokerHand.card(14, 2),
        PokerHand.card(13, 2),
        PokerHand.card(12, 2),
        PokerHand.card(11, 2),
        PokerHand.card(2, 0),
        PokerHand.card(10, 2),
        PokerHand.card(9, 1)
      };
      if (dealer != -1) throw new IllegalStateException("Showcase requires a fresh table");
      for (int i = 0; i < slots.length; i++) {
        int at = shuffled.indexOf(cards[i]);
        Collections.swap(shuffled, slots[i], at);
      }
    }
    startDeck(shuffled);
  }

  void startDeck(List<Integer> order) {
    if (playing()) throw new IllegalStateException("Hand still playing");
    if (order.size() != 52
        || new HashSet<>(order).size() != 52
        || order.stream().anyMatch(c -> c < 0 || c >= 52))
      throw new IllegalArgumentException("Deck must be a permutation of 52 cards");
    if (Arrays.stream(stacks).filter(v -> v > 0).count() < 2)
      throw new IllegalStateException("Reset the demo table to refill stacks");
    deck.clear();
    deck.addAll(order);
    cursor = 0;
    board.clear();
    pots.clear();
    hands.clear();
    awardedPot = 0;
    Arrays.fill(bets, 0);
    Arrays.fill(contributed, 0);
    Arrays.fill(payouts, 0);
    Arrays.fill(acted, false);
    Arrays.fill(lastActedBet, 0);
    for (int i = 0; i < 4; i++) {
      folded[i] = stacks[i] == 0;
      pending[i] = !folded[i];
    }
    dealer = nextFunded(dealer);
    handNumber++;
    smallBlind = live() == 2 ? dealer : nextFunded(dealer);
    bigBlind = nextFunded(smallBlind);
    for (int pass = 0; pass < 2; pass++)
      for (int n = 1; n <= 4; n++) {
        int i = (dealer + n) % 4;
        if (!folded[i]) holes[i][pass] = draw();
      }
    street = Street.PREFLOP;
    lastFullRaise = 20;
    pay(smallBlind, 10);
    pay(bigBlind, 20);
    currentBet = 20;
    message = "Blinds 10 / 20. Make your move.";
    actor = nextPending(bigBlind);
    progress(bigBlind);
  }

  private int draw() {
    return deck.get(cursor++);
  }

  private int pay(int seat, int amount) {
    int paid = Math.min(stacks[seat], Math.max(0, amount));
    stacks[seat] -= paid;
    bets[seat] += paid;
    contributed[seat] += paid;
    return paid;
  }

  public int owed(int seat) {
    return Math.max(0, currentBet - bets[seat]);
  }

  public int minimumRaise() {
    return currentBet + lastFullRaise;
  }

  public int maximumRaise(int seat) {
    return bets[seat] + stacks[seat];
  }

  public boolean canRaise(int seat) {
    return playing()
        && seat == actor
        && ready() > 1
        && maximumRaise(seat) > currentBet
        && (!acted[seat] || currentBet - lastActedBet[seat] >= lastFullRaise);
  }

  public void act(int seat, Move move, int target) {
    if (!playing() || seat != actor || folded[seat] || stacks[seat] == 0)
      throw new IllegalArgumentException("Not this seat's turn");
    int max = maximumRaise(seat);
    boolean raise = move == Move.RAISE || (move == Move.ALL_IN && max > currentBet);
    if (move == Move.ALL_IN) target = max;
    if (raise
        && (!canRaise(seat)
            || target <= currentBet
            || target > max
            || (target < minimumRaise() && target != max)))
      throw new IllegalArgumentException("Illegal raise or betting not reopened");
    int paid = 0;
    if (move == Move.FOLD) {
      folded[seat] = true;
      message = names(seat) + " folds.";
    } else if (raise) {
      int old = currentBet;
      paid = pay(seat, target - bets[seat]);
      currentBet = target;
      if (target - old >= lastFullRaise) {
        lastFullRaise = target - old;
        for (int i = 0; i < 4; i++) if (i != seat) acted[i] = false;
      }
      for (int i = 0; i < 4; i++)
        if (i != seat && !folded[i] && stacks[i] > 0 && bets[i] < currentBet) pending[i] = true;
      message =
          names(seat) + (stacks[seat] == 0 ? " is all-in for " : " raises to ") + target + ".";
    } else {
      paid = pay(seat, owed(seat));
      message =
          names(seat)
              + (paid == 0
                  ? " checks."
                  : stacks[seat] == 0 ? " calls all-in." : " calls " + paid + ".");
    }
    pending[seat] = false;
    acted[seat] = true;
    lastActedBet[seat] = currentBet;
    progress(seat);
    if (Arrays.stream(stacks).sum() + pot() != bankroll)
      throw new IllegalStateException("Chip conservation violated");
  }

  public static String names(int seat) {
    return List.of("You", "Violet", "Cedar", "Nova").get(seat);
  }

  private int nextPending(int from) {
    for (int n = 1; n <= 4; n++) {
      int i = (from + n) % 4;
      if (!folded[i] && stacks[i] > 0 && pending[i]) return i;
    }
    return -1;
  }

  private void progress(int from) {
    while (true) {
      if (live() == 1) {
        refundUncalled();
        award();
        return;
      }
      // A lone funded player cannot bet into a dry side pot. They can only call or fold.
      if (ready() <= 1)
        for (int i = 0; i < 4; i++)
          if (!folded[i] && stacks[i] > 0 && owed(i) == 0) pending[i] = false;
      actor = nextPending(from);
      if (actor >= 0) return;
      refundUncalled();
      if (street == Street.RIVER) {
        award();
        return;
      }
      Arrays.fill(bets, 0);
      Arrays.fill(acted, false);
      Arrays.fill(lastActedBet, 0);
      currentBet = 0;
      lastFullRaise = 20;
      draw(); // One burn before each community street.
      switch (street) {
        case PREFLOP -> {
          street = Street.FLOP;
          for (int i = 0; i < 3; i++) board.add(draw());
        }
        case FLOP -> {
          street = Street.TURN;
          board.add(draw());
        }
        case TURN -> {
          street = Street.RIVER;
          board.add(draw());
        }
        default -> throw new IllegalStateException();
      }
      for (int i = 0; i < 4; i++) pending[i] = !folded[i] && stacks[i] > 0;
      from = dealer;
    }
  }

  private void refundUncalled() {
    int first = -1, high = 0, second = 0;
    for (int i = 0; i < 4; i++) {
      if (bets[i] > high) {
        second = high;
        high = bets[i];
        first = i;
      } else second = Math.max(second, bets[i]);
    }
    if (first >= 0 && high > second) {
      int refund = high - second;
      stacks[first] += refund;
      bets[first] -= refund;
      contributed[first] -= refund;
    }
  }

  private void award() {
    if (ended()) return;
    if (board.size() == 5)
      for (int i = 0; i < 4; i++)
        if (!folded[i]) {
          var cards = new ArrayList<>(board);
          cards.add(holes[i][0]);
          cards.add(holes[i][1]);
          hands.put(i, PokerHand.evaluate(cards));
        }
    int total = pot();
    var levels = Arrays.stream(contributed).filter(v -> v > 0).distinct().sorted().toArray();
    int previous = 0;
    for (int level : levels) {
      var payers = new ArrayList<Integer>();
      var eligible = new ArrayList<Integer>();
      for (int i = 0; i < 4; i++)
        if (contributed[i] >= level) {
          payers.add(i);
          if (!folded[i]) eligible.add(i);
        }
      int amount = (level - previous) * payers.size();
      previous = level;
      if (eligible.isEmpty()) throw new IllegalStateException("Pot without eligible player");
      var winners = new ArrayList<Integer>();
      if (eligible.size() == 1) winners.add(eligible.getFirst());
      else {
        long best =
            eligible.stream()
                .map(hands::get)
                .mapToLong(PokerHand.Result::score)
                .max()
                .orElseThrow();
        for (int i : eligible) if (hands.get(i).score() == best) winners.add(i);
      }
      int each = amount / winners.size(), odd = amount % winners.size();
      for (int i : winners) payouts[i] += each;
      for (int n = 1; n <= 4 && odd > 0; n++) {
        int i = (dealer + n) % 4;
        if (winners.contains(i)) {
          payouts[i]++;
          odd--;
        }
      }
      pots.add(new Pot(amount, List.copyOf(eligible), List.copyOf(winners)));
    }
    for (int i = 0; i < 4; i++) stacks[i] += payouts[i];
    Arrays.fill(contributed, 0);
    Arrays.fill(bets, 0);
    awardedPot = total;
    street = Street.SHOWDOWN;
    actor = -1;
    var winners = new ArrayList<String>();
    for (int i = 0; i < 4; i++) if (payouts[i] > 0) winners.add(names(i) + " +" + payouts[i]);
    message = String.join(" / ", winners);
  }
}
