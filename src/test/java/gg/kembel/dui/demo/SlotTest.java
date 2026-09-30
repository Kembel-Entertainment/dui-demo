package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SlotTest {
  @Test
  void all216OutcomesHaveExplicitPayouts() {
    int triples = 0, pairs = 0, misses = 0, total = 0;
    for (int a = 0; a < 6; a++)
      for (int b = 0; b < 6; b++)
        for (int c = 0; c < 6; c++) {
          int m = SlotState.multiplier(new int[] {a, b, c});
          total += m;
          if (a == b && b == c) {
            triples++;
            assertEquals(SlotState.TRIPLES[a], m);
          } else if (a == b || b == c || a == c) {
            pairs++;
            assertEquals(1, m);
          } else {
            misses++;
            assertEquals(0, m);
          }
        }
    assertEquals(6, triples);
    assertEquals(90, pairs);
    assertEquals(120, misses);
    assertEquals(150, total);
    assertThrows(IllegalArgumentException.class, () -> SlotState.multiplier(new int[] {6, 0, 0}));
  }

  @Test
  void stakeReservedOnceAndFixedResultSurvivesRestart() {
    var s = new SlotState();
    assertTrue(s.spin(23999, new Random(5)));
    assertEquals(975, s.chips);
    int payout = s.payout;
    assertFalse(s.spin(24000, new Random()));
    s.apply("slot_mode");
    s.apply("slot_refill");
    assertEquals(975, s.chips);
    assertEquals(0, s.modeIndex);
    var json = new Gson();
    var restored = json.fromJson(json.toJson(s), SlotState.class);
    assertTrue(restored.valid());
    assertArrayEquals(s.reels, restored.reels);
    assertTrue(restored.settle());
    assertEquals(975 + payout, restored.chips);
    assertFalse(restored.settle());
    assertEquals(1, restored.spins);
    var settled = json.fromJson(json.toJson(restored), SlotState.class);
    assertFalse(settled.settle());
    assertEquals(restored.chips, settled.chips);
  }

  @Test
  void previewsNeverChangeLedgerAndLowWalletCannotBet() {
    var s = new SlotState();
    s.chips = 0;
    assertFalse(s.canSpin());
    assertFalse(s.spin(0, new Random()));
    for (int mode = 1; mode < 4; mode++) {
      s.modeIndex = mode;
      assertTrue(s.spin(4, new Random()));
      assertTrue(s.settle());
      assertEquals(0, s.chips);
      assertEquals(0, s.spins);
      assertEquals(0, s.wagered);
      assertEquals(0, s.paid);
    }
    s.apply("slot_refill");
    assertEquals(1000, s.chips);
    s.chips = 2000;
    s.apply("slot_refill");
    assertEquals(2000, s.chips);
  }

  @Test
  void moneyUsesActualReservedBetAndAllHitsFit() {
    for (boolean compact : List.of(false, true))
      for (boolean pending : List.of(false, true))
        for (int mode = 0; mode < 4; mode++) {
          var s = new SlotState();
          s.compact = compact;
          s.modeIndex = mode;
          if (pending) s.spin(200, new Random(6));
          var c = SlotView.render(s);
          assertEquals(compact ? 144 : 270, c.height);
          assertEquals(1, c.items.size());
          for (var hit : c.hits) {
            assertNotNull(c.at(hit.x() + hit.width() / 2, hit.y() + hit.height() / 2));
            if (pending) assertTrue(hit.action().isEmpty());
          }
          if (pending) {
            int bet = s.betIndex;
            s.apply("slot_bet_more");
            assertEquals(bet, s.betIndex);
          } else if (compact) {
            s.apply("slot_paytable");
            assertTrue(SlotView.render(s).items.isEmpty());
            s.apply("slot_size");
            assertEquals(1, SlotView.render(s).items.size());
          }
        }
  }

  @Test
  void transportSeparatesConfettiFromSlotsWithoutCroppingNativeItems() {
    var s = new SlotState();
    s.previous = new int[] {5, 4, 3};
    s.reels = new int[] {2, 1, 0};
    s.startedAt = 24007;
    long data = decode(ItemTransport.animationPayload(SlotView.render(s)).subList(0, 18));
    assertEquals(7, data & 32767);
    assertEquals(480, (data >>> 15) & 511);
    assertEquals(270, (data >>> 24) & 511);
    assertEquals(6, (data >>> 33) & 511);
    assertEquals(0, (data >>> 42) & 511);
    assertEquals(1, data >>> 51);
    assertEquals(0, decode(ItemTransport.confettiPayload(24001, 300, 144, 10, 20)) >>> 51);
  }

  private static long decode(List<Integer> colors) {
    long data = 0;
    for (int i = 0; i < colors.size(); i++) {
      int c = colors.get(i),
          bits =
              ((c & 0xFF0000) != 0 ? 1 : 0)
                  | ((c & 0xFF00) != 0 ? 2 : 0)
                  | ((c & 255) != 0 ? 4 : 0);
      data |= (long) bits << (i * 3);
    }
    return data;
  }
}
