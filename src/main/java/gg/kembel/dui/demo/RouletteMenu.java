package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;

/** Roulette application: pure projection, typed actions and scoped effects. */
final class RouletteMenu extends DemoMenu {
  @Override
  String id() {
    return "roulette";
  }

  @Override
  List<String> templates() {
    return List.of("roulette");
  }

  @Override
  Set<String> aliases() {
    return Set.of();
  }

  RouletteMenu(DemoServices services) {
    super(services);
    on("roulette_close", s -> {});
    on(
        "roulette_bet",
        v -> {
          RouletteGame.multiplier(v);
          return v;
        },
        (s, v) -> guard(() -> s.roulette.place(v, s.tick)));
    on("roulette_chip", Integer::parseInt, (s, v) -> guard(() -> s.roulette.choose(v)));
    on("roulette_spin", s -> guard(() -> s.roulette.spin(services.random(), s.tick)));
    on("roulette_undo", s -> s.roulette.undo());
    on("roulette_clear", s -> s.roulette.clear());
    on("roulette_repeat", s -> guard(() -> s.roulette.repeat(s.tick)));
    on(
        "roulette_reset",
        s -> {
          if (!s.roulette.locked()) s.roulette.reset();
        });
    on("roulette_motion", s -> s.roulette.toggleMotion(s.tick));
    on("roulette_size", s -> s.roulette.compact = !s.roulette.compact);
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.roulette.compact = compact;
  }

  @Override
  void advance(DemoSession s) {
    s.roulette.finish(s.tick);
  }

  @Override
  MenuView project(DemoSession s) {
    return view(
        "roulette",
        RouletteView.data(s.roulette),
        RouletteArt.images(),
        s,
        Map.of(),
        DialogOptions.notice("dui / Riviera Roulette", "Leave table", "roulette_close"));
  }

  @Override
  void presented(DemoSession s, Canvas c) {
    if (s.roulette.animated())
      later(
          s.roulette.duration() - (s.tick - s.roulette.startedAt),
          () -> s.roulette.finish(services.tick()));
  }

  @Override
  void closed(DemoSession s) {
    s.roulette.reset();
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    var state = s.roulette;
    return Map.of(
        "section",
        "roulette",
        "phase",
        state.phase,
        "busy",
        state.locked(),
        "balance",
        state.balance,
        "stake",
        state.stake(),
        "result",
        state.phase == RouletteGame.Phase.SPINNING ? -1 : state.lastResult,
        "return",
        state.lastReturn,
        "rounds",
        state.rounds,
        "motion",
        state.motion,
        "bets",
        state.bets);
  }

  @Override
  void validate(boolean compact) {
    var roulette = new RouletteGame();
    roulette.compact = compact;
    services.template("roulette").render(RouletteView.data(roulette), RouletteArt.images());
    roulette.place("n:17", 100);
    roulette.spin(new Random(2), 120);
    services.template("roulette").render(RouletteView.data(roulette), RouletteArt.images());
  }
}
