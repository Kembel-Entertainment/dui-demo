package gg.kembel.dui.demo;

import java.util.*;
import java.util.random.RandomGenerator;

/** Server-authoritative, single-zero demo ledger. Stakes are reserved when a chip is placed. */
public final class RouletteGame {
  public static final int[] WHEEL = {
    0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14,
    31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26
  };
  public static final int[] CHIPS = {5, 25, 100, 500};
  public static final int SPIN_TICKS = 160, PAYOUT_TICKS = 28;

  public enum Phase {
    BETTING,
    SPINNING,
    PAYOUT
  }

  public record Placement(String bet, int amount) {}

  public long balance = 5000;
  public int denomination = 25, lastResult = -1, previousWheel;
  public int lastStake, lastReturn, rounds;
  public Phase phase = Phase.BETTING;
  public final Map<String, Integer> bets = new LinkedHashMap<>(), lastBets = new LinkedHashMap<>();
  public final List<Placement> placements = new ArrayList<>();
  public final List<Integer> history = new ArrayList<>();
  public boolean compact, motion = true;
  public long startedAt = -1, generation;
  public String event = "", message = "Choose a chip. Place your bets.";
  public String chipBet = "";
  private transient int pendingResult = -1;

  public boolean locked() {
    return phase != Phase.BETTING;
  }

  public int stake() {
    return bets.values().stream().mapToInt(Integer::intValue).sum();
  }

  public int wheelValue() {
    return phase == Phase.SPINNING ? pendingResult : Math.max(0, lastResult);
  }

  public int duration() {
    return phase == Phase.SPINNING ? SPIN_TICKS : event.equals("payout") ? PAYOUT_TICKS : 18;
  }

  public boolean animated() {
    return startedAt >= 0;
  }

  public static boolean red(int n) {
    return Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36).contains(n);
  }

  /** Return multiplier includes the reserved stake: straight 36x, dozen/column 3x, outside 2x. */
  public static int multiplier(String bet) {
    if (bet.matches("n:(?:[0-9]|[12][0-9]|3[0-6])")) return 36;
    if (bet.matches("[dc]:[1-3]")) return 3;
    if (Set.of("red", "black", "odd", "even", "low", "high").contains(bet)) return 2;
    throw new IllegalArgumentException("Unknown roulette bet: " + bet);
  }

  public static boolean wins(String bet, int result) {
    multiplier(bet);
    if (result < 0 || result > 36) throw new IllegalArgumentException("Result must be 0..36");
    if (bet.startsWith("n:")) return result == Integer.parseInt(bet.substring(2));
    if (result == 0) return false;
    if (bet.startsWith("d:")) return (result - 1) / 12 + 1 == Integer.parseInt(bet.substring(2));
    if (bet.startsWith("c:")) return (result - 1) % 3 + 1 == Integer.parseInt(bet.substring(2));
    return switch (bet) {
      case "red" -> red(result);
      case "black" -> !red(result);
      case "odd" -> result % 2 == 1;
      case "even" -> result % 2 == 0;
      case "low" -> result <= 18;
      case "high" -> result >= 19;
      default -> throw new IllegalArgumentException();
    };
  }

  public void choose(int value) {
    if (Arrays.stream(CHIPS).noneMatch(n -> n == value))
      throw new IllegalArgumentException("Invalid chip");
    if (!locked()) denomination = value;
  }

  public boolean place(String bet, long tick) {
    multiplier(bet);
    if (locked()) return false;
    if (stake() + denomination > 5000) {
      message = "Table limit: 5,000 credits per round.";
      return false;
    }
    if (balance < denomination) {
      message = "Not enough credits. Undo, clear, or reset.";
      return false;
    }
    balance -= denomination;
    bets.merge(bet, denomination, Integer::sum);
    placements.add(new Placement(bet, denomination));
    chipBet = bet;
    message = label(bet) + " / " + bets.get(bet) + " credits";
    animate("chip", tick);
    return true;
  }

  public void undo() {
    if (locked() || placements.isEmpty()) return;
    var p = placements.removeLast();
    int left = bets.get(p.bet()) - p.amount();
    if (left == 0) bets.remove(p.bet());
    else bets.put(p.bet(), left);
    balance += p.amount();
    clearAnimation();
    generation++;
    message = "Last chip returned.";
  }

  public void clear() {
    if (locked()) return;
    balance += stake();
    bets.clear();
    placements.clear();
    clearAnimation();
    generation++;
    message = "Bets cleared. Credits returned.";
  }

  public boolean repeat(long tick) {
    if (locked() || !bets.isEmpty() || lastBets.isEmpty()) return false;
    int cost = lastBets.values().stream().mapToInt(Integer::intValue).sum();
    if (cost > balance) {
      message = "Not enough credits to repeat that round.";
      return false;
    }
    balance -= cost;
    bets.putAll(lastBets);
    lastBets.forEach((key, value) -> placements.add(new Placement(key, value)));
    message = "Previous bets restored.";
    clearAnimation();
    generation++;
    return true;
  }

  public boolean spin(RandomGenerator random, long tick) {
    if (locked() || bets.isEmpty()) return false;
    int result = random.nextInt(37);
    pendingResult = result;
    previousWheel = Math.max(0, lastResult);
    phase = Phase.SPINNING;
    message = "NO MORE BETS / BALL IN PLAY";
    animate("spin", tick);
    if (!motion) settle(tick);
    return true;
  }

  public void finish(long tick) {
    if (!animated() || tick - startedAt < duration()) return;
    if (phase == Phase.SPINNING) settle(tick);
    else {
      phase = Phase.BETTING;
      clearAnimation();
      generation++;
    }
  }

  /** Exactly once, independent of animation completion, sizing or repeat callbacks. */
  private void settle(long tick) {
    if (phase != Phase.SPINNING) return;
    lastResult = pendingResult;
    pendingResult = -1;
    lastStake = stake();
    lastReturn = 0;
    lastBets.clear();
    lastBets.putAll(bets);
    for (var e : bets.entrySet())
      if (wins(e.getKey(), lastResult)) lastReturn += e.getValue() * multiplier(e.getKey());
    balance += lastReturn;
    bets.clear();
    placements.clear();
    rounds++;
    history.addFirst(lastResult);
    if (history.size() > 9) history.removeLast();
    message =
        lastResult
            + " "
            + (lastResult == 0 ? "GREEN" : red(lastResult) ? "RED" : "BLACK")
            + " / "
            + (lastReturn > 0 ? "RETURN " + lastReturn : "NO WIN THIS ROUND");
    phase = Phase.PAYOUT;
    animate("payout", tick);
    if (!motion) {
      phase = Phase.BETTING;
      clearAnimation();
    }
  }

  private void animate(String kind, long tick) {
    event = kind;
    startedAt = motion ? tick : -1;
    generation++;
  }

  private void clearAnimation() {
    event = "";
    startedAt = -1;
    chipBet = "";
  }

  public void toggleMotion(long tick) {
    motion = !motion;
    if (!motion) {
      settle(tick);
      if (phase == Phase.PAYOUT) phase = Phase.BETTING;
      clearAnimation();
      generation++;
    }
  }

  public void reset() {
    balance = 5000;
    lastResult = -1;
    previousWheel = 0;
    pendingResult = -1;
    lastStake = lastReturn = rounds = 0;
    bets.clear();
    lastBets.clear();
    history.clear();
    placements.clear();
    phase = Phase.BETTING;
    clearAnimation();
    generation++;
    message = "Fresh table. 5,000 demo credits.";
  }

  public static String label(String bet) {
    multiplier(bet);
    if (bet.startsWith("n:")) return "Straight " + bet.substring(2);
    if (bet.startsWith("d:")) return "Dozen " + bet.substring(2);
    if (bet.startsWith("c:")) return "Column " + bet.substring(2);
    return bet.substring(0, 1).toUpperCase(Locale.ROOT) + bet.substring(1);
  }
}
