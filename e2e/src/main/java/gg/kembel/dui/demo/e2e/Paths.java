package gg.kembel.dui.demo.e2e;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;

final class Paths {
  static String server() {
    return "127.0.0.1:" + Integer.getInteger("dui.e2e.port", 25584);
  }

  // Keep the reference GUI viewport equivalent on Retina and ordinary displays.
  static int referenceScale(Minecraft mc) {
    var window = mc.getWindow();
    return Math.max(1, (int) Math.round((double) window.getWidth() / window.getScreenWidth()));
  }

  static Path output() {
    return Path.of(System.getProperty("dui.e2e.output"));
  }

  static Path plugin() {
    return Path.of(System.getProperty("dui.e2e.plugin"));
  }
}
