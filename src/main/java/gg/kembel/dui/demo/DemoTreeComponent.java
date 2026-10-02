package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

/**
 * Reusable directed tree component. Coordinates, edges, visual states and actions come from data.
 */
public final class DemoTreeComponent {
  private static final int WHITE = 0xEAEAF1,
      MUTED = 0x717482,
      CYAN = 0x58E6DB,
      GREEN = 0x62D394,
      GOLD = 0xF4D06B;

  public static void draw(Canvas c, MenuTemplate.Node tree, int x, int y, int w, int h) {
    var nodes = new LinkedHashMap<String, MenuTemplate.Node>();
    for (var node : tree.children()) {
      if (!node.type().equals("demo-node"))
        throw new IllegalArgumentException("dui-tree accepts dui-node only");
      String id = node.s("id", "");
      if (id.isBlank() || nodes.putIfAbsent(id, node) != null)
        throw new IllegalArgumentException("Duplicate / missing tree node id");
      int nx = node.n("x", 0), ny = node.n("y", 0);
      if (nx < 18
          || nx + 18 > w
          || ny < 0
          || ny + (node.s("shape", "").equals("root") ? 36 : 45) > h
          || ny % 9 != 0) throw new IllegalArgumentException("Tree node outside layout: " + id);
    }
    for (var node : nodes.values()) {
      String parentId = node.s("parent", "");
      if (parentId.isBlank()) continue;
      var parent = nodes.get(parentId);
      if (parent == null) throw new IllegalArgumentException("Unknown tree parent: " + parentId);
      if (parent.n("y", 0) <= node.n("y", 0))
        throw new IllegalArgumentException("Tree parents must be below children");
      int px = x + parent.n("x", 0),
          py = y + parent.n("y", 0),
          cx = x + node.n("x", 0),
          cy = y + node.n("y", 0) + 36;
      int middle = (py + cy) / 2;
      int color = node.n("rank", 0) > 0 ? GREEN : parent.n("rank", 0) > 0 ? 0x396762 : 0x393A46;
      c.rect(px - 1, middle, 2, py - middle, color);
      c.rect(Math.min(px, cx), middle, Math.abs(px - cx) + 1, 2, color);
      c.rect(cx - 1, cy, 2, middle - cy + 1, color);
    }
    for (var node : nodes.values()) {
      int cx = x + node.n("x", 0),
          ny = y + node.n("y", 0),
          rank = node.n("rank", 0),
          limit = node.n("limit", 1);
      String status = node.s("status", "locked"), shape = node.s("shape", "square");
      int color =
          switch (status) {
            case "learned" -> GREEN;
            case "available" -> CYAN;
            case "excluded" -> 0xAA626B;
            default -> MUTED;
          };
      frame(c, cx - 16, ny, 32, 36, shape, color, rank > 0 ? 0x19352F : 0x181920);
      c.icon(
          cx - 9,
          ny + 9,
          node.s("icon", "demo:star"),
          status.equals("locked") || status.equals("excluded") ? 0x626575 : 0xFFFFFF);
      if (!shape.equals("root")) {
        String text = rank + "/" + limit;
        int tw = c.metrics().width(text);
        c.rect(cx - 14, ny + 36, 28, 9, 0x111217);
        c.text(cx - tw / 2, ny + 36, 28, text, rank == limit ? GREEN : rank > 0 ? GOLD : MUTED);
      }
      c.hit(
          new Canvas.Hit(
              node.s("id", ""),
              node.s("action", ""),
              node.s("value", node.s("id", "")),
              node.s("tooltip", node.s("label", "")),
              cx - 16,
              ny,
              32,
              shape.equals("root") ? 36 : 45));
    }
  }

  private static void frame(
      Canvas c, int x, int y, int w, int h, String shape, int edge, int fill) {
    int cut = shape.equals("square") ? 2 : shape.equals("root") ? 8 : 6;
    polygon(c, x, y, w, h, cut, edge);
    polygon(c, x + 2, y + 2, w - 4, h - 4, Math.max(0, cut - 2), fill);
  }

  private static void polygon(Canvas c, int x, int y, int w, int h, int cut, int color) {
    int row = 0;
    while (row < h) {
      int inset = Math.max(0, cut - Math.min(row, h - row - 1)), end = row + 1;
      while (end < h && Math.max(0, cut - Math.min(end, h - end - 1)) == inset) end++;
      c.rect(x + inset, y + row, w - 2 * inset, end - row, color);
      row = end;
    }
  }
}
