package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;

/** Warps application: pure projection, typed actions and scoped effects. */
final class WarpsMenu extends DemoMenu {
  @Override
  String id() {
    return "warps";
  }

  @Override
  List<String> templates() {
    return List.of("warps");
  }

  @Override
  Set<String> aliases() {
    return Set.of();
  }

  WarpsMenu(DemoServices services) {
    super(services);
    on("warp_close", s -> {});
    on("warp_next", s -> s.warps.step(1, s.tick));
    on("warp_previous", s -> s.warps.step(-1, s.tick));
    on(
        "warp_select",
        Integer::parseInt,
        (s, slot) -> {
          if (s.warps.moving) return;
          if (slot < 0 || slot > 3) {
            reject("warp_select");
            return;
          }
          if (slot == 0) s.warps.preview(s.tick);
          else s.warps.step(slot, s.tick);
        });
    on(
        "warp_jump",
        Integer::parseInt,
        (s, target) -> {
          if (s.warps.moving) return;
          if (target < 0 || target >= WarpState.DESTINATIONS.size()) {
            reject("warp_jump");
            return;
          }
          int delta = Math.floorMod(target - s.warps.selected, 4);
          if (delta == 1) s.warps.step(1, s.tick);
          else if (delta == 3) s.warps.step(-1, s.tick);
          else s.warps.jump(target);
        });
    on(
        "warp_travel",
        s -> {
          if (!s.warps.moving) s.warps.preview(s.tick);
        });
    on(
        "warp_size",
        s -> {
          s.warps.stop();
          s.warps.compact = !s.warps.compact;
        });
    on(
        "warp_motion",
        s -> {
          s.warps.motion = !s.warps.motion;
          s.warps.stop();
        });
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.warps.compact = compact;
  }

  @Override
  void advance(DemoSession s) {
    s.warps.finish(s.warps.generation, s.tick);
  }

  @Override
  Map<String, org.bukkit.inventory.ItemStack> captureItems(DemoSession s) {
    return services.viewerItems(p -> WarpItems.stacks(s.warps));
  }

  @Override
  MenuView project(DemoSession s) {
    return view(
        "warps",
        WarpView.data(s.warps),
        Map.of(),
        s,
        Map.of(),
        DialogOptions.notice("dui / Wayfarer atlas", "Close atlas", "warp_close"));
  }

  @Override
  void presented(DemoSession s, Canvas c) {
    if (s.warps.startedAt >= 0)
      later(WarpState.SLIDE_TICKS - (s.tick - s.warps.startedAt), s.warps::stop);
  }

  @Override
  void closed(DemoSession s) {
    s.warps.stop();
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    return Map.of(
        "section",
        "warps",
        "state",
        s.warps,
        "selected",
        s.warps.selected,
        "moving",
        s.warps.moving,
        "arrived",
        s.warps.arrived,
        "clips",
        c.clips);
  }

  @Override
  void validate(boolean compact) {
    var warps = new WarpState();
    warps.compact = compact;
    for (int i = 0; i < 4; i++) {
      warps.jump(i);
      services.template("warps").render(WarpView.data(warps));
      warps.step(1, 100);
      services.template("warps").render(WarpView.data(warps));
      warps.stop();
      warps.step(-1, 100);
      services.template("warps").render(WarpView.data(warps));
      warps.stop();
    }
  }
}
