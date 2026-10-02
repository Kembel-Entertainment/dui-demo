package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

public final class DemoTemplates {
  public static GlyphFont font() {
    var widths = new HashMap<String, Integer>();
    for (int cp = 32; cp < 127; cp++) widths.put(Character.toString(cp), 6);
    " \"()*I[]t{}".codePoints().forEach(cp -> widths.put(Character.toString(cp), 4));
    "!',.:;i|".codePoints().forEach(cp -> widths.put(Character.toString(cp), 2));
    "<>fk".codePoints().forEach(cp -> widths.put(Character.toString(cp), 5));
    "@~".codePoints().forEach(cp -> widths.put(Character.toString(cp), 7));
    "`l".codePoints().forEach(cp -> widths.put(Character.toString(cp), 3));
    return new GlyphFont(widths);
  }

  public static RenderEnvironment environment(GlyphFont font) {
    return new RenderEnvironment(
        font,
        DemoTheme.DARK,
        DemoWidgets.registry(),
        DemoGlyphs.bindings(),
        (data, tokens) ->
            v ->
                DemoUiTheme.named(String.valueOf(data.getOrDefault("theme", "default")))
                    .color(DemoTheme.semantic(tokens, v)));
  }

  public static MenuTemplate parse(String source) throws Exception {
    return parse(source, font(), DemoVisualComponents.registry());
  }

  public static MenuTemplate parse(String source, GlyphFont font) throws Exception {
    return parse(source, font, DemoVisualComponents.registry());
  }

  public static MenuTemplate parse(String source, GlyphFont font, ComponentRegistry registry)
      throws Exception {
    return MenuTemplate.parse(normalize(source), environment(font), registry);
  }

  public static MenuTemplate parse(
      String source, GlyphFont font, ComponentRegistry registry, String name) throws Exception {
    return MenuTemplate.parse(normalize(source), environment(font), registry, name);
  }

  public static String normalize(String source) {
    String result = source.replaceAll("theme=\"[^\"]+\"", "");
    var root = java.util.regex.Pattern.compile("<dui-menu\\b[^>]*>").matcher(result);
    if (root.find() && !root.group().contains("background="))
      result =
          root.replaceFirst(
              java.util.regex.Matcher.quoteReplacement(
                  root.group().replace("<dui-menu", "<dui-menu background=\"$surface\"")));
    if (!result
        .substring(result.indexOf("<dui-menu"), result.indexOf(">", result.indexOf("<dui-menu")))
        .contains("focus-outline="))
      result =
          result.replaceFirst(
              "<dui-menu", "<dui-menu focus-outline=\"hidden\" focus-outline-color=\"#16171D\"");
    return result;
  }
}
