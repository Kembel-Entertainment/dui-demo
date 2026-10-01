package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;

/** Protocol application: pure projection, typed actions and scoped effects. */
final class ProtocolMenu extends DemoMenu {
  @Override
  String id() {
    return "protocol";
  }

  @Override
  List<String> templates() {
    return List.of();
  }

  @Override
  Set<String> aliases() {
    return Set.of();
  }

  ProtocolMenu(DemoServices services) {
    super(services);
    on("protocol_close", s -> {});
    on("protocol_animate", s -> s.protocolTick = s.tick);
    on("protocol_motion", s -> s.protocolMotion = !s.protocolMotion);
    on("protocol_popup", s -> s.protocolPopup = !s.protocolPopup);
    on("protocol_dismiss", s -> s.protocolPopup = false);
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.protocolTick = 0;
    s.protocolPopup = false;
  }

  @Override
  void advance(DemoSession s) {
    if (s.protocolTick == 0) s.protocolTick = s.tick;
  }

  @Override
  Map<String, org.bukkit.inventory.ItemStack> captureItems(DemoSession s) {
    return services.viewerItems(
        p -> Map.of("native", new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRASS_BLOCK)));
  }

  @Override
  MenuView project(DemoSession s) {
    return new MenuView(
        ProtocolView.render(s.protocolTick, s.protocolMotion, s.protocolPopup),
        new ViewModel(Map.of(), Map.of(), s.items, Map.of()),
        DialogOptions.notice("dui / Protocol lab", "Close", "protocol_close"));
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    return Map.of(
        "section",
        "protocol",
        "motion",
        s.protocolMotion,
        "popup",
        s.protocolPopup,
        "tick",
        s.protocolTick);
  }

  @Override
  void validate(boolean compact) {
    ProtocolView.render(100, true, false);
  }
}
