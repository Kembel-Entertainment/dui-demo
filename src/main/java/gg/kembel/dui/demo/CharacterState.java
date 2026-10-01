package gg.kembel.dui.demo;

final class CharacterState {
  boolean compact, motion = true, resolving;
  int facing = 1, page;
  EquipmentPort.Slot open;
  String status = "Select a slot. Wear your next adventure.";
  transient EquipmentPort.Snapshot snapshot = EquipmentPort.Snapshot.empty();
  transient gg.kembel.dui.paper.PlayerAppearance resolved;

  java.util.List<EquipmentPort.Entry> candidates() {
    return open == null
        ? java.util.List.of()
        : snapshot.storage().stream()
            .filter(
                e ->
                    open.accepts(e.item())
                        && (open != EquipmentPort.Slot.HAND || e.index() != snapshot.held()))
            .toList();
  }

  int pages() {
    int size = compact ? 2 : 4;
    return Math.max(1, (candidates().size() + size - 1) / size);
  }

  java.util.List<EquipmentPort.Entry> visibleCandidates() {
    var all = candidates();
    int from = Math.min(page * (compact ? 2 : 4), all.size());
    return all.subList(from, Math.min(from + (compact ? 2 : 4), all.size()));
  }
}
