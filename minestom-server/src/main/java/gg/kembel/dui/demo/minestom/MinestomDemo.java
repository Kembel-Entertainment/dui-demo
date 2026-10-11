package gg.kembel.dui.demo.minestom;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import gg.kembel.dui.demo.platform.PlatformShowcase;
import gg.kembel.dui.minestom.*;
import gg.kembel.dui.runtime.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.*;

/** Explicit local consumer host. The library never starts a server or HTTP endpoint. */
public final class MinestomDemo {
  public static void main(String[] args) throws Exception {
    var packDirectory = Path.of(System.getProperty("dui.demo.pack", "../build/pack")).toAbsolutePath();
    var reports = Path.of(System.getProperty("dui.demo.reports", "../run/minestom/plugins/dui-demo/layouts")).toAbsolutePath();
    int port = Integer.getInteger("dui.demo.port", 25586), packPort = Integer.getInteger("dui.demo.pack-port", 25587);
    var metadata = PackMetadata.read(packDirectory.resolve("dui.json"));
    byte[] packBytes = Files.readAllBytes(packDirectory.resolve("dui.zip"));
    if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(packBytes)).equals(metadata.sha1()))
      throw new IllegalArgumentException("Pack and metadata must match");
    var http = HttpServer.create(new InetSocketAddress("127.0.0.1", packPort), 0);
    http.createContext("/dui.zip", exchange -> {
      exchange.getResponseHeaders().set("Content-Type", "application/zip");
      exchange.sendResponseHeaders(200, packBytes.length);
      try (var output = exchange.getResponseBody()) { output.write(packBytes); }
    });
    http.start();
    var server = MinecraftServer.init();
    var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
    instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
    var dui = gg.kembel.dui.minestom.Dui.create(
        PackDescriptor.of(URI.create("http://127.0.0.1:" + packPort + "/dui.zip"), metadata), metadata);
    var showcase = new PlatformShowcase(dui.runtime());
    var gson = new Gson(); Files.createDirectories(reports);
    MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
      event.setSpawningInstance(instance); event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
    });
    MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
      var player = event.getPlayer(); player.setGameMode(GameMode.SURVIVAL);
      player.getInventory().setItemStack(0, ItemStack.of(Material.DIAMOND));
      player.setHelmet(ItemStack.of(Material.IRON_HELMET));
      player.setChestplate(ItemStack.of(Material.LEATHER_CHESTPLATE)
          .with(net.minestom.server.component.DataComponents.DYED_COLOR, new net.minestom.server.color.Color(0x8E56C4)));
      player.setLeggings(ItemStack.of(Material.IRON_LEGGINGS));
      player.setBoots(ItemStack.of(Material.DIAMOND_BOOTS));
    });
    var command = new Command("platform");
    command.setDefaultExecutor((sender, context) -> {
      if (!(sender instanceof Player player)) return;
      player.acquirable().sync(p -> {
        try {
          var viewer = dui.viewer(p);
          var raw = context.getInput().trim().split("\\s+");
          var mode = raw.length > 1 ? raw[1] : "menu";
          if (mode.equals("map")) { showcase.map(viewer); return; }
          if (mode.equals("video")) { showcase.video(viewer); return; }
          if (mode.equals("close")) { dui.runtime().spatial().closeViewer(viewer, true); dui.runtime().closeViewer(viewer, true); return; }
          showcase.open(viewer, MinestomItem.of(ItemStack.of(Material.DIAMOND)), MinestomAppearance.capture(p), data -> {
            try {
              var report = new LinkedHashMap<String, Object>(data); report.put("backend", "minestom");
              Files.writeString(reports.resolve(p.getUsername() + ".json"), gson.toJson(report));
            } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
          });
        } catch (Exception error) { p.sendMessage(Component.text(error.toString())); error.printStackTrace(); }
      });
    });
    MinecraftServer.getCommandManager().register(command);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> { dui.close(); http.stop(0); MinecraftServer.stopCleanly(); }));
    server.start("127.0.0.1", port);
    System.out.println("DUI_MINESTOM_READY pack=" + metadata.sha1());
    try (var input = new Scanner(System.in)) { while (input.hasNextLine()) if (input.nextLine().equals("stop")) break; }
    dui.close(); http.stop(0); MinecraftServer.stopCleanly();
  }
}
