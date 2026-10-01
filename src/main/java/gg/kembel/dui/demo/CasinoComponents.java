package gg.kembel.dui.demo;

import gg.kembel.dui.components.VisualComponents;
import gg.kembel.dui.core.*;
import java.util.*;

/** Registers consumer effects through the existing public extension APIs. */
public final class CasinoComponents {
  private CasinoComponents() {}

  public static ComponentRegistry registry() {
    var builder = ComponentRegistry.builder().include(VisualComponents.registry());
    try (var in = CasinoComponents.class.getResourceAsStream("/casino/chrome.xml")) {
      builder.template(
          "demo-arcade-chrome",
          Set.of(
              "title",
              "subtitle",
              "compact",
              "wide",
              "background",
              "panel",
              "accent",
              "ink",
              "motion-label",
              "size-label",
              "help-label",
              "busy",
              "credits",
              "returned",
              "status",
              "wager",
              "play-label",
              "play-locked",
              "less-locked",
              "more-locked"),
          new String(
              Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Arcade chrome", e);
    }
    register(builder, "race", 9);
    register(builder, "prize-wheel", 10);
    register(builder, "coin", 11);
    register(builder, "temple-reel", 12);
    return builder.build();
  }

  private static void register(ComponentRegistry.Builder builder, String name, int code) {
    builder.renderer(
        "demo-" + name,
        Set.of("id", "a", "b", "duration"),
        90,
        ctx -> {
          var node = ctx.node();
          int duration = node.n("duration", 0);
          ctx.canvas()
              .effect(
                  new ShaderEffect(
                      node.s("id", ""),
                      new ShaderEffect.Extension("demo:" + name, code, duration),
                      ctx.x(),
                      ctx.y(),
                      ctx.width(),
                      ctx.height(),
                      node.n("a", 0),
                      node.n("b", 0)));
        });
  }
}
