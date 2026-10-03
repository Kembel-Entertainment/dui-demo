package gg.kembel.dui.demo.browser;

import com.sun.net.httpserver.HttpServer;
import gg.kembel.dui.core.*;
import gg.kembel.dui.core.video.*;
import gg.kembel.dui.demo.DemoTemplates;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BrowserTest {
  @Test void mouseUsesViewportPixelsAndClampsBothAxesWithoutChangingHeldInput() {
    var controls = new BrowserControls(320, 180);
    assertEquals(new BrowserControls.Cursor(200, 70), controls.mouse(10, -5, 4));
    assertEquals(new BrowserControls.Cursor(200, 70), controls.advance(.05));
    assertEquals(new BrowserControls.Cursor(319, 0), controls.mouse(1000, -1000, 4));
    assertEquals(new BrowserControls.Cursor(0, 179), controls.mouse(-1000, 1000, 4));
    assertThrows(IllegalArgumentException.class, () -> controls.mouse(Double.NaN, 0, 4));
    assertThrows(IllegalArgumentException.class, () -> controls.mouse(0, 0, 0));
  }
  @Test void cursorUsesHeldInputClampsAndClicksOnlyOnThePressEdge() {
    var controls = new BrowserControls(320, 180);
    var input = new SurfaceInput(SurfaceInput.Type.STATE, false, false, false, true, true, false, false, -1, 0);
    assertTrue(controls.accept(input)); assertFalse(controls.accept(input));
    for (int i = 0; i < 100; i++) controls.advance(.05);
    assertEquals(319, controls.cursor().x()); assertEquals(90, controls.cursor().y());
    controls.release(); var at = controls.cursor(); controls.advance(.05); assertEquals(at, controls.cursor());
    assertTrue(controls.accept(input));
    assertThrows(IllegalArgumentException.class, () -> controls.advance(Double.NaN));
    assertEquals("https://www.youtube.com/", BrowserControls.address("https://www.youtube.com/"));
    assertEquals("https://www.youtube.com/results?search_query=pixel+art", BrowserControls.address("pixel art"));
    assertEquals("https://example.com", BrowserControls.address("example.com"));
    for (String url : List.of("file:///etc/passwd", "javascript:alert(1)", "data:text/html,x", "https://user:pass@example.com", ""))
      assertThrows(IllegalArgumentException.class, () -> BrowserControls.address(url));
  }
  @Test void framedPixelsRetainDimensionsChannelOrderAndRejectTruncatedOrUnboundedMessages() throws Exception {
    var image = new BufferedImage(3, 1, BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, 0xff0000); image.setRGB(1, 0, 0x00ff00); image.setRGB(2, 0, 0x0000ff);
    var rgb = BrowserFrameSource.frame(image, PixelFormat.RGB888, 7, null);
    assertEquals(0xff0000, rgb.pixel(0, 0)); assertEquals(0x00ff00, rgb.pixel(1, 0)); assertEquals(0x0000ff, rgb.pixel(2, 0));
    var bgr = BrowserFrameSource.frame(image, PixelFormat.BGR555, 8, null);
    assertEquals(31, bgr.pixel(0, 0)); assertEquals(31 << 5, bgr.pixel(1, 0)); assertEquals(31 << 10, bgr.pixel(2, 0));
    var bytes = new ByteArrayOutputStream(); var out = new DataOutputStream(bytes); out.writeInt(3); out.write(new byte[] {1, 2, 3});
    assertArrayEquals(new byte[] {1, 2, 3}, BrowserFrameSource.readPayload(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    assertThrows(EOFException.class, () -> BrowserFrameSource.readPayload(new DataInputStream(new ByteArrayInputStream(new byte[] {0, 0, 0, 2, 1}))));
    assertThrows(IOException.class, () -> BrowserFrameSource.readPayload(new DataInputStream(new ByteArrayInputStream(new byte[] {127, 0, 0, 0}))));
  }
  @Test void browserUsesPublicTemplatesWithoutPackOrLibraryChanges() throws Exception {
    var template = VideoSurfaceTemplate.parse(Files.readString(Path.of("src/main/resources/ui/browser-screen.html")),
        DemoTemplates.environment(DemoTemplates.font()), ComponentRegistry.EMPTY, "browser-screen");
    var view = template.render(BrowserDemo.data(640, 360, 10, 31, 16_777_216, PixelFormat.BGR555, "A page", "https://example.com", "LIVE"));
    assertEquals(640, view.specification().width()); assertEquals(PixelFormat.BGR555, view.specification().format());
    assertFalse(view.hud().surfaces().isEmpty());
    var menu = DemoTemplates.parse(Files.readString(Path.of("src/main/resources/ui/browser.html")));
    var canvas = menu.render(Map.of("title", "A page", "status", "Ready", "running", true, "zoom", "150%"));
    assertTrue(canvas.hits.stream().anyMatch(h -> h.action().equals("back")));
    assertTrue(canvas.hits.stream().anyMatch(h -> h.action().equals("resume")));
    assertTrue(canvas.hits.stream().anyMatch(h -> h.action().equals("zoom_in")));
    assertTrue(canvas.hits.stream().anyMatch(h -> h.action().equals("zoom_out")));
    assertThrows(IllegalArgumentException.class, () -> BrowserControls.zoom(49));
    assertThrows(IllegalArgumentException.class, () -> BrowserControls.zoom(251));
    var hd = template.render(BrowserDemo.data(1280, 720, 12, 121, 67_108_864, PixelFormat.BGR555, "HD", "", "LIVE"));
    assertEquals(1280, hd.specification().width()); assertEquals(12, hd.specification().sustainableFps());
  }
  private static void await(BooleanSupplier ready, AtomicReference<Throwable> failure) throws Exception {
    long deadline = System.nanoTime() + 15_000_000_000L;
    while (!ready.getAsBoolean()) {
      if (failure.get() != null) throw new AssertionError(failure.get());
      if (System.nanoTime() > deadline) fail("Browser action timed out");
      Thread.sleep(20);
    }
  }
  @Test void realHeadlessBrowserStreamsAndSupportsPointerTypingScrollingHistoryAndCleanup() throws Exception {
    String runtimeValue = System.getenv("DUI_BROWSER_RUNTIME");
    Assumptions.assumeTrue(runtimeValue != null, "Explicitly install a browser runtime and set DUI_BROWSER_RUNTIME");
    var http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    http.createContext("/", exchange -> {
      String name = exchange.getRequestURI().getPath().equals("/second") ? "Second" : "First";
      byte[] html = ("<!doctype html><title>" + name + "</title><style>body{margin:0;height:2000px;background:linear-gradient(120deg,#af2070,#225ad0)}button{position:absolute;left:110px;top:75px;width:100px;height:40px}input{position:absolute;left:10px;top:10px}</style>"
          + "<form onsubmit=\"event.preventDefault();document.title='Typed:'+document.querySelector('input').value\"><input><button type=button onclick=\"document.title='Clicked:'+Math.round(devicePixelRatio*100)\">Click</button></form>"
          + "<script>onscroll=()=>document.title='Scrolled:'+Math.round(scrollY)</script>").getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8"); exchange.sendResponseHeaders(200, html.length);
      try (var output = exchange.getResponseBody()) { output.write(html); }
    });
    http.start(); String url = "http://127.0.0.1:" + http.getAddress().getPort();
    var spec = new VideoSurfaceSpec(320, 180, PixelFormat.BGR555, 10, VideoSurfaceSpec.Viewport.FULL,
        new VideoSurfaceSpec.Budget(16, 16_777_216), 0, false);
    var failure = new AtomicReference<Throwable>(); var picture = new AtomicReference<VideoFrame>();
    var source = new BrowserFrameSource(System.getenv().getOrDefault("DUI_BROWSER_NODE", "node"),
        Path.of("src/main/resources/browser/bridge.mjs"), Path.of(runtimeValue), url, spec);
    try (source) {
      source.start(picture::set, failure::set, () -> {});
      await(() -> picture.get() != null && source.state().title().equals("First"), failure);
      source.click(); await(() -> source.state().title().equals("Clicked:100"), failure);
      source.command("zoom", Map.of("percent", 150));
      await(() -> source.state().zoomPercent() == 150, failure);
      source.pointer(new BrowserControls.Cursor(225, 142)); source.click();
      await(() -> source.state().title().equals("Clicked:150"), failure);
      assertEquals(320, picture.get().width()); assertEquals(180, picture.get().height());
      for (int percent : List.of(75, 125, 175, 200, 100)) {
        long previous = source.statistics().frames();
        source.command("zoom", Map.of("percent", percent));
        await(() -> source.state().zoomPercent() == percent && source.statistics().frames() > previous + 1, failure);
        assertEquals(320, picture.get().width()); assertEquals(180, picture.get().height());
      }
      source.pointer(new BrowserControls.Cursor(30, 20)); source.click();
      source.command("type", Map.of("text", "hello", "enter", true));
      await(() -> source.state().title().equals("Typed:hello"), failure);
      source.command("scroll", Map.of("dy", 400));
      await(() -> source.state().title().startsWith("Scrolled:"), failure);
      source.command("navigate", Map.of("url", url + "/second"));
      await(() -> source.state().title().equals("Second"), failure);
      source.command("back", Map.of()); await(() -> source.state().url().equals(url + "/"), failure);
      source.command("forward", Map.of()); await(() -> source.state().url().endsWith("/second"), failure);
      source.command("zoom", Map.of("percent", 125)); await(() -> source.state().zoomPercent() == 125, failure);
      source.command("reload", Map.of()); await(() -> source.state().lastAction().equals("reload"), failure);
      assertEquals(125, source.state().zoomPercent());
      source.pause(true); await(() -> source.state().lastAction().equals("pause"), failure);
      long frames = source.statistics().frames(); Thread.sleep(350); assertEquals(frames, source.statistics().frames());
      source.pause(false); await(() -> source.statistics().frames() > frames, failure);
      assertEquals(320, picture.get().width()); assertEquals(PixelFormat.BGR555, picture.get().format());
      assertNull(failure.get());
    } catch (Throwable e) {
      System.err.println("Browser state at failure: " + source.state() + " / " + source.statistics());
      throw e;
    } finally { http.stop(0); source.close(); }
    await(() -> !source.alive(), new AtomicReference<>());
  }
}
