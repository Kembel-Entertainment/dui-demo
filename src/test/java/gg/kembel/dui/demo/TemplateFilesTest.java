package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemplateFilesTest {
  @TempDir Path temporary;

  @Test
  void savedProjectFileIsReloadedInsteadOfStaleRuntimeCopy() throws Exception {
    Path project = Files.createDirectories(temporary.resolve("project/src/main/resources/ui"));
    Path data = temporary.resolve("server/plugins/dui-demo");
    Path runtime = Files.createDirectories(data.resolve("ui"));
    Files.writeString(runtime.resolve("example.html"), source("Old deployed copy"));
    Files.writeString(project.resolve("example.html"), source("Before save"));
    var files = new TemplateFiles(data, project.toString());
    var previous = DemoTemplates.parse(files.read("example"));
    Files.writeString(project.resolve("example.html"), source("Saved in Studio"));
    var reloaded = DemoTemplates.parse(files.read("example"));
    assertEquals("Saved in Studio", reloaded.render(Map.of()).paints.getFirst().text());
    assertEquals("Before save", previous.render(Map.of()).paints.getFirst().text());
    Files.writeString(project.resolve("example.html"), "<dui-menu><unknown/></dui-menu>");
    assertThrows(Exception.class, () -> DemoTemplates.parse(files.read("example")));
    assertEquals("Saved in Studio", reloaded.render(Map.of()).paints.getFirst().text());
    assertEquals(source("Old deployed copy"), Files.readString(runtime.resolve("example.html")));
  }

  @Test
  void productionDefaultsKeepRuntimeEditsAndNeverWriteToSources() throws Exception {
    Path data = temporary.resolve("plugin");
    var defaults = new TemplateFiles(data, "");
    defaults.installDefaults(List.of("example"), ignored -> bytes("bundled"));
    Files.writeString(defaults.directory().resolve("example.html"), "user edit");
    defaults.installDefaults(List.of("example"), ignored -> bytes("replacement"));
    assertEquals("user edit", defaults.read("example"));
    Path project = Files.createDirectories(temporary.resolve("project"));
    var sources = new TemplateFiles(data, project.toString());
    sources.installDefaults(List.of("example"), ignored -> fail("Must not request defaults"));
    assertFalse(Files.exists(project.resolve("example.html")));
    assertThrows(IOException.class, () -> sources.read("example"));
  }

  @Test
  void relativeSourcePathsResolveAgainstDataFolderAndInvalidRootsFailClearly() throws Exception {
    Path data = Files.createDirectories(temporary.resolve("plugin"));
    Path sources = Files.createDirectories(data.resolve("source"));
    var files = new TemplateFiles(data, "source");
    assertEquals(sources.toRealPath(), files.directory());
    assertThrows(IOException.class, () -> new TemplateFiles(data, "missing"));
    assertThrows(IllegalArgumentException.class, () -> files.read("../other"));
    Files.writeString(temporary.resolve("outside.html"), "private");
    Files.createSymbolicLink(sources.resolve("escape.html"), temporary.resolve("outside.html"));
    assertThrows(IOException.class, () -> files.read("escape"));
    // A relative plugin data path must not be appended to itself.
    Path relativeData = Path.of("").toAbsolutePath().relativize(data);
    var relativeDefaults = new TemplateFiles(relativeData, "");
    assertEquals(data.resolve("ui").toRealPath(), relativeDefaults.directory());
  }

  private static String source(String label) {
    return "<dui-menu background='none' width='240' height='18'><dui-text label='"
        + label + "'/></dui-menu>";
  }

  private static ByteArrayInputStream bytes(String value) {
    return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
  }
}
