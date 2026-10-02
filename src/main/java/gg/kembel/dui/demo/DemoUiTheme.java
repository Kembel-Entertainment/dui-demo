package gg.kembel.dui.demo;

/** Shared component palette. Native item textures and player skins keep their original colors. */
public enum DemoUiTheme {
  DEFAULT,
  STUDIO,
  STUDIO_DARK;

  public static DemoUiTheme named(String name) {
    return switch (name) {
      case "default" -> DEFAULT;
      case "studio" -> STUDIO;
      case "studio_dark" -> STUDIO_DARK;
      default -> throw new IllegalArgumentException("Unknown UI theme: " + name);
    };
  }

  public int color(int color) {
    if (this == DEFAULT) return color;
    if (this == STUDIO_DARK)
      return switch (color) {
        case 0x16171D -> 0x181C28;
        case 0x22232B, 0x181920 -> 0x222838;
        case 0x202127 -> 0x202432;
        case 0x34343F, 0x41434E, 0x282932, 0x393A46 -> 0x3B4358;
        case 0x9697A5, 0x717482, 0x626575 -> 0xA2ADC5;
        case 0xEAEAF1, 0xFFFFFF -> 0xEDF0F8;
        case 0x58E6DB, 0x376564, 0x42D8B8 -> 0x9CAEFF;
        case 0x273436, 0x24504C, 0x233B3B -> 0x303C62;
        case 0x292B35, 0x33313B -> 0x2D3348;
        case 0x30313D, 0x484955 -> 0x373E52;
        case 0x626372 -> 0x68728D;
        case 0x259F86 -> 0x536ABE;
        case 0xF4D06B -> 0xE8B989;
        case 0x62D394, 0x396762 -> 0x8BCDA8;
        case 0x19352F -> 0x293E38;
        case 0xEF818C, 0xAA626B -> 0xF19AA7;
        case 0x111217 -> 0x141925;
        default -> color;
      };
    return switch (color) {
      case 0x16171D -> 0xF3F0E8; // paper
      case 0x22232B, 0x181920 -> 0xFFFCF6; // raised surface
      case 0x202127 -> 0xECE9E1;
      case 0x34343F, 0x41434E, 0x282932, 0x393A46 -> 0xD0CCC2;
      case 0x9697A5, 0x717482, 0x626575 -> 0x696B77;
      case 0xEAEAF1, 0xFFFFFF -> 0x252938; // ink
      case 0x58E6DB, 0x376564, 0x42D8B8 -> 0x3C50D8;
      case 0x273436, 0x24504C, 0x233B3B -> 0xE0E5FF;
      case 0x292B35, 0x33313B -> 0xE9E6F0;
      case 0x30313D, 0x484955 -> 0xDAD7CE;
      case 0x626372 -> 0xB6B2AA;
      case 0x259F86 -> 0x3C50D8;
      case 0xF4D06B -> 0xA95522;
      case 0x62D394, 0x396762 -> 0x287854;
      case 0x19352F -> 0xE1EEDF;
      case 0xEF818C, 0xAA626B -> 0xB14654;
      case 0x111217 -> 0xE4E0D6;
      default -> color;
    };
  }
}
