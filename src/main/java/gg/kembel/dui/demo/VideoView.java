package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.time.*;
import java.util.*;

/** Feed presentation data; component geometry lives in videos.html. */
public final class VideoView {
  public record Model(
      Map<String, Object> data,
      Map<String, RasterImage> images,
      List<YouTubeFeed.Video> visible,
      int page,
      int pages) {}

  private VideoView() {}

  public static Model model(
      YouTubeFeed.Feed feed,
      int requestedPage,
      boolean compact,
      boolean loading,
      String error,
      Map<String, RasterImage> thumbnails) {
    var pagination =
        CollectionView.of(feed.videos(), requestedPage, compact ? 1 : 2, YouTubeFeed.Video::id)
            .page();
    int pages = pagination.pages(), page = pagination.index();
    var visible = pagination.items();
    var cards = new ArrayList<Map<String, Object>>();
    var images = new HashMap<String, RasterImage>();
    for (var video : visible) {
      var image = thumbnails.get(video.id());
      if (image != null) images.put(video.id(), image);
      cards.add(
          Map.of(
              "id",
              video.id(),
              "title",
              video.title(),
              "published",
              video.published().atOffset(ZoneOffset.UTC).toLocalDate().toString(),
              "hasThumbnail",
              image != null,
              "noThumbnail",
              image == null,
              "imageStatus",
              loading ? "Loading thumbnail..." : "Thumbnail unavailable"));
    }
    var data = new HashMap<String, Object>();
    data.put("compact", compact);
    data.put("spacious", !compact);
    data.put("channel", feed.channel());
    data.put("videos", cards);
    data.put("empty", cards.isEmpty());
    data.put("emptyTitle", loading ? "Loading latest uploads..." : "No videos available");
    data.put("emptyDetail", loading ? "Fetching the YouTube feed." : "Use Refresh to try again.");
    data.put("pageLabel", (page + 1) + " / " + pages + "  |  " + feed.videos().size() + " uploads");
    data.put("previousLocked", page == 0 || loading);
    data.put("nextLocked", page + 1 >= pages || loading);
    data.put("loading", loading);
    data.put(
        "status",
        !error.isBlank()
            ? "Feed unavailable / cached results"
            : loading
                ? "Loading..."
                : feed.checkedAt().equals(Instant.EPOCH)
                    ? "Not connected"
                    : "Checked "
                        + feed.checkedAt().atOffset(ZoneOffset.UTC).toLocalTime().withNano(0)
                        + " UTC");
    data.put("sizeLabel", compact ? "Wide" : "Compact");
    return new Model(Map.copyOf(data), Map.copyOf(images), List.copyOf(visible), page, pages);
  }
}
