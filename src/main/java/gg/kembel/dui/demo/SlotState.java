package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** A separate demo-chip ledger. A spin reserves its stake and its fixed result before animation. */
public final class SlotState {
  public static final int SPIN_TICKS = 70;
  public static final List<String> SYMBOLS =
      List.of("Seven", "Diamond", "Bell", "Cherries", "Lemon", "BAR");
  public static final int[] BETS = {10, 25, 50, 100}, TRIPLES = {25, 12, 8, 5, 4, 6};
  public static final List<String> MODES =
      List.of("Random", "Preview 777", "Preview pair", "Preview miss");
  public long chips = 1000, wagered, paid;
  public int betIndex = 1, modeIndex, spins, wager, payout;
  public int[] reels = {0, 0, 0}, previous = {0, 0, 0};
  public boolean motion = true, pending, preview;
  public String message = "Ready / Pull the lever";
  public transient boolean compact, paytable;
  public transient long startedAt = -1;

  public int bet() {
    return BETS[betIndex];
  }

  public boolean canSpin() {
    return !pending && (modeIndex != 0 || chips >= bet());
  }

  public boolean spin(long tick, RandomGenerator random) {
    if (!canSpin()) return false;
    previous = reels.clone();
    reels =
        switch (modeIndex) {
          case 1 -> new int[] {0, 0, 0};
          case 2 -> new int[] {3, 3, 4};
          case 3 -> new int[] {0, 2, 4};
          default -> new int[] {random.nextInt(6), random.nextInt(6), random.nextInt(6)};
        };
    wager = bet();
    payout = wager * multiplier(reels);
    preview = modeIndex != 0;
    if (!preview) {
      chips -= wager;
      wagered += wager;
    }
    pending = true;
    startedAt = tick;
    message = preview ? "PREVIEW / No chips change" : "Spinning / Good luck!";
    return true;
  }

  public boolean settle() {
    if (!pending) return false;
    pending = false;
    if (!preview) {
      chips += payout;
      paid += payout;
      spins++;
    }
    String result =
        multiplier(reels) == 25
            ? "777 JACKPOT!"
            : payout > wager ? "WIN!" : payout == wager ? "PAIR / Stake returned" : "No match";
    message =
        preview
            ? "PREVIEW / " + result
            : result + (payout > 0 ? " / +" + payout + " chips" : " / -" + wager + " chips");
    return true;
  }

  public void apply(String action) {
    if (pending) return;
    switch (action) {
      case "slot_bet_less" -> betIndex = Math.floorMod(betIndex - 1, BETS.length);
      case "slot_bet_more" -> betIndex = (betIndex + 1) % BETS.length;
      case "slot_mode" -> {
        modeIndex = (modeIndex + 1) % MODES.size();
        message = modeIndex == 0 ? "Random / Uniform reels" : "Preview / Your balance is unchanged";
      }
      case "slot_refill" -> {
        if (chips < 1000) {
          chips = 1000;
          message = "Demo wallet refilled to 1,000 chips";
        }
      }
      case "slot_motion" -> motion = !motion;
      case "slot_size" -> {
        compact = !compact;
        paytable = false;
      }
      case "slot_paytable" -> paytable = !paytable;
      default -> throw new IllegalArgumentException("Unknown slot action: " + action);
    }
    startedAt = -1;
  }

  public static int multiplier(int[] reels) {
    if (reels == null || reels.length != 3 || Arrays.stream(reels).anyMatch(i -> i < 0 || i >= 6))
      throw new IllegalArgumentException("Invalid reels");
    if (reels[0] == reels[1] && reels[1] == reels[2]) return TRIPLES[reels[0]];
    return reels[0] == reels[1] || reels[0] == reels[2] || reels[1] == reels[2] ? 1 : 0;
  }

  public boolean valid() {
    if (chips < 0
        || wagered < 0
        || paid < 0
        || spins < 0
        || betIndex < 0
        || betIndex >= BETS.length
        || modeIndex < 0
        || modeIndex >= MODES.size()
        || message == null) return false;
    try {
      multiplier(reels);
      multiplier(previous);
    } catch (IllegalArgumentException e) {
      return false;
    }
    return !pending
        || Arrays.stream(BETS).anyMatch(b -> b == wager) && payout == wager * multiplier(reels);
  }
}
