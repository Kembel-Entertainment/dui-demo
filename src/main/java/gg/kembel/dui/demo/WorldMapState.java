package gg.kembel.dui.demo;

import gg.kembel.dui.core.ActionRouter;
import gg.kembel.dui.core.world.*;
import java.util.*;

/** Pure demo application: 500ms gaze, locks, detail modes and replayable preview selection. */
final class WorldMapState {
  final boolean debug;
  String hudTitle = "";
  double hudProgress;
  String selected, hover, inhibited, chosen;
  long since;
  boolean close;
  private final ActionRouter<WorldMapState> actions = new ActionRouter<>((s, id) -> {});

  WorldMapState(boolean debug) {
    this.debug = debug;
    actions.on(
        "explore",
        value -> target(value),
        (s, t) -> {
          if (t.unlocked()) s.selected = t.id();
        });
    actions.on(
        "mode",
        value -> value,
        (s, mode) -> {
          if (s.selected == null || !WorldMapAssets.MODES.contains(mode)) return;
          if (mode.equals("back")) s.back();
          else {
            s.chosen = s.selected + " / " + mode.toUpperCase(Locale.ROOT);
            s.close = true;
          }
        });
  }

  private static WorldMapAssets.Target target(String id) {
    return WorldMapAssets.TARGETS.stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow();
  }

  void back() {
    inhibited = selected;
    selected = null;
    hover = null;
  }

  void accept(WorldMapInput input) {
    if (close) return;
    if (input.type() == WorldMapInput.Type.SECONDARY) {
      if (selected != null) back();
      else close = true;
      return;
    }
    if (debug) return;
    if (input.type() == WorldMapInput.Type.ZOOM) since = input.nanoTime();
    if (selected == null) {
      String next = input.region() == null ? null : input.region().id();
      if (!Objects.equals(next, hover)) {
        hover = next;
        since = input.nanoTime();
        if (!Objects.equals(next, inhibited)) inhibited = null;
      }
      if (hover != null
          && !Objects.equals(hover, inhibited)
          && target(hover).unlocked()
          && input.nanoTime() - since >= 500_000_000L) {
        selected = hover;
        return;
      }
    }
    if (input.type() == WorldMapInput.Type.PRIMARY && input.region() != null)
      actions.dispatch(this, input.region().action(), input.region().value());
  }

  WorldMapFrame project(int zoom, long now) {
    if (debug) {
      hudTitle = "CALIBRATION / 144 x 72 / centered at yaw 0, pitch 0";
      hudProgress = 0;
      return new WorldMapFrame("CALIBRATION", List.of("calibration"), List.of(), WorldHud.EMPTY);
    }
    var visible = new ArrayList<String>();
    var regions = new ArrayList<WorldMapFrame.Region>();
    visible.add("background");
    for (int i = 0; i < 20; i++) visible.add("tile_" + i);
    for (var target : WorldMapAssets.TARGETS) visible.add(target.id());
    String title = "ATLAS OF ELSEWHERE / Zoom " + zoom + " / First-person view";
    double progress = 0;
    if (selected != null) {
      var target = target(selected);
      visible.add("hover_" + selected);
      for (var mode : WorldMapAssets.MODES) {
        String id = selected + "_" + mode;
        visible.add(id);
        regions.add(new WorldMapFrame.Region(id, id, "mode", mode, true));
      }
      title = target.name() + " / Demo record " + target.record() + " / Choose a mode";
    } else {
      for (var target : WorldMapAssets.TARGETS)
        regions.add(
            new WorldMapFrame.Region(
                target.id(), target.id(), "explore", target.id(), target.unlocked()));
      if (hover != null) {
        var target = target(hover);
        visible.add("hover_" + hover);
        title = target.name() + (target.unlocked() ? " / Hold your aim or click" : " / Locked");
        if (target.unlocked() && !Objects.equals(hover, inhibited))
          progress = Math.clamp((now - since) / 500_000_000.0, 0, 1);
      }
    }
    hudTitle = title;
    hudProgress = progress;
    return new WorldMapFrame(
        selected == null ? "OVERVIEW" : "DETAIL:" + selected, visible, regions, WorldHud.EMPTY);
  }
}
