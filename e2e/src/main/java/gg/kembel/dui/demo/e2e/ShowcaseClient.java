package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import gg.kembel.dui.demo.e2e.mixin.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.input.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Real mouse/keyboard input and screenshots; no changes to the vanilla UI renderer. */
final class ShowcaseClient {
  private int ticks, stage, changed;
  private String inventoryBefore;
  private JsonObject savedDisplay;
  private static final String[] FULL_STEPS = {
    "SHOT:display-setup",
    "GUI_SCALE2",
    "PREVIEW_SPACIOUS",
    "KEEP",
    "DISPLAY:spacious",
    "SHOT:basics",
    "kit_theme",
    "STATE:dark:true",
    "SHOT:basics-dark",
    "kit_start",
    "STATE:dark:true",
    "kit_theme",
    "STATE:dark:false",
    "kit_press",
    "kit_disabled",
    "STATE:presses:1",
    "kit_enabled",
    "STATE:enabled:false",
    "kit_alerts",
    "kit_tab_Beta",
    "kit_choice_next",
    "STATE:choice:1",
    "kit_checkbox",
    "kit_checkbox_locked",
    "STATE:checkbox:false",
    "SHOT:actions",
    "kit_dropdown",
    "SHOT:dropdown",
    "kit_dropdown_option_2",
    "STATE:variant:alpha",
    "STATE:dropdownOpen:true",
    "kit_dropdown_option_1",
    "STATE:variant:beta",
    "STATE:dropdownOpen:false",
    "kit_dropdown",
    "OUTSIDE",
    "STATE:dropdownOpen:false",
    "kit_reset",
    "SHOT:confirmation",
    "kit_cancel",
    "STATE:presses:1",
    "kit_nav_cards",
    "kit_progress",
    "STATE:progress:60",
    "SHOT:cards",
    "kit_nav_lists",
    "kit_row_1",
    "STATE:selectedRow:Ipsum",
    "SHOT:lists",
    "kit_next",
    "kit_next",
    "kit_row_6",
    "STATE:selectedRow:Nunc",
    "kit_empty",
    "SHOT:empty",
    "kit_empty",
    "kit_prev",
    "STATE:listPage:2",
    "kit_nav_media",
    "ITEMS",
    "SHOT:media",
    "kit_theme",
    "ITEMS",
    "SHOT:media-dark",
    "kit_theme",
    "kit_tool",
    "HOVER:kit_tool",
    "SHOT:item-tooltip",
    "kit_repair",
    "STATE:damage:0",
    "kit_stack",
    "STATE:stack:32",
    "kit_head",
    "STATE:selectedItem:head",
    "SCALE1",
    "ITEMS",
    "SHOT:media-scale-1",
    "kit_banner",
    "STATE:selectedItem:banner",
    "SCALE2",
    "kit_nav_graph",
    "kit_rank",
    "STATE:rank:2",
    "kit_beta",
    "kit_rank",
    "STATE:rank:2",
    "kit_delta",
    "kit_alpha",
    "SHOT:graph",
    "kit_nav_forms",
    "kit_form",
    "TYPE",
    "CHECKBOX",
    "OPTION",
    "SLIDER",
    "SHOT:form",
    "SAVE",
    "STATE:formName:Hello dui",
    "STATE:formChecked:false",
    "STATE:formStyle:Option B",
    "SHOT:forms",
    "kit_form",
    "CHECK_FORM",
    "TYPE_CANCEL",
    "CANCEL",
    "STATE:formName:Hello dui",
    "kit_theme",
    "kit_reset_form",
    "SHOT:confirmation-dark",
    "kit_reset_yes",
    "STATE:dark:true",
    "STATE:presses:0",
    "STATE:formName:Lorem ipsum",
    "kit_nav_basics",
    "kit_setup",
    "PREVIEW_COMPACT",
    "SHOT:compact-preview",
    "KEEP",
    "DISPLAY:compact",
    "SMALL_WINDOW",
    "SHOT:compact-basics",
    "kit_part_next",
    "SHOT:compact-panels",
    "kit_media_link",
    "SHOT:compact-heads",
    "kit_part_next",
    "ITEMS_COMPACT",
    "SHOT:compact-grass",
    "kit_stack",
    "STATE:stack:32",
    "kit_part_next",
    "ITEMS_COMPACT",
    "kit_head",
    "SHOT:compact-head",
    "kit_part_next",
    "ITEMS_COMPACT",
    "kit_repair",
    "SHOT:compact-tool",
    "kit_part_next",
    "ITEMS_COMPACT",
    "kit_banner",
    "SHOT:compact-banner",
    "kit_nav_next",
    "kit_rank",
    "STATE:rank:2",
    "SHOT:compact-graph",
    "kit_part_next",
    "kit_delta",
    "SHOT:compact-branches",
    "kit_part_next",
    "SHOT:compact-inspector",
    "kit_nav_next",
    "kit_form",
    "SCROLL_DOWN",
    "SAVE",
    "kit_part_next",
    "SHOT:compact-results",
    "kit_part_next",
    "kit_reset_form",
    "kit_reset_yes",
    "DISPLAY:compact",
    "STATE:stack:24",
    "kit_nav_next",
    "kit_nav_next",
    "kit_press",
    "kit_disabled",
    "STATE:presses:1",
    "SHOT:compact-buttons",
    "kit_part_next",
    "kit_choice_next",
    "kit_enabled",
    "SHOT:compact-toggles",
    "kit_part_next",
    "kit_checkbox",
    "STATE:checkbox:false",
    "SHOT:compact-controls",
    "kit_dropdown",
    "SHOT:compact-dropdown",
    "kit_dropdown_option_2",
    "STATE:variant:alpha",
    "kit_dropdown_option_1",
    "STATE:variant:beta",
    "kit_dropdown",
    "OUTSIDE",
    "STATE:dropdownOpen:false",
    "kit_part_next",
    "kit_locked",
    "kit_locked_off",
    "SHOT:compact-disabled",
    "kit_nav_next",
    "kit_progress",
    "SHOT:compact-card",
    "kit_part_next",
    "SHOT:compact-completed",
    "kit_part_next",
    "SHOT:compact-waiting",
    "kit_nav_next",
    "kit_row_1",
    "SHOT:compact-list",
    "kit_part_next",
    "kit_empty",
    "kit_part_previous",
    "SHOT:compact-empty",
    "kit_part_next",
    "kit_empty",
    "kit_nav_next",
    "kit_nav_next",
    "kit_nav_next",
    "kit_part_next",
    "kit_part_next",
    "kit_reset_form",
    "kit_reset_yes",
    "DISPLAY:compact",
    "STATE:presses:0",
    "STATE:dark:true",
    "AUTO_WINDOW",
    "kit_nav_previous",
    "kit_nav_previous",
    "SHOT:auto-heads-dark",
    "kit_theme",
    "SHOT:auto-heads",
    "SCALE3",
    "SHOT:scale-3-heads",
    "SCALE4",
    "SHOT:scale-4-heads",
    "kit_part_next",
    "ITEMS_COMPACT",
    "SHOT:auto-grass",
    "kit_nav_previous",
    "kit_nav_previous",
    "kit_nav_previous",
    "kit_part_next",
    "kit_part_next",
    "SHOT:auto-controls",
    "kit_dropdown",
    "SHOT:auto-dropdown",
    "OUTSIDE",
    "STATE:dropdownOpen:false",
    "kit_theme",
    "STATE:dark:true",
    "CLOSE"
  };
  private static final String[] STEPS = steps();

  private static String[] steps() {
    if (!Boolean.getBoolean("dui.e2e.showcase.compactOnly")) return FULL_STEPS;
    var steps =
        new ArrayList<String>(List.of("GUI_SCALE2", "PREVIEW_COMPACT", "KEEP", "kit_theme"));
    steps.addAll(
        Arrays.asList(FULL_STEPS)
            .subList(Arrays.asList(FULL_STEPS).indexOf("SMALL_WINDOW"), FULL_STEPS.length));
    return steps.toArray(String[]::new);
  }

  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  }

  private void tick(Minecraft mc) {
    mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    ticks++;
    if (ticks > 14000 || stage > 0 && ticks - changed > 700) {
      System.err.println("SHOWCASE_TEST_FAILED timeout stage=" + stage);
      mc.stop();
      return;
    }
    if (mc.gui.overlay() != null || ticks - changed < 12) return;
    try {
      if (stage == 0 && ticks > 80) {
        var preference =
            Paths.plugin()
                .resolve(
                    "display/"
                        + UUID.nameUUIDFromBytes(
                            "OfflinePlayer:ShowcaseTest"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        + ".json");
        if (Files.exists(preference))
          savedDisplay = JsonParser.parseString(Files.readString(preference)).getAsJsonObject();
        mc.options.guiScale().set(2);
        mc.getWindow().setWindowed(1280, 900);
        mc.resizeGui();
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().handle());
        var data = new ServerData("dui fixture", "127.0.0.1:25584", ServerData.Type.OTHER);
        data.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
            new TitleScreen(), mc, ServerAddress.parseString(data.ip), data, false, null);
        advance();
        return;
      }
      if (stage == 1) {
        if (!(mc.gui.screen() instanceof DialogScreen<?>)) return;
        if (savedDisplay != null && savedDisplay.get("configured").getAsBoolean()) {
          if (!layout().getAsJsonObject("display").equals(savedDisplay)
              || layout().get("page").getAsString().equals("setup"))
            throw new IllegalStateException("Saved display preference was not restored on join");
          System.out.println("SHOWCASE_PREFERENCE_RESTORED " + savedDisplay);
        }
        inventoryBefore = mc.player.getInventory().getNonEquipmentItems().toString();
        mc.getConnection().sendCommand("uikit setup");
        advance();
        return;
      }
      // Let initial connection/resource-pack toasts expire before the overview screenshot.
      if (stage == 2 && ticks - changed < 360) return;
      if (stage == STEPS.length + 2) {
        if (mc.gui.screen() instanceof DialogScreen<?>)
          throw new IllegalStateException("Showcase did not close");
        if (!inventoryBefore.equals(mc.player.getInventory().getNonEquipmentItems().toString()))
          throw new IllegalStateException("Showcase changed real inventory");
        Files.writeString(
            Paths.output().resolve("client-result.json"),
            new Gson()
                .toJson(Map.of("passed", true, "steps", STEPS.length, "inventoryUnchanged", true)));
        System.out.println("SHOWCASE_TEST_COMPLETE steps=" + STEPS.length);
        mc.stop();
        return;
      }
      if (stage < 2 || !(mc.gui.screen() instanceof DialogScreen<?>)) return;
      String step = STEPS[stage - 2];
      if (step.startsWith("SHOT:")) snapshot(mc, step.substring(5));
      else if (step.startsWith("STATE:")) {
        var parts = step.split(":", 3);
        String actual = layout().getAsJsonObject("state").get(parts[1]).getAsString();
        if (!actual.equals(parts[2])) throw new IllegalStateException(step + " actual=" + actual);
      } else if (step.startsWith("DISPLAY:")) {
        var data = layout();
        var pref = data.getAsJsonObject("display");
        if (!pref.get("configured").getAsBoolean()
            || !pref.get("guiScale").getAsString().equals("2")
            || !pref.get("layout").getAsString().equals(step.substring(8))
            || data.get("preview").getAsBoolean())
          throw new IllegalStateException("Unsaved display preference: " + pref);
      } else if (step.startsWith("HOVER:")) hit(mc, step.substring(6), false);
      else
        switch (step) {
          case "ITEMS" -> {
            validateCanvas(mc);
            if (widgets(mc.gui.screen()).stream()
                    .filter(w -> w instanceof ItemDisplayWidget)
                    .count()
                != 5)
              throw new IllegalStateException("Four native models plus layer fence expected");
          }
          case "GUI_SCALE2" -> {
            var picker =
                widgets(mc.gui.screen()).stream()
                    .filter(w -> w instanceof CycleButton<?>)
                    .findFirst()
                    .orElseThrow();
            for (int i = 0; i < 6 && !picker.getMessage().getString().endsWith(": 2"); i++)
              click(mc, picker);
            if (!picker.getMessage().getString().endsWith(": 2"))
              throw new IllegalStateException(
                  "Could not select GUI scale: " + picker.getMessage().getString());
          }
          case "PREVIEW_SPACIOUS" -> label(mc, "Spacious preview");
          case "PREVIEW_COMPACT" -> label(mc, "Compact preview");
          case "KEEP" -> label(mc, "Use this layout");
          case "SMALL_WINDOW" -> {
            mc.getWindow().setWindowed(640, 480);
            mc.options.guiScale().set(2);
            mc.resizeGui();
          }
          case "AUTO_WINDOW" -> {
            mc.getWindow().setWindowed(1440, 1000);
            mc.options.guiScale().set(0);
            mc.resizeGui();
          }
          case "OUTSIDE" -> {
            var data = layout();
            var w = canvas(mc);
            clickAt(
                mc,
                w.getX() + (w.getWidth() - data.get("width").getAsInt() - 2) / 2.0 + 1,
                w.getY() + w.getPadding() + 1);
          }
          case "ITEMS_COMPACT" -> {
            validateCanvas(mc);
            var items =
                widgets(mc.gui.screen()).stream()
                    .filter(w -> w instanceof ItemDisplayWidget)
                    .toList();
            if (items.size() != 2)
              throw new IllegalStateException("One model plus layer fence expected");
            for (var item : items)
              if (item.getY() < 33
                  || item.getY() + item.getHeight() > mc.getWindow().getGuiScaledHeight() - 33)
                throw new IllegalStateException(
                    "Native item carrier outside viewport: " + item.getY());
          }
          case "SCALE1" -> {
            mc.options.guiScale().set(1);
            mc.resizeGui();
          }
          case "SCALE2" -> {
            mc.options.guiScale().set(2);
            mc.resizeGui();
          }
          case "SCALE3" -> {
            mc.options.guiScale().set(3);
            mc.resizeGui();
          }
          case "SCALE4" -> {
            mc.options.guiScale().set(4);
            mc.resizeGui();
          }
          case "TYPE", "TYPE_CANCEL" -> {
            var box =
                (EditBox)
                    widgets(mc.gui.screen()).stream()
                        .filter(w -> w instanceof EditBox)
                        .findFirst()
                        .orElseThrow();
            click(mc, box);
            box.setValue("");
            (step.equals("TYPE") ? "Hello dui" : "Do not save")
                .codePoints()
                .forEach(
                    code ->
                        ((FixtureKeyboardAccess) mc.keyboardHandler)
                            .dui$character(mc.getWindow().handle(), new CharacterEvent(code)));
          }
          case "CHECKBOX" ->
              click(
                  mc,
                  widgets(mc.gui.screen()).stream()
                      .filter(w -> w instanceof Checkbox)
                      .findFirst()
                      .orElseThrow());
          case "OPTION" ->
              click(
                  mc,
                  widgets(mc.gui.screen()).stream()
                      .filter(w -> w instanceof CycleButton<?>)
                      .findFirst()
                      .orElseThrow());
          case "SLIDER" -> {
            var slider =
                widgets(mc.gui.screen()).stream()
                    .filter(w -> w instanceof AbstractSliderButton)
                    .findFirst()
                    .orElseThrow();
            clickAt(
                mc,
                slider.getX() + slider.getWidth() * .8,
                slider.getY() + slider.getHeight() / 2.0);
          }
          case "CHECK_FORM" -> {
            var box =
                (EditBox)
                    widgets(mc.gui.screen()).stream()
                        .filter(w -> w instanceof EditBox)
                        .findFirst()
                        .orElseThrow();
            if (!box.getValue().equals("Hello dui"))
              throw new IllegalStateException("Form did not retain the saved text");
          }
          case "SCROLL_DOWN" -> {
            move(
                mc,
                mc.getWindow().getGuiScaledWidth() / 2.0,
                mc.getWindow().getGuiScaledHeight() / 2.0);
            ((FixtureMouseAccess) mc.mouseHandler).dui$scroll(mc.getWindow().handle(), 0, -100);
            move(mc, 5, 5);
          }
          case "SAVE" -> label(mc, "Save example");
          case "CANCEL" -> label(mc, "Cancel");
          case "CLOSE" -> label(mc, "Close showcase");
          default -> hit(mc, step, true);
        }
      advance();
    } catch (Exception e) {
      System.err.println(
          "SHOWCASE_TEST_FAILED stage="
              + stage
              + " step="
              + (stage >= 2 && stage < STEPS.length + 2 ? STEPS[stage - 2] : "setup"));
      e.printStackTrace();
      Screenshot.takeScreenshot(
          mc.gameRenderer.mainRenderTarget(),
          image -> {
            try (image) {
              image.writeToFile(Paths.output().resolve("failure.png"));
            } catch (Exception ignored) {
            }
          });
      mc.stop();
    }
  }

  private void advance() {
    stage++;
    changed = ticks;
    System.out.println("SHOWCASE_TEST_STAGE " + stage);
  }

  private static List<AbstractWidget> widgets(GuiEventListener parent) {
    var result = new ArrayList<AbstractWidget>();
    if (parent instanceof AbstractWidget w) result.add(w);
    if (parent instanceof ContainerEventHandler c)
      for (var child : c.children()) result.addAll(widgets(child));
    return result;
  }

  private static FocusableTextWidget canvas(Minecraft mc) {
    return widgets(mc.gui.screen()).stream()
        .filter(w -> w instanceof FocusableTextWidget)
        .map(w -> (FocusableTextWidget) w)
        .max(Comparator.comparingInt(AbstractWidget::getHeight))
        .orElseThrow();
  }

  private static JsonObject layout() throws Exception {
    return JsonParser.parseString(
            Files.readString(Paths.plugin().resolve("layouts/ShowcaseTest.json")))
        .getAsJsonObject();
  }

  private static void validateCanvas(Minecraft mc) throws Exception {
    var data = layout();
    var w = canvas(mc);
    int width = data.get("width").getAsInt(), height = data.get("height").getAsInt();
    int measured =
        mc.font.split(w.getMessage(), width + 4).stream()
            .mapToInt(mc.font::width)
            .max()
            .orElseThrow();
    if (measured != width + 2 || w.getHeight() != height + 8)
      throw new IllegalStateException("Canvas size mismatch: " + measured + " / " + w.getHeight());
    double originX = w.getX() + (w.getWidth() - width - 2) / 2.0;
    if (originX < 0
        || originX + width > mc.getWindow().getGuiScaledWidth()
        || w.getY() + w.getPadding() < 33
        || w.getY() + w.getPadding() + height > mc.getWindow().getGuiScaledHeight() - 33)
      throw new IllegalStateException(
          "Canvas clipped outside window: widget="
              + w.getX()
              + ","
              + w.getY()
              + " "
              + w.getWidth()
              + "x"
              + w.getHeight()
              + " viewport="
              + mc.getWindow().getGuiScaledWidth()
              + "x"
              + mc.getWindow().getGuiScaledHeight());
  }

  private static void hit(Minecraft mc, String id, boolean click) throws Exception {
    validateCanvas(mc);
    var data = layout();
    JsonObject region = null;
    for (var entry : data.getAsJsonArray("hits"))
      if (entry.getAsJsonObject().get("id").getAsString().equals(id))
        region = entry.getAsJsonObject();
    if (region == null) throw new IllegalStateException("Missing hit: " + id);
    var w = canvas(mc);
    double x =
        w.getX()
            + (w.getWidth() - data.get("width").getAsInt() - 2) / 2.0
            + region.get("x").getAsInt()
            + region.get("width").getAsInt() / 2.0;
    double y =
        w.getY()
            + w.getPadding()
            + region.get("y").getAsInt()
            + region.get("height").getAsInt() / 2.0;
    if (click) {
      System.out.println("SHOWCASE_HIT " + id + " " + x + "," + y);
      clickAt(mc, x, y);
    } else move(mc, x, y);
  }

  private static void label(Minecraft mc, String label) {
    click(
        mc,
        widgets(mc.gui.screen()).stream()
            .filter(w -> w.getMessage().getString().equals(label))
            .findFirst()
            .orElseThrow());
  }

  private static void click(Minecraft mc, AbstractWidget w) {
    clickAt(mc, w.getX() + w.getWidth() / 2.0, w.getY() + w.getHeight() / 2.0);
  }

  private static void move(Minecraft mc, double x, double y) {
    var w = mc.getWindow();
    ((FixtureMouseAccess) mc.mouseHandler)
        .dui$move(
            w.handle(),
            x * w.getScreenWidth() / w.getGuiScaledWidth(),
            y * w.getScreenHeight() / w.getGuiScaledHeight());
  }

  private static void clickAt(Minecraft mc, double x, double y) {
    move(mc, x, y);
    var mouse = (FixtureMouseAccess) mc.mouseHandler;
    mouse.dui$button(mc.getWindow().handle(), new MouseButtonInfo(0, 0), 1);
    mouse.dui$button(mc.getWindow().handle(), new MouseButtonInfo(0, 0), 0);
    move(mc, 5, 5);
  }

  private static void snapshot(Minecraft mc, String name) throws Exception {
    validateCanvas(mc);
    Path directory = Paths.output().resolve("screenshots");
    Files.createDirectories(directory);
    var meta = new JsonObject();
    var w = canvas(mc);
    var data = layout();
    meta.addProperty("screenWidth", mc.getWindow().getGuiScaledWidth());
    meta.addProperty("screenHeight", mc.getWindow().getGuiScaledHeight());
    meta.addProperty("scale", mc.getWindow().getGuiScale());
    meta.addProperty("canvasX", w.getX() + (w.getWidth() - data.get("width").getAsInt() - 2) / 2.0);
    meta.addProperty("canvasY", w.getY() + w.getPadding());
    meta.add("layout", data);
    Files.writeString(directory.resolve(name + ".json"), meta.toString());
    Screenshot.takeScreenshot(
        mc.gameRenderer.mainRenderTarget(),
        image -> {
          try (image) {
            image.writeToFile(directory.resolve(name + ".png"));
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }
}
