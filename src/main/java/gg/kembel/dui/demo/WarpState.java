package gg.kembel.dui.demo;

import java.util.List;

/** A session-only carousel. No permission gates, teleportation or saved player state. */
public final class WarpState {
  public record Destination(String id, String name, String subtitle, String material, int accent) {}

  public static final List<Destination> DESTINATIONS =
      List.of(
          new Destination(
              "willow",
              "Willow Vale",
              "Rivers, ruins & a slower kind of adventure.",
              "OAK_SAPLING",
              0xA6D879),
          new Destination(
              "bloom",
              "Bloomhaven",
              "A tiny cottage. An unreasonable amount of pink.",
              "CHERRY_SAPLING",
              0xF5A7C2),
          new Destination(
              "ember", "Ember Keep", "Cross the lava. Claim the sunset.", "BLAZE_POWDER", 0xFFA45E),
          new Destination(
              "astral",
              "Astral Reach",
              "Floating islands. No ordinary horizon.",
              "ENDER_PEARL",
              0xADAAFF));
  public static final int SLIDE_TICKS = 24;
  public int selected, direction;
  public long startedAt = -1, generation;
  public boolean compact, motion = true, moving, arrived;

  public boolean step(int direction, long tick) {
    if (Math.abs(direction) != 1 || tick < 0)
      throw new IllegalArgumentException("Step must be -1 or 1 with a world tick");
    if (moving) return false;
    selected = Math.floorMod(selected + direction, DESTINATIONS.size());
    this.direction = direction;
    startedAt = tick;
    moving = motion;
    arrived = false;
    generation++;
    return true;
  }

  public boolean finish(long token, long tick) {
    if (!moving || token != generation || tick - startedAt < SLIDE_TICKS) return false;
    moving = false;
    startedAt = -1;
    return true;
  }

  public void jump(int target) {
    if (target < 0 || target >= DESTINATIONS.size())
      throw new IllegalArgumentException("Unknown warp");
    selected = target;
    stop();
    arrived = false;
  }

  public void preview(long tick) {
    if (tick < 0) throw new IllegalArgumentException("World tick must be nonnegative");
    arrived = true;
    startedAt = motion ? tick : -1;
    generation++;
  }

  public void stop() {
    moving = false;
    startedAt = -1;
    generation++;
  }

  public Destination destination() {
    return DESTINATIONS.get(selected);
  }
}
