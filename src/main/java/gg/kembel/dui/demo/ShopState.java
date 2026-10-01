package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.util.*;

/** Disposable, per-session cart. Prices are authoritative integer cents, never client input. */
public final class ShopState {
  public static final String DEMO_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

  public record Product(
      String id, String category, String label, int cents, int was, String model, String badge) {}

  public static final List<Product> PRODUCTS =
      List.of(
          new Product("lantern", "objects", "Camp lantern", 425, 0, "lantern", ""),
          new Product("pot", "objects", "Pocket planter", 750, 0, "decorated_pot", ""),
          new Product("light", "objects", "Copper light", 1200, 1500, "copper_bulb", ""),
          new Product("library", "objects", "Field library", 1800, 2400, "bookshelf", "STAFF PICK"),
          new Product(
              "case", "objects", "Travel case", 2900, 3600, "light_blue_shulker_box", "SET"),
          new Product("garden", "kits", "Garden kit", 950, 0, "moss_block", ""),
          new Product("camp", "kits", "Camp kit", 1650, 2100, "campfire", "STAFF PICK"));
  public final Map<String, Integer> cart = new LinkedHashMap<>();
  public String category = "objects";
  public boolean checkout, compact;
  public int productPage, cartPage;

  public List<Product> products() {
    return PRODUCTS.stream().filter(p -> p.category.equals(category)).toList();
  }

  public static Product product(String id) {
    return PRODUCTS.stream()
        .filter(p -> p.id.equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown product"));
  }

  public int total() {
    return cart.entrySet().stream().mapToInt(e -> product(e.getKey()).cents * e.getValue()).sum();
  }

  public int count() {
    return cart.values().stream().mapToInt(Integer::intValue).sum();
  }

  public int perPage() {
    return compact ? 2 : 3;
  }

  public int cartPages() {
    return Page.count(cart.size(), perPage());
  }

  public static String money(int cents) {
    return String.format(Locale.ROOT, "$%,d.%02d", cents / 100, cents % 100);
  }

  public void apply(String action, String value) {
    switch (action) {
      case "shop_add" -> {
        product(value);
        cart.compute(value, (id, n) -> Math.min(9, n == null ? 1 : n + 1));
      }
      case "shop_remove" -> {
        if (cart.containsKey(value)) cart.compute(value, (id, n) -> n == 1 ? null : n - 1);
      }
      case "shop_category" -> {
        if (!Set.of("objects", "kits").contains(value))
          throw new IllegalArgumentException("Unknown category");
        category = value;
        productPage = 0;
        checkout = false;
      }
      case "shop_product_page" ->
          productPage =
              Math.floorMod(productPage + (value.equals("-1") ? -1 : 1), products().size());
      case "shop_cart_page" ->
          cartPage = Math.floorMod(cartPage + (value.equals("-1") ? -1 : 1), cartPages());
      case "shop_checkout" -> checkout = !cart.isEmpty();
      case "shop_back" -> checkout = false;
      case "shop_size" -> {
        compact = !compact;
        cartPage = 0;
      }
      default -> throw new IllegalArgumentException("Unknown shop action: " + action);
    }
    cartPage = Math.min(cartPage, cartPages() - 1);
  }
}
