package gg.kembel.dui.demo;

import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import gg.kembel.dui.core.*;
import java.util.Map;

/** Runtime QR matrix drawn with existing rectangle glyphs; no texture or pack rebuild. */
public final class QrCode {
  public record Region(int x, int y, int size, int modules, int moduleSize, String value) {}

  private QrCode() {}

  private static com.google.zxing.common.BitMatrix matrix(String value) {
    if (value == null || value.isBlank() || value.length() > 2048)
      throw new IllegalArgumentException("Invalid QR content");
    try {
      return new QRCodeWriter()
          .encode(
              value,
              BarcodeFormat.QR_CODE,
              0,
              0,
              Map.of(
                  EncodeHintType.CHARACTER_SET,
                  "UTF-8",
                  EncodeHintType.ERROR_CORRECTION,
                  ErrorCorrectionLevel.M,
                  EncodeHintType.MARGIN,
                  4));
    } catch (WriterException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static RasterImage raster(String value, int cells) {
    var m = matrix(value);
    int scale = cells / m.getWidth();
    if (scale < 1) throw new IllegalArgumentException("QR does not fit");
    int offset = (cells - m.getWidth() * scale) / 2;
    int[] rgb = new int[cells * cells];
    java.util.Arrays.fill(rgb, 0xFFFFFF);
    for (int y = 0; y < m.getHeight(); y++)
      for (int x = 0; x < m.getWidth(); x++)
        if (m.get(x, y))
          for (int yy = 0; yy < scale; yy++)
            for (int xx = 0; xx < scale; xx++)
              rgb[(offset + y * scale + yy) * cells + offset + x * scale + xx] = 0;
    return new RasterImage(cells, cells, rgb);
  }

  public static Region region(String value, int x, int y, int available, int pixelSize) {
    int modules = matrix(value).getWidth(),
        cells = available / pixelSize,
        scale = cells / modules,
        offset = (cells - modules * scale) / 2;
    return new Region(
        x + offset * pixelSize,
        y + offset * pixelSize,
        modules * scale * pixelSize,
        modules,
        scale * pixelSize,
        value);
  }
}
