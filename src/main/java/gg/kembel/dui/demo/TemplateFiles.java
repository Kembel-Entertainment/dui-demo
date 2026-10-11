package gg.kembel.dui.demo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

/** Consumer-owned template storage: deployed copies by default, project sources in development. */
public final class TemplateFiles {
  private final Path directory;
  private final boolean external;

  public TemplateFiles(Path dataDirectory, String sourceDirectory) throws IOException {
    external = sourceDirectory != null && !sourceDirectory.isBlank();
    Path selected = external ? Path.of(sourceDirectory) : dataDirectory.toAbsolutePath().resolve("ui");
    if (!selected.isAbsolute()) selected = dataDirectory.toAbsolutePath().resolve(selected);
    if (!external) Files.createDirectories(selected);
    if (!Files.isDirectory(selected))
      throw new IOException("Template directory does not exist: " + selected);
    directory = selected.toRealPath();
  }

  public Path directory() {
    return directory;
  }

  /** Never installs bundled examples over project sources or existing runtime edits. */
  public void installDefaults(Iterable<String> names, Function<String, InputStream> resources)
      throws IOException {
    if (external) return;
    for (String name : names) {
      Path file = path(name);
      if (!Files.exists(file))
        try (var input = Objects.requireNonNull(resources.apply("ui/" + name + ".html"))) {
          Files.copy(input, file);
        }
    }
  }

  /** Reads fresh bytes for every reload; no source/runtime-copy fallback. */
  public String read(String name) throws IOException {
    Path file = path(name).toRealPath();
    if (!file.startsWith(directory)) throw new IOException("Template escapes directory: " + name);
    return Files.readString(file);
  }

  private Path path(String name) {
    if (name == null || !name.matches("[A-Za-z0-9_-]+"))
      throw new IllegalArgumentException("Invalid template name: " + name);
    return directory.resolve(name + ".html");
  }
}
