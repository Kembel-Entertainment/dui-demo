package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CardEffectsTest {
  private static final String SHELL =
      "<dui-menu width='240' height='108' animation-start='23999'><dui-layer"
          + " height='fill'>%s</dui-layer></dui-menu>";

  @Test
  void cardsAndChipsConsumeTypedParametersAndShareOneCarrier() throws Exception {
    var c =
        DemoTemplates.parse(
                SHELL.formatted(
                    "<dui-demo-playing-card id='a' x='12' y='9' width='42' height='81' value='38'"
                        + " face-down='true' active='true' animation='flip' delay='8' duration='28'"
                        + " lift='15' palette='coral'/><dui-demo-chip-stack id='b' x='72' y='18'"
                        + " width='120' height='72' count='7' animation='transfer' palette='violet'"
                        + " from='left' to='bottom' duration='18' delay='12'/>"))
            .render(Map.of());
    assertEquals(1, c.items.size());
    assertEquals(2, c.effects.size());
    var card = c.effects.getFirst();
    assertEquals(DemoShaders.spec(DemoShaders.Kind.PLAYING_CARD), card.shader());
    assertEquals(38, DemoShaders.a(card) & 63);
    assertEquals(3, (DemoShaders.a(card) >> 6) & 3);
    assertEquals(4, (DemoShaders.a(card) >> 8) & 31);
    assertEquals(2, DemoShaders.a(card) >> 13);
    assertEquals(15, (DemoShaders.b(card) >> 7) & 63);
    assertEquals(2, DemoShaders.b(card) >> 13);
    assertEquals(36, card.lifetimeTicks());
    var chips = c.effects.getLast();
    assertEquals(6, (DemoShaders.a(chips) >> 7) & 7);
    assertEquals(5, (DemoShaders.a(chips) >> 10) & 7);
    assertEquals(35, chips.lifetimeTicks());
    assertEquals(
        18,
        DemoShaders.effect("one", DemoShaders.Kind.CHIP_STACK, 0, 0, 60, 60, 1, 18 | 128)
            .lifetimeTicks());
    assertEquals(
        0,
        DemoShaders.effect("empty", DemoShaders.Kind.CHIP_STACK, 0, 0, 60, 60, 0, 18 | 128)
            .lifetimeTicks());
    assertEquals(36, c.animation.durationTicks());
  }

  @Test
  void everyCardAndUnknownPlaceholderFitEncodingAndStaticDoesNotAnimate() throws Exception {
    for (int value = -1; value < 52; value++) {
      var c =
          DemoTemplates.parse(
                  SHELL.formatted(
                      "<dui-demo-playing-card id='c' width='42' height='81' value='"
                          + value
                          + "'/>"))
              .render(Map.of());
      assertEquals(value < 0 ? 63 : value, DemoShaders.a(c.effects.getFirst()) & 63);
      assertEquals(0, c.effects.getFirst().lifetimeTicks());
    }
  }

  @Test
  void invalidCardAndChipAttributesFailBeforeTheyReachClient() {
    for (String attr :
        List.of(
            "value='52'",
            "delay='3'",
            "animation='spin'",
            "palette='blue'",
            "lift='64'",
            "duration='0'"))
      assertThrows(
          IllegalArgumentException.class,
          () ->
              DemoTemplates.parse(
                      SHELL.formatted(
                          "<dui-demo-playing-card id='a' width='42' height='81' " + attr + "/>"))
                  .render(Map.of()));
    for (String attr : List.of("count='32'", "delay='3'", "from='center'", "animation='flip'"))
      assertThrows(
          IllegalArgumentException.class,
          () ->
              DemoTemplates.parse(
                      SHELL.formatted(
                          "<dui-demo-chip-stack id='a' width='120' height='72' " + attr + "/>"))
                  .render(Map.of()));
  }
}
