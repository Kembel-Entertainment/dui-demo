package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;

/** Acceptance application: pure projection, typed actions and scoped effects. */
final class AcceptanceMenu extends DemoMenu {
  @Override
  String id() {
    return "acceptance";
  }

  @Override
  List<String> templates() {
    return List.of("acceptance");
  }

  @Override
  Set<String> aliases() {
    return Set.of();
  }

  AcceptanceMenu(DemoServices services) {
    super(services);
    on("acceptance_close", s -> {});
    on(
        "acceptance_page",
        DemoMenu::direction,
        (s, delta) ->
            s.acceptancePage =
                Page.of(AcceptanceView.ITEMS, s.acceptancePage + delta, s.acceptanceCompact ? 3 : 6)
                    .index());
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.acceptanceCompact = compact;
  }

  @Override
  MenuView project(DemoSession s) {
    var data = AcceptanceView.data(s.acceptancePage, s.acceptanceCompact);
    var images = AcceptanceView.images();
    var palette =
        ThemeTokens.DARK.with(Map.of("surface", 0x172438, "raised", 0x233652, "accent", 0xFDBA74));
    return new MenuView(
        services.template("acceptance").render(data, images, palette),
        new ViewModel(data, images, Map.of(), Map.of()),
        DialogOptions.notice("dui / Field journal", "Close", "acceptance_close"));
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    return Map.of("section", "acceptance", "page", s.acceptancePage);
  }

  @Override
  void validate(boolean compact) {
    services
        .template("acceptance")
        .render(AcceptanceView.data(0, compact), AcceptanceView.images());
  }
}
