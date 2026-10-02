package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;

/** A canvas slot reserves a live native item; the pack shader places it over this hit region. */
public final class DemoSlotComponent {
  public static void draw(Canvas c, MenuTemplate.Node n, int x, int y, int w, int h) {
    if (w < 44 || h < 63) throw new IllegalArgumentException("Slot needs at least 44x63 pixels");
    String icon = n.s("icon", "");
    int count = n.n("count", 1);
    if (count < 1 || count > 99) throw new IllegalArgumentException("Slot count 1..99");
    boolean selected = n.b("active");
    c.panel(x, y, w, h - 1, selected ? 0x233B3B : 0x22232B, selected ? 0x58E6DB : 0x41434E);
    int ix = x + (w - 36) / 2, iy = y + 9;
    c.item(n.s("id", "slot:" + icon), ix, iy, 36);
    if (count > 1) {
      String text = "" + count;
      int tw = c.metrics().width(text);
      c.text(x + w - tw - 4, y, tw, text, 0xFFFFFF);
    }
    double durability = Double.parseDouble(n.s("durability", "-1"));
    if (!Double.isFinite(durability) || durability < -1 || durability > 1)
      throw new IllegalArgumentException("Invalid durability");
    if (durability >= 0) {
      c.rect(ix + 2, y + 47, 32, 2, 0x111217);
      c.rect(
          ix + 2,
          y + 47,
          (int) Math.round(32 * durability),
          1,
          durability > .4 ? 0x62D394 : 0xEF818C);
    }
    String label = c.metrics().fit(n.s("label", ""), w - 8);
    c.text(x + (w - c.metrics().width(label)) / 2, y + 54, w - 8, label, 0xEAEAF1);
    c.hit(
        new Canvas.Hit(
            n.s("id", "slot:" + icon),
            n.s("action", ""),
            n.s("value", icon),
            n.s("tooltip", label),
            x,
            y,
            w,
            h));
  }
}
