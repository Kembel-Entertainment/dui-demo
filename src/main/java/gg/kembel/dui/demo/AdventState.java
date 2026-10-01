package gg.kembel.dui.demo;

import gg.kembel.dui.core.AnimationTimeline;
import java.util.List;

/** Session-only playground: no date gates, claim ledger, inventory grants or persistence. */
public final class AdventState {
  public enum Phase {
    BOARD,
    OPENING,
    REVEALED
  }

  public record Gift(String name, String material, String caption) {}

  public static final AnimationTimeline OPENING =
      AnimationTimeline.sequence(
          AnimationTimeline.of("open", 24), AnimationTimeline.of("celebrate", 96));
  public static final int OPEN_TICKS = (int) OPENING.segment("open").duration();
  public static final List<Gift> GIFTS =
      List.of(
          new Gift("Pocket sunshine", "GOLDEN_APPLE", "A tiny snack. Main-character energy."),
          new Gift("Diamond dopamine", "DIAMOND", "Sparkle first. Questions later."),
          new Gift("Mint condition", "EMERALD", "Fresh out of the gift department."),
          new Gift("Snow day", "SNOWBALL", "Zero responsibilities. Maximum chill."),
          new Gift("Golden hour", "GOLD_INGOT", "Your daily dose of shiny."),
          new Gift("Bloom mode", "CHERRY_SAPLING", "A little room for something lovely."),
          new Gift("Cosmic cookie", "COOKIE", "Crumbs are a personality trait."),
          new Gift("Crystal crush", "AMETHYST_SHARD", "Purple. Pretty. Entirely unnecessary."),
          new Gift("Plot twist", "ENCHANTED_BOOK", "The next chapter looks good on you."),
          new Gift("Cloud nine", "FEATHER", "Travel light. Dream loudly."),
          new Gift("Glow up", "GLOWSTONE", "Consider this your soft-launch era."),
          new Gift("Heart of gold", "GOLDEN_CARROT", "A healthy amount of extra."),
          new Gift("Little legend", "TOTEM_OF_UNDYING", "Small gift. Ridiculous confidence."),
          new Gift("Star treatment", "NETHER_STAR", "You are the whole constellation."),
          new Gift("Blue mood", "LAPIS_LAZULI", "A very good kind of blue."),
          new Gift("Ocean energy", "HEART_OF_THE_SEA", "Deep breaths. Big possibilities."),
          new Gift("Sweet nothing", "HONEY_BOTTLE", "Sweet enough to replay."),
          new Gift("Hot chocolate", "COCOA_BEANS", "Officially a blanket-weather person."),
          new Gift("Tiny treasure", "CHEST", "Good things. Suspiciously small box."),
          new Gift("Winter wanderer", "COMPASS", "Find your next little adventure."),
          new Gift("Lucky lantern", "LANTERN", "A warm light for a cold night."),
          new Gift("Afterparty", "FIREWORK_ROCKET", "The celebration is the whole point."),
          new Gift("Pink promise", "PINK_TULIP", "A soft spot for bold colours."),
          new Gift("Grand finale", "BEACON", "Big glow. Absolutely no waiting."));

  public Phase phase = Phase.BOARD;
  public int selected;
  public long startedAt = -1, rewardAt = -1, generation;
  public boolean compact, motion = true, effectsFinished;

  public void open(int day, long tick) {
    if (day < 1 || day > 24 || tick < 0)
      throw new IllegalArgumentException("Gift must be 1..24 with a world tick");
    selected = day;
    effectsFinished = false;
    startedAt = tick;
    rewardAt = motion ? -1 : tick;
    phase = motion ? Phase.OPENING : Phase.REVEALED;
    generation++;
  }

  public boolean reveal(long token, long tick) {
    if (generation != token || phase != Phase.OPENING || tick - startedAt < OPEN_TICKS)
      return false;
    phase = Phase.REVEALED;
    rewardAt = tick;
    return true;
  }

  public void back() {
    phase = Phase.BOARD;
    selected = 0;
    effectsFinished = false;
    startedAt = rewardAt = -1;
    generation++;
  }

  public Gift gift() {
    if (selected == 0) throw new IllegalStateException("No gift selected");
    return GIFTS.get(selected - 1);
  }
}
