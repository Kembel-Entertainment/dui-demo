package gg.kembel.dui.demo;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Consumer-authored presentation files, shared by pack generation, Paper runtime and Studio. */
final class DemoPresentation {
  private DemoPresentation() {}

  static <T> T read(String path, Class<T> type) {
    try (var reader =
        new InputStreamReader(
            Objects.requireNonNull(DemoPresentation.class.getResourceAsStream(path), path),
            StandardCharsets.UTF_8)) {
      return new Gson().fromJson(reader, type);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid demo presentation: " + path, e);
    }
  }
}
