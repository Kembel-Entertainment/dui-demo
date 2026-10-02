package gg.kembel.dui.demo;

import static org.junit.jupiter.api.Assertions.*;

import gg.kembel.dui.core.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class VideoTest {
  private static final String FEED =
      """
      <feed xmlns="http://www.w3.org/2005/Atom" xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns:media="http://search.yahoo.com/mrss/">
      <yt:channelId>UC1sELGmy5jp5fQUugmuYlXQ</yt:channelId><title>Example channel</title>
      <entry><yt:videoId>dQw4w9WgXcQ</yt:videoId><title>Older &amp; first</title><published>2026-09-28T15:00:00+00:00</published><media:group><media:thumbnail url="https://i2.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg" /></media:group></entry>
      <entry><yt:videoId>abcdefghijk</yt:videoId><title>Newest upload</title><published>2026-09-29T15:00:00+00:00</published><media:group><media:thumbnail url="https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg" /></media:group></entry>
      </feed>
      """;

  private static YouTubeFeed.Feed feed() throws Exception {
    return YouTubeFeed.parse(FEED.getBytes(StandardCharsets.UTF_8), Instant.now());
  }

  @Test
  void readsAtomMediaRssAndSortsNewestFirst() throws Exception {
    var feed = feed();
    assertEquals("Example channel", feed.channel());
    assertEquals(2, feed.videos().size());
    assertEquals("abcdefghijk", feed.videos().getFirst().id());
    assertEquals("Older & first", feed.videos().getLast().title());
    assertEquals(
        "https://www.youtube.com/watch?v=abcdefghijk", feed.videos().getFirst().watchUrl());
  }

  @Test
  void supportsYouTubesUnprefixedRootChannelId() throws Exception {
    var parsed =
        YouTubeFeed.parse(
            FEED.replace(YouTubeFeed.CHANNEL, YouTubeFeed.CHANNEL.substring(2))
                .getBytes(StandardCharsets.UTF_8),
            Instant.now());
    assertEquals(2, parsed.videos().size());
  }

  @Test
  void rejectsEntitiesOtherChannelsAndThumbnailOrigins() {
    for (String bad :
        List.of(
            FEED.replace(
                "<feed ", "<!DOCTYPE feed [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><feed "),
            FEED.replace(YouTubeFeed.CHANNEL, "different"),
            FEED.replace("https://i2.ytimg.com/", "http://127.0.0.1/"),
            FEED.replace("i2.ytimg.com", "i2.ytimg.com.evil.example")))
      assertThrows(
          Exception.class,
          () -> YouTubeFeed.parse(bad.getBytes(StandardCharsets.UTF_8), Instant.now()));
    assertFalse(
        YouTubeFeed.allowedThumbnail(
            URI.create("https://i.ytimg.com:443/vi/abcdefghijk/hqdefault.jpg"), "abcdefghijk"));
  }

  @Test
  void coverCropsLetterboxingAndPreservesRuntimeRgb() {
    int[] pixels = new int[16 * 12];
    Arrays.fill(pixels, 0x000000);
    for (int y = 2; y < 10; y++)
      for (int x = 0; x < 16; x++) pixels[y * 16 + x] = x < 8 ? 0xFA1204 : 0x05BCDD;
    var image = new RasterImage(16, 12, pixels).cover(16, 8);
    assertEquals(0xFA1204, image.rgb(2, 0));
    assertEquals(0x05BCDD, image.rgb(13, 7));
    pixels[2 * 16] = 0xFFFFFF;
    assertEquals(0xFA1204, image.rgb(0, 0));
  }

  @Test
  void runtimeImagesRespectBoundsPixelBudgetAndTemplateBindings() throws Exception {
    var image = new RasterImage(2, 1, new int[] {0xFF0000, 0x00FF00});
    var c =
        DemoTemplates.parse(
                "<dui-menu width='150' height='36'><dui-layer height='fill'><dui-image id='test'"
                    + " source='{{source}}' x='9' y='9' width='60' height='18' pixel-size='2'"
                    + " action='open'/></dui-layer></dui-menu>",
                DemoTemplates.font(),
                DemoVisualComponents.registry(),
                "demo template")
            .render(Map.of("source", "sample"), Map.of("sample", image));
    assertEquals(1, c.images.size());
    assertEquals(30, c.images.getFirst().raster().width);
    assertEquals(9, c.images.getFirst().raster().height);
    assertEquals("open", c.at(10, 10).action());
    assertThrows(IllegalArgumentException.class, () -> c.image("huge", 0, 0, 150, 36, 0, image));
    var large = new Canvas(480, 360, DemoTemplates.environment(DemoTemplates.font()));
    assertThrows(
        IllegalArgumentException.class, () -> large.image("huge", 0, 0, 480, 360, 1, image));
  }

  @Test
  void galleryPaginationAndLoadingStatesRenderBothTemplates() throws Exception {
    var feed = feed();
    var image = new RasterImage(1, 1, new int[] {0xFF0077});
    MenuTemplate template;
    try (var in = getClass().getResourceAsStream("/ui/videos.html")) {
      template =
          DemoTemplates.parse(
              new String(in.readAllBytes(), StandardCharsets.UTF_8),
              DemoTemplates.font(),
              DemoVisualComponents.registry(),
              "demo template");
    }
    for (boolean compact : List.of(false, true))
      for (boolean loading : List.of(false, true)) {
        var model = VideoView.model(feed, 0, compact, loading, "", Map.of("abcdefghijk", image));
        var canvas = template.render(model.data(), model.images());
        assertEquals(compact ? 1 : 2, model.visible().size());
        assertEquals(1, canvas.images.size());
        assertEquals(compact ? 144 : 270, canvas.height);
        assertTrue(canvas.items.isEmpty());
        if (loading) assertTrue(canvas.hits.stream().allMatch(h -> h.action().isEmpty()));
      }
    var model = VideoView.model(feed, 99, true, false, "", Map.of());
    assertEquals(1, model.page());
    assertEquals("dQw4w9WgXcQ", model.visible().getFirst().id());
  }
}
