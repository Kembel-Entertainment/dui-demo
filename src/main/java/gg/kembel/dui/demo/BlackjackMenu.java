package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;

/** Blackjack application: pure projection, typed actions and scoped effects. */
final class BlackjackMenu extends DemoMenu {
  @Override
  String id() {
    return "blackjack";
  }

  @Override
  List<String> templates() {
    return List.of("blackjack");
  }

  @Override
  Set<String> aliases() {
    return Set.of();
  }

  private record HandPage(String hand, int direction) {
    static HandPage decode(String value) {
      var parts = value.split(":", -1);
      if (parts.length != 2) throw new IllegalArgumentException("Invalid hand page");
      return new HandPage(choice(parts[0], "dealer", "hero"), DemoMenu.direction(parts[1]));
    }
  }

  BlackjackMenu(DemoServices services) {
    super(services);
    on("blackjack_close", s -> {});
    on("blackjack_deal", s -> guard(() -> s.blackjack.deal(s.tick, false, services.random())));
    on("blackjack_demo", s -> guard(() -> s.blackjack.deal(s.tick, true, services.random())));
    for (var move : List.of("hit", "stand", "double", "split"))
      on("blackjack_" + move, s -> guard(() -> s.blackjack.move(move, s.tick)));
    on("blackjack_bet", Integer::parseInt, (s, v) -> guard(() -> s.blackjack.choose(v)));
    on("blackjack_size", s -> s.blackjack.compact = !s.blackjack.compact);
    on("blackjack_motion", s -> s.blackjack.toggleMotion(s.tick));
    on(
        "blackjack_hand",
        Integer::parseInt,
        (s, hand) -> {
          var state = s.blackjack;
          if (!state.busy()
              && state.game.phase == BlackjackGame.Phase.RESULT
              && hand >= 0
              && hand < state.game.hands.size()) {
            state.focus = hand;
            state.heroPage = 0;
          }
        });
    on(
        "blackjack_page",
        HandPage::decode,
        (s, page) -> {
          var state = s.blackjack;
          if (state.busy()) return;
          if (page.hand().equals("dealer"))
            state.dealerPage =
                Math.clamp(
                    state.dealerPage + page.direction(),
                    0,
                    Math.max(0, state.game.visibleDealer().size() - state.dealerLimit()));
          else if (!state.game.hands.isEmpty())
            state.heroPage =
                Math.clamp(
                    state.heroPage + page.direction(),
                    0,
                    Math.max(
                        0, state.game.hands.get(state.focus).cards.size() - state.heroLimit()));
        });
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.blackjack.compact = compact;
  }

  @Override
  void advance(DemoSession s) {
    s.blackjack.finish(s.tick);
  }

  @Override
  MenuView project(DemoSession s) {
    return view(
        "blackjack",
        BlackjackView.data(s.blackjack),
        BlackjackArt.images(),
        s,
        Map.of(),
        DialogOptions.notice("dui / Monarch Blackjack", "Leave table", "blackjack_close"));
  }

  @Override
  void presented(DemoSession s, Canvas c) {
    if (s.blackjack.busy())
      later(
          s.blackjack.duration - (s.tick - s.blackjack.startedAt),
          () -> s.blackjack.finish(services.tick()));
  }

  @Override
  void closed(DemoSession s) {
    s.blackjack.reset();
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    var extra = new HashMap<String, Object>(s.blackjack.publicState());
    extra.put("section", "blackjack");
    return extra;
  }

  @Override
  void validate(boolean compact) {
    var blackjack = new BlackjackState();
    blackjack.compact = compact;
    services.template("blackjack").render(BlackjackView.data(blackjack), BlackjackArt.images());
    blackjack.deal(100, true, new Random(1));
    services.template("blackjack").render(BlackjackView.data(blackjack), BlackjackArt.images());
  }
}
