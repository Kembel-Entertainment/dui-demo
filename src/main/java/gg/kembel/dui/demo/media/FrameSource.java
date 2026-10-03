package gg.kembel.dui.demo.media;

import gg.kembel.dui.core.video.*;
import java.util.function.Consumer;

/** Consumer-owned pixel producer. DUI has no dependency on codecs or the source of the pixels. */
public interface FrameSource extends AutoCloseable {
  record Statistics(long frames, double seconds, boolean paused, boolean ended,
      double framePreparationMillis, String error, double speed) {}
  void start(Consumer<VideoFrame> frames, Consumer<Throwable> failure, Runnable ended);
  void pause(boolean paused);
  boolean paused();
  Statistics statistics();
  default void input(SurfaceInput input) {}
  @Override void close();
}
