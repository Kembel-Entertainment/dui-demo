package gg.kembel.dui.demo;

import gg.kembel.dui.core.RasterImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Asynchronous feed and thumbnail service with a persistent last-known-good snapshot. */
public final class VideoService implements VideoProvider {
  record Response(int status, byte[] body, String etag, String modified) {}

  @FunctionalInterface
  interface Fetcher {
    Response fetch(URI uri, Map<String, String> headers, int limit) throws Exception;
  }

  private final ExecutorService worker =
      Executors.newFixedThreadPool(
          3, Thread.ofPlatform().daemon(true).name("dui-demo-videos-", 0).factory());
  private final VideoCache cache;
  private final Fetcher fetcher;
  private final Runnable closeTransport;
  private final CompletableFuture<Void> restored;
  private volatile YouTubeFeed.Feed feed =
      new YouTubeFeed.Feed("Minecraft", List.of(), Instant.EPOCH);
  private volatile String error = "";
  private CompletableFuture<YouTubeFeed.Feed> refresh;
  private Instant requested = Instant.EPOCH;
  private String etag = "", modified = "";

  private record Image(Instant at, CompletableFuture<RasterImage> value, RasterImage fallback) {}

  private final Map<URI, Image> images = new ConcurrentHashMap<>();

  public VideoService(Path cache) {
    this(
        cache,
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build());
  }

  private VideoService(Path cache, HttpClient client) {
    this(cache, (uri, headers, limit) -> fetch(client, uri, headers, limit), client::close);
  }

  VideoService(Path cache, Fetcher fetcher) {
    this(cache, fetcher, () -> {});
  }

  private VideoService(Path directory, Fetcher fetcher, Runnable closeTransport) {
    this.cache = new VideoCache(directory);
    this.fetcher = fetcher;
    this.closeTransport = closeTransport;
    restored = CompletableFuture.runAsync(this::restore, worker);
  }

  private void restore() {
    cache
        .feed()
        .ifPresent(
            saved -> {
              feed = saved;
              error = "Using saved videos; not checked online yet.";
              for (var video : saved.videos()) {
                cache
                    .thumbnail(video)
                    .ifPresent(
                        image ->
                            images.put(
                                video.thumbnail(),
                                new Image(
                                    Instant.now(),
                                    CompletableFuture.completedFuture(image),
                                    image)));
              }
            });
  }

  public YouTubeFeed.Feed feed() {
    return feed;
  }

  public String error() {
    return error;
  }

  public RasterImage cached(YouTubeFeed.Video video) {
    return available(images.get(video.thumbnail()));
  }

  private static RasterImage available(Image image) {
    if (image == null) return null;
    return image.value.isCompletedExceptionally()
        ? image.fallback
        : image.value.getNow(image.fallback);
  }

  public synchronized CompletableFuture<YouTubeFeed.Feed> refresh(boolean force) {
    var now = Instant.now();
    if (refresh != null && !refresh.isDone()) return refresh;
    if (Duration.between(requested, now).toSeconds() < 10
        || !force
            && !feed.videos().isEmpty()
            && Duration.between(feed.checkedAt(), now).toSeconds() < 300)
      return restored.thenApply(ignored -> feed);
    requested = now;
    refresh =
        restored.thenApplyAsync(
            ignored -> {
              try {
                var headers = new HashMap<String, String>();
                headers.put("User-Agent", "dui-demo/0.1 (https://kembel.gg)");
                if (!etag.isBlank()) headers.put("If-None-Match", etag);
                if (!modified.isBlank()) headers.put("If-Modified-Since", modified);
                var response = fetcher.fetch(YouTubeFeed.URL, headers, VideoCache.FEED_LIMIT);
                if (response.status == 304 && !feed.videos().isEmpty()) {
                  Instant checkedAt = Instant.now();
                  cache.checked(checkedAt);
                  feed = new YouTubeFeed.Feed(feed.channel(), feed.videos(), checkedAt);
                } else {
                  if (response.status != 200)
                    throw new IOException("YouTube feed HTTP " + response.status);
                  feed = cache.saveFeed(response.body, Instant.now());
                  etag = response.etag;
                  modified = response.modified;
                  var active = new HashSet<URI>();
                  for (var video : feed.videos()) active.add(video.thumbnail());
                  images.keySet().retainAll(active);
                }
                error = "";
              } catch (Exception e) {
                error =
                    "Feed unavailable; "
                        + (feed.videos().isEmpty()
                            ? "no cached videos. "
                            : "cached videos retained. ")
                        + e.getMessage();
              }
              return feed;
            },
            worker);
    return refresh;
  }

  public CompletableFuture<RasterImage> thumbnail(YouTubeFeed.Video video) {
    if (!YouTubeFeed.allowedThumbnail(video.thumbnail(), video.id()))
      return CompletableFuture.failedFuture(new IOException("Invalid thumbnail origin"));
    // A first request waits for disk restoration rather than racing a duplicate download.
    return restored.thenCompose(ignored -> thumbnailReady(video));
  }

  private CompletableFuture<RasterImage> thumbnailReady(YouTubeFeed.Video video) {
    var now = Instant.now();
    var entry =
        images.compute(
            video.thumbnail(),
            (uri, old) -> {
              if (old != null
                  && !old.value.isCompletedExceptionally()
                  && Duration.between(old.at, now).toSeconds() < 600) return old;
              RasterImage fallback = available(old);
              return new Image(
                  now,
                  CompletableFuture.supplyAsync(
                      () -> {
                        try {
                          var response = fetcher.fetch(uri, Map.of(), VideoCache.IMAGE_LIMIT);
                          if (response.status != 200)
                            throw new IOException("Thumbnail HTTP " + response.status);
                          return cache.saveThumbnail(video, response.body);
                        } catch (Exception e) {
                          var saved =
                              fallback != null ? Optional.of(fallback) : cache.thumbnail(video);
                          if (saved.isPresent()) return saved.get();
                          throw new CompletionException(e);
                        }
                      },
                      worker),
                  fallback);
            });
    return entry.value;
  }

  private static Response fetch(HttpClient http, URI uri, Map<String, String> headers, int limit)
      throws Exception {
    var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).GET();
    headers.forEach(request::header);
    var response = http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
    try (var body = response.body()) {
      byte[] bytes = response.statusCode() == 200 ? body.readNBytes(limit + 1) : new byte[0];
      if (bytes.length > limit) throw new IOException("Response too large");
      return new Response(
          response.statusCode(),
          bytes,
          response.headers().firstValue("ETag").orElse(""),
          response.headers().firstValue("Last-Modified").orElse(""));
    }
  }

  @Override
  public void close() {
    worker.shutdownNow();
    closeTransport.run();
  }
}
