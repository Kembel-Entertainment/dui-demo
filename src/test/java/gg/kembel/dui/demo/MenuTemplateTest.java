package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class MenuTemplateTest {
  @Test
  void repeatedBindingsStayLiteralAndDisabledControlsDoNotAct() throws Exception {
    var t =
        DemoTemplates.parse(
            "<dui-menu width='240' height='54'><dui-column><dui-repeat items='rows'"
                + " as='r'><dui-button id='{{r.id}}' label='{{r.label}}' action='press'"
                + " locked='{{r.locked}}'/></dui-repeat></dui-column></dui-menu>");
    var c =
        t.render(
            Map.of(
                "rows",
                List.of(
                    Map.of("id", "a", "label", "<script>& test", "locked", false),
                    Map.of("id", "b", "label", "Disabled", "locked", true))));
    assertEquals(2, c.hits.size());
    assertEquals("press", c.hits.getFirst().action());
    assertEquals("", c.hits.getLast().action());
    assertTrue(c.paints.stream().anyMatch(p -> "<script>& test".equals(p.text())));
  }

  @Test
  void invalidTemplatesBindingsAndOverflowAreRejected() throws Exception {
    for (String source :
        List.of(
            "<!DOCTYPE x [<!ENTITY z SYSTEM 'file:///etc/passwd'>]><dui-menu/>",
            "<dui-menu><script/></dui-menu>",
            "<dui-menu onclick='hello'/>",
            "<mc-menu/>")) assertThrows(Exception.class, () -> DemoTemplates.parse(source));
    var t =
        DemoTemplates.parse(
            "<dui-menu width='240' height='18'><dui-button label='{{name}}'/></dui-menu>");
    assertThrows(IllegalArgumentException.class, () -> t.render(Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Canvas(240, 18, DemoTemplates.environment(DemoTemplates.font()))
                .rect(230, 0, 20, 18, 0));
  }

  @Test
  void injectedMetricsDriveCanvasAndTemplateLayout() throws Exception {
    var font = new GlyphFont(Map.of("?", 4, "A", 10, ".", 2, " ", 3));
    var t =
        DemoTemplates.parse(
            "<dui-menu background='none' width='120' height='18'><dui-text label='AAA'"
                + " align='center'/></dui-menu>",
            font);
    var c = t.render(Map.of());
    assertEquals(30, c.paints.getFirst().width());
    assertEquals(45, c.paints.getFirst().x());
  }
}
