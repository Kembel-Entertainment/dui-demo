package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

/** Diagnostics consumer exercising public renderer APIs; no parser/shader changes per screen. */
public final class ProtocolView {
  private ProtocolView() {}

  public static Canvas render(long tick, boolean motion, boolean popup) {
    var c = new Canvas(360, 180, DemoTemplates.environment(DemoTemplates.font()));
    c.effectLimit = 16;
    c.motionEnabled = motion;
    c.rect(0, 0, 360, 180, 0x16171D);
    c.text(12, 9, 330, "dui / PROTOCOL LAB", 0x58E6DB);
    for (int i = 0; i < 12; i++)
      c.effect(
          DemoShaders.effect(
              "light_" + i,
              DemoShaders.Kind.LIGHTS,
              12 + i % 6 * 48,
              27 + i / 6 * 27,
              36,
              18,
              3,
              2));
    c.effect(example.proof.ExtensionProof.ring("pulse", 174, 99, 36, 36));
    c.effect(
        DemoShaders.effect(
            "card", DemoShaders.Kind.PLAYING_CARD, 30, 90, 36, 63, 12, 24 | (6 << 7)));
    c.effectMotion(
        "card",
        new Motion(
            tick, 24, 0, Motion.Easing.EASE_OUT, motion, -18, 0, .5, 1, -15, 0, 0, 1, .5, .5));
    c.item("native", 252, 108, 36, new ItemClip(240, 90, 108, 63));
    c.motion("native", DemoMotion.pop(tick, 24, 18, motion));
    c.playerModel("classic", "classic", 78, 81, 48, 72, 1, true, motion);
    c.playerModel("slim", "slim", 126, 81, 48, 72, 1, true, motion);
    c.head(270, 126, "texture:minecraft:entity/player/wide/steve", true);
    c.image("patch", 288, 126, 18, 18, 3, new RasterImage(1, 1, new int[] {0xFDBA74}));
    var overlays = new ArrayList<Runnable>();
    var options =
        List.of(
            new MenuTemplate.Node("option", Map.of("value", "a", "label", "Alpha"), List.of()),
            new MenuTemplate.Node("option", Map.of("value", "b", "label", "Beta"), List.of()));
    DropdownComponent.draw(
        c,
        new MenuTemplate.Node(
            "dropdown",
            Map.of(
                "id",
                "popup",
                "value",
                "a",
                "open",
                "" + popup,
                "action",
                "protocol_popup",
                "dismiss",
                "protocol_dismiss",
                "select",
                "protocol_dismiss"),
            options),
        240,
        90,
        108,
        27,
        overlays,
        DemoWidgets.registry().require("dropdown"));
    overlays.forEach(Runnable::run);
    c.item("__effects", 0, 0, 1);
    c.animation = new Canvas.Animation("__effects", tick, motion, 24);
    c.text(12, 162, 99, "Animate", 0xEAEAF1);
    c.hit(new Canvas.Hit("animate", "protocol_animate", "", "Animate", 9, 153, 108, 27));
    c.text(129, 162, 99, motion ? "Motion off" : "Motion on", 0xEAEAF1);
    c.hit(new Canvas.Hit("motion", "protocol_motion", "", "Toggle motion", 126, 153, 108, 27));
    return c;
  }
}
