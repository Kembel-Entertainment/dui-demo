package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import java.net.URI;
import java.util.*;

/** Videos application: pure projection, typed actions and scoped effects. */
final class VideosMenu extends DemoMenu {
  @Override
  String id() {
    return "videos";
  }

  @Override
  List<String> templates() {
    return List.of("videos");
  }

  @Override
  Set<String> aliases() {
    return Set.of("uivideos");
  }

  VideosMenu(DemoServices services) {
    super(services);
    on("video_close", s -> {});
    on(
        "video_previous",
        s -> {
          s.videoPage = Math.max(0, s.videoPage - 1);
          request(s, false);
        });
    on(
        "video_next",
        s -> {
          s.videoPage++;
          request(s, false);
        });
    on(
        "video_size",
        s -> {
          s.videoCompact = !s.videoCompact;
          s.videoPage = 0;
          request(s, false);
        });
    on("video_refresh", s -> request(s, true));
  }

  private void request(DemoSession s, boolean force) {
    s.videoFetchRequested = true;
    s.videoForce = force;
    s.videoLoading = true;
  }

  @Override
  void prepare(DemoSession s, boolean compact) {
    s.videoCompact = compact;
    s.videoPage = 0;
    request(s, s.videoForce);
  }

  @Override
  void advance(DemoSession s) {
    var provider = services.videos();
    var images = new HashMap<String, RasterImage>();
    s.videoFeed = provider.feed();
    s.videoError = provider.error();
    for (var video : s.videoFeed.videos()) {
      var image = provider.cached(video);
      if (image != null) images.put(video.id(), image);
    }
    s.videoModel =
        VideoView.model(
            s.videoFeed, s.videoPage, s.videoCompact, s.videoLoading, s.videoError, images);
    s.videoPage = s.videoModel.page();
  }

  @Override
  MenuView project(DemoSession s) {
    var model = s.videoModel;
    var links = new HashMap<String, URI>();
    for (var video : model.visible()) {
      links.put("watch_" + video.id(), URI.create(video.watchUrl()));
      links.put("thumb_" + video.id(), URI.create(video.watchUrl()));
    }
    return view(
        "videos",
        model.data(),
        model.images(),
        s,
        links,
        DialogOptions.notice("dui / YouTube uploads", "Close videos", "video_close"));
  }

  @Override
  void presented(DemoSession s, Canvas c) {
    if (!s.videoFetchRequested) return;
    s.videoFetchRequested = false;
    int page = s.videoPage;
    boolean compact = s.videoCompact;
    var provider = services.videos();
    var pending =
        provider
            .refresh(s.videoForce)
            .thenCompose(
                feed ->
                    java.util.concurrent.CompletableFuture.allOf(
                        VideoView.model(feed, page, compact, true, "", Map.of()).visible().stream()
                            .map(
                                video ->
                                    provider
                                        .resource(video)
                                        .completion()
                                        .handle((image, error) -> null))
                            .toArray(java.util.concurrent.CompletableFuture[]::new)));
    services
        .tasks()
        .latest(
            "video-fetch",
            pending,
            (value, error) -> {
              s.videoLoading = false;
              services.refresh();
            });
  }

  @Override
  void closed(DemoSession s) {
    s.videoFetchRequested = false;
    s.videoLoading = false;
  }

  @Override
  Map<String, Object> report(DemoSession s, Canvas c) {
    var model = s.videoModel;
    return Map.of(
        "section",
        "videos",
        "page",
        model.page(),
        "pages",
        model.pages(),
        "loading",
        s.videoLoading,
        "error",
        s.videoError,
        "checkedAt",
        s.videoFeed.checkedAt().toString(),
        "videos",
        model.visible().stream()
            .map(
                v ->
                    Map.of(
                        "id",
                        v.id(),
                        "title",
                        v.title(),
                        "published",
                        v.published().toString(),
                        "thumbnail",
                        v.thumbnail().toString(),
                        "url",
                        v.watchUrl()))
            .toList());
  }

  @Override
  void validate(boolean compact) {
    services
        .template("videos")
        .render(
            VideoView.model(new FixtureVideos().feed(), 0, compact, false, "", Map.of()).data());
  }
}
