package gg.kembel.dui.demo;

import gg.kembel.dui.pack.*;
import java.nio.file.*;
import java.util.*;

/** Consumer-owned shader package, installed at build time without changing dui. */
public final class DemoPackGenerator {
  private DemoPackGenerator() {}

  public static void main(String[] args) throws Exception {
    var pulse =
        new PackContribution.Effect(
            "demo:pulse",
            8,
            "demoPulse",
            "vec4 demoPulse(vec2 q,vec2 size,int a,int b,float t,bool live){vec2"
                + " p=(q-size*.5)/(size*.5);float r=length(p);if(r>1.0)return vec4(0);float"
                + " phase=live?sin(t*5.0)*.15:0.0;return"
                + " vec4(.91,.25,.61,smoothstep(1.0+phase,.65+phase,r));}");
    PackGenerator.generate(
        Path.of(args[0]),
        Path.of(args[2]),
        Path.of(args[1]),
        List.of(
            new PackContribution(
                "demo",
                Map.of(),
                List.of(
                    pulse,
                    effect("race", 9, "demoRace"),
                    effect("prize-wheel", 10, "demoPrizeWheel"),
                    effect("coin", 11, "demoCoin"),
                    effect("temple-reel", 12, "demoTempleReel")))));
  }

  private static PackContribution.Effect effect(String name, int code, String function)
      throws Exception {
    try (var in = DemoPackGenerator.class.getResourceAsStream("/casino/" + name + ".glsl")) {
      return new PackContribution.Effect(
          "demo:" + name,
          code,
          function,
          new String(
              Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }
  }
}
