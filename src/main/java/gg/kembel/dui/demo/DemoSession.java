package gg.kembel.dui.demo;

import gg.kembel.dui.paper.*;
import java.util.*;
import org.bukkit.scheduler.BukkitTask;

/** Per-viewer application state; rendering reads the captured frame only. */
final class DemoSession {
  String section = "components";
  DialogSession ui;
  MenuController<DemoSession> controller;
  boolean acceptanceCompact;
  int acceptancePage;
  long protocolTick;
  boolean protocolMotion = true, protocolPopup;
  CharacterState character = new CharacterState();
  ShowcaseState kit = new ShowcaseState();
  DisplayPreferences display = new DisplayPreferences(), preview;
  ShopState shop = new ShopState();
  RewardState rewards = new RewardState();
  AdventState advent = new AdventState();
  WarpState warps = new WarpState();
  RouletteGame roulette = new RouletteGame();
  BlackjackState blackjack = new BlackjackState();
  PokerState poker = new PokerState();
  SlotState slots = new SlotState();
  final Map<String, ArcadeGame> arcade = new HashMap<>();
  BukkitTask settlement;
  int videoPage;
  boolean videoLoading, videoCompact;

  DemoMenu menu;
  long tick;
  long slotAnimationEnd = -1;
  java.time.LocalDate date;
  String viewerName;
  Map<String, org.bukkit.inventory.ItemStack> items = Map.of();
  ShowcaseMenu.Screen screen = ShowcaseMenu.Screen.SHOWCASE;
  DisplayPreferences setupInitial;
  boolean reopenRequested, videoFetchRequested, videoForce;
  VideoView.Model videoModel;
  YouTubeFeed.Feed videoFeed;
  String videoError = "";
}
