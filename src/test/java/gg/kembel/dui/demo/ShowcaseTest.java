package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

class ShowcaseTest {
  private MenuTemplate template() throws Exception {
    try (var in = getClass().getResourceAsStream("/ui/showcase.html")) {
      return MenuTemplate.parse(
          new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8),
          new gg.kembel.dui.core.GlyphFont(),
          gg.kembel.dui.components.VisualComponents.registry(),
          "demo template");
    }
  }

  @Test
  void everyPageAndDynamicLayoutFitsWithoutOverlappingHitTargets() throws Exception {
    var template = template();
    var state = new ShowcaseState();
    for (var page : ShowcaseState.PAGES) {
      state.page = page.id();
      for (int variant = 0; variant < 8; variant++) {
        state.dark = variant >= 4;
        state.listPage = variant % 3;
        state.empty = variant % 4 == 3;
        state.rank = variant % 4;
        state.selectedNode = variant % 4 == 0 ? "Alpha" : "Beta";
        var canvas = template.render(state.data());
        assertEquals(480, canvas.width);
        assertEquals(306, canvas.height);
        assertTrue(
            canvas.paints.stream()
                .anyMatch(
                    p ->
                        p.width() == 480
                            && p.height() == 306
                            && p.color() == (state.dark ? 0x181C28 : 0xF3F0E8)));
        var themeHit =
            canvas.hits.stream().filter(h -> h.id().equals("kit_theme")).findFirst().orElseThrow();
        assertEquals("kit_theme", themeHit.action());
        assertTrue(themeHit.x() > 430 && themeHit.y() < 18);
        assertEquals(7, canvas.hits.stream().filter(h -> h.id().startsWith("kit_nav_")).count());
        for (var hit : canvas.hits) {
          assertSame(
              hit,
              canvas.at(hit.x() + hit.width() / 2, hit.y() + hit.height() / 2),
              page.id() + " / " + hit.id());
          assertTrue(
              hit.action().isBlank() || ShowcaseState.ROUTES.contains(hit.action()), hit.action());
          for (var other : canvas.hits)
            if (hit != other)
              assertFalse(
                  hit.x() < other.x() + other.width()
                      && hit.x() + hit.width() > other.x()
                      && hit.y() < other.y() + other.height()
                      && hit.y() + hit.height() > other.y(),
                  hit.id() + " overlaps " + other.id());
        }
        assertEquals(page.id().equals("media") ? 4 : 0, canvas.items.size());
      }
    }
  }

  @Test
  void exampleInteractionsUseIndependentStateAndStablePayloads() throws Exception {
    var state = new ShowcaseState();
    var other = new ShowcaseState();
    state.apply("kit_press", "");
    state.apply("kit_toggle", "enabled");
    state.apply("kit_choice", "-1");
    state.apply("kit_tab", "Beta");
    assertEquals(1, state.presses);
    assertFalse(state.enabled);
    assertEquals(2, state.choice);
    assertEquals("Beta", state.tab);
    assertEquals(0, other.presses);
    assertTrue(other.enabled);
    state.apply("kit_theme", "");
    state.apply("kit_page", "cards");
    assertTrue(state.dark);
    assertFalse(other.dark);
    assertEquals("sun", state.data().get("themeIcon"));
    state.apply("kit_theme", "");
    assertEquals("moon", state.data().get("themeIcon"));
    for (int i = 0; i < 4; i++) state.apply("kit_progress", "");
    assertEquals(0, state.progress);
    state.page = "lists";
    var seen = new HashSet<String>();
    for (int i = 0; i < 3; i++) {
      for (var hit : template().render(state.data()).hits)
        if (hit.action().equals("kit_row")) {
          state.apply(hit.action(), hit.value());
          assertEquals(hit.value(), state.selectedRow);
          assertTrue(seen.add(hit.value()));
        }
      state.apply("kit_list_page", "1");
    }
    assertEquals(7, seen.size());
    state.apply("kit_list_empty", "");
    assertTrue(state.empty);
    assertTrue(
        template().render(state.data()).hits.stream().noneMatch(h -> h.action().equals("kit_row")));
    state.selectedNode = "Beta";
    state.apply("kit_rank", "");
    assertEquals(1, state.rank);
    state.selectedNode = "Alpha";
    state.apply("kit_rank", "");
    assertEquals(2, state.rank);
    state.stack = 64;
    state.apply("kit_stack", "");
    assertEquals(1, state.stack);
    state.apply("kit_repair", "");
    assertEquals(0, state.damage);
  }

  @Test
  void themeIsOptInAndPreservesNativeIconTint() throws Exception {
    var dark = new Canvas(120, 18);
    dark.text(0, 0, 100, "Sample", 0xEAEAF1);
    assertEquals(0xEAEAF1, dark.paints.getFirst().color());
    var light = new Canvas(120, 18, UiTheme.STUDIO);
    light.text(0, 0, 100, "Sample", 0xEAEAF1);
    assertEquals(0x252938, light.paints.getFirst().color());
    light.icon(0, 0, "item/diamond", 0xFFFFFF);
    assertEquals(0xFFFFFF, light.paints.getLast().color());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MenuTemplate.parse(
                    "<dui-menu theme='missing'/>",
                    new gg.kembel.dui.core.GlyphFont(),
                    gg.kembel.dui.components.VisualComponents.registry(),
                    "demo template")
                .render(Map.of()));
  }
}
