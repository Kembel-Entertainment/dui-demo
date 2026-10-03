package gg.kembel.dui.demo.media;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Local files only; no caller-supplied paths or shell commands. */
public final class MediaFiles {
  private final Path directory;
  public MediaFiles(Path directory) throws IOException {
    this.directory = directory.toAbsolutePath().normalize();
    Files.createDirectories(this.directory);
  }
  public List<Path> list() throws IOException {
    try (var files = Files.list(directory)) {
      return files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
          .filter(p -> p.getFileName().toString().matches("[A-Za-z0-9_.-]+\\.(mp4|mkv|webm|mov)"))
          .sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
    }
  }
  public Path find(String alias) throws IOException {
    if (!alias.matches("[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Use a local media filename");
    var matching = list().stream().filter(p -> p.getFileName().toString().equals(alias)
        || stem(p).equals(alias)).toList();
    if (matching.size() != 1) throw new IllegalArgumentException(
        matching.isEmpty() ? "Unknown local video" : "Use the full filename, including its extension");
    return matching.getFirst();
  }
  public static String stem(Path p) {
    String name = p.getFileName().toString();
    return name.substring(0, name.lastIndexOf('.'));
  }
}
