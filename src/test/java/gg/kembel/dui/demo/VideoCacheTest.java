package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VideoCacheTest {
  @TempDir Path directory;
  private static final Instant SAVED_AT = Instant.parse("2026-09-01T12:00:00Z");
  private static final List<String> IDS = List.of("abcdefghijk", "dQw4w9WgXcQ");

  private static byte[] feedBytes(String title) {
    String entries = "";
    for (String id : IDS) {
      entries +=
          "<entry><yt:videoId>"
              + id
              + "</yt:videoId><title>Demo upload</title>"
              + "<published>2026-09-01T12:00:00Z</published><media:group><media:thumbnail url='"
              + "https://i.ytimg.com/vi/"
              + id
              + "/hqdefault.jpg'/></media:group></entry>";
    }
    return ("<feed xmlns='http://www.w3.org/2005/Atom' "
            + "xmlns:yt='http://www.youtube.com/xml/schemas/2015' "
            + "xmlns:media='http://search.yahoo.com/mrss/'><yt:channelId>"
            + YouTubeFeed.CHANNEL
            + "</yt:channelId><title>"
            + title
            + "</title>"
            + entries
            + "</feed>")
        .getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] imageBytes() throws IOException {
    var image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) image.setRGB(x, y, 0x4488CC);
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    return bytes.toByteArray();
  }

  private VideoCache seed() throws Exception {
    var cache = new VideoCache(directory);
    var saved = cache.saveFeed(feedBytes("Saved demo"), SAVED_AT);
    for (var video : saved.videos()) cache.saveThumbnail(video, imageBytes());
    return cache;
  }

  private static VideoService.Response response(int status, byte[] body) {
    return new VideoService.Response(status, body, "", "");
  }

  @Test
  void restoresFeedAndEveryThumbnailWhenTheLiveFeedReturns404() throws Exception {
    seed();
    var requests = new AtomicInteger();
    try (var service =
        new VideoService(
            directory,
            (uri, headers, limit) -> {
              assertEquals(YouTubeFeed.URL, uri, "Saved thumbnails should need no HTTP request");
              requests.incrementAndGet();
              return response(404, new byte[0]);
            })) {
      var feed = service.refresh(true).get(5, TimeUnit.SECONDS);
      assertEquals(IDS, feed.videos().stream().map(YouTubeFeed.Video::id).toList());
      assertEquals("Saved demo", feed.channel());
      assertEquals(SAVED_AT, feed.checkedAt(), "A failed request must not claim a fresh check");
      for (var video : feed.videos()) {
        assertEquals(0x4488CC, service.cached(video).rgb(0, 0));
        assertEquals(0x4488CC, service.thumbnail(video).get(5, TimeUnit.SECONDS).rgb(7, 7));
      }
      assertEquals(1, requests.get());
      assertTrue(service.error().contains("HTTP 404"));
      assertTrue(service.error().contains("cached videos retained"));
      assertArrayEquals(feedBytes("Saved demo"), Files.readAllBytes(directory.resolve("feed.xml")));
    }
  }

  @Test
  void successfulDownloadsRemainUsableAfterServiceRestartAndNetworkFailure() throws Exception {
    byte[] xml = feedBytes("Live demo"), png = imageBytes();
    try (var service =
        new VideoService(
            directory,
            (uri, headers, limit) -> response(200, uri.equals(YouTubeFeed.URL) ? xml : png))) {
      var feed = service.refresh(true).get(5, TimeUnit.SECONDS);
      service.thumbnail(feed.videos().getFirst()).get(5, TimeUnit.SECONDS);
      assertTrue(service.error().isEmpty());
    }
    try (var restarted =
        new VideoService(
            directory,
            (uri, headers, limit) -> {
              throw new IOException("Network offline");
            })) {
      var saved = restarted.refresh(true).get(5, TimeUnit.SECONDS);
      assertEquals("Live demo", saved.channel());
      assertEquals(0x4488CC, restarted.cached(saved.videos().getFirst()).rgb(0, 0));
      assertTrue(restarted.error().contains("Network offline"));
      assertArrayEquals(xml, Files.readAllBytes(directory.resolve("feed.xml")));
    }
  }

  @Test
  void aBrokenThumbnailDoesNotDiscardTheFeedOrOtherImages() throws Exception {
    seed();
    Files.writeString(directory.resolve(IDS.getFirst() + ".image"), "truncated");
    try (var service =
        new VideoService(directory, (uri, headers, limit) -> response(404, new byte[0]))) {
      var saved = service.refresh(true).get(5, TimeUnit.SECONDS);
      assertEquals(2, saved.videos().size());
      assertNull(service.cached(saved.videos().getFirst()));
      assertThrows(
          ExecutionException.class,
          () -> service.thumbnail(saved.videos().getFirst()).get(5, TimeUnit.SECONDS));
      assertEquals(0x4488CC, service.cached(saved.videos().getLast()).rgb(0, 0));
      assertEquals(
          0x4488CC, service.thumbnail(saved.videos().getLast()).get(5, TimeUnit.SECONDS).rgb(0, 0));
    }
  }

  @Test
  void invalidOrOversizedSavedFeedsAreIgnored() throws Exception {
    for (byte[] bytes :
        List.of(
            "truncated".getBytes(StandardCharsets.UTF_8),
            new byte[VideoCache.FEED_LIMIT + 1],
            new String(feedBytes("Saved demo"), StandardCharsets.UTF_8)
                .replace(YouTubeFeed.CHANNEL, "other-channel")
                .getBytes(StandardCharsets.UTF_8))) {
      Files.write(directory.resolve("feed.xml"), bytes);
      try (var service =
          new VideoService(directory, (uri, headers, limit) -> response(404, new byte[0]))) {
        assertTrue(service.refresh(true).get(5, TimeUnit.SECONDS).videos().isEmpty());
        assertTrue(service.error().contains("no cached videos"));
      }
    }
  }

  @Test
  void invalidDownloadsNeverOverwriteTheLastKnownGoodSnapshot() throws Exception {
    var cache = seed();
    var video = cache.feed().orElseThrow().videos().getFirst();
    assertThrows(IOException.class, () -> cache.saveThumbnail(video, new byte[] {1, 2, 3}));
    assertArrayEquals(imageBytes(), Files.readAllBytes(directory.resolve(video.id() + ".image")));
    try (var service =
        new VideoService(
            directory,
            (uri, headers, limit) -> response(200, new byte[VideoCache.FEED_LIMIT + 1]))) {
      assertEquals("Saved demo", service.refresh(true).get(5, TimeUnit.SECONDS).channel());
      assertArrayEquals(feedBytes("Saved demo"), Files.readAllBytes(directory.resolve("feed.xml")));
    }
    try (var files = Files.list(directory)) {
      assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".pending")));
    }
  }
}
