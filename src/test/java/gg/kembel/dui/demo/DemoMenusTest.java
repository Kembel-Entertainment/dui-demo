package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import gg.kembel.dui.testing.*;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;

/** Consumer tests for the actual menu definitions, not duplicate UI implementations. */
class DemoMenusTest {
  private static final Gson JSON =
      new GsonBuilder()
          .excludeFieldsWithModifiers(Modifier.STATIC)
          .setExclusionStrategies(
              new ExclusionStrategy() {
                public boolean shouldSkipField(FieldAttributes field) {
                  return Set.of(
                          "controller", "ui", "menu", "settlement", "items", "videoModel", "random")
                      .contains(field.getName());
                }

                public boolean shouldSkipClass(Class<?> type) {
                  return false;
                }
              })
          .registerTypeAdapter(
              LocalDate.class,
              (JsonSerializer<LocalDate>) (date, type, ctx) -> new JsonPrimitive(date.toString()))
          .registerTypeAdapter(
              Instant.class,
              (JsonSerializer<Instant>) (date, type, ctx) -> new JsonPrimitive(date.toString()))
          .create();

  private static final class Services implements DemoServices {
    final FakeScheduler scheduler = new FakeScheduler();
    TaskScope view = new TaskScope(scheduler), session = new TaskScope(scheduler);
    final Map<String, MenuTemplate> templates = new HashMap<>();
    VideoProvider provider = new FixtureVideos();
    final List<String> messages = new ArrayList<>();
    boolean projecting, failSave;
    int saves, refreshed, settlements;
    long settlementDelay;
    DemoSession state;
    DemoMenu menu;

    Services() {
      scheduler.advance(100);
    }

    private void external() {
      assertFalse(projecting, "Projection accessed an application service");
    }

    public MenuTemplate template(String name) {
      return templates.computeIfAbsent(
          name,
          id -> {
            try (var stream = DemoMenusTest.class.getResourceAsStream("/ui/" + id + ".html")) {
              return MenuTemplate.parse(
                  new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8),
                  new GlyphFont(),
                  CasinoComponents.registry(),
                  id);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          });
    }

    public String viewerName() {
      external();
      return "DemoViewer";
    }

    public long tick() {
      external();
      return scheduler.tick();
    }

    public LocalDate date() {
      external();
      return LocalDate.of(2026, 9, 30);
    }

    public java.util.random.RandomGenerator random() {
      external();
      return new Random(2);
    }

    public Map<String, ItemStack> viewerItems(Function<Player, Map<String, ItemStack>> factory) {
      external();
      return Map.of();
    }

    public void message(String text) {
      external();
      messages.add(text);
    }

    public void save(String collection, Object state) {
      external();
      if (failSave) throw new IllegalStateException("Disk unavailable");
      saves++;
    }

    public SlotState copySlots(SlotState value) {
      external();
      var result = new Gson().fromJson(new Gson().toJson(value), SlotState.class);
      result.compact = value.compact;
      result.paytable = value.paytable;
      result.startedAt = value.startedAt;
      return result;
    }

    public void settleAfter(long ticks) {
      external();
      settlements++;
      settlementDelay = ticks;
    }

    public VideoProvider videos() {
      external();
      return provider;
    }

    public TaskScope viewTasks() {
      external();
      return view;
    }

    public TaskScope tasks() {
      external();
      return session;
    }

    public void refresh() {
      external();
      refreshed++;
      menu.capture(state);
    }

    MenuView project() {
      projecting = true;
      try {
        return menu.project(state);
      } finally {
        projecting = false;
      }
    }

    void click(String action, String value) {
      state.tick = tick();
      state.date = date();
      menu.dispatch(state, action, value, null);
      menu.capture(state);
    }

    void close() {
      view.close();
      session.close();
      menu.closed(state);
    }
  }

  private Services open(Function<DemoServices, DemoMenu> factory, boolean compact) {
    var services = new Services();
    var state = new DemoSession();
    state.display = DisplayPreferences.selection("2", compact ? "compact" : "spacious");
    state.kit.layout = state.display.layout;
    services.state = state;
    services.menu = factory.apply(services);
    state.menu = services.menu;
    state.section = services.menu.id();
    services.menu.prepare(state, compact);
    services.menu.capture(state);
    return services;
  }

  private void assertProjection(Services services) {
    String before = JSON.toJson(services.state);
    var first = services.project();
    var second = services.project();
    assertEquals(before, JSON.toJson(services.state), "Rendering mutated application state");
    assertEquals(first.canvas().hits, second.canvas().hits);
    assertEquals(first.canvas().paints, second.canvas().paints);
    RenderAssertions.visibleHits(first.canvas());
    RenderAssertions.budget(first.canvas());
    for (var hit : first.canvas().hits) {
      if (!hit.action().isBlank() && !first.model().links().containsKey(hit.id()))
        assertTrue(
            services.menu.actionIds().contains(hit.action()),
            "Unregistered action: " + hit.action());
    }
    for (var button : first.options().buttons())
      assertTrue(services.menu.actionIds().contains(button.action()));
    if (first.options().exit() != null)
      assertTrue(services.menu.actionIds().contains(first.options().exit().action()));
  }

  @TestFactory
  Stream<DynamicTest> everyMenuProjectsWithoutMutatingStateOrCallingServices() {
    return DemoMenus.factories().stream()
        .flatMap(
            factory ->
                Stream.of(false, true)
                    .map(
                        compact -> {
                          var services = open(factory, compact);
                          return DynamicTest.dynamicTest(
                              services.menu.id() + " / " + (compact ? "compact" : "spacious"),
                              () -> assertProjection(services));
                        }));
  }

  @Test
  void everyRegisteredTemplateValidatesBothProfiles() {
    var ids = new HashSet<String>();
    for (var factory : DemoMenus.factories()) {
      var services = open(factory, false);
      var menu = services.menu;
      assertTrue(ids.add(menu.id()));
      menu.validate(false);
      menu.validate(true);
    }
    assertEquals(16, ids.size());
  }

  @Test
  void newArcadeMenusCancelClosedTimersAndSettleOneRoundOnly() {
    for (var game : ArcadeMenu.Game.values()) {
      var services = open(ports -> new ArcadeMenu(ports, game), false);
      var state = services.state.arcade.get(game.id);
      state.help = true;
      services.click("arcade_play", "");
      assertFalse(state.help, "Play from Rules must expose the illustration");
      assertTrue(state.pending);
      services.menu.presented(services.state, services.project().canvas());
      services.close();
      assertFalse(state.pending);
      long balance = state.credits;
      services.scheduler.advance(500);
      assertEquals(0, services.refreshed, "Closed animation reopened a menu");
      assertEquals(1, state.rounds);
      assertEquals(balance, state.credits);
    }
  }

  @Test
  void everyShowcasePageAndDropdownHasRegisteredActionsAndPureData() {
    for (boolean compact : List.of(false, true)) {
      var services = open(ShowcaseMenu::new, compact);
      for (var page : ShowcaseState.PAGES) {
        services.click("kit_page", page.id());
        for (int part = 0; part < (compact ? services.state.kit.partCount() : 1); part++) {
          services.state.kit.part = part;
          assertProjection(services);
          services.state.kit.dropdownOpen = true;
          assertProjection(services);
          services.state.kit.dropdownOpen = false;
        }
      }
      services.state.kit.part = 123;
      assertProjection(services);
      assertEquals(123, services.state.kit.part, "Projection must not normalise stored state");
    }
  }

  @Test
  void unknownActionsAndMalformedPayloadsDoNotChangeStateOrSave() {
    var malformed =
        Map.of(
            "components",
            "kit_page_cycle",
            "shop",
            "shop_product_page",
            "rewards",
            "reward_select",
            "advent",
            "advent_open",
            "warps",
            "warp_jump",
            "blackjack",
            "blackjack_page",
            "roulette",
            "roulette_chip",
            "acceptance",
            "acceptance_page");
    for (var factory : DemoMenus.factories()) {
      var services = open(factory, false);
      String before = JSON.toJson(services.state);
      services.menu.dispatch(services.state, "not-a-route", "", null);
      assertEquals(before, JSON.toJson(services.state));
      if (malformed.containsKey(services.menu.id())) {
        services.menu.dispatch(services.state, malformed.get(services.menu.id()), "invalid", null);
        assertEquals(before, JSON.toJson(services.state));
      }
      assertEquals(0, services.saves);
      assertFalse(services.messages.isEmpty());
    }
  }

  @Test
  void animatedProjectionsLeaveTransitionsToActionsAndScopedJobs() {
    var actions =
        Map.of(
            "advent",
            "advent_open",
            "blackjack",
            "blackjack_demo",
            "poker",
            "poker_showcase",
            "roulette",
            "roulette_spin",
            "slots",
            "slot_spin");
    for (var factory : DemoMenus.factories()) {
      var services = open(factory, false);
      String action = actions.get(services.menu.id());
      if (action == null) continue;
      if (services.menu.id().equals("roulette")) services.click("roulette_bet", "n:17");
      services.click(action, action.equals("advent_open") ? "24" : "");
      assertProjection(services);
      services.menu.presented(services.state, services.project().canvas());
      services.close();
      services.scheduler.advance(200);
      assertEquals(0, services.refreshed, "Closed menu timer reopened its view");
    }
  }

  @Test
  void giftAnimationCompletesThroughTheScopedControllerRefresh() {
    var services = open(AdventMenu::new, false);
    services.click("advent_open", "24");
    services.menu.presented(services.state, services.project().canvas());
    assertEquals(AdventState.Phase.OPENING, services.state.advent.phase);
    services.scheduler.advance(AdventState.OPEN_TICKS);
    assertEquals(AdventState.Phase.REVEALED, services.state.advent.phase);
    assertEquals(1, services.refreshed);
    assertProjection(services);
  }

  @Test
  void rewardAndSlotLedgersAreActionOwnedAndFailedReservationKeepsPreviousBalance() {
    var rewards = open(RewardsMenu::new, false);
    rewards.click("reward_claim", "");
    assertEquals(50, rewards.state.rewards.stars);
    assertEquals(1, rewards.saves);
    assertProjection(rewards);
    assertEquals(1, rewards.saves);
    var slots = open(SlotsMenu::new, false);
    String before = JSON.toJson(slots.state.slots);
    slots.failSave = true;
    assertThrows(IllegalStateException.class, () -> slots.click("slot_spin", ""));
    assertEquals(before, JSON.toJson(slots.state.slots));
    assertEquals(0, slots.settlements);
    slots.failSave = false;
    slots.click("slot_spin", "");
    assertTrue(slots.state.slots.pending);
    assertEquals(975, slots.state.slots.chips);
    assertEquals(1, slots.settlements);
    assertEquals(SlotState.SPIN_TICKS, slots.settlementDelay);
    slots.close();
    assertTrue(slots.state.slots.pending, "UI close cannot erase application-owned settlement");
  }

  @Test
  void lateVideoCompletionCannotRefreshAClosedMenu() {
    var services = open(VideosMenu::new, false);
    var fixture = new FixtureVideos();
    var pending = new CompletableFuture<YouTubeFeed.Feed>();
    services.provider =
        new VideoProvider() {
          public CompletableFuture<YouTubeFeed.Feed> refresh(boolean force) {
            return pending;
          }

          public CompletableFuture<RasterImage> thumbnail(YouTubeFeed.Video video) {
            return CompletableFuture.completedFuture(fixture.cached(video));
          }

          public YouTubeFeed.Feed feed() {
            return fixture.feed();
          }

          public RasterImage cached(YouTubeFeed.Video video) {
            return fixture.cached(video);
          }

          public String error() {
            return "";
          }

          public void close() {}
        };
    services.menu.presented(services.state, services.project().canvas());
    services.close();
    pending.complete(fixture.feed());
    services.scheduler.drain();
    assertEquals(0, services.refreshed);
    assertFalse(services.state.videoLoading);
  }

  @Test
  void warpResourceKeysCoverEveryNativeCarouselCarrierInBothDirections() {
    for (boolean compact : List.of(false, true)) {
      var services = open(WarpsMenu::new, compact);
      var keys = new HashSet<String>();
      for (int i = 0; i < WarpState.DESTINATIONS.size(); i++) keys.add(WarpItems.key(i));
      for (int index = 0; index < WarpState.DESTINATIONS.size(); index++) {
        services.state.warps.jump(index);
        for (int direction : List.of(-1, 1)) {
          services.state.warps.step(direction, services.tick());
          services.menu.capture(services.state);
          for (var item : services.project().canvas().items)
            assertTrue(
                keys.contains(item.id()),
                "Native carousel carrier missing a resource: " + item.id());
          services.state.warps.stop();
        }
      }
    }
  }

  @Test
  void incompleteNativeInputsKeepThePreviousFormAndLayout() {
    var services = open(ShowcaseMenu::new, false);
    ((ShowcaseMenu) services.menu).setup(services.state);
    String before = JSON.toJson(services.state);
    services.menu.dispatch(services.state, "kit_display_compact", "", null);
    assertEquals(before, JSON.toJson(services.state));
    services.state.screen = ShowcaseMenu.Screen.FORM;
    before = JSON.toJson(services.state);
    services.menu.dispatch(services.state, "kit_save", "", null);
    assertEquals(before, JSON.toJson(services.state));
    services.menu.dispatch(services.state, "kit_page", null, null);
    assertEquals(before, JSON.toJson(services.state));
    assertEquals(0, services.saves);
  }

  @Test
  void payoutParticlesRetainTheirActualCanvasLifetimeAfterSlotSettlement() {
    var services = open(SlotsMenu::new, false);
    services.state.slots.modeIndex = 1;
    services.click("slot_spin", "");
    var canvas = services.project().canvas();
    services.menu.presented(services.state, canvas);
    long deadline = services.state.slotAnimationEnd;
    assertTrue(deadline > services.state.slots.startedAt + SlotState.SPIN_TICKS + 20);
    services.scheduler.advance(SlotState.SPIN_TICKS);
    services.state.slots.settle();
    services.menu.capture(services.state);
    assertTrue(services.state.slots.startedAt >= 0);
    services.scheduler.advance(deadline - services.scheduler.tick());
    services.menu.capture(services.state);
    assertEquals(-1, services.state.slots.startedAt);
  }

  @Test
  void dayRolloverAndPartNormalisationAreNeverRenderingSideEffects() {
    var reward = new RewardState();
    reward.completed = 7;
    reward.stars = 695;
    reward.lastClaim = "2026-09-29";
    String before = JSON.toJson(reward);
    RewardView.data(reward, LocalDate.of(2026, 9, 30));
    assertEquals(before, JSON.toJson(reward));
    var services = open(RewardsMenu::new, false);
    services.state.rewards = reward;
    services.menu.capture(services.state);
    assertEquals(0, reward.completed);
    assertProjection(services);
  }
}
