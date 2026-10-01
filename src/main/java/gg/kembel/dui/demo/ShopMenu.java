package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.net.URI;
import java.util.*;

/** Shop application: pure projection, typed actions and scoped effects. */
final class ShopMenu extends DemoMenu {
  @Override
  String id() {
    return "shop";
  }

  @Override
  List<String> templates() {
    return List.of("shop");
  }

  @Override
  Set<String> aliases() {
    return Set.of("uishop");
  }

  ShopMenu(DemoServices services) {
    super(services);
    on("shop_close", s -> {});
    for (var id : List.of("shop_add", "shop_remove"))
      on(id, ShopState::product, (s, product) -> s.shop.apply(id, product.id()));
    on(
        "shop_category",
        v -> choice(v, "objects", "kits"),
        (s, v) -> s.shop.apply("shop_category", v));
    for (var id : List.of("shop_product_page", "shop_cart_page"))
      on(id, DemoMenu::direction, (s, v) -> s.shop.apply(id, v.toString()));
    for (var id : List.of("shop_checkout", "shop_back", "shop_size"))
      on(id, s -> s.shop.apply(id, ""));
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.shop.compact = compact;
    s.shop.checkout = false;
    s.shop.cartPage = 0;
  }

  @Override
  Map<String, org.bukkit.inventory.ItemStack> captureItems(DemoSession s) {
    return services.viewerItems(ShopItems::stacks);
  }

  @Override
  MenuView project(DemoSession s) {
    var images = ShopView.images(s.shop);
    var links =
        s.shop.checkout
            ? Map.of("shop_link", URI.create(ShopState.DEMO_URL))
            : Map.<String, URI>of();
    return view(
        "shop",
        ShopView.data(s.shop, s.viewerName),
        images,
        s,
        links,
        DialogOptions.notice("dui / Demo store", "Close store", "shop_close"));
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    var extra = new HashMap<String, Object>();
    extra.put("section", "shop");
    extra.put("state", s.shop);
    extra.put("total", s.shop.total());
    if (s.shop.checkout) {
      var i = c.images.getFirst();
      extra.put("qr", QrCode.region(ShopState.DEMO_URL, i.x(), i.y(), i.width(), i.pixelSize()));
    }
    return extra;
  }

  @Override
  void validate(boolean compact) {
    var shop = new ShopState();
    shop.compact = compact;
    shop.cart.put("lantern", 1);
    services.template("shop").render(ShopView.data(shop, "Example"));
    shop.checkout = true;
    services.template("shop").render(ShopView.data(shop, "Example"), ShopView.images(shop));
  }
}
