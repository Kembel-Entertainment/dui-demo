package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public final class RewardView {
  private RewardView() {}

  public static Map<String, Object> data(RewardState s, LocalDate date) {
    s.sync(date);
    int day = s.selection(date);
    var r = RewardState.WEEK.get(day);
    boolean ready = !s.claimedToday(date) && day == s.current(date);
    var d = new HashMap<String, Object>();
    d.put("width", s.compact ? 300 : 480);
    d.put("height", s.compact ? 144 : 306);
    d.put("compact", s.compact);
    d.put("spacious", !s.compact);
    d.put("day", day + 1);
    d.put("stars", s.stars);
    d.put("completed", s.completed);
    d.put("light", String.format("#%06X", r.light()));
    d.put("burstTick", s.celebrating && s.motion ? s.burstStarted : -1);
    d.put("name", s.celebrating ? "YOU GOT " + r.stars() + " STARS!" : r.name());
    d.put("giftLabel", s.celebrating ? "HAPPY LITTLE WIN!" : "A GIFT FOR YOU");
    d.put(
        "description",
        s.celebrating
            ? "Small wins deserve big confetti."
            : day < s.completed
                ? "Collected. Come back tomorrow!"
                : day > s.current(date)
                    ? "A future surprise. Keep your streak!"
                    : "+" + r.stars() + " stars / Ready when you are.");
    d.put(
        "detail",
        s.celebrating
            ? "Your demo star jar just got brighter."
            : "+" + r.stars() + " stars for your demo collection.");
    d.put("claimLocked", !s.celebrating && !ready);
    d.put("claimAction", s.celebrating ? "reward_done" : ready ? "reward_claim" : "");
    d.put("claimColor", s.celebrating ? "#B39BFF" : ready ? "#8BEBB0" : "#D5CCD9");
    d.put(
        "claimLabel",
        s.celebrating
            ? "Sweet! Keep going >"
            : ready
                ? "CLAIM YOUR GIFT >"
                : day < s.completed ? "Collected / See you tomorrow" : "Locked / Day " + (day + 1));
    d.put(
        "claimTooltip",
        ready
            ? "Collect today's demo stars"
            : s.celebrating
                ? "Return to your reward calendar"
                : "Use Next day to fast-forward this demo");
    d.put("motionLabel", s.motion ? "Motion: on" : "Motion: off");
    var days = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < 7; i++) {
      var reward = RewardState.WEEK.get(i);
      days.add(
          Map.of(
              "index",
              i,
              "day",
              i + 1,
              "stars",
              reward.stars(),
              "fill",
              String.format("#%06X", i == day ? reward.color() : reward.light()),
              "symbol",
              i < s.completed ? "+" : i == s.current(date) && !s.claimedToday(date) ? "*" : "-",
              "tooltip",
              "Day " + (i + 1) + " / " + reward.name() + " / +" + reward.stars() + " stars"));
    }
    d.put("days", days);
    return d;
  }

  public static Canvas render(RewardState s, LocalDate date) {
    try (var in = RewardView.class.getResourceAsStream("/ui/rewards.html")) {
      return MenuTemplate.parse(
              new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8))
          .render(data(s, date));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
