package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;

/** Poker application: pure projection, typed actions and scoped effects. */
final class PokerMenu extends DemoMenu {
  @Override
  String id() {
    return "poker";
  }

  @Override
  List<String> templates() {
    return List.of("poker");
  }

  @Override
  Set<String> aliases() {
    return Set.of();
  }

  PokerMenu(DemoServices services) {
    super(services);
    on("poker_close", s -> {});
    on("poker_deal", s -> guard(() -> s.poker.deal(s.tick, false)));
    on("poker_showcase", s -> guard(() -> s.poker.deal(s.tick, true)));
    on("poker_fold", s -> guard(() -> s.poker.human(HoldemGame.Move.FOLD, s.tick)));
    on("poker_call", s -> guard(() -> s.poker.human(HoldemGame.Move.CHECK_CALL, s.tick)));
    on("poker_raise", s -> guard(() -> s.poker.human(HoldemGame.Move.RAISE, s.tick)));
    on("poker_allin", s -> guard(() -> s.poker.human(HoldemGame.Move.ALL_IN, s.tick)));
    on("poker_minus", s -> s.poker.adjustRaise(-20));
    on("poker_plus", s -> s.poker.adjustRaise(20));
    on("poker_reset", s -> s.poker.reset());
    on(
        "poker_size",
        s -> {
          s.poker.stop();
          s.poker.compact = !s.poker.compact;
        });
    on(
        "poker_motion",
        s -> {
          s.poker.stop();
          s.poker.motion = !s.poker.motion;
        });
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.poker.compact = compact;
  }

  @Override
  void advance(DemoSession s) {
    s.poker.finish(s.tick);
  }

  @Override
  MenuView project(DemoSession s) {
    return view(
        "poker",
        PokerView.data(s.poker),
        PokerArt.images(s.poker.game),
        s,
        Map.of(),
        DialogOptions.notice("dui / Velvet Hold'em", "Leave table", "poker_close"));
  }

  @Override
  void presented(DemoSession s, Canvas c) {
    var state = s.poker;
    if (state.busy() || state.game.playing() && state.game.actor > 0) {
      long delay = state.busy() ? state.duration - (s.tick - state.startedAt) : 12;
      later(
          delay,
          () -> {
            if (state.busy()) state.settle();
            else state.bot(services.tick());
          });
    }
  }

  @Override
  void closed(DemoSession s) {
    s.poker.reset();
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    return Map.of(
        "section",
        "poker",
        "state",
        s.poker,
        "street",
        s.poker.game.street,
        "actor",
        s.poker.game.actor,
        "busy",
        s.poker.busy(),
        "board",
        s.poker.game.board);
  }

  @Override
  void validate(boolean compact) {
    var poker = new PokerState();
    poker.compact = compact;
    services.template("poker").render(PokerView.data(poker), PokerArt.images(poker.game));
    poker.deal(100, true);
    services.template("poker").render(PokerView.data(poker), PokerArt.images(poker.game));
  }
}
