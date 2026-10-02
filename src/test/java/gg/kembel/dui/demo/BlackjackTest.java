package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class BlackjackTest {
  private BlackjackGame game(int bet, Integer... cards) {
    var g = new BlackjackGame();
    g.deal(bet, new Random(1), List.of(cards));
    g.finishDeal();
    return g;
  }

  private void settle(BlackjackGame g) {
    g.reveal();
    for (int i = 0; g.phase == BlackjackGame.Phase.DEALER && i < 80; i++) g.dealerStep();
    assertEquals(BlackjackGame.Phase.RESULT, g.phase);
  }

  @Test
  void acesNaturalAndExactHalfCreditMoney() {
    assertEquals("-0.5", BlackjackGame.credits(-1));
    assertEquals(12, BlackjackGame.score(List.of(12, 25)));
    assertTrue(BlackjackGame.soft(List.of(12, 25)));
    assertEquals(21, BlackjackGame.score(List.of(12, 25, 7)));
    assertTrue(BlackjackGame.soft(List.of(12, 25, 7)));
    var g = game(5, 38, 7, 50, 18);
    assertEquals(BlackjackGame.Phase.REVEAL, g.phase);
    settle(g);
    assertEquals(10015, g.balanceHalf);
    assertEquals(25, g.returnedHalf);
    g.dealerStep();
    assertEquals(10015, g.balanceHalf);
    var both = game(25, 38, 12, 50, 24);
    settle(both);
    assertEquals(10000, both.balanceHalf);
    assertEquals("PUSH", both.current().outcome);
    var peek = game(25, 8, 12, 7, 24);
    assertEquals(BlackjackGame.Phase.REVEAL, peek.phase);
    assertFalse(peek.canDouble());
    settle(peek);
    assertEquals(9950, peek.balanceHalf);
  }

  @Test
  void splitDoubleAfterSplitAndDealerBustPayEachHandOnce() {
    var g = game(25, 6, 17, 45, 34, 1, 13, 29, 5, 43);
    g.split();
    assertEquals(2, g.hands.size());
    assertEquals(11, g.current().total());
    g.doubleDown();
    assertEquals(1, g.active);
    g.hit();
    g.stand();
    settle(g);
    assertEquals(10150, g.balanceHalf);
    assertEquals(300, g.returnedHalf);
    assertEquals(150, g.netHalf);
    assertEquals(1, g.rounds);
    g.dealerStep();
    assertEquals(10150, g.balanceHalf);
  }

  @Test
  void softSeventeenPushBustAndInvalidMovesAreAtomic() {
    var g = game(25, 8, 51, 32, 4);
    g.stand();
    settle(g);
    assertEquals(308, g.remaining());
    assertEquals(2, g.visibleDealer().size());
    assertEquals(10050, g.balanceHalf);
    var push = game(25, 21, 33, 5, 45);
    push.stand();
    settle(push);
    assertEquals(10000, push.balanceHalf);
    var bust = game(25, 10, 46, 30, 18, 37);
    bust.hit();
    settle(bust);
    assertEquals(9950, bust.balanceHalf);
    assertEquals(2, bust.visibleDealer().size());
    var invalid = game(25, 8, 5, 7, 6);
    int money = invalid.balanceHalf;
    assertThrows(IllegalArgumentException.class, invalid::split);
    assertEquals(money, invalid.balanceHalf);
    invalid.balanceHalf = 0;
    assertFalse(invalid.canDouble());
    assertThrows(IllegalArgumentException.class, invalid::doubleDown);
    assertEquals(2, invalid.current().cards.size());
  }

  @Test
  void splitAcesOneCardAndSplitTwentyOneIsOrdinaryWin() {
    var g = game(25, 12, 7, 25, 5, 8, 21, 0);
    g.split();
    assertEquals(BlackjackGame.Phase.REVEAL, g.phase);
    assertTrue(g.hands.stream().allMatch(h -> h.cards.size() == 2 && h.done && !h.natural()));
    settle(g);
    assertEquals(10100, g.balanceHalf);
    assertEquals(200, g.returnedHalf);
  }

  @Test
  void fourHandsAndNoMoreResplits() {
    var g = game(25, 6, 8, 19, 7, 32, 45, 6, 19, 32, 45);
    g.split();
    g.split();
    g.split();
    assertEquals(4, g.hands.size());
    assertFalse(g.canSplit());
    assertThrows(IllegalArgumentException.class, g::split);
    while (g.phase == BlackjackGame.Phase.PLAYER) g.stand();
    settle(g);
    assertEquals(1, g.rounds);
  }

  @Test
  void animationsGuardMoneyMotionAndUsePublicCardsInBothTemplates() {
    for (boolean compact : List.of(false, true)) {
      var s = new BlackjackState();
      s.compact = compact;
      s.deal(100, true, new Random(1));
      var cv = BlackjackView.render(s);
      assertEquals(4, cv.effects.size());
      assertEquals(1, cv.items.size());
      assertTrue(
          cv.images.stream().mapToInt(i -> i.raster().width * i.raster().height).sum() <= 16384);
      assertEquals(
          63,
          (Integer)
                  cv.effects.stream()
                      .filter(e -> e.id().equals("dealer_1"))
                      .findFirst()
                      .orElseThrow()
                      .parameters()
                      .get("a")
              & 63);
      assertEquals(3, (DemoShaders.a(cv.effects.getFirst()) >> 13) & 3);
      s.finish(153);
      assertTrue(s.busy());
      s.finish(154);
      assertFalse(s.busy());
      s.move("split", 200);
      assertDoesNotThrow(() -> BlackjackView.render(s));
      s.finish(244);
      s.move("double", 250);
      s.finish(278);
      s.move("hit", 290);
      s.finish(318);
      s.move("stand", 320);
      assertDoesNotThrow(() -> BlackjackView.render(s));
      s.toggleMotion(321);
      assertFalse(s.busy());
      assertEquals(10150, s.game.balanceHalf);
      s.finish(1000);
      assertEquals(10150, s.game.balanceHalf);
      assertDoesNotThrow(() -> BlackjackView.render(s));
      s.reset();
      s.finish(2000);
      assertEquals(10000, s.game.balanceHalf);
    }
  }

  @Test
  void longHandsPaginateInsteadOfExceedingEightEffects() {
    var g = game(25, 0, 8, 13, 7, 26, 39, 0, 13, 26);
    for (int i = 0; i < 5; i++) g.hit();
    var s = new BlackjackState();
    s.game = g;
    s.heroPage = 3;
    for (boolean c : List.of(false, true)) {
      s.compact = c;
      var cv = BlackjackView.render(s);
      assertEquals(8, cv.effects.size());
      assertTrue(cv.hits.stream().anyMatch(h -> h.id().equals("blackjack_hero_prev")));
    }
  }

  @Test
  void dealerUsesFreeSlotsAndPaginatesOnlyAtTheActualBudget() {
    for (boolean compact : List.of(false, true)) {
      for (List<Integer> shoe :
          List.of(List.of(8, 2, 34, 15, 28, 4), List.of(8, 0, 34, 13, 26, 39, 1, 4))) {
        var g = game(25, shoe.toArray(Integer[]::new));
        g.stand();
        settle(g);
        var s = new BlackjackState();
        s.game = g;
        s.compact = compact;
        var cv = BlackjackView.render(s);
        assertEquals(g.visibleDealer().size() + 2, cv.effects.size());
        assertEquals(
            g.visibleDealer().size(),
            cv.effects.stream().filter(e -> e.id().startsWith("dealer_")).count());
        assertFalse(cv.hits.stream().anyMatch(h -> h.id().startsWith("blackjack_dealer_")));
        s.event = "payout";
        s.dealerPage = Math.max(0, g.visibleDealer().size() - s.dealerLimit());
        cv = BlackjackView.render(s);
        assertTrue(cv.effects.size() <= 8);
        assertEquals(
            1,
            cv.effects.stream()
                .filter(e -> e.shader().equals(DemoShaders.spec(DemoShaders.Kind.CHIP_STACK)))
                .count());
        assertEquals(Math.min(5, g.visibleDealer().size()), s.dealerLimit());
        if (g.visibleDealer().size() > 5) {
          assertTrue(cv.effects.stream().anyMatch(e -> e.id().equals("dealer_5")));
          assertTrue(
              cv.hits.stream()
                  .anyMatch(h -> h.id().equals("blackjack_dealer_prev") && !h.action().isEmpty()));
        }
      }
    }
  }

  @Test
  void hundredsOfShuffledHandsFinishAndConserveTheLedger() {
    var g = new BlackjackGame();
    var rng = new Random(42);
    for (int i = 0; i < 600; i++) {
      if (g.balanceHalf < 10) g.balanceHalf = 10000;
      int before = g.balanceHalf;
      g.deal(5, rng);
      g.finishDeal();
      assertEquals(List.of(g.visibleDealer().getFirst(), -1), g.visibleDealer());
      while (g.phase == BlackjackGame.Phase.PLAYER) {
        if (g.current().total() < 16) g.hit();
        else g.stand();
      }
      settle(g);
      assertEquals(before + g.netHalf, g.balanceHalf);
      assertTrue(g.hands.stream().allMatch(h -> h.cards.stream().allMatch(c -> c >= 0 && c < 52)));
      assertEquals(i + 1, g.rounds);
    }
  }
}
