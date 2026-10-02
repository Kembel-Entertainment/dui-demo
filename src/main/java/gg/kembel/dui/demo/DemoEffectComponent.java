package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;

/** Turns ordinary layout nodes into typed, bounded shader parameters. */
public final class DemoEffectComponent {
  private DemoEffectComponent() {}

  static boolean supports(String tag) {
    return java.util.Set.of(
            "wheel", "reel", "lever", "particles", "lights", "playing-card", "chip-stack")
        .contains(tag);
  }

  private static int number(MenuTemplate.Node n, String key, int fallback, int min, int max) {
    int value = n.n(key, fallback);
    if (value < min || value > max)
      throw new IllegalArgumentException(key + " must be " + min + ".." + max);
    return value;
  }

  private static int choice(MenuTemplate.Node n, String key, String fallback, String... choices) {
    String value = n.s(key, fallback);
    for (int i = 0; i < choices.length; i++) if (choices[i].equals(value)) return i;
    throw new IllegalArgumentException("Unknown " + key + ": " + value);
  }

  public static void draw(Canvas c, MenuTemplate.Node n, int x, int y, int w, int h) {
    String id = n.s("id", "");
    DemoShaders.Kind kind;
    int a, b;
    switch (n.type()) {
      case "wheel" -> {
        choice(n, "variant", "european", "european");
        int value = number(n, "value", 0, 0, 36);
        int previous = number(n, "previous", value, 0, 36);
        int mode = choice(n, "animation", "static", "static", "spin");
        int palette = choice(n, "palette", "walnut", "walnut", "ebony");
        if (w != h || w < 96)
          throw new IllegalArgumentException("Wheel needs a square of at least 96px");
        kind = DemoShaders.Kind.WHEEL;
        a = value | (previous << 6) | (number(n, "turns", 4, 1, 7) << 12);
        b = number(n, "duration", 140, 20, 511) | (mode << 9) | (palette << 10);
      }
      case "playing-card" -> {
        int card = number(n, "value", -1, -1, 51), delay = number(n, "delay", 0, 0, 62);
        if (delay % 2 != 0) throw new IllegalArgumentException("Card delay uses even ticks");
        int mode = choice(n, "animation", "static", "static", "deal", "flip", "fly");
        int palette = choice(n, "palette", "mint", "classic", "mint", "coral", "violet");
        int duration = number(n, "duration", 18, 1, 127), lift = number(n, "lift", 12, 0, 63);
        if (mode == 3) lift = number(n, "card-height", 48, 18, 63);
        if (w < 12 || h < (mode == 3 ? lift + 6 : lift + 18))
          throw new IllegalArgumentException("Card needs width >=12 and height >= lift+18");
        kind = DemoShaders.Kind.PLAYING_CARD;
        a =
            (card < 0 ? 63 : card)
                | (n.b("face-down") ? 1 << 6 : 0)
                | (n.b("active") ? 1 << 7 : 0)
                | ((delay / 2) << 8)
                | (mode << 13);
        b = duration | (lift << 7) | (palette << 13);
      }
      case "chip-stack" -> {
        int count = number(n, "count", 0, 0, 31), delay = number(n, "delay", 0, 0, 126);
        if (delay % 2 != 0) throw new IllegalArgumentException("Chip delay uses even ticks");
        int mode = choice(n, "animation", "static", "static", "transfer");
        int palette = choice(n, "palette", "gold", "gold", "mint", "coral", "violet");
        int from =
            choice(
                n,
                "from",
                "bottom",
                "top-left",
                "top-right",
                "bottom-left",
                "bottom-right",
                "top",
                "bottom",
                "left",
                "right");
        int to =
            choice(
                n,
                "to",
                "top",
                "top-left",
                "top-right",
                "bottom-left",
                "bottom-right",
                "top",
                "bottom",
                "left",
                "right");
        kind = DemoShaders.Kind.CHIP_STACK;
        a = count | (palette << 5) | (from << 7) | (to << 10);
        b = number(n, "duration", 18, 1, 127) | (mode << 7) | ((delay / 2) << 9);
      }
      case "reel" -> {
        if (!n.s("symbols", "arcade").equals("arcade"))
          throw new IllegalArgumentException("Unknown reel symbol set");
        int sequence = number(n, "sequence", 0, 0, 7);
        int value = number(n, "value", 0, 0, 5),
            previous = number(n, "previous", value, 0, 5),
            turns = number(n, "turns", 18 + sequence * 6, 6, 63);
        int duration = number(n, "duration", 45 + sequence * 11, 1, 127),
            size = number(n, "symbol-size", Math.max(1, (int) Math.min(w * .39, h * .42)), 1, 127);
        kind = DemoShaders.Kind.REEL;
        a = value | (previous << 3) | (turns << 6);
        b = duration | (size << 7);
      }
      case "lever" -> {
        kind = DemoShaders.Kind.LEVER;
        a = number(n, "duration", 18, 1, 127);
        b = 0;
      }
      case "particles" -> {
        if (!java.util.Set.of("coins", "confetti").contains(n.s("effect", "coins")))
          throw new IllegalArgumentException("Unknown particle effect");
        int count = number(n, "count", 24, 0, 63),
            ox = number(n, "origin-x", w / 2, 0, w - 1),
            oy = number(n, "origin-y", h - 1, 0, h - 1),
            delay = number(n, "delay", 70, 0, 126);
        if (delay % 2 != 0)
          throw new IllegalArgumentException("Particle delay uses even world ticks");
        kind =
            n.s("effect", "coins").equals("confetti")
                ? DemoShaders.Kind.CONFETTI
                : DemoShaders.Kind.COINS;
        a = ox | (count << 9);
        b = oy | ((delay / 2) << 9);
      }
      case "lights" -> {
        kind = DemoShaders.Kind.LIGHTS;
        a = number(n, "count", 12, 1, 32);
        b = number(n, "radius", Math.max(1, h / 3), 1, 15);
      }
      default -> throw new IllegalArgumentException("Unknown effect component");
    }
    c.effect(DemoShaders.effect(id, kind, x, y, w, h, a, b));
    if (!n.s("action", "").isBlank())
      c.hit(
          new Canvas.Hit(
              id,
              n.b("locked") ? "" : n.s("action", ""),
              n.s("payload", ""),
              n.s("tooltip", ""),
              x,
              y,
              w,
              h));
  }
}
