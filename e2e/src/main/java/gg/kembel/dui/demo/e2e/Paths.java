package gg.kembel.dui.demo.e2e;

import java.nio.file.Path;

final class Paths {
  static Path output() {
    return Path.of(System.getProperty("dui.e2e.output"));
  }

  static Path plugin() {
    return Path.of(System.getProperty("dui.e2e.plugin"));
  }
}
