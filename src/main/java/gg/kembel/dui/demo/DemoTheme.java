package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

public final class DemoTheme {
  public static final ThemeTokens DARK =
      new ThemeTokens(
          Map.ofEntries(
              Map.entry("surface", 0x16171D),
              Map.entry("raised", 0x22232B),
              Map.entry("text", 0xEAEAF1),
              Map.entry("muted", 0x9697A5),
              Map.entry("accent", 0x58E6DB),
              Map.entry("border", 0x34343F),
              Map.entry("success", 0x62D394),
              Map.entry("warning", 0xF4D06B),
              Map.entry("danger", 0xEF818C),
              Map.entry("selected", 0x24504C),
              Map.entry("disabled", 0x202127)),
          Map.of("small", 3, "medium", 6, "large", 12),
          Map.of("body", "text"));

  public static int semantic(ThemeTokens tokens, int legacy) {
    return switch (legacy) {
      case 0x16171D -> tokens.color("surface");
      case 0x22232B, 0x292B35 -> tokens.color("raised");
      case 0xEAEAF1 -> tokens.color("text");
      case 0x9697A5 -> tokens.color("muted");
      case 0x58E6DB -> tokens.color("accent");
      case 0x34343F -> tokens.color("border");
      case 0x62D394 -> tokens.color("success");
      case 0xF4D06B -> tokens.color("warning");
      case 0xEF818C -> tokens.color("danger");
      case 0x24504C -> tokens.color("selected");
      case 0x202127 -> tokens.color("disabled");
      default -> legacy;
    };
  }
}
