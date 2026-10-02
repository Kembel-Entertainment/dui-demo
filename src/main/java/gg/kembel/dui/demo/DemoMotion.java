package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

public final class DemoMotion {
  public static Motion pop(long tick, int duration, int distance, boolean enabled) {
    return new Motion(
        tick,
        duration,
        0,
        Motion.Easing.BACK_OUT,
        enabled,
        0,
        distance,
        0,
        1,
        -10,
        0,
        0,
        1,
        .5,
        .5);
  }

  public static Motion preset(String name, long tick, int duration, int distance, boolean enabled) {
    return switch (name) {
      case "pop" -> pop(tick, duration, distance, enabled);
      case "slide" -> Motion.slide(tick, duration, distance, enabled);
      case "lift" ->
          new Motion(
              tick,
              duration,
              0,
              Motion.Easing.EASE_OUT,
              enabled,
              0,
              distance,
              1,
              1,
              -14,
              0,
              1,
              1,
              .5,
              .5);
      case "bounce" ->
          new Motion(
              tick,
              duration,
              0,
              Motion.Easing.BACK_OUT,
              enabled,
              0,
              -distance / 5,
              .85,
              1,
              -5,
              0,
              1,
              1,
              .5,
              .5);
      default -> throw new IllegalArgumentException("Unknown demo item preset: " + name);
    };
  }

  public static void draw(ComponentContext ctx) {
    var n = ctx.node();
    var props = new HashMap<>(n.props());
    for (String name :
        List.of(
            "transition",
            "transition-start",
            "transition-duration",
            "transition-distance",
            "burst-start")) props.remove(name);
    ctx.children()
        .draw(
            new MenuTemplate.Node("item", props, List.of()),
            ctx.x(),
            ctx.y(),
            ctx.width(),
            ctx.height());
    String preset = n.s("transition", "");
    if (!preset.isBlank())
      ctx.canvas()
          .motion(
              n.s("id", ""),
              preset(
                  preset,
                  Long.parseLong(n.s("transition-start", "0")),
                  n.n("transition-duration", 24),
                  n.n("transition-distance", 36),
                  ctx.canvas().motionEnabled));
    long start = Long.parseLong(n.s("burst-start", "-1"));
    if (start >= 0) {
      var c = ctx.canvas();
      c.animationStart = start;
      c.effect(
          DemoShaders.effect(
              n.s("id", "") + "_confetti",
              DemoShaders.spec("gift-confetti"),
              0,
              0,
              c.width,
              c.height,
              0,
              0,
              96));
    }
  }
}
