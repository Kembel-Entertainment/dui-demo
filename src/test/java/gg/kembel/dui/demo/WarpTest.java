package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class WarpTest {
  @Test
  void circularSelectionIsLockedDuringMotionAndStaleCallbacksCannotFinishIt() {
    var s = new WarpState();
    assertTrue(s.step(-1, 23990));
    assertEquals(3, s.selected);
    long token = s.generation;
    assertFalse(s.step(1, 23991));
    assertFalse(s.finish(token, 24013));
    s.stop();
    s.step(1, 24020);
    assertFalse(s.finish(token, 24044));
    assertTrue(s.finish(s.generation, 24044));
    assertEquals(0, s.selected);
    assertEquals(-1, s.startedAt);
    s.motion = false;
    for (int i = 0; i < 12; i++) {
      assertTrue(s.step(1, 24045));
      assertFalse(s.moving);
    }
    assertEquals(0, s.selected);
    s.preview(24050);
    assertTrue(s.arrived);
    assertEquals(-1, s.startedAt);
    s.jump(2);
    assertFalse(s.arrived);
    assertThrows(IllegalArgumentException.class, () -> s.step(0, 0));
    assertThrows(IllegalArgumentException.class, () -> s.jump(4));
  }

  @Test
  void bothLayoutsClipAnimationWithoutMovingTheHitTargets() {
    for (boolean compact : List.of(false, true))
      for (int index = 0; index < 4; index++)
        for (int direction : List.of(-1, 1)) {
          var s = new WarpState();
          s.compact = compact;
          s.jump(index);
          var resting = WarpView.render(s);
          assertEquals(3, resting.items.size());
          assertEquals(3, resting.clips.size());
          assertTrue(resting.motions.isEmpty());
          var previous =
              resting.hits.stream()
                  .filter(h -> h.id().equals("warp_previous"))
                  .findFirst()
                  .orElseThrow();
          s.step(direction, 200);
          var moving = WarpView.render(s);
          assertEquals(4, moving.items.size());
          assertEquals(4, moving.motions.size());
          assertTrue(
              moving.items.stream().anyMatch(i -> i.x() < 0 || i.x() + i.size() > moving.width));
          for (var t : moving.motions.values()) {
            assertEquals(Motion.Easing.EASE_OUT, t.easing());
            assertEquals(direction * (compact ? 66 : 108), t.tx());
          }
          var locked =
              moving.hits.stream()
                  .filter(h -> h.id().equals("warp_previous"))
                  .findFirst()
                  .orElseThrow();
          assertEquals(previous.x(), locked.x());
          assertEquals(previous.y(), locked.y());
          assertEquals("", locked.action());
          for (var h : moving.hits) assertTrue(h.y() % 9 == 0 && h.height() % 9 == 0);
          s.finish(s.generation, 224);
          assertEquals(3, WarpView.render(s).items.size());
          s.motion = false;
          s.step(direction, 230);
          assertTrue(WarpView.render(s).motions.isEmpty());
        }
  }

  @Test
  void ownLandscapesHaveTransparentPaddingAndSelectedFrames() {
    var fingerprints = new HashSet<Integer>();
    for (int i = 0; i < 4; i++) {
      var art = WarpArt.card(i, false);
      var selected = WarpArt.card(i, true);
      assertEquals(0, art.getRGB(0, 0));
      assertNotEquals(art.getRGB(18, 8), selected.getRGB(18, 8));
      fingerprints.add(Arrays.hashCode(art.getRGB(0, 0, 128, 128, null, 0, 128)));
    }
    assertEquals(4, fingerprints.size());
  }
}
