package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ShopTest {
  @Test
  void quantitiesTotalsAndEmptyCheckoutAreAuthoritative() {
    var s = new ShopState();
    s.apply("shop_checkout", "");
    assertFalse(s.checkout);
    for (int i = 0; i < 20; i++) s.apply("shop_add", "lantern");
    assertEquals(9, s.count());
    assertEquals(3825, s.total());
    s.apply("shop_checkout", "");
    assertTrue(s.checkout);
    for (int i = 0; i < 10; i++) s.apply("shop_remove", "lantern");
    assertEquals(0, s.total());
    assertThrows(IllegalArgumentException.class, () -> s.apply("shop_add", "missing"));
  }

  @Test
  void allCategoriesCartPagesAndCheckoutFitTheirTemplates() {
    for (boolean compact : List.of(false, true))
      for (String category : List.of("objects", "kits")) {
        var s = new ShopState();
        s.compact = compact;
        s.category = category;
        for (var p : ShopState.PRODUCTS) s.cart.put(p.id(), 1);
        for (int product = 0; product < s.products().size(); product++) {
          s.productPage = product;
          for (int page = 0; page < s.cartPages(); page++) {
            s.cartPage = page;
            var c = ShopView.render(s, "Example").canvas();
            for (var hit : c.hits)
              assertSame(hit, c.at(hit.x() + hit.width() / 2, hit.y() + hit.height() / 2));
          }
        }
        s.checkout = true;
        var c = ShopView.render(s, "Example").canvas();
        assertEquals(1, c.images.size());
        assertTrue(c.items.isEmpty());
      }
  }

  @Test
  void runtimeQrDecodesExactlyToTheDemoUrl() throws Exception {
    for (int cells : new int[] {52, 54}) {
      var raster = QrCode.raster(ShopState.DEMO_URL, cells);
      int scale = 4, width = cells * scale;
      int[] pixels = new int[width * width];
      for (int y = 0; y < width; y++)
        for (int x = 0; x < width; x++) pixels[y * width + x] = raster.rgb(x / scale, y / scale);
      var result =
          new MultiFormatReader()
              .decode(
                  new BinaryBitmap(
                      new HybridBinarizer(new RGBLuminanceSource(width, width, pixels))));
      assertEquals(ShopState.DEMO_URL, result.getText());
    }
  }
}
