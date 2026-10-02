package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.core.MenuTemplate.Node;
import java.util.*;

public final class DemoWidgets {
  private static final int BG = 0x16171D,
      EDGE = 0x34343F,
      MUTED = 0x9697A5,
      WHITE = 0xEAEAF1,
      CYAN = 0x58E6DB,
      GOLD = 0xF4D06B,
      GREEN = 0x62D394;

  private static int tone(Node n) {
    return switch (n.s("tone", "")) {
      case "gold" -> GOLD;
      case "green" -> GREEN;
      case "danger" -> 0xEF818C;
      case "muted" -> MUTED;
      default -> CYAN;
    };
  }

  private static int color(Node n, String key, int fallback) {
    String value = n.s(key, "");
    if (value.isEmpty()) return fallback;
    if (!value.matches("#[0-9a-fA-F]{6}"))
      throw new IllegalArgumentException("Expected #RRGGBB for " + key);
    return Integer.parseInt(value.substring(1), 16);
  }

  private static void surface(
      Canvas c, int x, int y, int w, int h, int fill, int border, int bevel) {
    if (bevel == 0) {
      c.rect(x, y, w, h, fill);
      return;
    }
    if (bevel < 0 || bevel > 4 || w < bevel * 2 + 1 || h < bevel * 2 + 2)
      throw new IllegalArgumentException("Invalid surface bevel");
    c.rect(x + bevel, y, w - bevel * 2, h, border);
    c.rect(x, y + bevel, w, h - bevel * 2, border);
    c.rect(x + bevel, y + bevel, w - bevel * 2, h - bevel * 2 - 1, fill);
  }

  public static WidgetSkinRegistry registry() {
    var skins = new HashMap<String, WidgetSkinRegistry.Skin>();
    for (String name :
        List.of(
            "panel",
            "card",
            "icon",
            "head",
            "heading",
            "text",
            "divider",
            "nav",
            "toggle",
            "checkbox",
            "button",
            "tab",
            "choice",
            "badge",
            "progress",
            "stat",
            "empty",
            "entry",
            "dropdown")) {
      int height =
          switch (name) {
            case "divider", "badge", "progress" -> 9;
            case "card", "empty" -> 72;
            case "stat" -> 45;
            case "nav", "entry", "dropdown" -> 27;
            default -> 18;
          };
      skins.put(
          name,
          new WidgetSkinRegistry.Skin(
              height,
              name.equals("panel") || name.equals("card") ? 6 : 0,
              name.equals("panel") || name.equals("card") ? 9 : 0,
              18,
              DemoWidgets::paint,
              (node, w, h) ->
                  name.equals("choice")
                      ? Map.of(
                          "previous",
                          new WidgetSkinRegistry.Part(w / 2 - 3, 0, 21, h),
                          "next",
                          new WidgetSkinRegistry.Part(w - 24, 0, 24, h))
                      : Map.of()));
    }
    return new WidgetSkinRegistry(skins);
  }

  public static void paint(ComponentContext context, String part) {
    var c = context.canvas();
    var n = context.node();
    int x = context.x(), y = context.y(), w = context.width(), h = context.height();
    if (n.type().equals("dropdown")) {
      dropdown(context, part);
      return;
    }
    if (n.type().equals("panel") || n.type().equals("card")) {
      c.panel(
          x,
          y,
          w,
          h,
          n.type().equals("card") ? 0x22232B : BG,
          n.type().equals("panel") ? CYAN : EDGE);
      return;
    }
    String label = n.s("label", ""),
        detail = n.s("detail", ""),
        value = n.s("value", ""),
        icon = n.s("icon", "");
    int accent = tone(n);
    int ty = y + Math.max(0, (h - 9) / 2);
    boolean locked = n.b("locked");
    switch (n.type()) {
      case "icon" -> c.icon(x, y, n.s("name", "demo:star"), color(n, "color", accent));
      case "player-model" ->
          c.playerModel(
              n.s("id", "player"),
              n.s("source", "viewer"),
              x,
              y,
              w,
              h,
              n.n("facing", 0),
              !n.s("outer-layer", "true").equals("false"),
              !n.s("idle", "true").equals("false"));
      case "head" -> {
        int hy = y + (h >= 27 ? 9 : 0);
        c.head(x, hy, n.s("player", "self"), !n.s("hat", "true").equals("false"));
        if (!label.isBlank()) c.text(x + 12, hy, w - 12, label, WHITE);
      }
      case "heading" -> {
        c.text(x, ty, w, label, color(n, "color", WHITE));
      }
      case "text" -> {
        TextLayout.draw(
            c,
            label,
            x,
            y,
            w,
            h,
            n.s("align", "left"),
            n.b("wrap"),
            n.n("max-lines", Math.max(1, h / 9)),
            color(n, "color", n.s("tone", "").isEmpty() ? MUTED : accent));
      }
      case "divider" -> c.rect(x, y + 3, w, 1, EDGE);
      case "spacer" -> {}
      case "nav" -> {
        if (n.b("active")) {
          c.panel(x, y, w, h - 1, 0x273436, 0x376564);
          c.rect(x, y + 3, 2, h - 7, CYAN);
        }
        if (!icon.isEmpty())
          c.icon(
              x + 6,
              icon.startsWith("demo:item/") ? y + (h >= 27 ? 9 : 0) : ty,
              icon,
              n.b("active") ? CYAN : MUTED);
        int labelOffset = icon.startsWith("demo:item/") ? 29 : 21;
        c.text(x + labelOffset, ty, w - labelOffset - 4, label, n.b("active") ? CYAN : MUTED);
      }
      case "toggle" -> {
        c.rect(x, y + h - 2, w, 1, 0x282932);
        c.text(x + 3, ty, w - 36, label, locked ? MUTED : WHITE);
        if (locked) c.icon(x + w - 18, ty, "demo:lock", GOLD);
        else {
          int sx = x + w - 25, sy = y + (h - 10) / 2;
          c.panel(
              sx,
              sy,
              23,
              10,
              n.b("checked") ? 0x259F86 : 0x484955,
              n.b("checked") ? 0x42D8B8 : 0x626372);
          c.rect(sx + (n.b("checked") ? 14 : 2), sy + 1, 7, 8, WHITE);
        }
      }
      case "checkbox" -> {
        if (h < 18 || w < 30)
          throw new IllegalArgumentException("Checkbox needs at least 30x18 pixels");
        int sy = y + (h - 12) / 2;
        c.panel(x, sy, 12, 12, n.b("checked") ? 0x259F86 : 0x22232B, locked ? 0x626372 : 0x58E6DB);
        if (n.b("checked")) c.icon(x + 1, sy + 1, "demo:check", locked ? 0x9697A5 : 0xF9FAFB);
        c.text(x + 18, ty, w - 21, label, locked ? MUTED : WHITE);
      }
      case "button", "tab", "choice" -> {
        if (n.type().equals("button") && n.props().containsKey("fill")) {
          surface(
              c,
              x,
              y,
              w,
              h,
              color(n, locked ? "disabled-fill" : "fill", locked ? 0x403041 : BG),
              color(n, locked ? "disabled-border" : "border", EDGE),
              n.n("bevel", 1));
          if (n.props().containsKey("highlight"))
            c.rect(x + 3, y + 3, w - 6, 1, color(n, "highlight", WHITE));
          String fit = c.metrics().fit(label, w - 6);
          int tx =
              n.s("align", "center").equals("left") ? x + 3 : x + (w - c.metrics().width(fit)) / 2;
          c.text(
              tx,
              ty,
              w - (tx - x),
              fit,
              color(n, locked ? "disabled-color" : "color", locked ? MUTED : WHITE));
          break;
        }
        boolean active = n.b("active");
        c.panel(
            x, y, w, h - 1, locked ? 0x202127 : active ? 0x24504C : 0x292B35, active ? CYAN : EDGE);
        if (n.type().equals("choice")) {
          c.text(x + 6, ty, w / 2 - 9, label, MUTED);
          c.text(x + w / 2, ty, 9, "<", CYAN);
          c.text(x + w / 2 + 18, ty, w / 2 - 42, value, CYAN);
          c.text(x + w - 15, ty, 9, ">", CYAN);
        } else if (label.isBlank() && !icon.isBlank()) {
          int size = icon.startsWith("demo:item/") ? 18 : 9;
          c.icon(
              x + (w - size) / 2,
              icon.startsWith("demo:item/") ? y + (h >= 27 ? 9 : 0) : ty,
              icon,
              locked ? MUTED : accent);
        } else {
          String fitted = c.metrics().fit(label, w - 10);
          int tx =
              n.s("align", "center").equals("left")
                  ? x + 6
                  : x + (w - c.metrics().width(fitted)) / 2;
          c.text(tx, ty, w - (tx - x) - 3, fitted, locked ? MUTED : active ? CYAN : accent);
        }
      }
      case "badge" -> {
        c.rect(x, y, w, 9, 0x33313B);
        c.text(x + 3, y, w - 6, label, accent);
      }
      case "progress" -> {
        double progress =
            Math.max(
                0,
                Math.min(
                    1,
                    Double.parseDouble(value)
                        / Math.max(1, Double.parseDouble(n.s("max", "100")))));
        c.panel(x, y + 1, w, 6, 0x30313D, EDGE);
        c.rect(x + 1, y + 2, (int) ((w - 2) * progress), 4, color(n, "color", accent));
      }
      case "stat" -> {
        c.panel(x, y, w, h - 1, 0x22232B, EDGE);
        if (!icon.isEmpty()) c.icon(x + 6, y + 9, icon, accent);
        c.text(x + (icon.isEmpty() ? 6 : 21), y + 9, w - 27, label, MUTED);
        c.text(x + 6, y + 27, w - 12, value, accent);
      }
      case "empty" -> {
        c.panel(x, y, w, h - 1, 0x202127, EDGE);
        c.icon(x + w / 2 - 4, y + 9, icon.isEmpty() ? "demo:book" : icon, MUTED);
        String text = c.metrics().fit(label, w - 18);
        c.text(x + (w - c.metrics().width(text)) / 2, y + 27, w - 12, text, WHITE);
        c.text(x + 9, y + 45, w - 18, detail, MUTED);
      }
      case "entry" -> {
        boolean head = !n.s("player", "").isBlank();
        int ey = head ? y + (h >= 27 ? 9 : 0) : ty;
        c.rect(x, y, w, h - 1, 0x22232B);
        if (head) c.head(x + 6, ey, n.s("player", ""), !n.s("hat", "true").equals("false"));
        else if (!icon.isEmpty())
          c.icon(x + 6, icon.startsWith("demo:item/") ? y + (h >= 27 ? 9 : 0) : ty, icon, accent);
        c.text(x + 21, ey, w - 100, label, WHITE);
        c.text(x + w - 76, ey, 70, value, accent);
      }
      default -> throw new IllegalArgumentException("Unsupported component " + n.type());
    }
  }

  private static void dropdown(ComponentContext ctx, String part) {
    var c = ctx.canvas();
    var n = ctx.node();
    int x = ctx.x(), y = ctx.y(), w = ctx.width(), h = ctx.height();
    boolean locked = n.b("locked"), selected = n.b("selected"), open = n.b("open");
    int ty = y + (h - 9) / 2;
    if (part.equals("popup")) {
      c.panel(x, y, w, h, 0x22232B, 0x58E6DB);
      return;
    }
    if (part.equals("option")) {
      if (selected) c.rect(x + 1, y + 1, w - 2, h - 2, 0x24504C);
      c.text(
          x + 7,
          y + 4,
          w - 30,
          n.s("label", n.s("value", "")),
          locked ? 0x9697A5 : selected ? 0x58E6DB : 0xEAEAF1);
      if (selected) c.icon(x + w - 17, y + 4, "demo:check", 0x58E6DB);
      if (locked) c.icon(x + w - 17, y + 4, "demo:lock", 0x9697A5);
      return;
    }
    c.panel(x, y, w, h - 1, locked ? 0x202127 : 0x292B35, open ? 0x58E6DB : 0x34343F);
    c.text(x + 6, ty, w - 30, n.s("label", "Select an option"), locked ? 0x9697A5 : 0xEAEAF1);
    c.text(x + w - 17, ty, 12, open ? "^" : "v", locked ? 0x9697A5 : 0x58E6DB);
  }
}
