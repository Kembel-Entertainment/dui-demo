package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class WheelComponentTest {
  private static Canvas render(String child) throws Exception {
    return DemoTemplates.parse(
            "<dui-menu background='none' width='240' height='243'"
                + " animation-start='23990'><dui-layer height='fill'>"
                + child
                + "</dui-layer></dui-menu>")
        .render(Map.of());
  }

  @Test
  void everyPocketRetainsItsNumberAndUsesTheExistingTransport() throws Exception {
    for (int value = 0; value < 37; value++) {
      var c =
          render(
              "<dui-demo-wheel id='w' width='216' height='216' value='"
                  + value
                  + "' previous='36' animation='spin' duration='160' turns='4'/>");
      var e = c.effects.getFirst();
      assertEquals(DemoShaders.spec(DemoShaders.Kind.WHEEL), e.shader());
      assertEquals(value, DemoShaders.a(e) & 63);
      assertEquals(36, (DemoShaders.a(e) >> 6) & 63);
      assertEquals(4, DemoShaders.a(e) >> 12);
      assertEquals(160, e.lifetimeTicks());
      assertEquals(160, c.animation.durationTicks());
      assertEquals(24, TestTransport.effectPayload(e).size());
      assertEquals(1, c.items.size());
    }
    assertEquals(
        0,
        render("<dui-demo-wheel id='w' width='216' height='216' value='0'/>")
            .effects
            .getFirst()
            .lifetimeTicks());
  }

  @Test
  void unsupportedWheelParametersAndInvisibleHitboxErrorsFailEarly() {
    for (String attrs :
        List.of(
            "value='37'",
            "previous='-1'",
            "duration='512'",
            "duration='19'",
            "turns='8'",
            "animation='flip'",
            "palette='pink'",
            "variant='american'"))
      assertThrows(
          IllegalArgumentException.class,
          () -> render("<dui-demo-wheel id='w' width='216' height='216' " + attrs + "/>"));
    assertThrows(
        IllegalArgumentException.class,
        () -> render("<dui-demo-wheel id='w' width='216' height='210'/>"));
    assertThrows(
        IllegalArgumentException.class,
        () -> render("<dui-hitbox id='h' width='24' height='18'/>"));
    assertThrows(
        IllegalArgumentException.class,
        () -> render("<dui-hitbox id='h' action='choose' y='1' width='24' height='18'/>"));
  }

  @Test
  void hitboxesPreserveExactBoundsWithoutAddingPaintOrCarriers() throws Exception {
    var c =
        render(
            "<dui-hitbox id='h' action='choose' payload='n:17' tooltip='Straight 17' x='11' y='18'"
                + " width='16' height='27'/>");
    assertTrue(c.paints.isEmpty());
    assertTrue(c.items.isEmpty());
    assertEquals("n:17", c.at(11, 18).value());
    assertNull(c.at(27, 18));
    var locked =
        render("<dui-hitbox id='h' action='choose' locked='true' width='16' height='27'/>");
    assertEquals("", locked.hits.getFirst().action());
  }
}
