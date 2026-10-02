package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CardFlightTest {
  private Canvas render(String attrs) throws Exception {
    return DemoTemplates.parse(
            "<dui-menu width='320' height='153'><dui-demo-playing-card id='c' x='10' y='18'"
                + " width='290' height='90' animation='fly' "
                + attrs
                + " /></dui-menu>")
        .render(Map.of());
  }

  @Test
  void reusesCardTransportAndHasDelayedLifetime() throws Exception {
    var c = render("value='-1' face-down='true' duration='28' delay='24' card-height='30'");
    var e = c.effects.getFirst();
    assertEquals(DemoShaders.spec(DemoShaders.Kind.PLAYING_CARD), e.shader());
    assertEquals(3, DemoShaders.a(e) >> 13);
    assertEquals(63, DemoShaders.a(e) & 63);
    assertEquals(30, (DemoShaders.b(e) >> 7) & 63);
    assertEquals(52, e.lifetimeTicks());
    assertEquals(1, c.items.size());
  }

  @Test
  void rejectsInvalidFaceSizeAndOddDelay() {
    assertThrows(IllegalArgumentException.class, () -> render("card-height='64'"));
    assertThrows(IllegalArgumentException.class, () -> render("delay='3'"));
    assertThrows(IllegalArgumentException.class, () -> render("card-height='12'"));
  }
}
