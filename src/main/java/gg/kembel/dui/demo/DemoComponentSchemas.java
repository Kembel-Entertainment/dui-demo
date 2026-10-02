package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

/**
 * Built-in property support and cost categories; extension packages declare their own attributes.
 */
public final class DemoComponentSchemas {
  public record Schema(
      String name,
      Set<String> attributes,
      Map<String, String> defaults,
      String constraints,
      String cost,
      String backend) {
    public Schema {
      attributes = Set.copyOf(attributes);
      defaults = Map.copyOf(defaults);
    }
  }

  private static final Set<String> PLACEMENT =
      Set.of(
          "id",
          "class",
          "x",
          "y",
          "width",
          "height",
          "anchor-x",
          "anchor-y",
          "min-width",
          "max-width",
          "min-height",
          "max-height",
          "dock",
          "column-span",
          "row-span");
  private static final Set<String> STYLE =
      Set.of(
          "fill",
          "border",
          "color",
          "disabled-fill",
          "disabled-border",
          "disabled-color",
          "selected-fill",
          "selected-border",
          "selected-color",
          "active-fill",
          "active-border",
          "active-color",
          "highlight",
          "bevel",
          "padding");
  private static final Map<String, Schema> ALL = build();

  private DemoComponentSchemas() {}

  public static Map<String, Schema> all() {
    return ALL;
  }

  public static Schema get(String name) {
    Schema s = ALL.get(name);
    if (s == null) throw new IllegalArgumentException("Unknown component schema: " + name);
    return s;
  }

  public static boolean supports(String tag, String attr) {
    return get(tag).attributes().contains(attr);
  }

  private static Map<String, Schema> build() {
    var map = new TreeMap<String, Schema>();
    put(
        map,
        "menu",
        "gap padding theme background compact compact-width compact-height focus-outline motion"
            + " animation-start effect-budget",
        "Canvas 120..480 by 9..360; height multiple of nine",
        "canvas",
        "font/native/effects");
    for (String tag : List.of("row", "column", "panel", "card", "layer"))
      put(
          map,
          tag,
          "gap padding columns cross-align title label detail icon tone locked active action"
              + " payload tooltip"
              + (tag.equals("layer") ? " cover dismiss" : ""),
          "Allocated rectangles must fit; hit rows use multiples of nine",
          "paints/hits",
          "font");
    put(
        map,
        "grid",
        "columns gap padding cell-height cross-align",
        "1..16 columns; spanning cells cannot overlap",
        "paints/hits",
        "font");
    put(map, "icon", "name tone", "Named nine-pixel icon", "paints", "font");
    for (String tag : List.of("heading", "text"))
      put(
          map,
          tag,
          "label align wrap max-lines tone",
          "Injected font metrics; bounded wrapping and ellipsis",
          "paints",
          "font");
    for (String tag :
        List.of(
            "nav",
            "toggle",
            "checkbox",
            "button",
            "choice",
            "tab",
            "badge",
            "progress",
            "stat",
            "empty",
            "entry"))
      put(
          map,
          tag,
          "label detail value max checked active locked icon action payload tooltip tone player"
              + " hat",
          "Nine-pixel hit rows; application authorizes actions",
          "paints/hits",
          "font");
    for (String tag : List.of("rect", "surface", "divider", "spacer"))
      put(map, tag, "tone", "Positive bounded geometry", "paints", "font");
    put(
        map,
        "dropdown",
        "label value open locked action select dismiss tooltip",
        "1..8 distinct options; popup must fit above or below",
        "paints/hits/coverage",
        "font/occlusion");
    put(
        map,
        "option",
        "label value locked tooltip",
        "Distinct nonempty value",
        "none",
        "structural");
    put(map, "tree", "action tooltip", "Bounded canvas tree", "paints/hits", "font");
    put(
        map,
        "node",
        "label detail value icon parent rank max limit status shape active locked action payload"
            + " tooltip tone",
        "Unique identity; valid parent references",
        "paints/hits",
        "font");
    put(
        map,
        "player-model",
        "source facing outer-layer idle",
        "Full-body GPU skin and vanilla armor; source is an appearance key; facing 0..7; contained"
            + " in 48x72, 72x108, 108x162 or 144x216",
        "player model / nine-pixel skin bands / up to four armor carriers",
        "player-model-v1");
    put(
        map,
        "head",
        "player hat label action tooltip payload locked",
        "Native 8x8 profile; nine-pixel rows",
        "one portrait",
        "head");
    put(
        map,
        "slot",
        "label value icon count durability enchanted action payload tooltip locked active tone",
        "Visual slot; count and durability ranges are validated",
        "paints/hits",
        "font");
    put(
        map,
        "hitbox",
        "action payload tooltip locked",
        "Unique id and action; full nine-pixel rows",
        "one hit",
        "font");
    put(
        map,
        "image",
        "source pixel-size image-layer action payload tooltip locked",
        "Source snapshot required; pixel-size 1..8; total sample budget 16384",
        "sampled pixels",
        "runtime image");
    put(
        map,
        "item",
        "size transition transition-start transition-duration transition-distance clip-x clip-y"
            + " clip-width clip-height burst-start "
            + String.join(" ", Motion.ATTRIBUTES),
        "Size 1..127; clip offsets -512..511; motion bounded by protocol",
        "one native body (11 GUI units)",
        "native/model/clip/motion");
    put(
        map,
        "wheel",
        "value previous turns duration animation variant palette action payload tooltip locked",
        "European wheel; square >=96; value 0..36",
        "one effect",
        "shader");
    put(
        map,
        "playing-card",
        "value face-down active delay animation palette duration lift card-height action payload"
            + " tooltip locked",
        "Card -1..51; even delay 0..62; bounded visual modes",
        "one effect",
        "shader");
    put(
        map,
        "chip-stack",
        "count delay animation palette from to duration action payload tooltip locked",
        "Count 0..31; even delay 0..126; visual anchors only",
        "one effect",
        "shader");
    put(
        map,
        "reel",
        "symbols sequence value previous turns duration symbol-size action payload tooltip locked",
        "Six symbols; bounded duration; symbol resources are build-time",
        "one effect",
        "shader");
    put(
        map,
        "lever",
        "duration action payload tooltip locked",
        "Duration 1..127",
        "one effect",
        "shader");
    put(
        map,
        "particles",
        "effect count origin-x origin-y delay action payload tooltip locked",
        "Coins/confetti; count 0..63; even delay",
        "one effect",
        "shader");
    put(
        map,
        "lights",
        "count radius action payload tooltip locked",
        "Count 1..32; radius 1..15",
        "one effect",
        "shader");
    put(
        map,
        "repeat",
        "items as",
        "Bound list <=200; stable ids supplied by consumer",
        "expanded nodes",
        "structural");
    put(map, "if", "test", "Boolean binding", "expanded nodes", "structural");
    put(map, "style", "", "Local style properties", "none", "structural");
    put(
        map,
        "component",
        "name props",
        "One visual root; scoped properties and outlets",
        "expanded nodes",
        "structural");
    put(map, "outlet", "name", "Declared named content", "expanded nodes", "structural");
    put(map, "content", "name", "Caller-scoped content", "expanded nodes", "structural");
    return Map.copyOf(map);
  }

  private static void put(
      Map<String, Schema> map,
      String name,
      String attrs,
      String constraints,
      String cost,
      String backend) {
    var set = new HashSet<>(PLACEMENT);
    set.addAll(STYLE);
    if (!attrs.isBlank()) set.addAll(List.of(attrs.split(" ")));
    var defaults = new HashMap<String, String>();
    if (set.contains("gap")) defaults.put("gap", "0");
    if (set.contains("locked")) defaults.put("locked", "false");
    if (name.equals("player-model"))
      defaults.putAll(
          Map.of("source", "viewer", "facing", "0", "outer-layer", "true", "idle", "true"));
    if (name.equals("layer")) defaults.put("cover", "false");
    if (name.equals("menu")) {
      defaults.put("background", "theme");
      defaults.put("width", "440");
      defaults.put("height", "306");
      defaults.put("effect-budget", "8");
      defaults.put("focus-outline", "hidden");
    }
    map.put(name, new Schema(name, set, defaults, constraints, cost, backend));
  }
}
