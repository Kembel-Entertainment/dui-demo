package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

/** All application effects and their parameter/lifetime policy belong to the demo. */
public final class DemoShaders {
  private DemoShaders() {}

  public enum Kind {
    WHEEL(0),
    REEL(1),
    LEVER(2),
    COINS(3),
    LIGHTS(4),
    CONFETTI(5),
    PLAYING_CARD(6),
    CHIP_STACK(7);
    public final int originalCode;

    public int code() {
      return originalCode;
    }

    Kind(int code) {
      this.originalCode = code;
    }
  }

  public static ShaderSpec spec(String name) {
    return new ShaderSpec(
        "demo:" + name,
        List.of(
            ShaderSpec.Parameter.integer("a", 0, 32767),
            ShaderSpec.Parameter.integer("b", 0, 32767)));
  }

  public static ShaderSpec spec(Kind kind) {
    return spec(kind.name().toLowerCase(Locale.ROOT).replace('_', '-'));
  }

  public static ShaderInvocation effect(
      String id, Kind kind, int x, int y, int w, int h, int a, int b) {
    return effect(id, spec(kind), x, y, w, h, a, b, lifetime(kind, a, b));
  }

  public static ShaderInvocation effect(
      String id, ShaderSpec spec, int x, int y, int w, int h, int a, int b, int lifetime) {
    return new ShaderInvocation(id, spec, x, y, w, h, Map.of("a", a, "b", b), lifetime);
  }

  public static int a(ShaderInvocation effect) {
    return ((Number) effect.parameters().get("a")).intValue();
  }

  public static int b(ShaderInvocation effect) {
    return ((Number) effect.parameters().get("b")).intValue();
  }

  public static int lifetime(Kind kind, int a, int b) {
    return switch (kind) {
      case WHEEL -> (b & 512) == 0 ? 0 : b & 511;
      case REEL -> b & 127;
      case LEVER -> a;
      case COINS -> (a >> 9) == 0 ? 0 : ((b >> 9) & 63) * 2 + 94;
      case LIGHTS -> 0;
      case PLAYING_CARD -> (a >> 13) == 0 ? 0 : ((a >> 8) & 31) * 2 + (b & 127);
      case CHIP_STACK ->
          ((b >> 7) & 3) == 0 || (a & 31) == 0
              ? 0
              : ((b >> 9) & 63) * 2 + (b & 127) + (int) Math.ceil((Math.min(a & 31, 7) - 1) * .7);
      case CONFETTI -> ((b >> 9) & 63) * 2 + 72;
    };
  }
}
