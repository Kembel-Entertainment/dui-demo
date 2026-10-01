package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;

/** Slots application: pure projection, typed actions and scoped effects. */
final class SlotsMenu extends DemoMenu {
  @Override
  String id() {
    return "slots";
  }

  @Override
  List<String> templates() {
    return List.of("slots");
  }

  @Override
  Set<String> aliases() {
    return Set.of();
  }

  SlotsMenu(DemoServices services) {
    super(services);
    on("slot_close", s -> {});
    on(
        "slot_spin",
        s -> {
          var next = services.copySlots(s.slots);
          if (!next.spin(s.tick, services.random())) return;
          services.save("slots", next);
          s.slots = next;
          s.slotAnimationEnd = -1;
          services.settleAfter(next.motion ? SlotState.SPIN_TICKS : 1);
        });
    for (var id :
        List.of(
            "slot_bet_less",
            "slot_bet_more",
            "slot_mode",
            "slot_refill",
            "slot_motion",
            "slot_size",
            "slot_paytable"))
      on(
          id,
          s -> {
            var next = services.copySlots(s.slots);
            next.apply(id);
            services.save("slots", next);
            s.slots = next;
          });
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.slots.compact = compact;
    s.slots.paytable = false;
  }

  @Override
  void advance(DemoSession s) {
    long age = s.tick - s.slots.startedAt;
    if (!s.slots.pending && (age < 0 || s.slotAnimationEnd >= 0 && s.tick >= s.slotAnimationEnd))
      s.slots.startedAt = -1;
  }

  @Override
  MenuView project(DemoSession s) {
    return view(
        "slots",
        SlotView.data(s.slots),
        Map.of(),
        s,
        Map.of(),
        DialogOptions.notice("dui / Demo arcade", "Leave arcade", "slot_close"));
  }

  @Override
  void presented(DemoSession s, Canvas c) {
    if (s.slots.startedAt >= 0 && c.animation != null)
      s.slotAnimationEnd = s.slots.startedAt + c.animation.durationTicks() + 20;
    if (!s.slots.pending && s.slots.startedAt >= 0 && c.animation != null)
      later(
          c.animation.durationTicks() + 20 - (s.tick - s.slots.startedAt),
          () -> s.slots.startedAt = -1);
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    var result = new HashMap<String, Object>();
    result.put("section", "slots");
    result.put("state", s.slots);
    result.put("effect", c.animation);
    result.put("effects", c.effects);
    result.put("focusOutlineHidden", c.hideFocusOutline);
    return result;
  }

  @Override
  void validate(boolean compact) {
    var slots = new SlotState();
    slots.compact = compact;
    services.template("slots").render(SlotView.data(slots));
  }
}
