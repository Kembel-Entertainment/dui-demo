package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import io.papermc.paper.registry.data.dialog.input.*;
import java.util.*;
import net.kyori.adventure.text.Component;

/** Components application: pure projection, typed actions and scoped effects. */
final class ShowcaseMenu extends DemoMenu {
  @Override
  String id() {
    return "components";
  }

  @Override
  List<String> templates() {
    return List.of("showcase", "showcase-compact", "setup", "form", "confirm");
  }

  @Override
  Set<String> aliases() {
    return Set.of("dui", "uikit");
  }

  enum Screen {
    SHOWCASE,
    SETUP,
    FORM,
    CONFIRM
  }

  ShowcaseMenu(DemoServices services) {
    super(services);
    on("kit_close", s -> {});
    on("kit_form", s -> s.screen = Screen.FORM);
    on("kit_reset", s -> s.screen = Screen.CONFIRM);
    on("kit_setup", this::setup);
    on("kit_display_change", this::setup);
    on(
        "kit_display_keep",
        s -> {
          if (s.preview != null) {
            s.display = s.preview;
            s.preview = null;
            services.save("display", s.display);
            s.kit.notice = "Layout saved. Change it with the settings icon.";
          }
          s.screen = Screen.SHOWCASE;
        });
    input("kit_display_compact", (s, response) -> select(s, response, "compact"));
    input("kit_display_spacious", (s, response) -> select(s, response, "spacious"));
    on(
        "kit_display_cancel",
        s -> {
          s.preview = null;
          s.kit.layout = s.display.layout;
          s.screen = Screen.SHOWCASE;
          s.reopenRequested = s.display.configured;
        });
    on(
        "kit_cancel",
        s -> {
          s.kit.notice = "Edit cancelled. Previous values kept.";
          s.screen = Screen.SHOWCASE;
          s.reopenRequested = true;
        });
    input("kit_save", this::saveForm);
    on(
        "kit_reset_yes",
        s -> {
          var previous = s.kit;
          s.kit = new ShowcaseState();
          s.kit.page = previous.page;
          s.kit.layout = previous.layout;
          s.kit.part = previous.part;
          s.kit.dark = previous.dark;
          s.screen = Screen.SHOWCASE;
        });
    for (var id : List.of("kit_page_cycle", "kit_part", "kit_choice", "kit_list_page"))
      on(id, DemoMenu::direction, (s, v) -> s.kit.apply(id, v.toString()));
    for (var id :
        List.of(
            "kit_theme",
            "kit_checkbox",
            "kit_dropdown",
            "kit_dropdown_close",
            "kit_press",
            "kit_progress",
            "kit_list_empty",
            "kit_stack",
            "kit_repair",
            "kit_rank")) on(id, s -> s.kit.apply(id, ""));
    on(
        "kit_page",
        v ->
            choice(
                v, ShowcaseState.PAGES.stream().map(ShowcaseState.Page::id).toArray(String[]::new)),
        (s, v) -> s.kit.apply("kit_page", v));
    on(
        "kit_dropdown_select",
        v -> choice(v, "alpha", "beta"),
        (s, v) -> s.kit.apply("kit_dropdown_select", v));
    on("kit_toggle", v -> choice(v, "enabled", "alerts"), (s, v) -> s.kit.apply("kit_toggle", v));
    on("kit_tab", v -> choice(v, "Alpha", "Beta", "Gamma"), (s, v) -> s.kit.apply("kit_tab", v));
    on(
        "kit_row",
        v -> choice(v, "Lorem", "Ipsum", "Dolor", "Sit", "Amet", "Elit", "Nunc"),
        (s, v) -> s.kit.apply("kit_row", v));
    on(
        "kit_item",
        v -> choice(v, "grass", "head", "tool", "banner"),
        (s, v) -> s.kit.apply("kit_item", v));
    on(
        "kit_node",
        v -> choice(v, "Origin", "Alpha", "Beta", "Gamma", "Delta"),
        (s, v) -> s.kit.apply("kit_node", v));
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.kit.part = 0;
    s.kit.dropdownOpen = false;
    s.screen = Screen.SHOWCASE;
    if (!s.display.configured && s.preview == null) setup(s);
  }

  void setup(DemoSession s) {
    s.setupInitial = s.preview != null ? s.preview : s.display;
    s.preview = null;
    s.kit.layout = s.display.layout;
    s.kit.dropdownOpen = false;
    s.screen = Screen.SETUP;
  }

  private void select(
      DemoSession s, io.papermc.paper.dialog.DialogResponseView response, String layout) {
    String scale = response == null ? null : response.getText("gui_scale");
    if (scale == null || !DisplayPreferences.SCALES.contains(scale)) return;
    s.preview = DisplayPreferences.selection(scale, layout);
    s.kit.layout = layout;
    s.kit.part = 0;
    s.screen = Screen.SHOWCASE;
  }

  private void saveForm(DemoSession s, io.papermc.paper.dialog.DialogResponseView response) {
    String name = response == null ? null : response.getText("sample_text"),
        option = response == null ? null : response.getText("sample_option");
    Boolean checked = response == null ? null : response.getBoolean("sample_check");
    Float amount = response == null ? null : response.getFloat("sample_amount");
    if (name == null
        || name.length() > 24
        || name.codePoints().anyMatch(Character::isISOControl)
        || option == null
        || !List.of("Option A", "Option B", "Option C").contains(option)
        || checked == null
        || amount == null
        || !Float.isFinite(amount)
        || amount < 0
        || amount > 100) {
      reject("kit_save");
      return;
    }
    var state = s.kit;
    state.formName = name.strip();
    state.formStyle = option;
    state.formChecked = checked;
    state.formAmount = Math.round(amount);
    state.page = "forms";
    state.notice = "Saved. Your sample values are shown on the cards.";
    s.screen = Screen.SHOWCASE;
  }

  private String theme(DemoSession s) {
    return s.kit.dark ? "studio_dark" : "studio";
  }

  private Canvas canvas(String name, Map<String, Object> data, Map<String, RasterImage> images) {
    return services.template(name).render(data, images);
  }

  @Override
  Map<String, org.bukkit.inventory.ItemStack> captureItems(DemoSession s) {
    return s.screen == Screen.SHOWCASE && s.kit.page.equals("media")
        ? services.viewerItems(p -> ShowcaseItems.stacks(p, s.kit))
        : Map.of();
  }

  @Override
  MenuView project(DemoSession s) {
    return switch (s.screen) {
      case SHOWCASE -> showcase(s);
      case SETUP -> setupView(s);
      case FORM -> formView(s);
      case CONFIRM -> confirmView(s);
    };
  }

  private MenuView showcase(DemoSession s) {
    var options =
        s.preview == null
            ? DialogOptions.notice("dui / Component showcase", "Close showcase", "kit_close")
            : new DialogOptions(
                Component.text("dui / Layout preview"),
                List.of(),
                List.of(
                    new DialogOptions.Button("Use this layout", "kit_display_keep", 128),
                    new DialogOptions.Button("Change size", "kit_display_change", 128)),
                null,
                2,
                true);
    return view(
        s.kit.compact() ? "showcase-compact" : "showcase",
        s.kit.data(),
        Map.of(),
        s,
        Map.of(),
        options);
  }

  private MenuView setupView(DemoSession s) {
    var c = canvas("setup", Map.of("theme", theme(s)), Map.of());
    var input =
        DialogInput.singleOption(
                "gui_scale",
                Component.text("Which GUI scale do you use?"),
                List.of("auto", "1", "2", "3", "4", "5+").stream()
                    .map(
                        v ->
                            SingleOptionDialogInput.OptionEntry.create(
                                v,
                                Component.text(v.equals("auto") ? "Auto / not sure" : v),
                                v.equals(s.setupInitial.guiScale)))
                    .toList())
            .width(260)
            .build();
    var options =
        new DialogOptions(
            Component.text("dui / Display setup"),
            List.of(input),
            List.of(
                new DialogOptions.Button("Compact preview", "kit_display_compact", 128),
                new DialogOptions.Button("Spacious preview", "kit_display_spacious", 128)),
            new DialogOptions.Button("Cancel", "kit_display_cancel", 128),
            2,
            false);

    return new MenuView(c, ViewModel.data(Map.of()), options);
  }

  private MenuView formView(DemoSession s) {
    int width = s.kit.compact() ? 260 : 320;
    var state = s.kit;
    var c = canvas("form", Map.of("width", width + 24, "theme", theme(s)), Map.of());
    var inputs =
        List.<DialogInput>of(
            DialogInput.text("sample_text", Component.text("Text / sample title"))
                .initial(state.formName)
                .maxLength(24)
                .width(width)
                .build(),
            DialogInput.bool("sample_check", Component.text("Checkbox / example enabled"))
                .initial(state.formChecked)
                .build(),
            DialogInput.singleOption(
                    "sample_option",
                    Component.text("Option picker / sample variant"),
                    List.of("Option A", "Option B", "Option C").stream()
                        .map(
                            v ->
                                SingleOptionDialogInput.OptionEntry.create(
                                    v, Component.text(v), v.equals(state.formStyle)))
                        .toList())
                .width(width)
                .build(),
            DialogInput.numberRange(
                    "sample_amount", Component.text("Slider / sample amount"), 0, 100)
                .initial((float) state.formAmount)
                .step(5f)
                .width(width)
                .build());
    var options =
        new DialogOptions(
            Component.text("dui / Sample form"),
            inputs,
            List.of(new DialogOptions.Button("Save example", "kit_save")),
            new DialogOptions.Button("Cancel", "kit_cancel"),
            1,
            false);

    return new MenuView(c, ViewModel.data(Map.of()), options);
  }

  private MenuView confirmView(DemoSession s) {
    return view(
        "confirm",
        Map.of("width", s.kit.compact() ? 284 : 344, "theme", theme(s)),
        Map.of(),
        s,
        Map.of(),
        DialogOptions.notice("dui / Reset examples", "Back to showcase", "kit_cancel"));
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    return switch (s.screen) {
      case SHOWCASE ->
          Map.of(
              "section",
              "showcase",
              "page",
              s.kit.page,
              "state",
              s.kit,
              "display",
              s.display,
              "preview",
              s.preview != null);
      case SETUP -> Map.of("section", "showcase", "page", "setup", "display", s.display);
      case FORM -> Map.of("section", "showcase", "page", "input", "state", s.kit);
      case CONFIRM -> Map.of("section", "showcase", "page", "confirm", "state", s.kit);
    };
  }

  @Override
  void validate(boolean compact) {
    var kit = new ShowcaseState();
    kit.layout = compact ? "compact" : "spacious";
    for (var page : ShowcaseState.PAGES) {
      kit.page = page.id();
      for (int part = 0; part < (compact ? kit.partCount() : 1); part++) {
        kit.part = part;
        for (boolean popup : List.of(false, true)) {
          kit.dropdownOpen = popup;
          services.template(compact ? "showcase-compact" : "showcase").render(kit.data());
        }
      }
    }
  }
}
