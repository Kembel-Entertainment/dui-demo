package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

/** Optional namespaced visual package; business rules and assets are supplied by the consumer. */
public final class DemoVisualComponents {
  private DemoVisualComponents() {}

  public static ComponentRegistry registry() {
    var builder = ComponentRegistry.builder();
    builder.template(
        "demo-header",
        Set.of("title", "detail"),
        "<dui-fragment><dui-column height=\"36\" gap=\"9\"><dui-heading label=\"{{props.title}}\""
            + " height=\"18\" color=\"$accent\"/><dui-text label=\"{{props.detail}}\" height=\"9\""
            + " color=\"$muted\"/></dui-column></dui-fragment>");
    builder.template(
        "demo-pagination",
        Set.of("label", "previous", "next", "previous-locked", "next-locked"),
        "<dui-fragment><dui-row height=\"18\" gap=\"9\"><dui-button id=\"previous\""
            + " action=\"{{props.previous}}\" payload=\"-1\" label=\"Previous\""
            + " locked=\"{{props.previous-locked}}\"/><dui-text label=\"{{props.label}}\""
            + " align=\"center\"/><dui-button id=\"next\" action=\"{{props.next}}\" payload=\"1\""
            + " label=\"Next\" locked=\"{{props.next-locked}}\"/></dui-row></dui-fragment>");
    for (String name :
        List.of("playing-card", "chip-stack", "wheel", "reel", "lever", "particles", "lights")) {
      var attributes = new HashSet<>(DemoComponentSchemas.get(name).attributes());
      attributes.addAll(Motion.ATTRIBUTES);
      attributes.add("animation-start");
      builder.renderer(
          "demo-" + name,
          attributes,
          54,
          ctx -> {
            var node = ctx.node();
            var properties = new HashMap<>(node.props());
            properties.remove("class");
            DemoEffectComponent.draw(
                ctx.canvas(),
                new MenuTemplate.Node(name, properties, node.children()),
                ctx.x(),
                ctx.y(),
                ctx.width(),
                ctx.height());
            Motion.from(
                    node.props(),
                    Long.parseLong(
                        node.s("animation-start", Long.toString(ctx.canvas().animationStart))),
                    ctx.canvas().motionEnabled)
                .ifPresent(m -> ctx.canvas().effectMotion(node.s("id", ""), m));
          });
    }
    builder.renderer(
        "demo-tree",
        DemoComponentSchemas.get("tree").attributes(),
        18,
        ctx ->
            DemoTreeComponent.draw(
                ctx.canvas(), ctx.node(), ctx.x(), ctx.y(), ctx.width(), ctx.height()));
    builder.renderer(
        "demo-node",
        DemoComponentSchemas.get("node").attributes(),
        18,
        ctx -> {
          throw new IllegalArgumentException("Node belongs to a tree");
        });
    builder.renderer(
        "demo-slot",
        DemoComponentSchemas.get("slot").attributes(),
        63,
        ctx ->
            DemoSlotComponent.draw(
                ctx.canvas(), ctx.node(), ctx.x(), ctx.y(), ctx.width(), ctx.height()));
    builder.renderer(
        "demo-item", DemoComponentSchemas.get("item").attributes(), 18, DemoMotion::draw);
    for (String name : List.of("nav", "stat", "empty", "entry"))
      builder.renderer(
          "demo-" + name,
          DemoComponentSchemas.get(name).attributes(),
          DemoWidgets.registry().require(name).height(),
          ctx -> {
            var n = new MenuTemplate.Node(name, ctx.node().props(), ctx.node().children());
            var c =
                new ComponentContext(
                    ctx.canvas(),
                    n,
                    ctx.x(),
                    ctx.y(),
                    ctx.width(),
                    ctx.height(),
                    ctx.images(),
                    ctx.children(),
                    ctx.overlays());
            var skin = DemoWidgets.registry().require(name);
            skin.painter().draw(c, "field");
            ControlBehavior.hits(c, skin);
          });
    builder.template(
        "demo-card",
        new PropertySchema(
            Map.of(
                "padding",
                PropertySchema.Property.integer(0, 60, "6"),
                "padding-y",
                PropertySchema.Property.integer(0, 60, "9"))),
        "<dui-fragment><dui-panel padding=\"{{props.padding}}\""
            + " padding-y=\"{{props.padding-y}}\"><dui-outlet"
            + " name=\"default\"/></dui-panel></dui-fragment>");
    return builder.build();
  }
}
