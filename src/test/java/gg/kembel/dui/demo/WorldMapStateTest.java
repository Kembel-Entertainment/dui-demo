package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.world.*;
import org.junit.jupiter.api.Test;

class WorldMapStateTest {
  private WorldMapInput input(WorldMapInput.Type type, String target, long now) {
    var t =
        WorldMapAssets.TARGETS.stream()
            .filter(v -> v.id().equals(target))
            .findFirst()
            .orElseThrow();
    return new WorldMapInput(
        type,
        new WorldMapGeometry.Point(t.x(), t.y()),
        new WorldMapFrame.Region(t.id(), t.id(), "explore", t.id(), t.unlocked()),
        12,
        1,
        now);
  }

  @Test
  void dwellLocksBackAndReentryAreApplicationRules() {
    var state = new WorldMapState(false);
    state.accept(input(WorldMapInput.Type.AIM, "sylvan", 100));
    state.accept(input(WorldMapInput.Type.AIM, "sylvan", 500_000_099));
    assertNull(state.selected);
    state.accept(input(WorldMapInput.Type.AIM, "sylvan", 500_000_100));
    assertEquals("sylvan", state.selected);
    state.accept(input(WorldMapInput.Type.SECONDARY, "sylvan", 600_000_000));
    assertNull(state.selected);
    state.accept(input(WorldMapInput.Type.AIM, "sylvan", 2_000_000_000));
    assertNull(state.selected);
    state.accept(input(WorldMapInput.Type.PRIMARY, "ember", 3_000_000_000L));
    assertNull(state.selected);
    state.accept(input(WorldMapInput.Type.PRIMARY, "cloud", 4_000_000_000L));
    assertEquals("cloud", state.selected);
    state.project(12, 4_000_000_000L).validate(WorldMapAssets.definition());
  }

  @Test
  void detailOptionsUseTheSameDefinitionAsTheRenderer() {
    var state = new WorldMapState(false);
    state.accept(input(WorldMapInput.Type.PRIMARY, "sylvan", 0));
    var frame = state.project(12, 0);
    var d = WorldMapAssets.definition();
    frame.validate(d);
    var region = WorldMapGeometry.hit(d, frame, new WorldMapGeometry.Point(400, 300));
    assertEquals("solo", region.value());
    state.accept(
        new WorldMapInput(
            WorldMapInput.Type.PRIMARY, new WorldMapGeometry.Point(400, 300), region, 12, 2, 1));
    assertTrue(state.close);
    assertEquals("sylvan / SOLO", state.chosen);
  }
}
