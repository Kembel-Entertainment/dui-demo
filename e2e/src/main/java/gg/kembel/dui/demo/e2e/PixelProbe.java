package gg.kembel.dui.demo.e2e;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.util.function.IntConsumer;

/** Bounded native readback using the same API as Screenshot, on OpenGL and Vulkan. */
final class PixelProbe implements AutoCloseable {
  private GpuBuffer buffer;
  private boolean pending, closed;
  void sample(RenderTarget target, double[][] points, int size, IntConsumer result) {
    if (pending || closed || target.getColorTexture() == null) return;
    var texture = target.getColorTexture();
    int bytes = Math.multiplyExact(size * size * texture.getFormat().blockSize(), points.length);
    if (buffer == null || buffer.size() < bytes) {
      if (buffer != null) buffer.close();
      buffer = RenderSystem.getDevice().createBuffer(() -> "dui test pixel probe", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bytes);
    }
    pending = true;
    var encoder = RenderSystem.getDevice().createCommandEncoder();
    for (int i = 0; i < points.length; i++) {
      int x = Math.clamp((int)(target.width * points[i][0]), 0, target.width - size);
      int y = Math.clamp((int)(target.height * points[i][1]), 0, target.height - size);
      Runnable ready = i == points.length - 1 ? () -> {
        try (var mapping = buffer.map(true, false)) {
          int signature = 1;
          for (int at = 0; at < bytes; at++) signature = 31 * signature + Byte.toUnsignedInt(mapping.data().get(at));
          if (!closed) result.accept(signature);
        } finally {
          pending = false;
          if (closed) buffer.close();
        }
      } : () -> {};
      encoder.copyTextureToBuffer(texture, buffer, (long)i * size * size * texture.getFormat().blockSize(), ready, 0, x, y, size, size);
    }
  }
  public void close() { closed = true; if (buffer != null && !pending) buffer.close(); }
}
