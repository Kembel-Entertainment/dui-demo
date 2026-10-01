package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import gg.kembel.dui.testing.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AcceptanceTest {
  @Test
  void unrelatedMenuPagesTwelveEntriesThroughPublicApis() throws Exception {
    String source =
        new String(
            Objects.requireNonNull(getClass().getResourceAsStream("/ui/acceptance.html"))
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);
    var template =
        MenuTemplate.parse(
            source,
            new gg.kembel.dui.core.GlyphFont(),
            gg.kembel.dui.components.VisualComponents.registry(),
            "demo template");
    for (boolean compact : List.of(false, true))
      for (int page = 0; page < 4; page++) {
        var data = AcceptanceView.data(page, compact);
        var c =
            template.render(
                data, AcceptanceView.images(), ThemeTokens.DARK.with(Map.of("accent", 0xFDBA74)));
        RenderAssertions.visibleHits(c);
        RenderAssertions.budget(c);
        assertEquals(compact ? 320 : 480, c.width);
        RenderAssertions.action(
            c,
            "previous",
            Page.of(AcceptanceView.ITEMS, page, compact ? 3 : 6).index() == 0
                ? ""
                : "acceptance_page",
            Page.of(AcceptanceView.ITEMS, page, compact ? 3 : 6).index() == 0 ? "" : "-1");
      }
  }
}
