package gg.kembel.dui.demo;

import java.util.*;

/** One transient table per player. Rendering timers do not affect cards, bets, or payouts. */
public final class PokerState {
  public HoldemGame game = new HoldemGame();
  public boolean compact, motion = true, showcase;
  public int raiseTarget = 40, newBoardFrom = 5, chipSeat = 0;
  public String event = "", chipFrom = "bottom", chipTo = "top";
  public long startedAt = -1, generation;
  public int duration;
  private final transient Random random = new Random();

  public boolean busy() {
    return startedAt >= 0;
  }

  public void reset() {
    generation++;
    game = new HoldemGame();
    settle();
    showcase = false;
    raiseTarget = 40;
  }

  public void deal(long tick, boolean script) {
    if (game.playing() || busy()) return;
    if (script) {
      game = new HoldemGame();
      showcase = true;
    } else showcase = false;
    game.start(random, script);
    newBoardFrom = 5;
    chipSeat = 0;
    chipFrom = "bottom";
    chipTo = "top";
    animate("deal", tick, 36);
    selectRaise();
  }

  public void human(HoldemGame.Move move, long tick) {
    if (busy() || game.actor != 0 || !game.playing()) return;
    change(0, () -> game.act(0, move, raiseTarget), tick);
  }

  public void bot(long tick) {
    if (busy() || game.actor <= 0 || !game.playing()) return;
    int actor = game.actor;
    change(actor, () -> PokerBots.act(game, random, showcase), tick);
  }

  private void change(int seat, Runnable action, long tick) {
    int oldBoard = game.board.size(), oldStack = game.stacks[seat];
    action.run();
    newBoardFrom = oldBoard;
    chipSeat = seat;
    chipFrom =
        switch (seat) {
          case 1 -> "left";
          case 2 -> "top-left";
          case 3 -> "right";
          default -> "bottom";
        };
    chipTo = "top";
    if (game.ended()) {
      chipFrom = "top";
      chipTo =
          game.payouts[0] > 0
              ? "bottom"
              : game.payouts[1] > 0 ? "left" : game.payouts[2] > 0 ? "top-left" : "right";
    }
    String next =
        game.board.size() > oldBoard
            ? "flip"
            : game.ended() ? "win" : oldStack != game.stacks[seat] ? "chips" : "pause";
    animate(next, tick, next.equals("flip") ? 44 : next.equals("pause") ? 10 : 24);
    selectRaise();
  }

  private void animate(String kind, long tick, int ticks) {
    generation++;
    event = kind;
    startedAt = motion ? tick : -1;
    duration = motion ? ticks : 0;
    if (!motion) event = "";
  }

  public void settle() {
    startedAt = -1;
    event = "";
    newBoardFrom = 5;
  }

  public void finish(long tick) {
    if (busy() && tick - startedAt >= duration) settle();
  }

  public void stop() {
    generation++;
    settle();
  }

  public void selectRaise() {
    raiseTarget = Math.min(game.maximumRaise(0), game.minimumRaise());
  }

  public void adjustRaise(int delta) {
    if (busy() || !game.canRaise(0)) return;
    int min = Math.min(game.minimumRaise(), game.maximumRaise(0));
    raiseTarget = Math.clamp(raiseTarget + delta, min, game.maximumRaise(0));
  }
}
