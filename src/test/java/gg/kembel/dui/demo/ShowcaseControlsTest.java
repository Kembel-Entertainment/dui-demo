package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

class ShowcaseControlsTest {
  private MenuTemplate template(boolean compact) throws Exception {
    try (var in =
        getClass()
            .getResourceAsStream(compact ? "/ui/showcase-compact.html" : "/ui/showcase.html")) {
      return DemoTemplates.parse(
          new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8),
          DemoTemplates.font(),
          DemoVisualComponents.registry(),
          "demo template");
    }
  }

  @Test
  void compactExamplesFitAndEveryOriginalGraphNodeAndItemRemainsReachable() throws Exception {
    var state = new ShowcaseState();
    state.layout = "compact";
    var template = template(true);
    var nodes = new HashSet<String>();
    var items = new HashSet<String>();
    int examples = 0;
    for (var page : ShowcaseState.PAGES)
      for (boolean dark : List.of(false, true)) {
        state.page = page.id();
        state.dark = dark;
        for (int part = 0; part < state.partCount(); part++) {
          state.part = part;
          var canvas = template.render(state.data());
          examples++;
          assertEquals(300, canvas.width);
          assertEquals(page.id().equals("media") ? 144 : 153, canvas.height);
          assertTrue(canvas.items.size() <= 1);
          for (var hit : canvas.hits) {
            assertSame(
                hit, canvas.at(hit.x() + hit.width() / 2, hit.y() + hit.height() / 2), hit.id());
            if (hit.action().equals("kit_node")) nodes.add(hit.value());
          }
          canvas.items.forEach(i -> items.add(i.id()));
        }
      }
    assertEquals(44, examples);
    assertEquals(Set.of("Origin", "Alpha", "Beta", "Gamma", "Delta"), nodes);
    assertEquals(Set.of("kit_grass", "kit_head", "kit_tool", "kit_banner"), items);
    state.page = "lists";
    state.part = 0;
    state.empty = true;
    template.render(state.data());
    state.empty = false;
    state.listPage = 2;
    template.render(state.data());
  }

  @Test
  void dropdownPopupOwnsItsHitRegionsAndOutsideClickClosesWithoutSelecting() throws Exception {
    for (boolean compact : List.of(false, true)) {
      var state = new ShowcaseState();
      state.page = "actions";
      state.part = compact ? 2 : 0;
      state.dropdownOpen = true;
      var canvas = template(compact).render(state.data());
      var disabled =
          canvas.hits.stream()
              .filter(h -> h.id().equals("kit_dropdown_option_2"))
              .findFirst()
              .orElseThrow();
      assertEquals("", disabled.action());
      assertSame(disabled, canvas.at(disabled.x() + 5, disabled.y() + 5));
      var selected =
          canvas.hits.stream()
              .filter(h -> h.id().equals("kit_dropdown_option_1"))
              .findFirst()
              .orElseThrow();
      assertEquals("beta", selected.value());
      assertSame(selected, canvas.at(selected.x() + 5, selected.y() + 5));
      var outside = canvas.at(1, 1);
      assertEquals("kit_dropdown_close", outside.action());
      state.apply(outside.action(), outside.value());
      assertFalse(state.dropdownOpen);
      assertEquals("alpha", state.variant);
      state.dropdownOpen = true;
      state.apply(selected.action(), selected.value());
      assertEquals("beta", state.variant);
      assertFalse(state.dropdownOpen);
      state.dropdownOpen = true;
      state.apply("kit_dropdown_select", "gamma");
      assertEquals("beta", state.variant);
      assertTrue(state.dropdownOpen);
      var checkbox =
          canvas.hits.stream()
              .filter(h -> h.id().equals("kit_checkbox_locked"))
              .findFirst()
              .orElseThrow();
      assertEquals("", checkbox.action());
    }
  }

  @Test
  void popupCoversNativeObjectsAndRejectsDuplicateOrUnplaceableOptions() throws Exception {
    var canvas = new Canvas(160, 90, DemoTemplates.environment(DemoTemplates.font()));
    canvas.item("under", 12, 27, 18);
    canvas.head(50, 27, "self", true);
    canvas.item("outside", 120, 54, 18);
    var option = new MenuTemplate.Node("option", Map.of("label", "Alpha", "value", "a"), List.of());
    var props =
        Map.of(
            "id", "select", "action", "open", "open", "true", "select", "choose", "dismiss",
            "close");
    var overlays = new ArrayList<Runnable>();
    TestDropdown.draw(
        canvas, new MenuTemplate.Node("dropdown", props, List.of(option)), 0, 0, 100, 27, overlays);
    overlays.forEach(Runnable::run);
    assertEquals(List.of("under", "outside"), canvas.items.stream().map(Canvas.Item::id).toList());
    assertEquals(1, canvas.heads.size());
    assertEquals(
        List.of("outside"), canvas.renderPlan().items.stream().map(Canvas.Item::id).toList());
    assertTrue(canvas.renderPlan().heads.isEmpty());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            TestDropdown.draw(
                new Canvas(160, 90, DemoTemplates.environment(DemoTemplates.font())),
                new MenuTemplate.Node("dropdown", props, List.of(option, option)),
                0,
                0,
                100,
                27,
                new ArrayList<>()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            TestDropdown.draw(
                new Canvas(160, 27, DemoTemplates.environment(DemoTemplates.font())),
                new MenuTemplate.Node("dropdown", props, List.of(option)),
                0,
                0,
                100,
                27,
                new ArrayList<>()));
  }

  @Test
  void preferencesValidateExplicitChoicesAndNavigationDismissesPopups() {
    for (String scale : DisplayPreferences.SCALES)
      for (String layout : List.of("compact", "spacious")) {
        var p = DisplayPreferences.selection(scale, layout);
        assertTrue(p.valid());
        assertTrue(p.configured);
      }
    assertThrows(
        IllegalArgumentException.class, () -> DisplayPreferences.selection("9000", "compact"));
    assertThrows(IllegalArgumentException.class, () -> DisplayPreferences.selection("2", "auto"));
    var invalid = new DisplayPreferences();
    invalid.guiScale = null;
    assertFalse(invalid.valid());
    var state = new ShowcaseState();
    state.page = "actions";
    state.part = 2;
    state.dropdownOpen = true;
    state.apply("kit_checkbox", "");
    assertFalse(state.checkbox);
    state.apply("kit_part", "1");
    assertFalse(state.dropdownOpen);
    assertEquals(3, state.part);
    state.dropdownOpen = true;
    state.apply("kit_page_cycle", "1");
    assertEquals("cards", state.page);
    assertEquals(0, state.part);
    assertFalse(state.dropdownOpen);
  }
}
