package gg.kembel.dui.demo;

import gg.kembel.dui.core.RasterImage;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;

/** Validated local snapshots. Downloaded media stays in the ignored runtime directory. */
final class VideoCache {
  static final int FEED_LIMIT = 512_000, IMAGE_LIMIT = 1_500_000;
  private final Path directory;

  VideoCache(Path directory) {
    this.directory = directory;
  }

  Optional<YouTubeFeed.Feed> feed() {
    Path file = directory.resolve("feed.xml");
    try {
      return Optional.of(
          YouTubeFeed.parse(read(file, FEED_LIMIT), Files.getLastModifiedTime(file).toInstant()));
    } catch (Exception ignored) {
      // A missing, truncated or invalid snapshot must not prevent the live request.
      return Optional.empty();
    }
  }

  Optional<RasterImage> thumbnail(YouTubeFeed.Video video) {
    try {
      return Optional.of(RasterImage.decode(read(imagePath(video), IMAGE_LIMIT), 0x16171D));
    } catch (Exception ignored) {
      // Recover individual images independently; keep the rest of the feed usable.
      return Optional.empty();
    }
  }

  YouTubeFeed.Feed saveFeed(byte[] bytes, Instant checkedAt) throws Exception {
    var parsed = YouTubeFeed.parse(bytes, checkedAt);
    write(directory.resolve("feed.xml"), bytes);
    checked(checkedAt);
    return parsed;
  }

  RasterImage saveThumbnail(YouTubeFeed.Video video, byte[] bytes) throws IOException {
    if (bytes.length > IMAGE_LIMIT) throw new IOException("Thumbnail too large");
    var image = RasterImage.decode(bytes, 0x16171D);
    write(imagePath(video), bytes);
    return image;
  }

  void checked(Instant at) throws IOException {
    Path file = directory.resolve("feed.xml");
    if (Files.exists(file)) Files.setLastModifiedTime(file, FileTime.from(at));
  }

  private Path imagePath(YouTubeFeed.Video video) throws IOException {
    if (!video.id().matches("[A-Za-z0-9_-]{11}")
        || !YouTubeFeed.allowedThumbnail(video.thumbnail(), video.id()))
      throw new IOException("Invalid cached thumbnail key");
    return directory.resolve(video.id() + ".image");
  }

  private static byte[] read(Path file, int limit) throws IOException {
    try (var in = Files.newInputStream(file)) {
      byte[] bytes = in.readNBytes(limit + 1);
      if (bytes.length > limit) throw new IOException("Cached response too large");
      return bytes;
    }
  }

  private static void write(Path file, byte[] bytes) throws IOException {
    Files.createDirectories(file.getParent());
    Path temporary =
        Files.createTempFile(file.getParent(), file.getFileName().toString(), ".pending");
    try {
      Files.write(temporary, bytes);
      try {
        Files.move(
            temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
