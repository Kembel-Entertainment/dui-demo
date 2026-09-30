package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.time.LocalDate;
import java.util.List;

/** Server-owned demo ledger. Days use UTC plus a per-player preview offset. */
public final class RewardState {
  public record Reward(String name, String model, int stars, int color, int light) {}

  public static final List<Reward> WEEK =
      List.of(
          new Reward("Pocket sunshine", "honey_bottle", 50, 0xFFC857, 0xFFF1B9),
          new Reward("Cookie break", "cookie", 75, 0xFF936C, 0xFFE0CA),
          new Reward("Mint condition", "emerald", 100, 0x66D9B1, 0xD4F8E5),
          new Reward("Cosmic candy", "amethyst_shard", 125, 0xB39BFF, 0xE9DFFF),
          new Reward("Blue-sky bottle", "experience_bottle", 150, 0x71CAFF, 0xDBF2FF),
          new Reward("Party starter", "firework_rocket", 200, 0xFF87B6, 0xFFE0EC),
          new Reward("The grand finale", "yellow_shulker_box", 350, 0xFFD65E, 0xFFF4C9));
  public int completed, demoDays;
  public long stars;
  public String lastClaim = "";
  public boolean motion = true;
  public transient boolean compact, celebrating;
  public transient int selected = -1;
  public transient long burstStarted = -1;

  public LocalDate today(LocalDate utc) {
    return utc.plusDays(demoDays);
  }

  public boolean claimedToday(LocalDate utc) {
    return !lastClaim.isEmpty() && !today(utc).isAfter(LocalDate.parse(lastClaim));
  }

  public void sync(LocalDate utc) {
    if (!lastClaim.isEmpty() && today(utc).isAfter(LocalDate.parse(lastClaim))) {
      if (today(utc).isAfter(LocalDate.parse(lastClaim).plusDays(1)) || completed == 7)
        completed = 0;
    }
  }

  public int current(LocalDate utc) {
    return claimedToday(utc) ? Math.max(0, completed - 1) : Math.min(6, completed);
  }

  public int selection(LocalDate utc) {
    return selected < 0 ? current(utc) : selected;
  }

  public boolean claim(LocalDate utc) {
    sync(utc);
    if (claimedToday(utc)) return false;
    stars += WEEK.get(completed).stars();
    selected = completed++;
    lastClaim = today(utc).toString();
    celebrating = true;
    return true;
  }

  public void apply(String action, String value, LocalDate utc) {
    sync(utc);
    switch (action) {
      case "reward_claim" -> claim(utc);
      case "reward_select" -> {
        int day = Integer.parseInt(value);
        if (day < 0 || day >= WEEK.size()) throw new IllegalArgumentException("Unknown reward day");
        selected = day;
        celebrating = false;
      }
      case "reward_next" -> {
        demoDays++;
        selected = -1;
        celebrating = false;
        sync(utc);
      }
      case "reward_reset" -> {
        completed = 0;
        demoDays = 0;
        stars = 0;
        lastClaim = "";
        selected = -1;
        celebrating = false;
      }
      case "reward_done" -> {
        celebrating = false;
        selected = -1;
      }
      case "reward_motion" -> motion = !motion;
      case "reward_size" -> compact = !compact;
      default -> throw new IllegalArgumentException("Unknown reward action: " + action);
    }
    if (!celebrating || !motion) burstStarted = -1;
  }

  public boolean valid() {
    if (completed < 0 || completed > 7 || demoDays < 0 || stars < 0 || lastClaim == null)
      return false;
    try {
      if (!lastClaim.isEmpty()) LocalDate.parse(lastClaim);
    } catch (RuntimeException e) {
      return false;
    }
    return lastClaim.isEmpty() ? completed == 0 && stars == 0 : true;
  }
}
