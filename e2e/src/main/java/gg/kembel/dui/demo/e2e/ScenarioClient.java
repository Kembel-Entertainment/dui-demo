package gg.kembel.dui.demo.e2e;

import net.fabricmc.api.ClientModInitializer;

public final class ScenarioClient implements ClientModInitializer {
  public void onInitializeClient() {
    switch (System.getProperty("dui.e2e.scenario", "showcase")) {
      case "showcase" -> new ShowcaseClient().initialize();
      case "shop" -> new ShopClient().initialize();
      case "roulette" -> new RouletteClient().initialize();
      case "poker" -> new PokerClient().initialize();
      case "warps" -> new WarpClient().initialize();
      case "advent" -> new AdventClient().initialize();
      case "rewards" -> new RewardClient().initialize();
      case "slots" -> new SlotClient().initialize();
      case "confetti" -> new ConfettiClient().initialize();
      case "videos" -> new VideoClient().initialize();
      default -> throw new IllegalArgumentException("Unknown dui test scenario");
    }
  }
}
