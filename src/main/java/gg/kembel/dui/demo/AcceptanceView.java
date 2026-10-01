package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

/** An unrelated application assembled using only public dui APIs. */
public final class AcceptanceView {
  public static final List<String> ITEMS =
      List.of(
          "Rainfall monitor",
          "Garden irrigation",
          "Solar capacity",
          "Air quality",
          "Warehouse lights",
          "Energy storage",
          "Greenhouse climate",
          "Water reserve",
          "Wind forecast",
          "Delivery schedule",
          "Sensor alerts",
          "Team checklist");

  private AcceptanceView() {}

  private static final CachedResourceProvider<String, RasterImage> resources =
      new CachedResourceProvider<>(
          2,
          1024,
          key -> 256,
          key ->
              java.util.concurrent.CompletableFuture.completedFuture(
                  new RgbaImage(1, 1, new int[] {0x80FDBA74})
                      .flatten(
                          16, 16, RgbaImage.Fit.CONTAIN, RgbaImage.Sampling.NEAREST, 0x172438)),
          key -> null);

  private static final ResourceHandle<RasterImage> mark = resources.resolve("journal_mark:v1:dark");

  public static Map<String, RasterImage> images() {
    return Map.of("journal_mark", mark.snapshot().available());
  }

  public static Map<String, Object> data(int requested, boolean compact) {
    var collection =
        CollectionView.of(
            ITEMS,
            requested,
            compact ? 3 : 6,
            name -> name.toLowerCase(Locale.ROOT).replace(' ', '_'));
    return Map.of(
        "width",
        LayoutProfile.choose(compact).width(),
        "height",
        compact ? 162 : 252,
        "rows",
        collection.entries().stream().map(e -> Map.of("id", e.key(), "label", e.value())).toList(),
        "page",
        (collection.page().index() + 1) + " / " + collection.page().pages(),
        "previous",
        collection.page().index() == 0,
        "next",
        collection.page().index() + 1 >= collection.page().pages());
  }
}
