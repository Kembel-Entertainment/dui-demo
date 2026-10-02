package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

final class TestDropdown {
  static void draw(
      Canvas c, MenuTemplate.Node n, int x, int y, int w, int h, List<Runnable> overlays) {
    DropdownComponent.draw(c, n, x, y, w, h, overlays, DemoWidgets.registry().require("dropdown"));
  }
}
