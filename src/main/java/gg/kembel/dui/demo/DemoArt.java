package gg.kembel.dui.demo;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public final class DemoArt {
  public static void main(String[] args) throws Exception {
    Path output = Path.of(args[0]);
    Files.createDirectories(output);
    var bytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(bytes)) {
      RewardArt.write(zip);
      AdventArt.write(zip);
      WarpArt.write(zip);
      for (String id : RewardArt.MODELS) {
        var e = new ZipEntry("assets/dui_demo/items/" + id + ".json");
        e.setTime(0);
        zip.putNextEntry(e);
        zip.write(
            RewardArt.definition(id).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
      }
    }
    try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
        Path path = output.resolve(entry.getName());
        Files.createDirectories(path.getParent());
        Files.write(path, zip.readAllBytes());
      }
    }
  }
}
