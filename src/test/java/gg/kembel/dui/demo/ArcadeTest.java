package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import gg.kembel.dui.testing.RenderAssertions;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

class ArcadeTest {
  private static Random ticket(int ticket) {
    return new Random(1) {
      @Override
      public int nextInt(int bound) {
        return Math.floorMod(ticket, bound);
      }
    };
  }

  @Test
  void horseOddsCoverEveryTicketAndPayTheAdvertisedTotalReturn() {
    int[] expected = {0, 0, 0, 0, 1, 1, 1, 2, 2, 3};
    for (int i = 0; i < expected.length; i++) {
      var g = new HorseRaceGame();
      g.motion = false;
      g.select(expected[i]);
      assertTrue(g.play(ticket(i), 100));
      assertEquals(expected[i], g.winner);
      assertEquals(50L * HorseRaceGame.RETURNS[expected[i]], g.returned);
      assertEquals(4950 + g.returned, g.credits);
      assertFalse(g.finish(999, true));
    }
  }

  @Test
  void everyWheelSectorPaysItsDisplayedMultiplier() {
    int[] returns = {0, 1, 2, 0, 3, 1, 5, 10};
    for (int i = 0; i < 8; i++) {
      var g = new PrizeWheelGame();
      g.motion = false;
      g.play(ticket(i), 100);
      assertEquals(i, g.sector);
      assertEquals(50L * returns[i], g.returned);
      assertEquals(4950 + g.returned, g.credits);
    }
  }

  @Test
  void coinCallsPayExactlyTwiceOrZeroForEitherSide() {
    for (int face = 0; face < 2; face++)
      for (int call = 0; call < 2; call++) {
        var g = new CoinflipGame();
        g.motion = false;
        g.select(call);
        g.play(ticket(face), 100);
        assertEquals(call == face ? 100 : 0, g.returned);
        assertEquals(call == face ? 5050 : 4950, g.credits);
      }
    assertThrows(IllegalArgumentException.class, () -> new CoinflipGame().select(2));
  }

  @Test
  void pendingRoundsRejectDuplicateSpinsBetChangesAndReset() {
    for (var kind : ArcadeMenu.Game.values()) {
      var g = kind.fresh();
      assertTrue(g.play(new Random(2), 100));
      long reserved = g.credits;
      int wager = g.wager();
      assertFalse(g.play(new Random(3), 101));
      g.bet(1);
      g.reset();
      assertEquals(reserved, g.credits);
      assertEquals(wager, g.wager());
      assertFalse(g.finish(100 + g.duration() - 1, false));
      g.compact = true;
      assertEquals(100, g.startedAt);
      assertTrue(g.finish(100 + g.duration(), false));
      long balance = g.credits;
      assertFalse(g.finish(999, true));
      assertEquals(balance, g.credits);
    }
  }

  @Test
  void disablingMotionSettlesOnceAndInsufficientFundsNeverDrawARound() {
    var g = new CoinflipGame();
    g.play(ticket(0), 100);
    g.toggleMotion(102);
    assertFalse(g.pending);
    assertEquals(5050, g.credits);
    assertEquals(1, g.rounds);
    assertFalse(g.finish(1000, true));
    g.credits = 0;
    long started = g.startedAt;
    assertFalse(g.play(ticket(1), 200));
    assertEquals(started, g.startedAt);
  }

  @Test
  void wildSubstitutionRequiresLeftToRightThreeAndChoosesTheBestMatch() {
    assertEquals(5, TempleSlotsGame.lineMultiplier(new int[] {0, 8, 0, 2, 3}));
    assertEquals(0, TempleSlotsGame.lineMultiplier(new int[] {0, 2, 0, 0, 0}));
    assertEquals(300, TempleSlotsGame.lineMultiplier(new int[] {8, 8, 7, 7, 7}));
    assertEquals(500, TempleSlotsGame.lineMultiplier(new int[] {8, 8, 8, 8, 8}));
    assertThrows(
        IllegalArgumentException.class,
        () -> TempleSlotsGame.lineMultiplier(new int[] {0, 9, 0, 0, 0}));
  }

  @Test
  void explicitBonusPreviewNeverOverwritesAnActiveRound() {
    var game = new TempleSlotsGame();
    game.play(ticket(0), 100);
    game.demoBonus();
    assertEquals(0, game.freeSpins);
    game.finish(240, false);
    long credits = game.credits;
    game.demoBonus();
    assertEquals(10, game.freeSpins);
    assertEquals(6, game.expanding);
    assertEquals(credits, game.credits);
  }

  @Test
  void tenBookLinesPlusScatterAwardFreeSpinsAndKeepTheOriginalWager() {
    var g = new TempleSlotsGame();
    g.motion = false;
    g.play(ticket(101), 100);
    assertEquals(27500, g.returned);
    assertEquals(10, g.freeSpins);
    assertEquals(5, g.expanding);
    assertEquals(10, g.winningLines.size());
    int wager = g.wager();
    g.bet(1);
    assertEquals(wager, g.wager());
    long before = g.credits;
    g.play(ticket(83), 200);
    assertEquals(9, g.freeSpins);
    assertEquals(5, g.countExpanded());
    assertEquals(10000, g.returned);
    assertEquals(before + 10000, g.credits, "Free round debited a wager");
    g.play(ticket(101), 300);
    assertEquals(18, g.freeSpins, "Book retrigger should consume one then add ten");
  }

  @Test
  void allTemplatesRenderIdlePendingResultAndRulesWithinPublicBudgets() throws Exception {
    for (var game : ArcadeMenu.Game.values())
      for (boolean compact : List.of(false, true)) {
        String source;
        try (var in = getClass().getResourceAsStream("/ui/" + game.id + ".html")) {
          source = new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        }
        var template =
            MenuTemplate.parse(source, new GlyphFont(), CasinoComponents.registry(), game.id);
        var state = game.fresh();
        state.compact = compact;
        for (int phase = 0; phase < 4; phase++) {
          if (phase == 1) state.play(new Random(42), 100);
          if (phase == 2) state.finish(100 + state.duration(), false);
          if (phase == 3) state.help = true;
          var canvas = template.render(ArcadeView.data(game, state));
          RenderAssertions.budget(canvas);
          RenderAssertions.visibleHits(canvas);
          assertTrue(canvas.hideFocusOutline);
          assertEquals(compact ? 320 : 480, canvas.width);
          if (phase < 3) assertEquals(game == ArcadeMenu.Game.BOOK ? 5 : 1, canvas.effects.size());
          else assertTrue(canvas.effects.isEmpty());
          for (var e : canvas.effects) assertInstanceOf(ShaderEffect.Extension.class, e.kind());
        }
      }
  }
}
