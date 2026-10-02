package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class AbstractionContractTest {
  @Test
  void catalogueOwnsResourcesAliasesAndValidation() {
    int[] calls = {0};
    var def =
        new MenuCatalogue.Definition<>(
            "journal", Set.of("notes"), List.of("journal"), "controller", () -> calls[0]++);
    var cat = new MenuCatalogue<>(List.of(def));
    assertEquals(def, cat.find("notes").orElseThrow());
    assertEquals(List.of("journal"), cat.templates());
    cat.validate();
    assertEquals(1, calls[0]);
    assertThrows(IllegalArgumentException.class, () -> new MenuCatalogue<>(List.of(def, def)));
  }

  @Test
  void actionDecoderRejectsBadPayloadButDoesNotSwallowBusinessFailure() {
    var seen = new ArrayList<String>();
    var router = new ActionRouter<List<String>>((ctx, a) -> ctx.add("rejected:" + a));
    router.on("page", Integer::parseInt, (ctx, n) -> ctx.add("page:" + n));
    router.dispatch(seen, "page", "2");
    router.dispatch(seen, "page", "oops");
    router.dispatch(seen, "unknown", "");
    assertEquals(List.of("page:2", "rejected:page", "rejected:unknown"), seen);
    router.on(
        "business",
        ctx -> {
          throw new IllegalStateException("authoritative failure");
        });
    assertThrows(IllegalStateException.class, () -> router.dispatch(seen, "business", ""));
  }

  @Test
  void spansValidateAllCellsAndStableCollectionsPaginate() {
    var b =
        GridLayout.place(
            List.of(
                new GridLayout.Cell("wide", 0, 0, 2, 1), new GridLayout.Cell("tall", 2, 0, 1, 2)),
            3,
            2,
            0,
            0,
            30,
            27,
            3,
            0);
    assertEquals(57, b.getFirst().width());
    assertEquals(54, b.getLast().height());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            GridLayout.place(
                List.of(new GridLayout.Cell("a", 0, 0, 2, 1), new GridLayout.Cell("b", 1, 0, 1, 1)),
                3,
                2,
                0,
                0,
                30,
                27,
                3,
                0));
    var v = CollectionView.of(List.of("one", "two", "three"), 99, 2, s -> s);
    assertEquals("three", v.entries().getFirst().key());
    assertThrows(
        IllegalArgumentException.class,
        () -> CollectionView.of(List.of("same", "same"), 0, 1, s -> s));
  }

  @Test
  void layoutProfilesIncludeBodyFootprintAndCardsReportOverflow() {
    assertEquals(191, new LayoutProfile("compact", 320, 180, 1).requiredDialogHeight());
    assertThrows(
        IllegalArgumentException.class, () -> HorizontalStrip.layout(12, 0, 0, 180, 30, 45, 3));
    assertEquals(3, HorizontalStrip.layout(3, 0, 0, 120, 30, 45, 3).size());
    assertEquals(4, Carousel.window(4, 0, 1, 100, 30, 1).size());
  }

  @Test
  void customThemeAppliesAcrossDropdownAndTextWithoutEnumChanges() throws Exception {
    var theme =
        DemoTheme.DARK.with(
            Map.of(
                "surface", 0xEEEEEE, "raised", 0xFFFFFF, "text", 0x111111, "accent", 0x993399,
                "border", 0xBBBBBB));
    var t =
        DemoTemplates.parse(
            "<dui-menu width=\"180\" height=\"72\"><dui-column><dui-dropdown id=\"pick\""
                + " action=\"toggle\" value=\"a\"><dui-option value=\"a\""
                + " label=\"Alpha\"/></dui-dropdown><dui-text label=\"Example\""
                + " color=\"$accent\"/></dui-column></dui-menu>");
    var canvas = t.render(Map.of(), Map.of(), theme);
    assertTrue(canvas.paints.stream().anyMatch(x -> x.color() == 0x111111));
    assertTrue(canvas.paints.stream().anyMatch(x -> x.color() == 0x993399));
  }

  @Test
  void popupPlanningPreservesSourceAndCoversEveryLateBackend() {
    var c = new Canvas(180, 90, DemoTemplates.environment(DemoTemplates.font()));
    c.head(9, 9, "example", true);
    c.item("native", 9, 9, 16);
    c.image("image", 9, 9, 18, 18, 3, new RasterImage(1, 1, new int[] {0xff0000}));
    c.effect(DemoShaders.effect("light", DemoShaders.Kind.LIGHTS, 9, 9, 18, 18, 2, 2));
    c.cover("popup", 9, 9, 36, 36);
    var scene = Scene.of(c);
    var planned = scene.plan(c);
    assertTrue(planned.heads.isEmpty());
    assertTrue(planned.items.isEmpty());
    assertTrue(planned.images.isEmpty());
    assertTrue(planned.effects.isEmpty());
    assertEquals(1, c.items.size());
    assertEquals(1, scene.items().size());
    assertThrows(UnsupportedOperationException.class, () -> scene.items().clear());
  }

  @Test
  void resourcesKeepFallbackAndCacheBounds() {
    var cache = new BoundedCache<String, RasterImage>(2, 3, i -> (long) i.width * i.height);
    var pixel = new RasterImage(1, 1, new int[] {0});
    cache.put("a", pixel);
    cache.put("b", pixel);
    cache.get("a");
    cache.put("c", pixel);
    assertNull(cache.get("b"));
    assertEquals(2, cache.size());
    var future = new CompletableFuture<String>();
    var handle = new ResourceHandle<>(future, "fallback");
    assertEquals(ResourceHandle.Status.LOADING, handle.snapshot().status());
    future.completeExceptionally(new IllegalStateException("offline"));
    assertEquals("fallback", handle.snapshot().available());
    assertEquals(ResourceHandle.Status.ERROR, handle.snapshot().status());
  }

  @Test
  void alphaCompositionUsesTheSuppliedBackgroundAndContainKeepsLetterbox() {
    var image = new RgbaImage(1, 1, new int[] {0x80ff0000});
    assertEquals(
        0x800000,
        image.flatten(1, 1, RgbaImage.Fit.CONTAIN, RgbaImage.Sampling.NEAREST, 0).rgb(0, 0));
    assertEquals(
        0xff7f7f,
        image.flatten(1, 1, RgbaImage.Fit.CONTAIN, RgbaImage.Sampling.NEAREST, 0xffffff).rgb(0, 0));
    var wide = new RgbaImage(2, 1, new int[] {0xffff0000, 0xff00ff00});
    assertEquals(
        0x123456,
        wide.flatten(2, 4, RgbaImage.Fit.CONTAIN, RgbaImage.Sampling.NEAREST, 0x123456).rgb(0, 0));
  }

  @Test
  void genericMotionFieldsRoundTripAndStillModeIsExplicit() {
    var m =
        new Motion(
            100, 127, 126, Motion.Easing.BACK_OUT, true, -256, 255, 0, 2, -256, 255, 0, 1, 0, 1);
    BigInteger data = decode(m.payload());
    assertEquals(127, field(data, RendererProtocol.DURATION_OFFSET, 7));
    assertEquals(126, field(data, RendererProtocol.DELAY_OFFSET, 7));
    assertEquals(0, field(data, RendererProtocol.TX_OFFSET, 9));
    assertEquals(511, field(data, RendererProtocol.TY_OFFSET, 9));
    assertEquals(128, field(data, RendererProtocol.SCALETO_OFFSET, 8));
    assertFalse(m.still().enabled());
    assertThrows(IllegalArgumentException.class, () -> DemoMotion.pop(1, 10, 256, true));
  }

  @Test
  void explicitLargerBudgetSplitsBoundedPayloads() throws Exception {
    StringBuilder parts = new StringBuilder();
    for (int i = 0; i < 12; i++)
      parts.append("<dui-demo-lights id=\"l" + i + "\" width=\"18\" height=\"18\"/>");
    var c =
        DemoTemplates.parse(
                "<dui-menu width=\"180\" height=\"90\" effect-budget=\"16\"><dui-layer"
                    + " height=\"fill\">"
                    + parts
                    + "</dui-layer></dui-menu>")
            .render(Map.of());
    assertEquals(12, c.effects.size());
    assertEquals(4, RenderReport.of(c).remainingEffects());
    assertEquals(18 + 8 * 24, TestTransport.animationPayload(c, c.effects.subList(0, 8)).size());
    assertThrows(IllegalArgumentException.class, () -> TestTransport.animationPayload(c));
  }

  @Test
  void trackedEffectsAndExtensionKindsRoundTrip() {
    var c = new Canvas(180, 90, DemoTemplates.environment(DemoTemplates.font()));
    c.animation = new Canvas.Animation("effects", 10, true, 24);
    var e = DemoShaders.effect("pulse", DemoShaders.spec("pulse"), 1, 2, 30, 27, 32767, 32767, 24);
    c.effect(e);
    c.effectMotion(e.id(), Motion.slide(10, 24, 30, true));
    var p = TestTransport.animationPayload(c);
    assertEquals(18 + 24 + 33, p.size());
    BigInteger header = decode(p.subList(0, 18));
    assertEquals(6, field(header, 51, 3));
    BigInteger payload = decode(p.subList(18, 42));
    assertEquals(0, field(payload, 0, 6));
    assertEquals(1, field(payload, 6, 9));
    assertEquals(32767, field(payload, 42, 15));
    assertEquals(32767, field(payload, 57, 15));
  }

  @Test
  void cachedProviderSharesInFlightWorkAcrossConsumers() {
    int[] calls = {0};
    var pending = new CompletableFuture<String>();
    var provider =
        new CachedResourceProvider<String, String>(
            2,
            2,
            key -> 1,
            key -> {
              calls[0]++;
              return pending;
            },
            key -> "fallback");
    var a = provider.resolve("asset:v1");
    var b = provider.resolve("asset:v1");
    assertSame(a, b);
    assertEquals(1, calls[0]);
    pending.complete("ready");
    assertEquals("ready", b.snapshot().available());
    provider.resolve("b");
    provider.resolve("c");
    assertEquals(2, provider.retained());
  }

  @Test
  void unsupportedPropertiesFailWithComponentAndResource() throws Exception {
    var error =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                DemoTemplates.parse(
                    "<dui-menu><dui-text symbols=\"nonsense\"/></dui-menu>",
                    DemoTemplates.font(),
                    ComponentRegistry.EMPTY,
                    "journal.html"));
    assertTrue(error.getMessage().contains("journal.html"));
    assertTrue(error.getMessage().contains("symbols"));
  }

  @Test
  void constraintsAndDockingUseDeclaredGeometry() throws Exception {
    var t =
        DemoTemplates.parse(
            "<dui-menu width=\"180\" height=\"90\"><dui-layer height=\"fill\"><dui-text"
                + " dock=\"top\" height=\"18\" label=\"Header\"/><dui-button dock=\"bottom\""
                + " height=\"18\" id=\"done\" action=\"done\""
                + " label=\"Done\"/></dui-layer></dui-menu>");
    assertEquals(72, t.render(Map.of()).hits.getFirst().y());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DemoTemplates.parse(
                    "<dui-menu width=\"180\" height=\"90\"><dui-text"
                        + " min-width=\"200\"/></dui-menu>")
                .render(Map.of()));
  }

  @Test
  void cacheRemembersAdmissionWeightsAndCannotOverflow() {
    var cache = new BoundedCache<String, long[]>(3, Long.MAX_VALUE, v -> v[0]);
    long[] mutable = {Long.MAX_VALUE - 1};
    cache.put("a", mutable);
    mutable[0] = 0;
    cache.put("b", new long[] {2});
    assertNull(cache.get("a"));
    assertEquals(2, cache.weight());
    cache.retain(Set.of());
    assertEquals(0, cache.weight());
  }

  @Test
  void spacingAndDisabledStateResolveBeforeExplicitOverrides() throws Exception {
    var template =
        DemoTemplates.parse(
            "<dui-menu width=\"180\" height=\"54\" padding=\"$medium\"><dui-style id=\"primary\""
                + " fill=\"$raised\" color=\"$text\" disabled-color=\"$danger\"/><dui-button"
                + " id=\"a\" action=\"go\" label=\"Locked\" class=\"primary\" locked=\"true\""
                + " height=\"18\"/></dui-menu>");
    var canvas = template.render(Map.of(), Map.of(), DemoTheme.DARK);
    assertEquals(6, canvas.hits.getFirst().x());
    assertTrue(
        canvas.paints.stream()
            .anyMatch(
                p -> "Locked".equals(p.text()) && p.color() == DemoTheme.DARK.color("danger")));
  }

  @Test
  void nativeAndProceduralPropertiesShareMotionParser() throws Exception {
    var canvas =
        DemoTemplates.parse(
                "<dui-menu width=\"180\" height=\"54\" animation-start=\"120\"><dui-layer"
                    + " height=\"fill\"><dui-demo-item id=\"gift\" x=\"9\" y=\"9\" size=\"18\""
                    + " width=\"18\" height=\"18\" scale-from=\"0.5\""
                    + " rotate-from=\"-20\"/></dui-layer></dui-menu>")
            .render(Map.of());
    assertEquals(120, canvas.motions.get("gift").startedAt());
    assertEquals(.5, canvas.motions.get("gift").scaleFrom());
    assertEquals(-20, canvas.motions.get("gift").rotateFrom());
    assertEquals(canvas.metrics(), canvas.renderPlan().metrics());
  }

  @Test
  void packagesMergeAndHyphenatedPropertiesBindWithoutExpressions() throws Exception {
    var a =
        ComponentRegistry.builder()
            .template(
                "journal-note",
                Set.of("is-locked"),
                "<dui-fragment><dui-button id=\"note\" action=\"open\""
                    + " locked=\"{{props.is-locked}}\" label=\"Note\"/></dui-fragment>")
            .build();
    var combined = ComponentRegistry.builder().include(a).build();
    var c =
        DemoTemplates.parse(
                "<dui-menu width=\"180\" height=\"36\"><dui-journal-note"
                    + " is-locked=\"{{disabled}}\"/></dui-menu>",
                DemoTemplates.font(),
                combined,
                "journal.html")
            .render(Map.of("disabled", true));
    assertEquals("", c.hits.getFirst().action());
    assertThrows(
        IllegalArgumentException.class, () -> ComponentRegistry.builder().include(a).include(a));
  }

  @Test
  void batchingKeepsDrawOrderAndDoesNotInflateOrdinaryPayloads() {
    var c = new Canvas(180, 90, DemoTemplates.environment(DemoTemplates.font()));
    c.effectLimit = 32;
    c.animation = new Canvas.Animation("carrier", 100, true, 24);
    for (int i = 0; i < 14; i++)
      c.effect(DemoShaders.effect("e" + i, DemoShaders.Kind.LIGHTS, 0, 0, 18, 18, 2, 2));
    c.effectMotion("e13", Motion.slide(100, 24, 20, true));
    var batches = EffectBatches.of(c);
    assertEquals(List.of(8, 5, 1), batches.stream().map(List::size).toList());
    assertEquals(c.effects, batches.stream().flatMap(List::stream).toList());
    assertEquals(18 + 8 * 24, TestTransport.animationPayload(c, batches.getFirst()).size());
    c.effectMotion("e6", DemoMotion.pop(100, 24, 20, true));
    var interleaved = EffectBatches.of(c);
    assertEquals(c.effects, interleaved.stream().flatMap(List::stream).toList());
    assertTrue(
        interleaved.stream()
            .filter(b -> b.stream().anyMatch(e -> c.effectMotions.containsKey(e.id())))
            .allMatch(b -> b.size() <= 3));
  }

  @Test
  void globalMotionOffAppliesToImperativelySuppliedNativeTracks() {
    var c = new Canvas(180, 90, DemoTemplates.environment(DemoTemplates.font()));
    c.item("reward", 9, 9, 18);
    c.motion("reward", DemoMotion.pop(100, 24, 20, true));
    c.motionEnabled = false;
    var payload = ItemTransport.motionPayload(c, c.items.getFirst(), c.motions.get("reward"));
    assertEquals(
        0,
        field(
            decode(payload.subList(37, payload.size())),
            RendererProtocol.ENABLED_OFFSET,
            RendererProtocol.ENABLED_BITS));
  }

  private static BigInteger decode(List<Integer> colors) {
    BigInteger data = BigInteger.ZERO;
    for (int i = 0; i < colors.size(); i++) {
      int c = colors.get(i),
          b =
              ((c & 0xff0000) != 0 ? 1 : 0)
                  | ((c & 0xff00) != 0 ? 2 : 0)
                  | ((c & 0xff) != 0 ? 4 : 0);
      data = data.or(BigInteger.valueOf(b).shiftLeft(i * 3));
    }
    return data;
  }

  private static int field(BigInteger data, int offset, int bits) {
    return data.shiftRight(offset)
        .and(BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE))
        .intValue();
  }
}
