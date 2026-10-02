package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class RouletteTest {
  private static Random result(int value) {
    return new Random(0) {
      @Override
      public int nextInt(int bound) {
        assertEquals(37, bound);
        return value;
      }
    };
  }

  @Test
  void singleZeroWheelAndEveryOutsideBetHaveCorrectCoverageAndReturns() {
    assertEquals(37, Arrays.stream(RouletteGame.WHEEL).distinct().count());
    assertEquals(0, Arrays.stream(RouletteGame.WHEEL).min().orElseThrow());
    assertEquals(36, Arrays.stream(RouletteGame.WHEEL).max().orElseThrow());
    for (String bet :
        List.of(
            "red", "black", "even", "odd", "low", "high", "d:1", "d:2", "d:3", "c:1", "c:2",
            "c:3")) {
      int winners = 0;
      for (int n = 0; n < 37; n++) if (RouletteGame.wins(bet, n)) winners++;
      assertEquals(RouletteGame.multiplier(bet) == 2 ? 18 : 12, winners);
      assertFalse(RouletteGame.wins(bet, 0));
    }
    for (int n = 0; n < 37; n++) {
      assertTrue(RouletteGame.wins("n:" + n, n));
      assertEquals(36, RouletteGame.multiplier("n:" + n));
    }
  }

  @Test
  void reservingUndoAndClearAreAtomicAndRespectTableLimit() {
    var s = new RouletteGame();
    assertTrue(s.place("n:17", 1));
    assertTrue(s.place("red", 2));
    assertEquals(4950, s.balance);
    assertEquals(50, s.stake());
    s.undo();
    assertEquals(4975, s.balance);
    assertEquals(Map.of("n:17", 25), s.bets);
    assertThrows(IllegalArgumentException.class, () -> s.place("n:37", 3));
    assertEquals(4975, s.balance);
    s.clear();
    assertEquals(5000, s.balance);
    s.choose(500);
    for (int i = 0; i < 10; i++) assertTrue(s.place("black", 10 + i));
    assertFalse(s.place("red", 30));
    assertEquals(5000, s.stake());
    assertEquals(0, s.balance);
    s.clear();
    assertEquals(5000, s.balance);
    assertThrows(IllegalArgumentException.class, () -> s.choose(7));
  }

  @Test
  void zeroAndOverlappingWinningBetsPayExactlyOnceAndCannotChangeDuringSpin() {
    for (int winner : List.of(0, 17, 36)) {
      var s = new RouletteGame();
      s.place("n:" + winner, 1);
      s.place("red", 2);
      s.place("d:2", 3);
      s.place("c:2", 4);
      s.place("odd", 5);
      var bets = Map.copyOf(s.bets);
      int expected =
          bets.entrySet().stream()
              .mapToInt(
                  e ->
                      RouletteGame.wins(e.getKey(), winner)
                          ? e.getValue() * RouletteGame.multiplier(e.getKey())
                          : 0)
              .sum();
      assertTrue(s.spin(result(winner), 100));
      assertFalse(s.spin(result(2), 101));
      assertFalse(s.place("red", 102));
      s.choose(500);
      assertEquals(25, s.denomination);
      s.undo();
      s.clear();
      assertEquals(bets, s.bets);
      s.finish(259);
      assertEquals(4875, s.balance);
      s.finish(260);
      assertEquals(4875 + expected, s.balance);
      assertEquals(expected, s.lastReturn);
      assertEquals(winner, s.lastResult);
      assertEquals(1, s.rounds);
      s.finish(288);
      s.finish(400);
      assertEquals(4875 + expected, s.balance);
      assertEquals(List.of(winner), s.history);
      assertFalse(s.locked());
      assertTrue(s.repeat(401));
      assertEquals(bets, s.bets);
      assertEquals(4750 + expected, s.balance);
    }
  }

  @Test
  void motionChangesAndResetInvalidateTimersWithoutDuplicatingMoney() {
    var s = new RouletteGame();
    s.place("n:0", 1);
    s.spin(result(0), 2);
    long token = s.generation;
    s.toggleMotion(10);
    assertFalse(s.locked());
    assertEquals(5875, s.balance);
    assertEquals(1, s.rounds);
    assertTrue(s.generation > token);
    s.finish(500);
    assertEquals(5875, s.balance);
    assertTrue(s.repeat(600));
    s.spin(result(0), 601);
    assertEquals(6750, s.balance);
    assertEquals(2, s.rounds);
    s.reset();
    assertEquals(5000, s.balance);
    assertFalse(s.animated());
    assertTrue(s.history.isEmpty());
    s.finish(900);
    assertEquals(5000, s.balance);
  }

  @Test
  void bothLayoutsFitWithEveryChipVisibleAndUseTwoEffectsAtMost() {
    for (boolean compact : List.of(false, true)) {
      var s = new RouletteGame();
      s.compact = compact;
      s.choose(5);
      var keys = new ArrayList<String>();
      for (int n = 0; n < 37; n++) keys.add("n:" + n);
      keys.addAll(
          List.of(
              "red", "black", "low", "high", "even", "odd", "d:1", "d:2", "d:3", "c:1", "c:2",
              "c:3"));
      for (String key : keys) s.place(key, 100);
      var c = RouletteView.render(s);
      var bindings = RouletteView.data(s);
      @SuppressWarnings("unchecked")
      var cells = (List<Map<String, Object>>) bindings.get("cells");
      for (var cell : cells) {
        String id = (String) cell.get("id");
        var hit = c.hits.stream().filter(h -> h.id().equals(id)).findFirst().orElseThrow();
        var marker =
            c.images.stream().filter(i -> i.id().equals("chip_" + id)).findFirst().orElseThrow();
        assertTrue(marker.x() > hit.x(), id + " left padding");
        assertTrue(marker.x() + marker.width() < hit.x() + hit.width(), id + " right padding");
        assertTrue(marker.y() > hit.y(), id + " top padding");
        assertTrue(marker.y() + marker.height() < hit.y() + hit.height(), id + " bottom padding");
        int tx = (int) cell.get("textX"), ty = (int) cell.get("textY");
        int tw = (int) cell.get("textW");
        assertEquals(cell.get("label"), c.metrics().fit((String) cell.get("label"), tw), id);
        assertTrue(
            tx + tw <= marker.x()
                || marker.x() + marker.width() <= tx
                || ty + 9 <= marker.y()
                || marker.y() + marker.height() <= ty,
            id + " label overlaps the placed chip");
      }
      var destination =
          cells.stream()
              .filter(cell -> cell.get("key").equals(s.chipBet))
              .findFirst()
              .orElseThrow();
      var flight =
          c.effects.stream()
              .filter(e -> e.shader().equals(DemoShaders.spec(DemoShaders.Kind.CHIP_STACK)))
              .findFirst()
              .orElseThrow();
      assertEquals(
          (int) destination.get("chipX") + (int) destination.get("chipSize") / 2.0,
          flight.x() + flight.width() * .5,
          .5);
      assertEquals(
          (int) destination.get("chipY") + (int) destination.get("chipSize") / 2.0,
          flight.y() + flight.height() * .14,
          .5);
      assertEquals(compact ? 153 : 324, c.height);
      assertEquals(2, c.effects.size());
      assertEquals(1, c.items.size());
      assertEquals(49, c.hits.stream().filter(h -> h.action().equals("roulette_bet")).count());
      assertTrue(c.images.getFirst().background());
      assertTrue(
          c.images.stream().mapToInt(i -> i.raster().width * i.raster().height).sum() <= 16384);
      for (var h : c.hits) {
        assertEquals(0, h.y() % 9);
        assertEquals(0, h.height() % 9);
      }
      s.spin(result(17), 200);
      assertDoesNotThrow(() -> RouletteView.render(s));
      s.finish(360);
      assertDoesNotThrow(() -> RouletteView.render(s));
      s.finish(388);
      assertFalse(s.locked());
      s.toggleMotion(400);
      s.place("red", 401);
      assertFalse((boolean) RouletteView.data(s).get("chipFlight"));
      assertTrue(
          RouletteView.render(s).effects.stream()
              .noneMatch(e -> e.shader().equals(DemoShaders.spec(DemoShaders.Kind.CHIP_STACK))),
          "Still mode must not cover the stationary marker with a frozen flight chip");
    }
  }
}
