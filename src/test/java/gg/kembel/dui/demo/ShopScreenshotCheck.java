package gg.kembel.dui.demo;

import com.google.gson.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Decode the actual Minecraft framebuffer, including its font rendering and GUI scaling. */
public final class ShopScreenshotCheck {
  public static void main(String[] args) throws Exception {
    Path root = Path.of(args[0]);
    var checked = new ArrayList<Map<String, Object>>();
    for (String name : List.of("checkout", "compact-checkout", "auto-checkout")) {
      var meta =
          JsonParser.parseString(Files.readString(root.resolve("screenshots/" + name + ".json")))
              .getAsJsonObject();
      var qr = meta.getAsJsonObject("layout").getAsJsonObject("qr");
      double scale = meta.get("scale").getAsDouble();
      int x =
          (int) Math.round((meta.get("canvasX").getAsDouble() + qr.get("x").getAsInt()) * scale);
      int
          y =
              (int)
                  Math.round((meta.get("canvasY").getAsDouble() + qr.get("y").getAsInt()) * scale),
          size = (int) (qr.get("size").getAsInt() * scale);
      var shot = ImageIO.read(root.resolve("screenshots/" + name + ".png").toFile());
      int[] pixels = shot.getRGB(x, y, size, size, null, 0, size);
      var decoded =
          new MultiFormatReader()
              .decode(
                  new BinaryBitmap(
                      new HybridBinarizer(new RGBLuminanceSource(size, size, pixels))));
      if (!decoded.getText().equals(ShopState.DEMO_URL))
        throw new IllegalStateException("QR target mismatch: " + name);
      checked.add(
          Map.of(
              "screenshot", name, "scale", scale, "decoded", decoded.getText(), "qrPixels", size));
    }
    Files.writeString(
        root.resolve("qr-verification.json"),
        new GsonBuilder().setPrettyPrinting().create().toJson(checked) + "\n");
    System.out.println(
        "PASS: actual in-game QR screenshots decoded to the requested YouTube URL: "
            + checked.size());
  }
}
