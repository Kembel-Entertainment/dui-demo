package gg.kembel.dui.demo;

import java.util.*;
import java.util.function.Function;

/** Single consumer-owned registration list, shared by the plugin and portable menu tests. */
final class DemoMenus {
  private DemoMenus() {}

  static List<Function<DemoServices, DemoMenu>> factories() {
    return List.of(
        ShowcaseMenu::new,
        ShopMenu::new,
        RewardsMenu::new,
        AdventMenu::new,
        WarpsMenu::new,
        PokerMenu::new,
        RouletteMenu::new,
        BlackjackMenu::new,
        SlotsMenu::new,
        VideosMenu::new,
        AcceptanceMenu::new,
        ProtocolMenu::new);
  }
}
