package example.proof;

import gg.kembel.dui.core.*;
import gg.kembel.dui.pack.*;
import java.util.*;

/** Independent consumer: no imports from the demo and no library source modifications. */
public final class ExtensionProof {
  public static final PlayerRenderSpec PUPPET =
      new PlayerRenderSpec(
          "proof:puppet",
          List.of(new PlayerRenderSpec.Viewport(90, 135)),
          List.of(
              new PlayerRenderSpec.Pose(25, -8, 40, List.of(0., 0., -22., 18., 8., -8.), 3, 1.2),
              new PlayerRenderSpec.Pose(-35, -4, 38, List.of(0., 0., 32., -26., -8., 8.), 2, 2)));

  public static BitmapFont displayFont() {
    var glyphs = new HashMap<String, BitmapFont.Glyph>();
    String[] shapes = {"111101101101111", "110101101101110", "101101101101111", "111010010010111"};
    String letters = "?DUI";
    for (int i = 0; i < letters.length(); i++) {
      var pixels = new ArrayList<Integer>();
      for (char value : shapes[i].toCharArray()) pixels.add(value == '1' ? 0xffffffff : 0);
      glyphs.put(letters.substring(i, i + 1), new BitmapFont.Glyph(3, 5, 4, pixels));
    }
    return new BitmapFont("proof:display", 9, glyphs);
  }

  public static final AnimationTrack SLIDE =
      new AnimationTrack(
          List.of(
              new AnimationTrack.Keyframe(0, 0),
              new AnimationTrack.Keyframe(40, 36),
              new AnimationTrack.Keyframe(80, 0)),
          AnimationTrack.Playback.ONCE);

  public static final String DYNAMIC_TEMPLATE =
      """
      <dui-menu width="360" height="216"><dui-layer>
        <dui-rect width="fill" height="fill" fill="#101C2A"/>
        <dui-text x="18" y="18" width="324" label="INDEPENDENT EXTENSION LAB" color="#FFFFFF"/>
        <dui-group x="18" y="54" width="126" height="72" translate-x="{{offset}}" opacity="0.8"
                   clip-x="18" clip-y="54" clip-width="162" clip-height="72">
          <dui-proof-rows entries="{{entries}}"/>
        </dui-group>
        <dui-text x="18" y="144" width="180" label="Shared geometry / live input" color="#80E6D1"/>
        <dui-text x="18" y="162" width="180" label="Typed data / measured layout" color="#A7BCDA"/>
        <dui-hitbox id="animate" x="18" y="189" width="144" height="18" action="animate" tooltip="Replay the shared track"/>
        <dui-rect x="18" y="189" width="144" height="18" fill="#264D60"/>
        <dui-text x="27" y="189" width="126" label="Replay animation" color="#FFFFFF"/>
        <dui-player-model id="puppet" source="self" renderer="proof:puppet" x="252" y="54" width="90" height="135" facing="{{pose}}" idle="true"/>
      </dui-layer></dui-menu>
      """;

  public static Canvas dynamic(RenderEnvironment environment, long age) {
    try {
      var template =
          MenuTemplate.parse(DYNAMIC_TEMPLATE, environment, components(), "proof:dynamic");
      var canvas =
          template.render(
              Map.of(
                  "offset",
                  Math.round(SLIDE.sample(age, true)),
                  "pose",
                  age < 40 ? 0 : 1,
                  "entries",
                  List.of("Typed list", "Measured rows", "Click while moving")),
              Map.of());
      canvas.text(
          189,
          27,
          40,
          new RichText(List.of(new RichText.Span("DUI", "proof:display", 0xFFD375, 1))));
      var transform =
          Transform2D.translation(204, 99)
              .multiply(Transform2D.rotation(age * 1.5))
              .multiply(Transform2D.translation(-30, -30));
      canvas.group(
          "native",
          new SceneGroup(transform, new Scene.Rect(180, 54, 63, 72), .9),
          c -> c.item("gem", 18, 18, 24));
      canvas.primitive(
          new RenderPrimitive(
              "note",
              "proof:note",
              new Scene.Rect(0, 0, 1, 1),
              Map.of("text", "Consumer backend / no demo imports")));
      canvas.hideFocusOutline = true;
      canvas.focusOutlineColor = 0x101C2A;
      return canvas;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static final ShaderSpec RING =
      new ShaderSpec(
          "proof:ring",
          List.of(
              ShaderSpec.Parameter.rgb("inner"),
              ShaderSpec.Parameter.rgb("outer"),
              ShaderSpec.Parameter.decimal("thickness", .05, .5, .01),
              ShaderSpec.Parameter.integer("segments", 3, 24),
              ShaderSpec.Parameter.enumeration("mode", "solid", "pulse", "orbit"),
              ShaderSpec.Parameter.bool("invert")));

  public static ShaderInvocation ring(String id, int x, int y, int width, int height) {
    return new ShaderInvocation(
        id,
        RING,
        x,
        y,
        width,
        height,
        Map.of(
            "inner",
            0x4DDCCA,
            "outer",
            0xCD5293,
            "thickness",
            .18,
            "segments",
            12,
            "mode",
            "pulse",
            "invert",
            false),
        0);
  }

  public static ComponentRegistry components() {
    return ComponentRegistry.builder()
        .renderer(
            "proof-ring",
            new PropertySchema(
                Map.of(
                    "segments",
                    PropertySchema.Property.integer(3, 24, "12"),
                    "inner",
                    PropertySchema.Property.color("#4DDCCA"))),
            54,
            ctx -> {
              var defaults = new HashMap<>(ring("r", 0, 0, 1, 1).parameters());
              defaults.put("segments", Integer.parseInt(ctx.node().s("segments", "12")));
              defaults.put("inner", ctx.node().s("inner", "#4DDCCA"));
              ctx.canvas()
                  .effect(
                      new ShaderInvocation(
                          ctx.node().s("id", "ring"),
                          RING,
                          ctx.x(),
                          ctx.y(),
                          ctx.width(),
                          ctx.height(),
                          defaults,
                          0));
            })
        .renderer(
            "proof-rows",
            new ComponentContract(
                new PropertySchema(Map.of()),
                Map.of(
                    "entries",
                    new ComponentContract.Value(
                        ValueCodec.list(ValueCodec.string(), 8), true, null))),
            ctx ->
                new Measure.Size(
                    ctx.constraints().maxWidth(),
                    ctx.node().value("entries", List.class).size() * 18),
            ctx -> {
              var entries = ctx.node().value("entries", List.class);
              for (int i = 0; i < entries.size(); i++) {
                int y = ctx.y() + i * 18;
                ctx.canvas().rect(ctx.x(), y, ctx.width(), 18, i % 2 == 0 ? 0x197B74 : 0x355D99);
                ctx.canvas()
                    .text(ctx.x() + 3, y, ctx.width() - 6, entries.get(i).toString(), 0xffffff);
                ctx.canvas()
                    .hit(
                        new Canvas.Hit(
                            "row_" + i,
                            "choose",
                            String.valueOf(i),
                            "Independent typed row",
                            ctx.x(),
                            y,
                            ctx.width(),
                            18));
              }
            })
        .build();
  }

  public static WidgetSkinRegistry skins() {
    var skin =
        new WidgetSkinRegistry.Skin(
            27,
            0,
            0,
            18,
            (ctx, part) -> {
              ctx.canvas()
                  .rect(
                      ctx.x(),
                      ctx.y(),
                      ctx.width(),
                      ctx.height(),
                      Integer.parseInt(ctx.node().s("skin-tint", "#16303F").substring(1), 16));
              ctx.canvas()
                  .text(
                      ctx.x() + 3,
                      ctx.y() + 9,
                      ctx.width() - 6,
                      ctx.node().s("label", ""),
                      0x76EEDC);
            },
            (node, w, h) -> Map.of(),
            new PropertySchema(Map.of("skin-tint", PropertySchema.Property.color("#16303F"))));
    return new WidgetSkinRegistry(Map.of("button", skin, "checkbox", skin, "dropdown", skin));
  }

  public static PackContribution contribution() {
    var image = new java.awt.image.BufferedImage(9, 9, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    for (int i = 0; i < 9; i++) {
      image.setRGB(i, i, 0xFFFFFFFF);
      image.setRGB(8 - i, i, 0xFFFFFFFF);
    }
    try {
      var out = new java.io.ByteArrayOutputStream();
      javax.imageio.ImageIO.write(image, "png", out);
      return new PackContribution(
          "proof",
          Map.of(),
          List.of(new PackContribution.Shader(RING, "proofRing", GLSL)),
          List.of(),
          Map.of(),
          List.of(
              new PackContribution.Glyph(new GlyphSpec("proof:cross", 9, 9), out.toByteArray())),
          List.of(displayFont()),
          Set.of("proof:note"),
          List.of(PUPPET));
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  private static final String GLSL =
      """
      vec4 proofRing(vec2 q,vec2 size,DUI_PARAMETERS p,float t,bool live){
       vec2 v=(q-size*.5)/min(size.x,size.y)*2.0;float r=length(v);float phase=live&&p.mode==1?.06*sin(t*5.0):0.0;
       float ring=step(.70-p.thickness+phase,r)*step(r,.70+phase);if(p.invert)ring=1.0-ring;
       float angle=atan(v.y,v.x);float segment=fract((angle+3.141593)/6.283185*float(p.segments));
       return vec4(mix(p.inner,p.outer,segment),ring);
      }
      """;
}
