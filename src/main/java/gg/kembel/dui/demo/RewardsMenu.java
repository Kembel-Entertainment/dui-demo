package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.time.LocalDate;
import java.util.*;

/** Rewards application: pure projection, typed actions and scoped effects. */
final class RewardsMenu extends DemoMenu {
  @Override
  String id() {
    return "rewards";
  }

  @Override
  List<String> templates() {
    return List.of("rewards");
  }

  @Override
  Set<String> aliases() {
    return Set.of("dailyrewards");
  }

  RewardsMenu(DemoServices services) {
    super(services);
    on("reward_close", s -> {});
    on(
        "reward_select",
        Integer::parseInt,
        (s, day) -> {
          if (day < 0 || day >= RewardState.WEEK.size()) {
            reject("reward_select");
            return;
          }
          apply(s, "reward_select", day.toString());
        });
    for (var id :
        List.of(
            "reward_claim",
            "reward_next",
            "reward_reset",
            "reward_done",
            "reward_motion",
            "reward_size")) on(id, s -> apply(s, id, ""));
  }

  private void apply(DemoSession s, String id, String value) {
    long before = s.rewards.stars;
    s.rewards.apply(id, value, s.date);
    if (id.equals("reward_claim") && s.rewards.stars > before && s.rewards.motion)
      s.rewards.burstStarted = s.tick;
    services.save("rewards", s.rewards);
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.rewards.compact = compact;
    s.rewards.selected = -1;
    s.rewards.celebrating = false;
    s.rewards.burstStarted = -1;
  }

  @Override
  void advance(DemoSession s) {
    s.rewards.sync(s.date);
    long age = s.tick - s.rewards.burstStarted;
    if (age < 0 || age >= ItemTransport.BURST_TICKS) s.rewards.burstStarted = -1;
  }

  @Override
  Map<String, org.bukkit.inventory.ItemStack> captureItems(DemoSession s) {
    return services.viewerItems(p -> RewardItems.stacks(s.rewards, s.date));
  }

  @Override
  MenuView project(DemoSession s) {
    return view(
        "rewards",
        RewardView.data(s.rewards, s.date),
        Map.of(),
        s,
        Map.of(),
        DialogOptions.notice("dui / Daily rewards", "Close rewards", "reward_close"));
  }

  @Override
  void presented(DemoSession s, Canvas c) {
    if (c.confetti != null)
      later(
          ItemTransport.BURST_TICKS + 40 - (s.tick - s.rewards.burstStarted),
          () -> s.rewards.burstStarted = -1);
  }

  @Override
  void closed(DemoSession s) {
    s.rewards.burstStarted = -1;
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    var result = new HashMap<String, Object>();
    result.put("section", "rewards");
    result.put("state", s.rewards);
    result.put("celebrating", s.rewards.celebrating);
    result.put("selected", s.rewards.selection(s.date));
    result.put("today", s.rewards.today(s.date).toString());
    result.put("confetti", c.confetti);
    return result;
  }

  @Override
  void validate(boolean compact) {
    var reward = new RewardState();
    reward.compact = compact;
    services.template("rewards").render(RewardView.data(reward, LocalDate.of(2026, 9, 30)));
  }
}
