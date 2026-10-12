package pl.mcmmorpg.mapa;

import java.util.List;

final class PanelMath {
  enum Filter {
    ALL("Wszystko", "mapa:ikona/pin_biala"),
    PETS("Chowańce", "mapa:ikona/chw_wilkor"),
    CITY("Miasto", "mapa:ikona/ender_eye"),
    OWN("Moje", "mapa:ikona/pin_czerwona");

    final String label;
    final String model;

    Filter(String label, String model) { this.label = label; this.model = model; }

    Filter next() { return values()[(ordinal() + 1) % values().length]; }

    boolean accepts(String type) {
      return switch (this) {
        case ALL -> true;
        case PETS -> type.equals("strefa") || type.equals("legendarny");
        case CITY -> type.equals("miasto") || type.equals("start");
        case OWN -> type.equals("wlasny");
      };
    }
  }

  enum Action { NONE, ADD, FILTER, NAVIGATION, MINIMAP, FULL, CLOSE }

  record Page<T>(List<T> items, int index, int total) {
    boolean previous() { return index > 0; }
    boolean next() { return index + 1 < total; }
    T marker(int slot) {
      int offset = previous() ? 1 : 0;
      int position = slot - offset;
      return position >= 0 && position < items.size() ? items.get(position) : null;
    }
  }

  static <T> Page<T> page(List<T> values, int requested) {
    int total = 1;
    int remaining = values.size();
    if (remaining > 27) {
      remaining -= 26;
      total++;
      while (remaining > 26) { remaining -= 25; total++; }
    }
    int index = Math.max(0, Math.min(requested, total - 1));
    int start = index == 0 ? 0 : 26 + (index - 1) * 25;
    int capacity = total == 1 ? 27 : index == 0 ? 26 : index == total - 1 ? 26 : 25;
    return new Page<>(values.subList(start, Math.min(values.size(), start + capacity)), index, total);
  }

  static Action action(int rawSlot) {
    return switch (rawSlot) {
      case 81 -> Action.ADD;
      case 82 -> Action.FILTER;
      case 83 -> Action.NAVIGATION;
      case 84 -> Action.MINIMAP;
      case 88 -> Action.FULL;
      case 89 -> Action.CLOSE;
      default -> Action.NONE;
    };
  }

  private PanelMath() {}
}
