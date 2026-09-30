package gg.kembel.dui.demo;

import gg.kembel.dui.core.RasterImage;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Deterministic own fixtures; no remote thumbnails or network dependency in E2E. */
final class FixtureVideos implements VideoProvider {
  private final YouTubeFeed.Feed feed;
  private final Map<String, RasterImage> images = new HashMap<>();

  FixtureVideos() {
    var videos = new ArrayList<YouTubeFeed.Video>();
    for (int n = 0; n < 6; n++) {
      String id = "DuiDemo000" + n;
      videos.add(
          new YouTubeFeed.Video(
              id,
              "dui demo / Colour study " + (n + 1),
              Instant.parse("2026-09-30T00:00:00Z").minusSeconds(n * 86400),
              URI.create("https://i.ytimg.com/vi/" + id + "/hqdefault.jpg")));
      int[] pixels = new int[320 * 180];
      for (int y = 0; y < 180; y++)
        for (int x = 0; x < 320; x++)
          pixels[y * 320 + x] =
              ((x * 255 / 319) << 16) | ((y * 255 / 179) << 8) | ((x + y + n * 37) % 256);
      images.put(id, new RasterImage(320, 180, pixels));
    }
    feed = new YouTubeFeed.Feed("dui demo", videos, Instant.parse("2026-09-30T00:00:00Z"));
  }

  public CompletableFuture<YouTubeFeed.Feed> refresh(boolean force) {
    return CompletableFuture.completedFuture(feed);
  }

  public CompletableFuture<RasterImage> thumbnail(YouTubeFeed.Video video) {
    return CompletableFuture.completedFuture(cached(video));
  }

  public YouTubeFeed.Feed feed() {
    return feed;
  }

  public RasterImage cached(YouTubeFeed.Video video) {
    return images.get(video.id());
  }

  public String error() {
    return "";
  }

  public void close() {}
}
