package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.util.*;
import org.bukkit.inventory.ItemStack;

/** Inventory controller; HTML owns layout, the library owns the GPU model. */
final class CharacterMenu extends DemoMenu {
  CharacterMenu(DemoServices services) {
    super(services);
    on("character_close", s -> {});
    on(
        "character_turn",
        DemoMenu::direction,
        (s, d) -> s.character.facing = Math.floorMod(s.character.facing + d, 8));
    on("character_motion", s -> s.character.motion = !s.character.motion);
    on(
        "character_size",
        s -> {
          s.character.compact = !s.character.compact;
          s.character.open = null;
        });
    on(
        "character_slot",
        EquipmentPort.Slot::valueOf,
        (s, slot) -> {
          s.character.open = s.character.open == slot ? null : slot;
          s.character.page = 0;
        });
    on("character_dismiss", s -> s.character.open = null);
    on(
        "character_page",
        DemoMenu::direction,
        (s, d) -> {
          if (s.character.open != null)
            s.character.page = Math.max(0, Math.min(s.character.pages() - 1, s.character.page + d));
        });
    on(
        "character_equip",
        Integer::parseInt,
        (s, index) -> {
          var c = s.character;
          if (c.open == null) return;
          try {
            services.equipment().equip(c.snapshot, c.open, index);
            c.status = "Equipped. Your inventory is up to date.";
            c.open = null;
          } catch (IllegalArgumentException e) {
            c.status = e.getMessage();
            services.message(c.status);
          }
        });
    on(
        "character_unequip",
        s -> {
          var c = s.character;
          if (c.open == null) return;
          try {
            services.equipment().unequip(c.snapshot, c.open);
            c.status = "Unequipped. Item returned to your inventory.";
            c.open = null;
          } catch (IllegalArgumentException e) {
            c.status = e.getMessage();
            services.message(c.status);
          }
        });
  }

  String id() {
    return "character";
  }

  List<String> templates() {
    return List.of("character");
  }

  void prepare(DemoSession s, boolean compact) {
    s.character.compact = compact;
    s.character.open = null;
    s.character.resolving = false;
    s.character.resolved = null;
  }

  void advance(DemoSession s) {
    var c = s.character;
    c.snapshot = services.equipment().capture();
    c.page = Math.min(c.page, c.pages() - 1);
  }

  Map<String, ItemStack> captureItems(DemoSession s) {
    var out = new HashMap<String, ItemStack>();
    s.character
        .snapshot
        .equipped()
        .forEach((slot, item) -> out.put("slot_" + slot.name(), item.item()));
    for (var item : s.character.visibleCandidates())
      out.put("candidate_" + item.index(), item.item());
    return out;
  }

  MenuView project(DemoSession s) {
    var c = s.character;
    var captured = c.snapshot.appearance();
    var appearance =
        c.resolved == null
            ? captured
            : new PlayerAppearance(c.resolved.profile(), captured.armor());
    return MenuView.of(
        services.template("character"),
        new ViewModel(
            CharacterView.data(c, s.viewerName),
            Map.of("character_background", CharacterArt.background(c.compact)),
            s.items,
            Map.of(),
            Map.of("viewer", appearance)),
        DialogOptions.notice("dui / Aster character", "Close character sheet", "character_close"));
  }

  void validate(boolean compact) {
    var c = new CharacterState();
    c.compact = compact;
    services
        .template("character")
        .render(
            CharacterView.data(c, "Adventurer"),
            Map.of("character_background", CharacterArt.background(compact)));
  }

  void presented(DemoSession s, Canvas canvas) {
    var c = s.character;
    String fingerprint = c.snapshot.fingerprint();
    var stats = c.snapshot.stats();
    services
        .viewTasks()
        .later(
            "character_poll",
            10,
            () -> {
              var next = services.equipment().capture();
              if (!next.fingerprint().equals(fingerprint) || !next.stats().equals(stats))
                services.refresh();
              else presented(s, canvas);
            });
    if (!c.resolving) {
      c.resolving = true;
      services
          .tasks()
          .latest(
              "character_profile",
              services.resolveAppearance(c.snapshot.appearance()),
              (result, error) -> {
                if (error == null && result != null && !result.fallback()) {
                  c.resolved = result;
                  services.refresh();
                }
              });
    }
  }

  Map<String, Object> report(DemoSession s, Canvas canvas) {
    var c = s.character;
    return Map.of(
        "section",
        "character",
        "status",
        c.status,
        "facing",
        c.facing,
        "motion",
        c.motion,
        "popup",
        c.open == null ? "" : c.open.name(),
        "page",
        c.page,
        "equipped",
        c.snapshot.equipped().keySet(),
        "fingerprint",
        c.snapshot.fingerprint(),
        "skinFallback",
        c.resolved == null ? c.snapshot.appearance().fallback() : c.resolved.fallback());
  }
}
