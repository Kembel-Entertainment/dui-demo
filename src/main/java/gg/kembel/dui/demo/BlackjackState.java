package gg.kembel.dui.demo;

import java.util.*;
import java.util.random.RandomGenerator;

/** Transient presentation controller: one timer per event, never server-driven animation frames. */
public final class BlackjackState {
  public BlackjackGame game = new BlackjackGame();
  public boolean compact, motion = true, demo;
  public int bet = 25, focus, heroPage, dealerPage, preview, splitFocus;
  public long startedAt, generation;
  public int duration;
  public String event = "", previewName = "";
  private static final List<List<Integer>> PREVIEWS =
      List.of(
          List.of(6, 17, 45, 34, 1, 13, 29, 5, 43),
          List.of(38, 7, 50, 18),
          List.of(8, 51, 32, 4),
          List.of(21, 33, 5, 45),
          List.of(10, 46, 30, 18, 37));
  private static final String[] NAMES = {
    "Split eights", "Natural blackjack", "Soft seventeen", "Seventeen push", "Hit or hold"
  };

  public boolean busy() {
    return !event.isEmpty();
  }

  public void reset() {
    game = new BlackjackGame();
    event = "";
    duration = 0;
    focus = heroPage = dealerPage = 0;
    demo = false;
    generation++;
  }

  public void deal(long tick, boolean previewDeal, RandomGenerator rng) {
    if (busy()) return;
    game.deal(bet, rng, previewDeal ? PREVIEWS.get(preview % PREVIEWS.size()) : List.of());
    demo = previewDeal;
    previewName = previewDeal ? NAMES[preview++ % NAMES.length] : "Shuffled six-deck shoe";
    focus = heroPage = dealerPage = 0;
    begin("deal", 54, tick);
    drain(tick);
  }

  public void move(String action, long tick) {
    if (busy() || game.phase != BlackjackGame.Phase.PLAYER) return;
    focus = game.active;
    heroPage = 0;
    switch (action) {
      case "hit" -> {
        game.hit();
        begin("hit", 28, tick);
      }
      case "double" -> {
        game.doubleDown();
        begin("hit", 28, tick);
      }
      case "split" -> {
        splitFocus = focus;
        game.split();
        begin("split", 44, tick);
      }
      case "stand" -> {
        game.stand();
        progress(tick);
      }
      default -> throw new IllegalArgumentException("Unknown move");
    }
    heroPage = Math.max(0, game.hands.get(focus).cards.size() - 4);
    drain(tick);
  }

  private void begin(String next, int ticks, long tick) {
    event = next;
    duration = ticks;
    startedAt = tick;
    generation++;
  }

  public void finish(long tick) {
    if (busy() && tick - startedAt >= duration) complete(tick);
    drain(tick);
  }

  private void complete(long tick) {
    String was = event;
    event = "";
    generation++;
    if (was.equals("deal")) game.finishDeal();
    if (was.equals("reveal") || was.equals("dealer")) {
      if (game.dealerStep()) {
        dealerPage = Math.max(0, game.visibleDealer().size() - 3);
        begin("dealer", 28, tick);
      } else begin("payout", 32, tick);
    } else if (!was.equals("payout")) progress(tick);
  }

  private void progress(long tick) {
    if (game.phase == BlackjackGame.Phase.REVEAL) {
      game.reveal();
      begin("reveal", 30, tick);
    } else if (game.phase == BlackjackGame.Phase.PLAYER) {
      focus = game.active;
      heroPage = Math.max(0, game.current().cards.size() - 4);
    }
  }

  private void drain(long tick) {
    if (!motion) for (int i = 0; busy() && i < 80; i++) complete(tick);
  }

  public void toggleMotion(long tick) {
    motion = !motion;
    generation++;
    drain(tick);
  }

  public void choose(int value) {
    if (!busy()
        && (game.phase == BlackjackGame.Phase.BETTING || game.phase == BlackjackGame.Phase.RESULT)
        && Arrays.stream(BlackjackGame.BETS).anyMatch(b -> b == value)) bet = value;
  }

  public Map<String, Object> publicState() {
    var d = new HashMap<String, Object>();
    d.put("phase", game.phase);
    d.put("event", event);
    d.put("busy", busy());
    d.put("balanceHalf", game.balanceHalf);
    d.put("returnHalf", game.returnedHalf);
    d.put("netHalf", game.netHalf);
    d.put("hands", game.hands);
    d.put("dealer", game.visibleDealer());
    d.put("dealerTotal", game.dealerTotal());
    d.put("active", game.active);
    d.put("focus", focus);
    d.put("rounds", game.rounds);
    d.put("demo", demo);
    d.put("motion", motion);
    d.put("preview", previewName);
    return d;
  }
}
