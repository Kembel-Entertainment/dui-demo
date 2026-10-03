package gg.kembel.dui.demo.e2e;

import net.fabricmc.api.ClientModInitializer;

public final class ScenarioClient implements ClientModInitializer {
  public void onInitializeClient() {
    switch (System.getProperty("dui.e2e.scenario", "showcase")) {
      case "cinema" -> new CinemaClient().initialize();
      case "gba" -> new GbaClient().initialize();
      case "dynamic" -> new DynamicClient().initialize();
      case "map-observer" -> new MapObserverClient().initialize();
      case "map" -> new MapClient().initialize();
      case "character" -> new CharacterClient().initialize();
      case "casino" -> new CasinoClient().initialize();
      case "showcase" -> new ShowcaseClient().initialize();
      case "shop" -> new ShopClient().initialize();
      case "blackjack" -> new BlackjackClient().initialize();
      case "roulette" -> new RouletteClient().initialize();
      case "poker" -> new PokerClient().initialize();
      case "warps" -> new WarpClient().initialize();
      case "advent" -> new AdventClient().initialize();
      case "rewards" -> new RewardClient().initialize();
      case "slots" -> new SlotClient().initialize();
      case "protocol" -> new ProtocolClient().initialize();
      case "confetti" -> new ConfettiClient().initialize();
      case "videos" -> new VideoClient().initialize();
      default -> throw new IllegalArgumentException("Unknown dui test scenario");
    }
  }
}
