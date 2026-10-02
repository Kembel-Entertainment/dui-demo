package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ShopView {
  public record View(Canvas canvas, QrCode.Region qr) {}

  private ShopView() {}

  private static final BoundedCache<String, RasterImage> QRS =
      new BoundedCache<>(8, 32768, i -> (long) i.width * i.height);

  private static Map<String, Object> product(ShopState state, ShopState.Product p) {
    return Map.of(
        "id",
        p.id(),
        "label",
        p.label(),
        "price",
        ShopState.money(p.cents()),
        "full",
        state.cart.getOrDefault(p.id(), 0) >= 9);
  }

  public static Map<String, Object> data(ShopState state, String player) {
    var products = state.products().stream().map(p -> product(state, p)).toList();
    var pagination =
        CollectionView.of(
                new ArrayList<>(state.cart.entrySet()),
                state.cartPage,
                state.perPage(),
                Map.Entry::getKey)
            .page();
    var cart =
        pagination.items().stream()
            .map(
                e ->
                    Map.<String, Object>of(
                        "id",
                        e.getKey(),
                        "label",
                        e.getValue() + "x " + ShopState.product(e.getKey()).label(),
                        "price",
                        ShopState.money(ShopState.product(e.getKey()).cents() * e.getValue())))
            .toList();
    var d = new HashMap<String, Object>();
    d.put("width", state.compact ? 300 : 480);
    d.put("height", state.compact ? (state.checkout ? 153 : 144) : (state.checkout ? 252 : 279));
    d.put("compact", state.compact);
    d.put("spacious", !state.compact);
    d.put("checkout", state.checkout);
    d.put("browsing", !state.checkout);
    d.put("player", player);
    d.put("topProducts", products.subList(0, Math.min(3, products.size())));
    d.put("bottomProducts", products.subList(Math.min(3, products.size()), products.size()));
    d.put("selectedProducts", List.of(products.get(state.productPage)));
    d.put("productPage", (state.productPage + 1) + " / " + products.size());
    d.put("cart", cart);
    d.put(
        "cartPage",
        pagination.pages() > 1 ? (pagination.index() + 1) + "/" + pagination.pages() : "");
    d.put("cartPagination", pagination.pages() > 1);
    d.put("empty", state.cart.isEmpty());
    d.put("total", ShopState.money(state.total()));
    return d;
  }

  public static Map<String, RasterImage> images(ShopState state) {
    return state.checkout
        ? Map.of(
            "checkout_qr",
            QRS.computeIfAbsent(
                ShopState.DEMO_URL + ":" + state.compact,
                key -> QrCode.raster(ShopState.DEMO_URL, state.compact ? 54 : 52)))
        : Map.of();
  }

  public static View render(ShopState state, String player) {
    try (var in = ShopView.class.getResourceAsStream("/ui/shop.html")) {
      var c =
          DemoTemplates.parse(
                  new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8),
                  DemoTemplates.font(),
                  DemoVisualComponents.registry(),
                  "demo template")
              .render(data(state, player), images(state));
      QrCode.Region qr = null;
      if (state.checkout) {
        var i = c.images.getFirst();
        qr = QrCode.region(ShopState.DEMO_URL, i.x(), i.y(), i.width(), i.pixelSize());
      }
      return new View(c, qr);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
