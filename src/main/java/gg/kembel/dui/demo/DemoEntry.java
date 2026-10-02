package gg.kembel.dui.demo;

import java.util.function.Function;

/** Catalogue registration supports both dialog and in-game presentations. */
sealed interface DemoEntry {
  record Dialogue(Function<DemoServices, DemoMenu> factory) implements DemoEntry {}

  record Atlas() implements DemoEntry {}
}
