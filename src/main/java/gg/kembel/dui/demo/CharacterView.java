package gg.kembel.dui.demo;

import java.util.*;

/** Pure bindings. Equipment rarity is a presentation convention, not invented gameplay stats. */
final class CharacterView {
  static Map<String, Object> data(CharacterState c, String viewer) {
    boolean compact = c.compact;
    int width = compact ? 320 : 480, height = compact ? 180 : 315;
    var d = new HashMap<String, Object>();
    d.put("width", width);
    d.put("height", height);
    d.put("compact", compact);
    d.put("wide", !compact);
    d.put("pixel", compact ? 2 : 4);
    d.put("motion", c.motion);
    d.put("motionLabel", c.motion ? "Motion" : "Still");
    d.put("sizeLabel", compact ? "Wide" : "Compact");
    d.put("navX", width - 116);
    d.put("sizeX", width - 58);
    d.put("name", viewer == null ? "Adventurer" : viewer);
    d.put("facing", c.facing);
    d.put("modelX", compact ? 60 : 78);
    d.put("modelY", compact ? 36 : 63);
    d.put("modelW", compact ? 90 : 150);
    d.put("modelH", compact ? 108 : 216);
    d.put("turnY", compact ? 144 : 279);
    d.put("turnX", compact ? 65 : 109);
    d.put("turnRightX", compact ? 111 : 169);
    d.put("statusY", height - 18);
    d.put("status", c.status);
    d.put("statsX", compact ? 202 : 308);
    d.put("statsW", compact ? 106 : 156);
    var slots = new ArrayList<Map<String, Object>>();
    for (var slot : EquipmentPort.Slot.values()) {
      var e = c.snapshot.equipped().get(slot);
      var row = new HashMap<String, Object>();
      row.put("id", slot.name());
      row.put("letter", new String[] {"H", "C", "L", "F", "M", "O"}[slot.ordinal()]);
      row.put("empty", e == null);
      row.put(
          "label",
          compact
              ? new String[] {"Head", "Chest", "Legs", "Feet", "Main", "Off"}[slot.ordinal()]
              : slot.label);
      row.put("x", compact ? (slot.ordinal() < 4 ? 12 : 156) : (slot.ordinal() < 4 ? 24 : 246));
      row.put("y", compact ? (36 + slot.ordinal() % 4 * 27) : (63 + slot.ordinal() % 4 * 45));
      row.put("size", compact ? 27 : 36);
      row.put("iconSize", compact ? 18 : 27);
      row.put("ix", (int) row.get("x") + (compact ? 3 : 4));
      row.put("barY", (int) row.get("y") + (compact ? 18 : 27));
      row.put("iy", (int) row.get("y") + (compact ? 3 : 4));
      row.put("equipped", e != null);
      row.put("color", String.format("#%06X", e == null ? 0x746443 : e.color()));
      row.put("durability", e == null ? 0 : e.durability());
      row.put(
          "tooltip",
          e == null
              ? slot.label + " / Empty / click to equip"
              : e.rarity() + " / " + e.name() + " / " + e.durability() + "% durability");
      slots.add(row);
    }
    d.put("slots", slots);
    var stats = new ArrayList<Map<String, Object>>();
    int i = 0;
    for (String key : List.of("Health", "Armor", "Toughness", "Attack", "Attack speed", "Speed"))
      stats.add(
          Map.of(
              "label",
              key,
              "value",
              c.snapshot.stats().getOrDefault(key, "0"),
              "y",
              (compact ? 45 : 99) + i++ * (compact ? 18 : 27)));
    d.put("stats", stats);
    d.put("popup", c.open != null);
    d.put("pickerX", compact ? 192 : 298);
    d.put("pickerY", compact ? 27 : 63);
    d.put("pickerW", compact ? 116 : 170);
    d.put("pickerH", compact ? 126 : 225);
    d.put("pickerTitle", c.open == null ? "Equipment" : c.open.label.toUpperCase(Locale.ROOT));
    d.put("pageLabel", (c.page + 1) + " / " + c.pages());
    d.put("prevLocked", c.page == 0);
    d.put("nextLocked", c.page + 1 >= c.pages());
    d.put("pagerY", compact ? 99 : 189);
    d.put("unequipY", compact ? 81 : 162);
    var candidates = new ArrayList<Map<String, Object>>();
    var all = c.candidates();
    int pageSize = compact ? 2 : 4, from = c.page * pageSize;
    for (int n = from; n < Math.min(from + pageSize, all.size()); n++) {
      var e = all.get(n);
      var row = new HashMap<String, Object>();
      row.put("index", e.index());
      row.put("name", e.name());
      row.put("rarity", e.rarity());
      row.put("color", String.format("#%06X", e.color()));
      row.put("y", 18 + (n - from) * (compact ? 27 : 36));
      row.put("icon", compact ? 18 : 27);
      row.put("tooltip", e.rarity() + " / " + e.name() + " / Equip now");
      candidates.add(row);
    }
    d.put("candidates", candidates);
    d.put("empty", all.isEmpty());
    return d;
  }
}
