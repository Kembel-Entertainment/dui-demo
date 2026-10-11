package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** The same actual vanilla-client acceptance runs against both server hosts. */
final class PlatformClient {
  private int ticks, changed, stage;
  private String inventory;
  private final Path out = Paths.output();
  private final PixelProbe pixels = new PixelProbe();
  private long firstPixels = Long.MIN_VALUE;
  private int videoChanges;
  private String packSha1;
  void initialize() {
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> pixels.close());
  }
  private void step() { stage++; changed = ticks; System.out.println("PLATFORM_TEST_STAGE " + stage); }
  private JsonObject data() throws Exception {
    return JsonParser.parseString(Files.readString(Paths.plugin().resolve("layouts/PlatformTest.json"))).getAsJsonObject();
  }
  private String inventory(Minecraft mc) {
    var result = new StringBuilder();
    for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) result.append(mc.player.getInventory().getItem(i)).append(';');
    return result.toString();
  }
  private void tick(Minecraft mc) {
    ticks++;
    try {
      mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
      if (ticks > 2400) throw new IllegalStateException("Platform acceptance timeout at " + stage);
      if (mc.gui.overlay() != null || ticks - changed < 10) return;
      if (stage == 0 && ticks > 80) {
        mc.options.guiScale().set(Paths.referenceScale(mc)); mc.getWindow().setWindowed(1280, 900); mc.resizeGui();
        org.lwjgl.sdl.SDLVideo.SDL_HideWindow(mc.getWindow().handle());
        var server = new ServerData("Portable fixture", Paths.server(), ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
        step(); return;
      }
      if (stage == 1 && mc.player != null) {
        if (System.getProperty("dui.e2e.platform", "paper").equals("paper") && !(mc.gui.screen() instanceof DialogScreen<?>)) return;
        if (System.getProperty("dui.e2e.platform", "paper").equals("minestom") &&
            (mc.player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) != 1 ||
             java.util.stream.Stream.of(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                 net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET)
                 .anyMatch(slot -> mc.player.getItemBySlot(slot).isEmpty()))) return;
        inventory = inventory(mc); mc.player.connection.sendCommand("platform"); step(); return;
      }
      if (stage == 2 && mc.gui.screen() instanceof DialogScreen<?> && Files.exists(Paths.plugin().resolve("layouts/PlatformTest.json"))) {
        var d = data(); if (!d.has("backend")) return;
        packSha1 = d.get("packSha1").getAsString();
        RealClientHarness.snapshot(mc, out, "portable", d); RealClientHarness.hit(mc, d, "increment"); step(); return;
      }
      if (stage == 3 && data().get("count").getAsInt() == 1) {
        var edit = RealClientHarness.widgets(mc.gui.screen()).stream().filter(w -> w instanceof EditBox).map(w -> (EditBox)w).findFirst().orElseThrow();
        edit.setValue("Both platforms");
        var checkbox = RealClientHarness.widgets(mc.gui.screen()).stream().filter(w -> w instanceof Checkbox)
            .map(w -> (Checkbox)w).findFirst().orElseThrow();
        if (!checkbox.selected()) throw new IllegalStateException("Initial boolean input missing");
        RealClientHarness.clickAt(mc, checkbox.getX() + 5., checkbox.getY() + checkbox.getHeight() / 2.);
        var apply = RealClientHarness.widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals("Apply")).findFirst().orElseThrow();
        RealClientHarness.clickAt(mc, apply.getX() + apply.getWidth() / 2., apply.getY() + apply.getHeight() / 2.);
        step(); return;
      }
      if (stage == 4 && data().get("text").getAsString().equals("Both platforms")) {
        if (data().get("enabled").getAsBoolean()) throw new IllegalStateException("Boolean response was not applied");
        RealClientHarness.snapshot(mc, out, "input-response", data());
        var close = RealClientHarness.widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals("Close")).findFirst().orElseThrow();
        RealClientHarness.clickAt(mc, close.getX() + close.getWidth() / 2., close.getY() + close.getHeight() / 2.);
        step(); return;
      }
      if (stage == 5 && data().get("closed").getAsBoolean() && mc.gui.screen() == null) {
        if (!inventory.equals(inventory(mc))) throw new IllegalStateException("Dialog changed inventory");
        mc.player.connection.sendCommand("platform map"); step(); return;
      }
      if (stage == 6 && mc.player.getVehicle() != null && ticks - changed > 60) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), img -> { try (img) { img.writeToFile(out.resolve("map.png")); } catch (Exception e) { throw new RuntimeException(e); } });
        mc.player.connection.sendCommand("platform close"); step(); return;
      }
      if (stage == 7 && mc.player.getVehicle() == null && ticks - changed > 20) {
        if (!inventory.equals(inventory(mc))) throw new IllegalStateException("Map did not restore inventory");
        mc.player.connection.sendCommand("platform video"); step(); return;
      }
      if (stage == 8 && mc.player.getVehicle() != null) {
        var target = mc.gameRenderer.mainRenderTarget();
        pixels.sample(target, new double[][] {{.5, .5}}, 8, value -> {
          if (firstPixels != Long.MIN_VALUE && firstPixels != value) videoChanges++;
          firstPixels = value;
        });
        if (ticks - changed < 160 || videoChanges < 3) return;
        Screenshot.takeScreenshot(target, img -> { try (img) { img.writeToFile(out.resolve("video.png")); } catch (Exception e) { throw new RuntimeException(e); } });
        mc.player.connection.sendCommand("platform close"); step(); return;
      }
      if (stage == 9 && mc.player.getVehicle() == null && ticks - changed > 30) {
        if (!inventory.equals(inventory(mc))) throw new IllegalStateException("Video did not restore inventory");
        var result = data(); result.addProperty("passed", true); result.addProperty("steps", 10);
        result.addProperty("inventoryUnchanged", true); result.addProperty("videoChanges", videoChanges);
        result.addProperty("booleanResponseVerified", true); result.addProperty("packSha1", packSha1);
        Files.writeString(out.resolve("client-result.json"), result.toString());
        System.out.println("PLATFORM_TEST_COMPLETE"); stage = 10; mc.stop();
      }
    } catch (Exception error) {
      try { Files.createDirectories(out); Files.writeString(out.resolve("failure.txt"), error.toString()); } catch (Exception ignored) {}
      error.printStackTrace(); System.out.println("PLATFORM_TEST_FAILED"); stage = 10; mc.stop();
    }
  }
}
