package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

final class TestTransport {
  static List<Integer> animationPayload(Canvas c) {
    return animationPayload(c, c.effects);
  }

  static List<Integer> animationPayload(Canvas c, List<ShaderInvocation> effects) {
    return ItemTransport.animationPayload(
        c,
        effects,
        ShaderRegistry.bind(c.effects.stream().map(ShaderInvocation::shader).distinct().toList()));
  }

  static List<Integer> effectPayload(ShaderInvocation e) {
    var b = new ShaderParameters.Bits();
    b.put(0, 6);
    b.put(e.x(), 9);
    b.put(e.y(), 9);
    b.put(e.width(), 9);
    b.put(e.height(), 9);
    for (var p : e.shader().parameters()) b.put(p.encode(e.parameters().get(p.name())), p.bits());
    return b.colors();
  }
}
