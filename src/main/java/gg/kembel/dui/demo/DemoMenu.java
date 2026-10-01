package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import io.papermc.paper.dialog.DialogResponseView;
import java.util.*;
import java.util.function.*;

/** Consumer-side controller definition. Projection has no timers, mutation or external I/O. */
abstract class DemoMenu {
  protected final DemoServices services;

  private record Input(DemoSession state, DialogResponseView response) {}

  private final ActionRouter<Input> actions;

  DemoMenu(DemoServices services) {
    this.services = services;
    actions = new ActionRouter<>((input, id) -> reject(id));
  }

  abstract String id();

  abstract List<String> templates();

  Set<String> aliases() {
    return Set.of();
  }

  abstract void prepare(DemoSession state, boolean compact);

  abstract MenuView project(DemoSession state);

  abstract void validate(boolean compact);

  void advance(DemoSession state) {}

  void closed(DemoSession state) {}

  void presented(DemoSession state, Canvas canvas) {}

  Map<String, Object> report(DemoSession state, Canvas canvas) {
    return Map.of("section", id());
  }

  final void capture(DemoSession state) {
    state.tick = services.tick();
    state.date = services.date();
    state.viewerName = services.viewerName();
    advance(state);
    state.items = Map.copyOf(captureItems(state));
  }

  Map<String, org.bukkit.inventory.ItemStack> captureItems(DemoSession state) {
    return Map.of();
  }

  final Set<String> actionIds() {
    return actions.actions();
  }

  final void dispatch(DemoSession state, String action, String value, DialogResponseView response) {
    actions.dispatch(new Input(state, response), action, value);
  }

  protected final void on(String id, Consumer<DemoSession> handler) {
    actions.on(id, input -> handler.accept(input.state()));
  }

  protected final <T> void on(
      String id, Function<String, T> decoder, BiConsumer<DemoSession, T> handler) {
    actions.on(id, decoder, (input, value) -> handler.accept(input.state(), value));
  }

  protected final void input(String id, BiConsumer<DemoSession, DialogResponseView> handler) {
    actions.on(id, input -> handler.accept(input.state(), input.response()));
  }

  protected void reject(String id) {
    services.message("That action is no longer available.");
  }

  protected final void guard(Runnable action) {
    try {
      action.run();
    } catch (IllegalArgumentException e) {
      reject("unavailable");
    }
  }

  protected final MenuView view(
      String template,
      Map<String, Object> data,
      Map<String, RasterImage> images,
      DemoSession state,
      Map<String, java.net.URI> links,
      DialogOptions options) {
    var model = new ViewModel(data, images, state.items, links);
    return MenuView.of(services.template(template), model, options);
  }

  protected final void later(long ticks, Runnable action) {
    services
        .viewTasks()
        .later(
            "animation",
            Math.max(1, ticks),
            () -> {
              action.run();
              services.refresh();
            });
  }

  static int direction(String value) {
    int n = Integer.parseInt(value);
    if (n != -1 && n != 1) throw new IllegalArgumentException("Direction must be -1 or 1");
    return n;
  }

  static String choice(String value, String... choices) {
    if (value == null || !List.of(choices).contains(value))
      throw new IllegalArgumentException("Unknown choice");
    return value;
  }
}
