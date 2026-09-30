package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.Set;

/**
 * A declared preference, not client telemetry. GUI scale alone cannot determine window dimensions.
 */
public final class DisplayPreferences {
  public static final Set<String> SCALES = Set.of("auto", "1", "2", "3", "4", "5+");
  public boolean configured;
  public String guiScale = "auto", layout = "compact";

  public boolean valid() {
    return guiScale != null
        && layout != null
        && SCALES.contains(guiScale)
        && Set.of("compact", "spacious").contains(layout);
  }

  public static DisplayPreferences selection(String guiScale, String layout) {
    var result = new DisplayPreferences();
    result.guiScale = guiScale;
    result.layout = layout;
    result.configured = true;
    if (!result.valid()) throw new IllegalArgumentException("Unknown GUI scale or layout");
    return result;
  }
}
