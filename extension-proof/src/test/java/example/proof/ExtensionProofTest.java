package example.proof;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ExtensionProofTest {
  @Test
  void dynamicConsumerUsesAllRegisteredCapabilitiesWithoutPatches() {
    var env =
        RenderEnvironment.plain(new GlyphFont(Map.of("?", 6)))
            .withResources(
                Map.of(
                    "dui:default",
                    new BitmapFont(
                        "dui:default",
                        9,
                        Map.of("?", new BitmapFont.Glyph(1, 1, 2, List.of(0xffffffff)))),
                    "proof:display",
                    ExtensionProof.displayFont()),
                Map.of())
            .withPlayerRenderers(PlayerRenderBinding.bind(List.of(ExtensionProof.PUPPET)));
    var initial = ExtensionProof.dynamic(env, 0);
    var middle = ExtensionProof.dynamic(env, 40);
    var end = ExtensionProof.dynamic(env, 80);
    assertEquals(18, initial.hits.getFirst().x());
    assertEquals(54, middle.hits.getFirst().x());
    assertEquals(18, end.hits.getFirst().x());
    assertEquals("proof:puppet", middle.playerModels.getFirst().renderer());
    assertEquals(135, middle.playerModels.getFirst().height());
    assertEquals(1, middle.playerModels.getFirst().facing());
    assertEquals("proof:note", end.primitives.getFirst().type());
    assertEquals(.9, end.motions.get("native/gem").opacityTo());
  }

  @Test
  void extendsWithoutDemoOrLibrarySourceChanges() throws Exception {
    var font = new GlyphFont(Map.of("?", 6, "R", 6, "u", 6, "n", 6));
    var environment = new RenderEnvironment(font, ThemeTokens.EMPTY, ExtensionProof.skins());
    var template =
        MenuTemplate.parse(
            "<dui-menu width=\"180\" height=\"90\""
                + " animation-start=\"7\"><dui-column><dui-proof-ring id=\"own\" height=\"54\""
                + " inner=\"#DE2299\" segments=\"18\"/><dui-button id=\"run\" action=\"run\""
                + " label=\"Run\" height=\"27\"/></dui-column></dui-menu>",
            environment,
            ExtensionProof.components());
    var canvas = template.render(Map.of());
    assertEquals(1, canvas.effects.size());
    assertEquals("run", canvas.at(10, 60).action());
    var invocation = canvas.effects.getFirst();
    assertTrue(invocation.shader().bits() > 30);
    assertEquals(
        invocation.parameters(),
        ShaderParameters.decode(
            invocation.shader(),
            ShaderParameters.encode(invocation.shader(), invocation.parameters())));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MenuTemplate.parse(
                    "<dui-menu width=\"180\" height=\"90\"><dui-proof-ring"
                        + " segments=\"100\"/></dui-menu>",
                    environment,
                    ExtensionProof.components())
                .render(Map.of()));
    assertEquals(
        "proof:ring", ExtensionProof.contribution().shaders().getFirst().specification().id());
    assertEquals(
        "proof:cross", ExtensionProof.contribution().glyphs().getFirst().specification().id());
  }

  @Test
  void skinPropertiesAndFragmentDefaultsAreCallerOwned() throws Exception {
    var env =
        new RenderEnvironment(
            new GlyphFont(Map.of("?", 6)),
            new ThemeTokens(Map.of("ink", 0x123456), Map.of(), Map.of()),
            ExtensionProof.skins());
    var registry =
        ComponentRegistry.builder()
            .include(ExtensionProof.components())
            .template(
                "proof-label",
                new PropertySchema(
                    Map.of("label", PropertySchema.Property.string(false, "Default"))),
                "<dui-fragment><dui-text label=\"{{props.label}}\" color=\"$ink\""
                    + " height=\"18\"/></dui-fragment>")
            .build();
    var source =
        "<dui-menu width=\"180\" height=\"90\"><dui-column><dui-button height=\"27\" label=\"Own\""
            + " skin-tint=\"$ink\"/><dui-proof-label/><dui-proof-ring inner=\"$ink\""
            + " height=\"45\"/></dui-column></dui-menu>";
    var result = MenuTemplate.parse(source, env, registry).render(Map.of());
    assertEquals(0x123456, result.paints.getFirst().color());
    assertTrue(result.paints.stream().anyMatch(p -> p.text() != null && p.text().startsWith("?")));
    assertEquals(0x123456, result.effects.getFirst().parameters().get("inner"));
    assertThrows(
        IllegalArgumentException.class,
        () -> MenuTemplate.parse(source.replace("skin-tint", "unknown-skin-key"), env, registry));
  }
}
