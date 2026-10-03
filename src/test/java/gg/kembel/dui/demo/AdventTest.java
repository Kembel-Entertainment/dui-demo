package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AdventTest {
  @Test
  void giftOpensUpwardAndRewardAndConfettiShareItsCenter() {
    for (boolean compact : List.of(false, true)) {
      var s = new AdventState();
      s.compact = compact;
      s.open(24, 100);
      var opening = AdventView.render(s, 100);
      var body = opening.items.stream().filter(i -> i.id().equals("parcel_body")).findFirst().orElseThrow();
      var lid = opening.items.stream().filter(i -> i.id().equals("parcel_lid")).findFirst().orElseThrow();
      assertTrue(lid.y() < body.y(), "Open lid must finish above the box");
      assertEquals(body.y(), lid.y() + opening.motions.get("parcel_lid").ty(), "Closed starting pose");
      // The authored lid has transparent padding above its bow (19 of 96 pixels).
      assertTrue(lid.y() + 19.0 / 96 * lid.size() >= (compact ? 27 : 45), "Lid must clear the title");
      var confetti = opening.effects.getFirst();
      assertTrue(confetti.y() >= (compact ? 27 : 45), "Confetti must clear the title");
      assertTrue(confetti.y() + confetti.height() <= opening.height - 27, "Confetti must clear controls");
      assertEquals(body.x() + body.size() / 2, confetti.x() + (DemoShaders.a(confetti) & 511));
      assertEquals(body.y() + body.size() / 2, confetti.y() + (DemoShaders.b(confetti) & 511));
      s.reveal(s.generation, 124);
      var reward = AdventView.render(s, 124).items.stream().filter(i -> i.id().equals("advent_reward")).findFirst().orElseThrow();
      assertEquals(body.x() + body.size() / 2.0, reward.x() + reward.size() / 2.0, .5);
      assertEquals(body.y() + body.size() / 2.0, reward.y() + reward.size() / 2.0, .5);
    }
  }

  @Test
  void everyGiftCanBeOpenedRepeatedlyWithoutAClaimLedger() {
    var s = new AdventState();
    long tick = 0;
    for (int round = 0; round < 2; round++)
      for (int day = 1; day <= 24; day++) {
        s.open(day, tick);
        assertEquals(AdventState.Phase.OPENING, s.phase);
        assertEquals(day, s.selected);
        assertFalse(s.reveal(s.generation, tick + 23));
        assertTrue(s.reveal(s.generation, tick + 24));
        assertFalse(s.gift().name().isBlank());
        s.back();
        assertEquals(0, s.selected);
        assertEquals(-1, s.startedAt);
        tick += 30;
      }
  }

  @Test
  void cancellationAndReplayRejectStaleCallbacks() {
    var s = new AdventState();
    s.open(5, 100);
    long old = s.generation;
    s.back();
    assertFalse(s.reveal(old, 124));
    s.open(5, 200);
    assertFalse(s.reveal(old, 224));
    assertTrue(s.reveal(s.generation, 224));
    s.open(5, 250);
    assertEquals(AdventState.Phase.OPENING, s.phase);
    assertEquals(-1, s.rewardAt);
    s.motion = false;
    s.open(1, 300);
    assertEquals(AdventState.Phase.REVEALED, s.phase);
    assertEquals(300, s.rewardAt);
    assertThrows(IllegalArgumentException.class, () -> s.open(0, 0));
    assertThrows(IllegalArgumentException.class, () -> s.open(25, 0));
  }

  @Test
  void shelfHasAll24DistinctNonoverlappingHitRegionsAndFitsImageBudget() {
    for (boolean compact : List.of(false, true)) {
      var s = new AdventState();
      s.compact = compact;
      var c = AdventView.render(s, 100);
      var gifts = c.hits.stream().filter(h -> h.action().equals("advent_open")).toList();
      assertEquals(24, gifts.size());
      assertEquals(24, gifts.stream().map(Canvas.Hit::value).distinct().count());
      for (var a : gifts)
        for (var b : gifts)
          if (a != b)
            assertFalse(
                a.x() < b.x() + b.width()
                    && a.x() + a.width() > b.x()
                    && a.y() < b.y() + b.height()
                    && a.y() + a.height() > b.y());
      assertEquals(26, c.images.size());
      assertEquals(compact ? 0 : 1, c.items.size());
      assertTrue(
          c.images.stream().mapToInt(i -> i.raster().width * i.raster().height).sum() <= 16384);
      for (var hit : gifts)
        assertEquals(hit.id(), c.at(hit.x() + hit.width() / 2, hit.y() + hit.height() / 2).id());
    }
  }

  @Test
  void openingSceneUsesReusableTransitionsAndRealRewardCarrier() {
    for (boolean compact : List.of(false, true))
      for (boolean motion : List.of(false, true))
        for (int day = 1; day <= 24; day++) {
          var s = new AdventState();
          s.compact = compact;
          s.motion = motion;
          s.open(day, 23990);
          var opening = AdventView.render(s, 23990);
          assertEquals(motion ? 2 : (compact ? 1 : 3), opening.motions.size());
          assertEquals(
              DemoShaders.spec(DemoShaders.Kind.CONFETTI), opening.effects.getFirst().shader());
          if (motion) {
            assertEquals(
                "",
                opening.hits.stream()
                    .filter(h -> h.id().equals("advent_again"))
                    .findFirst()
                    .orElseThrow()
                    .action());
            s.reveal(s.generation, 24014);
          }
          var reveal = AdventView.render(s, 24014);
          assertEquals(compact ? 1 : 3, reveal.motions.size());
          assertEquals(compact ? 2 : 4, reveal.items.size());
          assertEquals(Motion.Easing.BACK_OUT, reveal.motions.get("advent_reward").easing());
          if (!compact) assertEquals(motion, reveal.motions.get("parcel_lid").enabled());
          assertEquals(
              "advent_again",
              reveal.hits.stream()
                  .filter(h -> h.id().equals("advent_again"))
                  .findFirst()
                  .orElseThrow()
                  .action());
        }
  }
}
