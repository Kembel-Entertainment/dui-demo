package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PokerTest {
  private static List<Integer> cards(String text) {
    var result = new ArrayList<Integer>();
    for (String s : text.split(" "))
      result.add(
          PokerHand.card("23456789TJQKA".indexOf(s.charAt(0)) + 2, "CDHS".indexOf(s.charAt(1))));
    return result;
  }

  @Test
  void everyCategoryWheelAndBestSevenAreRankedCorrectly() {
    String[] examples = {
      "AC KD 9H 7C 3D",
      "AC AD 9H 7C 3D",
      "AC AD 9H 9C 3D",
      "AC AD AH 7C 3D",
      "AC 2D 3H 4C 5D",
      "AC JC 9C 7C 3C",
      "AC AD AH 7C 7D",
      "AC AD AH AS 3D",
      "AH KH QH JH TH"
    };
    long last = -1;
    for (int i = 0; i < examples.length; i++) {
      var hand = PokerHand.evaluate(cards(examples[i]));
      assertEquals(i, hand.category());
      assertTrue(hand.score() > last);
      last = hand.score();
    }
    assertEquals(5, PokerHand.evaluate(cards(examples[4])).high());
    assertEquals("Royal flush", PokerHand.evaluate(cards("AH KH QH JH TH 2C 3D")).name());
    assertTrue(
        PokerHand.evaluate(cards("AC AD KC QD 9C")).score()
            > PokerHand.evaluate(cards("AS AH KS JD 9D")).score());
    assertThrows(IllegalArgumentException.class, () -> PokerHand.evaluate(cards("AC AC KC QD 9C")));
  }

  @Test
  void outOfTurnAndInvalidRaisesDoNotMutateStateAndShortAllInDoesNotReopen() {
    var g = new HoldemGame(new int[] {1000, 1000, 55, 1000});
    g.start(new Random(2), false);
    assertEquals(3, g.actor);
    int total = g.pot();
    assertThrows(IllegalArgumentException.class, () -> g.act(0, HoldemGame.Move.RAISE, 40));
    assertEquals(total, g.pot());
    assertThrows(IllegalArgumentException.class, () -> g.act(3, HoldemGame.Move.RAISE, 39));
    assertEquals(total, g.pot());
    g.act(3, HoldemGame.Move.RAISE, 40);
    g.act(0, HoldemGame.Move.CHECK_CALL, 0);
    g.act(1, HoldemGame.Move.CHECK_CALL, 0);
    g.act(2, HoldemGame.Move.ALL_IN, 0);
    assertEquals(3, g.actor);
    assertEquals(15, g.owed(3));
    assertFalse(g.canRaise(3));
    assertThrows(IllegalArgumentException.class, () -> g.act(3, HoldemGame.Move.RAISE, 75));
    g.act(3, HoldemGame.Move.CHECK_CALL, 0);
    g.act(0, HoldemGame.Move.CHECK_CALL, 0);
    g.act(1, HoldemGame.Move.CHECK_CALL, 0);
    assertEquals(HoldemGame.Street.FLOP, g.street);
  }

  @Test
  void unequalAllInsCreateSidePotsRefundExcessAndAwardExactlyOnce() {
    var g = new HoldemGame(new int[] {100, 200, 300, 400});
    g.start(new Random(14), false);
    while (g.playing()) g.act(g.actor, HoldemGame.Move.ALL_IN, 0);
    assertEquals(5, g.board.size());
    assertEquals(900, g.awardedPot);
    assertEquals(3, g.pots.size());
    assertEquals(List.of(400, 300, 200), g.pots.stream().map(HoldemGame.Pot::amount).toList());
    assertEquals(1000, Arrays.stream(g.stacks).sum());
    assertEquals(0, g.pot());
    var stacks = g.stacks.clone();
    assertThrows(IllegalArgumentException.class, () -> g.act(0, HoldemGame.Move.CHECK_CALL, 0));
    assertArrayEquals(stacks, g.stacks);
    assertEquals(3, g.pots.getLast().eligible().getLast());
  }

  @Test
  void splitBoardAwardsOddChipsClockwiseAndFoldedContributionsStayInPot() {
    var g = new HoldemGame(new int[] {21, 21, 21, 21});
    var order = new ArrayList<Integer>();
    for (int i = 0; i < 52; i++) order.add(i);
    var royal = cards("AH KH QH JH TH");
    int[] slots = {9, 10, 11, 13, 15};
    for (int i = 0; i < 5; i++) Collections.swap(order, slots[i], order.indexOf(royal.get(i)));
    g.startDeck(order);
    g.act(3, HoldemGame.Move.FOLD, 0);
    g.act(0, HoldemGame.Move.ALL_IN, 0);
    g.act(1, HoldemGame.Move.ALL_IN, 0);
    g.act(2, HoldemGame.Move.ALL_IN, 0);
    assertEquals(List.of(0, 1, 2), g.pots.getFirst().winners());
    assertArrayEquals(new int[] {21, 21, 21, 0}, g.payouts);
    assertEquals(84, Arrays.stream(g.stacks).sum());
  }

  @Test
  void headsUpBlindsAndActionOrderRotateAndOneSurvivorEndsWithoutBoard() {
    var g = new HoldemGame(new int[] {1000, 1000, 0, 0});
    g.start(new Random(0), false);
    assertEquals(0, g.dealer);
    assertEquals(0, g.smallBlind);
    assertEquals(1, g.bigBlind);
    assertEquals(0, g.actor);
    g.act(0, HoldemGame.Move.CHECK_CALL, 0);
    g.act(1, HoldemGame.Move.CHECK_CALL, 0);
    assertEquals(1, g.actor);
    g.act(1, HoldemGame.Move.FOLD, 0);
    assertTrue(g.ended());
    assertEquals(40, g.awardedPot);
    g.start(new Random(1), false);
    assertEquals(1, g.dealer);
    assertEquals(1, g.actor);
    g.act(1, HoldemGame.Move.FOLD, 0);
    assertEquals(20, g.awardedPot);
    assertTrue(g.board.isEmpty());
  }

  @Test
  void shortBigBlindKeepsNominalBringInAndCumulativeShortRaisesReopen() {
    var shortBlind = new HoldemGame(new int[] {1000, 1000, 5, 1000});
    shortBlind.start(new Random(1), false);
    assertEquals(20, shortBlind.currentBet);
    assertEquals(20, shortBlind.owed(3));
    assertEquals(40, shortBlind.minimumRaise());
    var g = new HoldemGame(new int[] {1000, 55, 70, 1000});
    g.start(new Random(2), false);
    g.act(3, HoldemGame.Move.RAISE, 40);
    g.act(0, HoldemGame.Move.CHECK_CALL, 0);
    g.act(1, HoldemGame.Move.ALL_IN, 0);
    g.act(2, HoldemGame.Move.ALL_IN, 0);
    assertTrue(g.canRaise(3));
    g.act(3, HoldemGame.Move.CHECK_CALL, 0);
    assertTrue(g.canRaise(0));
    assertEquals(90, g.minimumRaise());
  }

  @Test
  void incompleteOpeningDoesNotReopenCheckedSeatButUnactedSeatCanRaise() {
    var g = new HoldemGame(new int[] {1000, 1000, 30, 1000});
    g.start(new Random(3), false);
    while (g.street == HoldemGame.Street.PREFLOP) g.act(g.actor, HoldemGame.Move.CHECK_CALL, 0);
    assertEquals(1, g.actor);
    g.act(1, HoldemGame.Move.CHECK_CALL, 0);
    g.act(2, HoldemGame.Move.ALL_IN, 0);
    assertEquals(10, g.currentBet);
    assertEquals(30, g.minimumRaise());
    assertTrue(g.canRaise(3));
    g.act(3, HoldemGame.Move.CHECK_CALL, 0);
    g.act(0, HoldemGame.Move.CHECK_CALL, 0);
    assertEquals(1, g.actor);
    assertFalse(g.canRaise(1));
  }

  @Test
  void oddSplitChipsGoLeftOfDealerAndIncludeFoldedBlind() {
    var g = new HoldemGame(new int[] {21, 21, 21, 21});
    var order = new ArrayList<Integer>();
    for (int i = 0; i < 52; i++) order.add(i);
    var royal = cards("AH KH QH JH TH");
    int[] slots = {9, 10, 11, 13, 15};
    for (int i = 0; i < 5; i++) Collections.swap(order, slots[i], order.indexOf(royal.get(i)));
    g.startDeck(order);
    g.act(3, HoldemGame.Move.ALL_IN, 0);
    g.act(0, HoldemGame.Move.ALL_IN, 0);
    g.act(1, HoldemGame.Move.FOLD, 0);
    g.act(2, HoldemGame.Move.ALL_IN, 0);
    assertEquals(List.of(40, 33), g.pots.stream().map(HoldemGame.Pot::amount).toList());
    assertArrayEquals(new int[] {24, 0, 25, 24}, g.payouts);
    assertEquals(84, Arrays.stream(g.stacks).sum());
  }

  @Test
  void seededBotGamesAlwaysFinishWithUniqueCardsAndConservedChips() {
    var random = new Random(839);
    for (int hand = 0; hand < 200; hand++) {
      var g = new HoldemGame();
      g.start(random, false);
      int steps = 0;
      while (g.playing()) {
        assertTrue(steps++ < 150);
        if (g.actor == 0) {
          if (g.canRaise(0) && random.nextDouble() < .1) g.act(0, HoldemGame.Move.ALL_IN, 0);
          else g.act(0, HoldemGame.Move.CHECK_CALL, 0);
        } else PokerBots.act(g, random, false);
        assertEquals(4000, Arrays.stream(g.stacks).sum() + g.pot());
      }
      var seen = new HashSet<>(g.board);
      for (int seat = 0; seat < 4; seat++)
        for (int i = 0; i < 2; i++) assertTrue(seen.add(g.hole(seat, i)));
    }
  }

  @Test
  void templatesShareOneCarrierAndNeverExposeHiddenBotCardIds() {
    for (boolean compact : List.of(false, true)) {
      var s = new PokerState();
      s.compact = compact;
      assertTrue(PokerView.render(s).effects.isEmpty());
      s.deal(200, true);
      var c = PokerView.render(s);
      assertEquals(8, c.effects.size());
      assertEquals(1, c.items.size());
      assertTrue(c.images.getFirst().background());
      assertEquals(compact ? 153 : 324, c.height);
      var holes = c.effects.stream().filter(e -> e.id().startsWith("hero_")).toList();
      assertEquals(2, holes.size());
      for (var effect : c.effects)
        if (effect.shader().equals(DemoShaders.spec(DemoShaders.Kind.PLAYING_CARD))
            && effect.id().startsWith("board")) assertEquals(63, DemoShaders.a(effect) & 63);
      for (var hit : c.hits) assertTrue(hit.y() % 9 == 0 && hit.height() % 9 == 0);
      s.motion = false;
      s.stop();
      int steps = 0;
      while (s.game.playing()) {
        assertTrue(steps++ < 100);
        if (s.game.actor == 0) s.human(HoldemGame.Move.CHECK_CALL, 210);
        else s.bot(210);
        assertDoesNotThrow(() -> PokerView.render(s));
        assertFalse(s.busy());
      }
      assertEquals("Royal flush", s.game.hands.get(0).name());
      assertTrue(s.game.payouts[0] > 0);
      assertTrue(
          PokerView.render(s).effects.stream()
              .filter(e -> e.shader().equals(DemoShaders.spec(DemoShaders.Kind.PLAYING_CARD)))
              .anyMatch(e -> (DemoShaders.a(e) & 128) != 0));
      long old = s.generation;
      s.reset();
      assertTrue(s.generation > old);
      assertEquals(HoldemGame.Street.LOBBY, s.game.street);
    }
  }
}
