package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;

/** Advent application: pure projection, typed actions and scoped effects. */
final class AdventMenu extends DemoMenu {
  @Override
  String id() {
    return "advent";
  }

  @Override
  List<String> templates() {
    return List.of("advent");
  }

  @Override
  Set<String> aliases() {
    return Set.of();
  }

  AdventMenu(DemoServices services) {
    super(services);
    on("advent_close", s -> {});
    on("advent_open", Integer::parseInt, (s, day) -> guard(() -> s.advent.open(day, s.tick)));
    on("advent_back", s -> s.advent.back());
    on(
        "advent_again",
        s -> {
          if (s.advent.phase == AdventState.Phase.REVEALED)
            s.advent.open(s.advent.selected, s.tick);
        });
    on("advent_size", s -> s.advent.compact = !s.advent.compact);
    on(
        "advent_motion",
        s -> {
          s.advent.motion = !s.advent.motion;
          if (!s.advent.motion && s.advent.phase == AdventState.Phase.OPENING) {
            s.advent.phase = AdventState.Phase.REVEALED;
            s.advent.rewardAt = s.tick;
          }
        });
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.advent.compact = compact;
  }

  @Override
  void advance(DemoSession s) {
    var state = s.advent;
    if (state.phase == AdventState.Phase.OPENING) state.reveal(state.generation, s.tick);
    if (state.phase == AdventState.Phase.REVEALED
        && s.tick - state.startedAt >= AdventState.OPENING.duration()) state.effectsFinished = true;
  }

  @Override
  Map<String, org.bukkit.inventory.ItemStack> captureItems(DemoSession s) {
    return services.viewerItems(p -> AdventItems.stacks(s.advent));
  }

  @Override
  MenuView project(DemoSession s) {
    return view(
        "advent",
        AdventView.data(s.advent, s.tick),
        AdventView.images(s.advent),
        s,
        Map.of(),
        DialogOptions.notice("dui / Gift drop", "Close calendar", "advent_close"));
  }

  @Override
  void presented(DemoSession s, Canvas c) {
    var state = s.advent;
    if (state.phase == AdventState.Phase.OPENING) {
      long token = state.generation;
      later(
          AdventState.OPEN_TICKS - (s.tick - state.startedAt),
          () -> state.reveal(token, services.tick()));
    } else if (state.phase == AdventState.Phase.REVEALED && state.motion && !state.effectsFinished)
      later(
          AdventState.OPENING.remaining(state.startedAt, s.tick),
          () -> state.effectsFinished = true);
  }

  @Override
  void closed(DemoSession s) {
    s.advent.back();
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    return Map.of(
        "section",
        "advent",
        "state",
        s.advent,
        "phase",
        s.advent.phase,
        "selected",
        s.advent.selected,
        "effects",
        c.effects);
  }

  @Override
  void validate(boolean compact) {
    var advent = new AdventState();
    advent.compact = compact;
    services.template("advent").render(AdventView.data(advent, 100), AdventView.images(advent));
    advent.open(24, 100);
    services.template("advent").render(AdventView.data(advent, 100), AdventView.images(advent));
    advent.reveal(advent.generation, 124);
    services.template("advent").render(AdventView.data(advent, 124), AdventView.images(advent));
  }
}
