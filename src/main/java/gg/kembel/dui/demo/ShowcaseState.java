package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

/** Disposable, per-viewer mock data; never reads or changes a player's inventory or economy. */
public final class ShowcaseState {
  public record Page(String id, String label, String title, String detail) {}

  public static final List<Page> PAGES =
      List.of(
          new Page(
              "basics",
              "Basics",
              "01 / The building blocks",
              "Type, spacing, surfaces and navigation. Start anywhere."),
          new Page(
              "actions",
              "Actions",
              "02 / Small actions, clear feedback",
              "Try the controls. Every example has its own visible state."),
          new Page(
              "cards",
              "Cards",
              "03 / Compose a little dashboard",
              "Mock numbers, reusable cards and progress you can change."),
          new Page(
              "lists",
              "Lists",
              "04 / A collection of placeholders",
              "Repeated rows, pagination and a useful empty state."),
          new Page(
              "media",
              "Media",
              "05 / More than flat icons",
              "Player portraits and live 3D items inside the same canvas."),
          new Page(
              "graph",
              "Graph",
              "06 / Connect a few ideas",
              "An original sample graph: nodes, edges, ranks and states."),
          new Page(
              "forms",
              "Forms",
              "07 / Make it your own",
              "Text, checkbox, option picker and slider in one sample form."));
  public static final Set<String> ROUTES =
      Set.of(
          "kit_page",
          "kit_theme",
          "kit_setup",
          "kit_page_cycle",
          "kit_part",
          "kit_checkbox",
          "kit_dropdown",
          "kit_dropdown_close",
          "kit_dropdown_select",
          "kit_press",
          "kit_toggle",
          "kit_choice",
          "kit_tab",
          "kit_progress",
          "kit_list_page",
          "kit_list_empty",
          "kit_row",
          "kit_item",
          "kit_stack",
          "kit_repair",
          "kit_node",
          "kit_rank",
          "kit_form",
          "kit_reset",
          "kit_cancel",
          "kit_reset_yes");
  public String page = "basics",
      notice = "Welcome to dui. Choose a page and explore.",
      tab = "Alpha",
      selectedRow = "None",
      selectedItem = "grass",
      selectedNode = "Alpha";
  public boolean enabled = true, alerts = false, empty = false, dark = false;
  public int presses = 0,
      choice = 0,
      progress = 40,
      listPage = 0,
      stack = 24,
      damage = 400,
      rank = 1;
  public String formName = "Lorem ipsum", formStyle = "Option A";
  public boolean formChecked = true;
  public int formAmount = 35;
  public String layout = "spacious", variant = "alpha";
  public int part;
  public boolean checkbox = true, dropdownOpen;
  private static final Map<String, List<String>> PARTS =
      Map.of(
          "basics", List.of("Type & badges", "Panels & navigation"),
          "actions",
              List.of(
                  "Buttons & tabs", "Choice & toggles", "Checkbox & dropdown", "Disabled & reset"),
          "cards", List.of("Progress card", "Completed card", "Waiting card"),
          "lists", List.of("Collection", "Empty state"),
          "media",
              List.of(
                  "Portraits", "Grass block", "Profile head", "Enchanted tool", "Layered banner"),
          "graph", List.of("Root & branches", "Upper branches", "Node inspector"),
          "forms", List.of("Open the form", "Result cards", "Reset examples"));

  public int partCount() {
    return PARTS.get(page).size();
  }

  public boolean compact() {
    return layout.equals("compact");
  }

  public void apply(String action, String value) {
    switch (action) {
      case "kit_page" -> {
        if (PAGES.stream().anyMatch(p -> p.id.equals(value))) page = value;
        part = 0;
        dropdownOpen = false;
        notice = "Explore, hover and click. All values are demo data.";
      }
      case "kit_page_cycle" -> {
        int i = 0;
        for (; i < PAGES.size(); i++) if (PAGES.get(i).id.equals(page)) break;
        page = PAGES.get(Math.floorMod(i + direction(value), PAGES.size())).id;
        part = 0;
        dropdownOpen = false;
      }
      case "kit_part" -> {
        part = Math.floorMod(part + direction(value), partCount());
        dropdownOpen = false;
      }
      case "kit_checkbox" -> {
        checkbox = !checkbox;
        notice = checkbox ? "Checkbox checked." : "Checkbox unchecked.";
      }
      case "kit_dropdown" -> dropdownOpen = !dropdownOpen;
      case "kit_dropdown_close" -> dropdownOpen = false;
      case "kit_dropdown_select" -> {
        if (Set.of("alpha", "beta").contains(value)) {
          variant = value;
          dropdownOpen = false;
          notice = "Selected variant: " + value + ".";
        }
      }
      case "kit_theme" -> {
        dark = !dark;
        notice = (dark ? "Dark" : "Light") + " mode enabled.";
      }
      case "kit_press" ->
          notice = "Button pressed " + (++presses) + " time" + (presses == 1 ? "." : "s.");
      case "kit_toggle" -> {
        if (value.equals("enabled")) enabled = !enabled;
        else if (value.equals("alerts")) alerts = !alerts;
        notice = "Toggle updated.";
      }
      case "kit_choice" -> {
        choice = Math.floorMod(choice + direction(value), 3);
        notice = "Choice: " + List.of("Low", "Mid", "High").get(choice) + ".";
      }
      case "kit_tab" -> {
        if (List.of("Alpha", "Beta", "Gamma").contains(value)) tab = value;
        notice = "Selected tab: " + tab + ".";
      }
      case "kit_progress" -> {
        progress = progress >= 100 ? 0 : Math.min(100, progress + 20);
        notice = "Progress updated to " + progress + "%.";
      }
      case "kit_list_page" -> {
        listPage = Math.floorMod(listPage + direction(value), 3);
        notice = "Collection page " + (listPage + 1) + " of 3.";
      }
      case "kit_list_empty" -> {
        empty = !empty;
        listPage = 0;
        selectedRow = "None";
        notice = empty ? "Empty state is now visible." : "Seven demo entries restored.";
      }
      case "kit_row" -> {
        if (List.of("Lorem", "Ipsum", "Dolor", "Sit", "Amet", "Elit", "Nunc").contains(value))
          selectedRow = value;
        notice = "Selected row: " + selectedRow + ".";
      }
      case "kit_item" -> {
        if (List.of("grass", "head", "tool", "banner").contains(value)) selectedItem = value;
        notice = "Selected item: " + itemLabel() + ".";
      }
      case "kit_stack" -> {
        stack = stack == 64 ? 1 : Math.min(64, stack + 8);
        notice = "Demo stack: " + stack + " blocks.";
      }
      case "kit_repair" -> {
        damage = damage == 0 ? 400 : 0;
        notice = damage == 0 ? "Tool repaired." : "Tool wear restored.";
      }
      case "kit_node" -> {
        if (List.of("Origin", "Alpha", "Beta", "Gamma", "Delta").contains(value))
          selectedNode = value;
        notice = "Selected node: " + selectedNode + ".";
      }
      case "kit_rank" -> {
        if (selectedNode.equals("Alpha")) {
          rank = (rank + 1) % 4;
          notice = "Alpha rank: " + rank + " / 3.";
        }
      }
      default -> throw new IllegalArgumentException("Unknown showcase action: " + action);
    }
  }

  private static int direction(String value) {
    return value.equals("-1") ? -1 : 1;
  }

  public String itemLabel() {
    return switch (selectedItem) {
      case "head" -> "Profile head";
      case "tool" -> "Glint + wear";
      case "banner" -> "Patterned banner";
      default -> "Grass block";
    };
  }

  public Map<String, Object> data() {
    var data = new HashMap<String, Object>();
    var current = PAGES.stream().filter(p -> p.id.equals(page)).findFirst().orElseThrow();
    data.put("title", current.title);
    data.put("detail", current.detail);
    data.put("notice", notice);
    data.put("theme", dark ? "studio_dark" : "studio");
    data.put("themeIcon", dark ? "sun" : "moon");
    data.put("themeHint", dark ? "Switch to light mode" : "Switch to dark mode");
    data.put("checkbox", checkbox);
    data.put("variant", variant);
    data.put("dropdownOpen", dropdownOpen);
    int part = Math.floorMod(this.part, partCount());
    data.put("part", part + 1);
    data.put("partCount", partCount());
    data.put("partLabel", PARTS.get(page).get(part));
    data.put("pageLabel", current.label);
    data.put("compactHeight", page.equals("media") ? 144 : 153);
    data.put("compactBodyHeight", page.equals("media") ? 72 : 81);
    for (var p : PAGES)
      for (int i = 0; i < PARTS.get(p.id).size(); i++)
        data.put(p.id + i, p.id.equals(page) && part == i);
    data.put(
        "navigation",
        PAGES.stream()
            .map(
                p ->
                    Map.<String, Object>of(
                        "id",
                        p.id,
                        "label",
                        p.label,
                        "active",
                        p.id.equals(page),
                        "detail",
                        p.detail))
            .toList());
    for (var p : PAGES) data.put(p.id + "Page", p.id.equals(page));
    data.put("enabled", enabled);
    data.put("alerts", alerts);
    data.put("presses", presses);
    data.put("choice", List.of("Low", "Mid", "High").get(choice));
    data.put("tab", tab);
    data.put(
        "tabs",
        List.of("Alpha", "Beta", "Gamma").stream()
            .map(t -> Map.<String, Object>of("label", t, "active", t.equals(tab)))
            .toList());
    data.put("progress", progress);
    data.put("remaining", 100 - progress);
    data.put("listEmpty", empty);
    data.put("listFilled", !empty);
    data.put("listPage", listPage + 1);
    data.put("selectedRow", selectedRow);
    data.put("emptyLabel", empty ? "Restore entries" : "Show empty state");
    var names = List.of("Lorem", "Ipsum", "Dolor", "Sit", "Amet", "Elit", "Nunc");
    var rows = new ArrayList<Map<String, Object>>();
    if (!empty)
      for (int i = listPage * 3; i < Math.min(listPage * 3 + 3, names.size()); i++)
        rows.add(
            Map.of(
                "id",
                i,
                "label",
                names.get(i),
                "value",
                names.get(i).equals(selectedRow) ? "Selected" : "View >"));
    data.put("rows", rows);
    data.put("rowSpace", (3 - rows.size()) * 27);
    data.put("selectedItem", itemLabel());
    data.put("stack", stack);
    data.put("toolWear", 1 - damage / 1561.0);
    for (String id : List.of("grass", "head", "tool", "banner"))
      data.put(id + "Selected", id.equals(selectedItem));
    data.put("selectedNode", selectedNode);
    data.put("rank", rank);
    data.put("alphaStatus", rank > 0 ? "learned" : "available");
    data.put("rankLocked", !selectedNode.equals("Alpha"));
    data.put(
        "nodeDetail",
        switch (selectedNode) {
          case "Origin" -> "Root / starting point";
          case "Alpha" -> "Available / adjustable rank";
          case "Beta" -> "Locked / a future step";
          case "Gamma" -> "Complete / 1 of 1";
          default -> "Excluded / alternate route";
        });
    data.put("formName", formName);
    data.put("formStyle", formStyle);
    data.put("formChecked", formChecked ? "Checked" : "Unchecked");
    data.put("formAmount", formAmount);
    return data;
  }
}
