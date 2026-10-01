package gg.kembel.dui.demo;

import gg.kembel.dui.core.RasterImage;
import java.util.concurrent.CompletableFuture;

interface VideoProvider extends AutoCloseable {
  default gg.kembel.dui.core.ResourceHandle<RasterImage> resource(YouTubeFeed.Video video) {
    return new gg.kembel.dui.core.ResourceHandle<>(thumbnail(video), cached(video));
  }

  CompletableFuture<YouTubeFeed.Feed> refresh(boolean force);

  CompletableFuture<RasterImage> thumbnail(YouTubeFeed.Video video);

  YouTubeFeed.Feed feed();

  RasterImage cached(YouTubeFeed.Video video);

  String error();

  void close();
}
