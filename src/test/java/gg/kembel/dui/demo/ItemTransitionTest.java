package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ItemTransitionTest {
  private static Canvas render(String attrs, boolean motion) throws Exception {
    return DemoTemplates.parse(
            "<dui-menu width=\"300\" height=\"144\" motion=\""
                + motion
                + "\"><dui-layer height=\"fill\"><dui-demo-item id=\"gift\" x=\"18\" y=\"27\""
                + " width=\"72\" height=\"72\" size=\"72\" "
                + attrs
                + " /></dui-layer></dui-menu>")
        .render(Map.of());
  }

  @Test
  void templateParametersAndTransportRoundTrip() throws Exception {
    for (String name : List.of("pop", "slide", "lift", "bounce")) {
      var c =
          render(
              "transition=\""
                  + name
                  + "\" transition-start=\"23999\" transition-duration=\"127\""
                  + " transition-distance=\"127\"",
              false);
      var t = c.motions.get("gift");
      assertFalse(t.enabled());
      assertEquals(23999, t.startedAt());
      assertEquals(127, t.duration());
      assertEquals(85, ItemTransport.motionPayload(c, c.items.getFirst(), t).size());
    }
    assertTrue(
        render("transition=\"pop\" transition-start=\"24001\"", true)
            .motions
            .get("gift")
            .enabled());
    assertTrue(render("", true).motions.isEmpty());
  }

  @Test
  void invalidNamesAndTimingFail() {
    for (String attrs :
        List.of(
            "transition=\"spin\"",
            "transition=\"lift\" transition-duration=\"0\"",
            "transition=\"pop\" transition-distance=\"256\""))
      assertThrows(IllegalArgumentException.class, () -> render(attrs, true));
  }

  @Test
  void confettiIsAnIndependentBoundedEffect() throws Exception {
    var c =
        DemoTemplates.parse(
                "<dui-menu width=\"300\" height=\"144\" animation-start=\"40\""
                    + " motion=\"false\"><dui-layer height=\"fill\"><dui-demo-particles"
                    + " id=\"party\" effect=\"confetti\" width=\"300\" height=\"144\" count=\"52\""
                    + " delay=\"24\" origin-x=\"70\" origin-y=\"80\"/></dui-layer></dui-menu>")
            .render(Map.of());
    assertEquals(DemoShaders.spec(DemoShaders.Kind.CONFETTI), c.effects.getFirst().shader());
    assertEquals(96, c.animation.durationTicks());
    assertFalse(c.animation.motion());
    assertEquals(1, c.items.size());
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
