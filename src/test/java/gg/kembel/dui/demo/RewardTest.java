package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import gg.kembel.dui.core.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class RewardTest {
  private static final LocalDate DAY = LocalDate.of(2026, 9, 29);

  @Test
  void aClaimIsIdempotentAndSurvivesReloadAndClockRollback() {
    var s = new RewardState();
    assertTrue(s.claim(DAY));
    assertEquals(50, s.stars);
    assertFalse(s.claim(DAY));
    assertFalse(s.claim(DAY.minusDays(1)));
    assertEquals(50, s.stars);
    var restored = new Gson().fromJson(new Gson().toJson(s), RewardState.class);
    assertTrue(restored.valid());
    assertFalse(restored.claim(DAY));
    assertEquals(-1, restored.selected);
    assertFalse(restored.celebrating);
    assertTrue(restored.claim(DAY.plusDays(1)));
    assertEquals(125, restored.stars);
    assertEquals(0, new RewardState().stars);
  }

  @Test
  void aWeekWrapsAndMissedDaysRestartWithoutLosingStars() {
    var s = new RewardState();
    for (int i = 0; i < 7; i++) assertTrue(s.claim(DAY.plusDays(i)));
    assertEquals(1050, s.stars);
    assertEquals(7, s.completed);
    assertFalse(s.claim(DAY.plusDays(6)));
    assertTrue(s.claim(DAY.plusDays(7)));
    assertEquals(1, s.completed);
    assertEquals(1100, s.stars);
    assertTrue(s.claim(DAY.plusDays(10)));
    assertEquals(1, s.completed);
    assertEquals(1150, s.stars);
  }

  @Test
  void previewDoesNotClaimAndDemoClockAndResetArePlayerLocal() {
    var s = new RewardState();
    s.apply("reward_select", "6", DAY);
    assertEquals(6, s.selection(DAY));
    assertEquals(0, s.stars);
    assertThrows(IllegalArgumentException.class, () -> s.apply("reward_select", "7", DAY));
    s.apply("reward_claim", "", DAY);
    s.apply("reward_done", "", DAY);
    assertFalse(s.celebrating);
    s.apply("reward_next", "", DAY);
    assertFalse(s.claimedToday(DAY));
    s.apply("reward_claim", "", DAY);
    assertEquals(125, s.stars);
    s.apply("reward_next", "", DAY);
    s.apply("reward_next", "", DAY);
    assertEquals(0, s.completed);
    assertEquals(125, s.stars);
    s.apply("reward_reset", "", DAY);
    assertTrue(s.valid());
    assertEquals(0, s.demoDays);
    assertEquals(0, s.stars);
  }

  @Test
  void everyDayAndStateFitsBothLayoutsAndDisabledButtonsHaveNoAction() {
    for (boolean compact : List.of(false, true)) {
      var s = new RewardState();
      s.compact = compact;
      for (int day = 0; day < 8; day++) {
        var date = DAY.plusDays(day);
        for (int selected = 0; selected < 7; selected++) {
          s.selected = selected;
          var c = RewardView.render(s, date);
          for (var hit : c.hits)
            assertSame(hit, c.at(hit.x() + hit.width() / 2, hit.y() + hit.height() / 2));
          assertEquals(compact ? 1 : 2, c.items.size());
          assertEquals(7, c.hits.stream().filter(h -> h.action().equals("reward_select")).count());
          var claim =
              c.hits.stream().filter(h -> h.id().equals("reward_claim")).findFirst().orElseThrow();
          assertEquals(selected == s.current(date), claim.action().equals("reward_claim"));
        }
        s.claim(date);
        var celebration = RewardView.render(s, date);
        assertEquals(
            "reward_done",
            celebration.hits.stream()
                .filter(h -> h.id().equals("reward_claim"))
                .findFirst()
                .orElseThrow()
                .action());
        s.apply("reward_done", "", date);
        assertTrue(
            RewardView.render(s, date).hits.stream()
                .filter(h -> h.id().equals("reward_claim"))
                .findFirst()
                .orElseThrow()
                .action()
                .isEmpty());
      }
    }
  }

  @Test
  void originalMascotFramesActuallyAnimateAndOnlyKnownModelsAreAccepted() {
    var a = RewardArt.frame(false, 0);
    var b = RewardArt.frame(false, 8);
    var party = RewardArt.frame(true, 8);
    int motion = 0, changed = 0;
    for (int y = 0; y < 96; y++)
      for (int x = 0; x < 96; x++) {
        if (a.getRGB(x, y) != b.getRGB(x, y)) motion++;
        if (b.getRGB(x, y) != party.getRGB(x, y)) changed++;
      }
    assertTrue(motion > 400);
    assertTrue(changed > 400);
    assertEquals(4, RewardArt.MODELS.size());
    assertTrue(RewardArt.MODELS.contains("reward/idle"));
  }

  @Test
  void burstIsTransientAndDisabledMotionStopsItWithoutReplayingOnEnable() {
    var s = new RewardState();
    s.claim(DAY);
    s.burstStarted = 23999;
    var c = RewardView.render(s, DAY);
    assertEquals(23999, c.animation.startedAt());
    var restored = new Gson().fromJson(new Gson().toJson(s), RewardState.class);
    assertEquals(-1, restored.burstStarted);
    s.apply("reward_motion", "", DAY);
    assertTrue(RewardView.render(s, DAY).effects.isEmpty());
    s.apply("reward_motion", "", DAY);
    assertTrue(RewardView.render(s, DAY).effects.isEmpty());
    s.burstStarted = 24020;
    s.apply("reward_done", "", DAY);
    assertTrue(RewardView.render(s, DAY).effects.isEmpty());
  }

  @Test
  void burstMetadataRoundTripsAllBitsAndWrapsWithTheVanillaClock() {
    for (long tick : new long[] {0, 23999, 24000, 1_000_000})
      for (boolean compact : List.of(false, true)) {
        int w = compact ? 300 : 480,
            h = compact ? 144 : 306,
            x = compact ? 27 : 45,
            y = compact ? 74 : 75;
        var colors = ItemTransport.headerPayload(tick, w, h, x, y);
        long data = 0;
        for (int i = 0; i < colors.size(); i++) {
          int rgb = colors.get(i);
          long bits =
              ((rgb & 0xFF0000) != 0 ? 1 : 0)
                  | ((rgb & 0x00FF00) != 0 ? 2 : 0)
                  | ((rgb & 0x0000FF) != 0 ? 4 : 0);
          data |= bits << (i * 3);
        }
        assertEquals(tick % 24000, data & 32767);
        assertEquals(w, (data >> 15) & 511);
        assertEquals(h, (data >> 24) & 511);
        assertEquals(x, (data >> 33) & 511);
        assertEquals(y, (data >> 42) & 511);
        var plain = ItemTransport.payload(-20, -220, 96);
        var burst = ItemTransport.payload(-20, -220, 96, true);
        for (int i = 0; i < 9; i++) assertEquals(plain.get(i), burst.get(i));
        assertEquals(0x0000FF, plain.get(9) ^ burst.get(9));
      }
    assertThrows(
        IllegalArgumentException.class, () -> ItemTransport.headerPayload(-1, 480, 306, 45, 75));
  }
}
