package gg.kembel.dui.demo;

import example.proof.ExtensionProof;
import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;
import java.util.function.BiConsumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Paper adapter only; the independent consumer owns the entire scene. */
final class ExtensionLab {
  private final RenderEnvironment environment;
  private final ViewModel resources;
  private final BiConsumer<Canvas, Map<String, Object>> report;
  private DialogSession session;
  private long age;
  private int clicks;

  ExtensionLab(
      Dui dui,
      PackMetadata metadata,
      Player player,
      BiConsumer<Canvas, Map<String, Object>> report) {
    this.report = report;
    environment =
        RenderEnvironment.plain(metadata.font())
            .withResources(metadata.bitmapFonts(), metadata.glyphPixels())
            .withPlayerRenderers(metadata.playerRenderers());
    resources =
        new ViewModel(
            Map.of(),
            Map.of(),
            Map.of("native/gem", new ItemStack(Material.EMERALD)),
            Map.of(),
            Map.of("self", PlayerAppearance.capture(player)));
    session =
        dui.open(
            player,
            frame(0).canvas(),
            resources,
            DialogOptions.notice("Extension lab", "Close", "close"),
            context -> {
              if (context.action().equals("animate")) play();
              if (context.action().equals("choose")) {
                clicks++;
                write(session.canvas());
              }
            });
  }

  private DialogSession.Frame frame(long tick) {
    age = tick;
    return new DialogSession.Frame(ExtensionProof.dynamic(environment, tick), resources);
  }

  void play() {
    session.animate(
        80,
        4,
        tick -> {
          return frame(tick);
        },
        frame -> write(frame.canvas()));
  }

  private void write(Canvas canvas) {
    report.accept(canvas, Map.of("section", "extensions", "age", age, "clicks", clicks));
  }

  DialogSession session() {
    return session;
  }
}
