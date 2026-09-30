package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Shared asynchronous feed and thumbnail cache. No HTTP or image decoding on Paper's main thread.
 */
public final class VideoService implements VideoProvider {
  private final ExecutorService worker =
      Executors.newFixedThreadPool(
          3, Thread.ofPlatform().daemon(true).name("dui-demo-videos-", 0).factory());
  private final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(8))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();
  private final Path cache;
  private volatile YouTubeFeed.Feed feed =
      new YouTubeFeed.Feed("Minecraft", List.of(), Instant.EPOCH);
  private volatile String error = "";
  private CompletableFuture<YouTubeFeed.Feed> refresh;
  private Instant requested = Instant.EPOCH;
  private String etag = "", modified = "";

  private record Image(Instant at, CompletableFuture<RasterImage> value) {}

  private final Map<URI, Image> images = new ConcurrentHashMap<>();

  public VideoService(Path cache) {
    this.cache = cache;
  }

  public YouTubeFeed.Feed feed() {
    return feed;
  }

  public String error() {
    return error;
  }

  public RasterImage cached(YouTubeFeed.Video video) {
    var image = images.get(video.thumbnail());
    return image == null || image.value.isCompletedExceptionally()
        ? null
        : image.value.getNow(null);
  }

  public synchronized CompletableFuture<YouTubeFeed.Feed> refresh(boolean force) {
    var now = Instant.now();
    if (refresh != null && !refresh.isDone()) return refresh;
    if (Duration.between(requested, now).toSeconds() < 10
        || !force
            && !feed.videos().isEmpty()
            && Duration.between(feed.checkedAt(), now).toSeconds() < 300)
      return CompletableFuture.completedFuture(feed);
    requested = now;
    refresh =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                var builder =
                    HttpRequest.newBuilder(YouTubeFeed.URL)
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "dui-demo/0.1 (https://kembel.gg)")
                        .GET();
                if (!etag.isBlank()) builder.header("If-None-Match", etag);
                if (!modified.isBlank()) builder.header("If-Modified-Since", modified);
                var response =
                    http.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
                try (var body = response.body()) {
                  if (response.statusCode() == 304 && !feed.videos().isEmpty())
                    feed = new YouTubeFeed.Feed(feed.channel(), feed.videos(), Instant.now());
                  else {
                    if (response.statusCode() != 200)
                      throw new IOException("YouTube feed HTTP " + response.statusCode());
                    byte[] bytes = bounded(body, 512_000);
                    var next = YouTubeFeed.parse(bytes, Instant.now());
                    Files.createDirectories(cache);
                    Files.write(cache.resolve("feed.xml"), bytes);
                    feed = next;
                    etag = response.headers().firstValue("ETag").orElse("");
                    modified = response.headers().firstValue("Last-Modified").orElse("");
                    var active = new HashSet<URI>();
                    for (var video : feed.videos()) active.add(video.thumbnail());
                    images.keySet().retainAll(active);
                  }
                }
                error = "";
                return feed;
              } catch (Exception e) {
                error = "Feed unavailable; cached videos retained. " + e.getMessage();
                return feed;
              }
            },
            worker);
    return refresh;
  }

  public CompletableFuture<RasterImage> thumbnail(YouTubeFeed.Video video) {
    if (!YouTubeFeed.allowedThumbnail(video.thumbnail(), video.id()))
      return CompletableFuture.failedFuture(new IOException("Invalid thumbnail origin"));
    var now = Instant.now();
    var entry =
        images.compute(
            video.thumbnail(),
            (uri, old) -> {
              if (old != null
                  && !old.value.isCompletedExceptionally()
                  && Duration.between(old.at, now).toSeconds() < 600) return old;
              return new Image(
                  now,
                  CompletableFuture.supplyAsync(
                      () -> {
                        try {
                          var request =
                              HttpRequest.newBuilder(uri)
                                  .timeout(Duration.ofSeconds(15))
                                  .GET()
                                  .build();
                          var response =
                              http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                          try (var body = response.body()) {
                            if (response.statusCode() != 200)
                              throw new IOException("Thumbnail HTTP " + response.statusCode());
                            byte[] bytes = bounded(body, 1_500_000);
                            var image = RasterImage.decode(bytes);
                            Files.createDirectories(cache);
                            Files.write(cache.resolve(video.id() + ".image"), bytes);
                            return image;
                          }
                        } catch (Exception e) {
                          throw new CompletionException(e);
                        }
                      },
                      worker));
            });
    return entry.value;
  }

  private static byte[] bounded(InputStream stream, int limit) throws IOException {
    byte[] bytes = stream.readNBytes(limit + 1);
    if (bytes.length > limit) throw new IOException("Response too large");
    return bytes;
  }

  @Override
  public void close() {
    worker.shutdownNow();
    http.close();
  }
}
