package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;

/** Four independent consumer games using one round lifecycle and public template components. */
final class ArcadeMenu extends DemoMenu {
  enum Game {
    HORSES("horses", "NIGHTFALL DERBY", Set.of("horserace", "pferderennen")),
    WHEEL("wheel", "PRISM WHEEL", Set.of("gluecksrad", "fortunewheel")),
    COIN("coinflip", "SUN & MOON", Set.of()),
    BOOK("bookofra", "TEMPLE OF DAWN", Set.of("book"));
    final String id, title;
    final Set<String> aliases;

    Game(String id, String title, Set<String> aliases) {
      this.id = id;
      this.title = title;
      this.aliases = aliases;
    }

    ArcadeGame fresh() {
      return switch (this) {
        case HORSES -> new HorseRaceGame();
        case WHEEL -> new PrizeWheelGame();
        case COIN -> new CoinflipGame();
        case BOOK -> new TempleSlotsGame();
      };
    }
  }

  private final Game game;

  ArcadeMenu(DemoServices services, Game game) {
    super(services);
    this.game = game;
    on("arcade_close", s -> {});
    on(
        "arcade_play",
        s -> {
          var state = state(s);
          if (state.play(services.random(), s.tick)) state.help = false;
        });
    on(
        "arcade_bonus",
        s -> {
          if (state(s) instanceof TempleSlotsGame book) book.demoBonus();
        });
    on("arcade_bet", DemoMenu::direction, (s, value) -> state(s).bet(value));
    on("arcade_motion", s -> state(s).toggleMotion(s.tick));
    on("arcade_size", s -> state(s).compact = !state(s).compact);
    on("arcade_reset", s -> state(s).reset());
    on(
        "arcade_help",
        s -> {
          if (!state(s).pending) state(s).help = !state(s).help;
        });
    on(
        "arcade_select",
        Integer::parseInt,
        (s, value) ->
            guard(
                () -> {
                  var state = state(s);
                  if (state instanceof HorseRaceGame race) race.select(value);
                  else if (state instanceof CoinflipGame coin) coin.select(value);
                  else throw new IllegalArgumentException("No selection in this game");
                }));
  }

  ArcadeGame state(DemoSession s) {
    return s.arcade.computeIfAbsent(game.id, id -> game.fresh());
  }

  @Override
  String id() {
    return game.id;
  }

  @Override
  List<String> templates() {
    return List.of(game.id);
  }

  @Override
  Set<String> aliases() {
    return game.aliases;
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    state(s).compact = compact;
    state(s).help = false;
  }

  @Override
  void advance(DemoSession s) {
    state(s).finish(s.tick, false);
  }

  @Override
  MenuView project(DemoSession s) {
    // The state exists after prepare/capture. Projection itself must never create or mutate it.
    var state = Objects.requireNonNull(s.arcade.get(game.id));
    return view(
        game.id,
        ArcadeView.data(game, state),
        Map.of(),
        s,
        Map.of(),
        DialogOptions.notice("dui / " + game.title, "Leave demo", "arcade_close"));
  }

  @Override
  void presented(DemoSession s, Canvas canvas) {
    var state = state(s);
    if (state.pending)
      later(
          state.duration() - (s.tick - state.startedAt),
          () -> state.finish(services.tick(), false));
  }

  @Override
  void closed(DemoSession s) {
    state(s).finish(s.tick, true);
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    var state = state(s);
    var data = new LinkedHashMap<String, Object>();
    data.put("section", game.id);
    data.put("busy", state.pending);
    data.put("credits", state.credits);
    data.put("wager", state.wager());
    data.put("rounds", state.rounds);
    data.put("returned", state.returned);
    data.put("motion", state.motion);
    data.put("compact", state.compact);
    data.put("help", state.help);
    data.put("startedAt", state.startedAt);
    if (state instanceof HorseRaceGame race) {
      data.put("selected", race.selected);
      data.put("result", race.pending ? -1 : race.winner);
    }
    if (state instanceof CoinflipGame coin) {
      data.put("selected", coin.selected);
      data.put("result", coin.pending ? -1 : coin.face);
    }
    if (state instanceof PrizeWheelGame wheel)
      data.put("result", wheel.pending ? -1 : wheel.sector);
    if (state instanceof TempleSlotsGame book) {
      data.put("freeSpins", book.freeSpins);
      data.put("expanding", book.expanding);
      data.put("winningLines", book.winningLines);
      data.put("grid", book.grid);
    }
    return data;
  }

  @Override
  void validate(boolean compact) {
    var state = game.fresh();
    state.compact = compact;
    services.template(game.id).render(ArcadeView.data(game, state));
    state.play(new Random(42), 100);
    services.template(game.id).render(ArcadeView.data(game, state));
    state.finish(100 + state.duration(), false);
    state.help = true;
    services.template(game.id).render(ArcadeView.data(game, state));
  }
}
