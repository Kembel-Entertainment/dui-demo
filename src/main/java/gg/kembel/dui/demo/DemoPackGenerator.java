package gg.kembel.dui.demo;

import gg.kembel.dui.pack.*;
import java.nio.file.*;
import java.util.*;

/** Consumer-owned shader package, installed at build time without changing dui. */
public final class DemoPackGenerator {
  private DemoPackGenerator() {}

  public static void main(String[] args) throws Exception {
    var effects = new ArrayList<PackContribution.Shader>();
    String builtins;
    try (var in = DemoPackGenerator.class.getResourceAsStream("/casino/builtin.glsl")) {
      builtins =
          new String(
              Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
    // One helper module; every concrete effect is registered by logical ID.
    for (var kind : DemoShaders.Kind.values()) {
      String function = "demo" + kind.name().replace("_", "");
      String text =
          "\nvec4 "
              + function
              + "(vec2 q,vec2 size,DUI_PARAMETERS p,float t,bool live){return"
              + " demoEffectPixel(q,size,"
              + kind.originalCode
              + ",p.a,p.b,t,live,live);}";
      effects.add(new PackContribution.Shader(DemoShaders.spec(kind), function, text));
    }
    effects.add(
        new PackContribution.Shader(
            DemoShaders.spec("gift-confetti"), "demoGiftConfetti", giftConfetti()));
    effects.add(
        new PackContribution.Shader(
            DemoShaders.spec("pulse"),
            "demoPulse",
            "vec4 demoPulse(vec2 q,vec2 size,DUI_PARAMETERS p,float t,bool live){vec2"
                + " v=(q-size*.5)/(size*.5);float r=length(v);if(r>1.0)return vec4(0);float"
                + " phase=live?sin(t*5.0)*.15:0.0;return"
                + " vec4(.91,.25,.61,smoothstep(1.0+phase,.65+phase,r));}"));
    for (var name : List.of("race", "prize-wheel", "coin", "temple-reel"))
      effects.add(effect(name));
    PackGenerator.generate(
        Path.of(args[0]),
        Path.of(args[2]),
        Path.of(args[1]),
        List.of(
            new PackContribution(
                "demo",
                Map.of(),
                effects,
                List.of(),
                Map.of("demo:builtin", builtins, "demo:casino", casinoPrimitives()),
                DemoGlyphs.contributions(Path.of(args[0]))),
            WorldMapAssets.contribution(),
            example.proof.ExtensionProof.contribution()));
  }

  private static String casinoPrimitives() throws Exception {
    try (var in = DemoPackGenerator.class.getResourceAsStream("/casino/casino-primitives.glsl")) {
      return new String(
          Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
  }

  private static String giftConfetti() throws Exception {
    try (var in = DemoPackGenerator.class.getResourceAsStream("/casino/gift-confetti.glsl")) {
      return new String(
          Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
  }

  private static PackContribution.Shader effect(String name) throws Exception {
    try (var in = DemoPackGenerator.class.getResourceAsStream("/casino/" + name + ".glsl")) {
      String original =
          new String(
              Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      String function =
          switch (name) {
            case "race" -> "demoRace";
            case "prize-wheel" -> "demoPrizeWheel";
            case "coin" -> "demoCoin";
            default -> "demoTempleReel";
          };
      String wrapper = function + "Typed";
      return new PackContribution.Shader(
          DemoShaders.spec(name),
          wrapper,
          original
              + "\nvec4 "
              + wrapper
              + "(vec2 q,vec2 size,DUI_PARAMETERS p,float t,bool live){return "
              + function
              + "(q,size,p.a,p.b,t,live);}");
    }
  }
}
