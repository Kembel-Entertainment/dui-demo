package gg.kembel.dui.demo;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import gg.kembel.dui.core.*;
import gg.kembel.dui.paper.*;
import io.papermc.paper.registry.data.dialog.input.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** An ordinary consumer of dui. Business rules, files and background work live here. */
public final class DuiDemoPlugin extends JavaPlugin implements Listener {
  private static final Gson JSON = new Gson();
  private final Map<UUID, Session> sessions = new HashMap<>();
  private final SecureRandom random = new SecureRandom();
  private final Map<String, MenuTemplate> templates = new HashMap<>();
  private Dui dui;
  private HttpServer http;
  private PackMetadata metadata;
  private Path directory;
  private VideoProvider videos;

  private static final class Session {
    String section = "components";
    DialogSession ui;
    ShowcaseState kit = new ShowcaseState();
    DisplayPreferences display = new DisplayPreferences(), preview;
    ShopState shop = new ShopState();
    RewardState rewards = new RewardState();
    AdventState advent = new AdventState();
    SlotState slots = new SlotState();
    BukkitTask settlement, expiry;
    int videoPage;
    long request;
    boolean videoLoading, videoCompact;
  }

  @Override
  public void onEnable() {
    try {
      saveDefaultConfig();
      directory = getDataFolder().toPath();
      for (String name : List.of("layouts", "display", "rewards", "slots", "ui"))
        Files.createDirectories(directory.resolve(name));
      for (String name :
          List.of(
              "showcase",
              "showcase-compact",
              "shop",
              "rewards",
              "advent",
              "slots",
              "videos",
              "setup",
              "form",
              "confirm"))
        if (!Files.exists(directory.resolve("ui/" + name + ".html")))
          try (var in = getResource("ui/" + name + ".html")) {
            Files.copy(Objects.requireNonNull(in), directory.resolve("ui/" + name + ".html"));
          }
      var packDirectory = directory.resolve("pack");
      metadata = PackMetadata.read(packDirectory.resolve("dui.json"));
      byte[] pack = Files.readAllBytes(packDirectory.resolve("dui.zip"));
      if (!HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-1").digest(pack))
          .equals(metadata.sha1()))
        throw new IllegalStateException("Pack file and metadata do not match");
      http =
          HttpServer.create(
              new InetSocketAddress(
                  getConfig().getString("pack.bind-address", "127.0.0.1"),
                  getConfig().getInt("pack.port", 25585)),
              0);
      http.createContext(
          "/dui.zip",
          exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, pack.length);
            try (var out = exchange.getResponseBody()) {
              out.write(pack);
            }
          });
      http.start();
      dui =
          Dui.create(
              this,
              PackDescriptor.of(URI.create(getConfig().getString("pack.public-url")), metadata),
              metadata);
      reloadTemplates();
      videos =
          getConfig().getBoolean("videos.live", true)
              ? new VideoService(directory.resolve("videos-cache"))
              : new FixtureVideos();
      videos.refresh(false);
      getServer().getPluginManager().registerEvents(this, this);
      for (String command :
          List.of("dui", "uikit", "uishop", "dailyrewards", "slots", "uivideos", "advent"))
        Objects.requireNonNull(getCommand(command)).setExecutor(this);
      getLogger().info("DUI_DEMO_READY pack=" + metadata.sha1() + " minecraft=26.2");
    } catch (Exception e) {
      getLogger()
          .log(
              java.util.logging.Level.SEVERE,
              "dui-demo startup failed. Run the pack build and install tasks first.",
              e);
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (videos != null) videos.close();
    for (var entry : sessions.entrySet()) finishSlots(entry.getKey(), entry.getValue());
    if (dui != null) dui.close();
    if (http != null) http.stop(0);
    sessions.clear();
  }

  private void reloadTemplates() throws Exception {
    var next = new HashMap<String, MenuTemplate>();
    for (String name :
        List.of(
            "showcase",
            "showcase-compact",
            "shop",
            "rewards",
            "advent",
            "slots",
            "videos",
            "setup",
            "form",
            "confirm"))
      next.put(name, dui.compile(Files.readString(directory.resolve("ui/" + name + ".html"))));
    for (boolean compact : List.of(false, true)) {
      var kit = new ShowcaseState();
      kit.layout = compact ? "compact" : "spacious";
      for (var page : ShowcaseState.PAGES) {
        kit.page = page.id();
        for (int part = 0; part < (compact ? kit.partCount() : 1); part++) {
          kit.part = part;
          for (boolean popup : List.of(false, true)) {
            kit.dropdownOpen = popup;
            next.get(compact ? "showcase-compact" : "showcase").render(kit.data());
          }
        }
      }
      var shop = new ShopState();
      shop.compact = compact;
      shop.cart.put("lantern", 1);
      next.get("shop").render(ShopView.data(shop, "Example"));
      shop.checkout = true;
      next.get("shop").render(ShopView.data(shop, "Example"), ShopView.images(shop));
      var reward = new RewardState();
      reward.compact = compact;
      next.get("rewards").render(RewardView.data(reward, LocalDate.of(2026, 9, 30)));
      var advent = new AdventState();
      advent.compact = compact;
      next.get("advent").render(AdventView.data(advent, 100), AdventView.images(advent));
      advent.open(24, 100);
      next.get("advent").render(AdventView.data(advent, 100), AdventView.images(advent));
      advent.reveal(advent.generation, 124);
      next.get("advent").render(AdventView.data(advent, 124), AdventView.images(advent));
      var slots = new SlotState();
      slots.compact = compact;
      next.get("slots").render(SlotView.data(slots));
      next.get("videos")
          .render(
              VideoView.model(new FixtureVideos().feed(), 0, compact, false, "", Map.of()).data());
    }
    templates.clear();
    templates.putAll(next);
  }

  private Session session(Player player) {
    return sessions.computeIfAbsent(
        player.getUniqueId(),
        id -> {
          var s = new Session();
          s.display = read("display", id, DisplayPreferences.class, s.display);
          if (!s.display.valid()) s.display = new DisplayPreferences();
          s.kit.layout = s.display.layout;
          s.rewards = read("rewards", id, RewardState.class, s.rewards);
          if (!s.rewards.valid()) s.rewards = new RewardState();
          s.slots = read("slots", id, SlotState.class, s.slots);
          if (!s.slots.valid()) throw new IllegalStateException("Invalid demo chip ledger");
          if (s.slots.settle()) save("slots", id, s.slots);
          return s;
        });
  }

  private <T> T read(String folder, UUID id, Class<T> type, T fallback) {
    try {
      var file = directory.resolve(folder + "/" + id + ".json");
      if (!Files.exists(file)) return fallback;
      T value = JSON.fromJson(Files.readString(file), type);
      if (value == null) throw new IllegalStateException("Empty saved state");
      return value;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read demo state " + folder, e);
    }
  }

  private void save(String folder, UUID id, Object value) {
    try {
      var file = directory.resolve(folder + "/" + id + ".json");
      var temp = file.resolveSibling(file.getFileName() + ".tmp");
      Files.writeString(temp, JSON.toJson(value));
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot save demo state " + folder, e);
    }
  }

  private String theme(Session s) {
    return s.kit.dark ? "studio_dark" : "studio";
  }

  private Canvas canvas(String name, Map<String, Object> data, Map<String, RasterImage> images) {
    return templates.get(name).render(data, images);
  }

  private void present(
      Player p,
      Session s,
      Canvas c,
      ViewModel model,
      DialogOptions options,
      ActionHandler handler) {
    ActionHandler audited =
        ctx -> {
          handler.handle(ctx);
          audit(p, s, ctx.action());
        };
    if (s.ui != null && s.ui.isActive()) s.ui.update(c, model, options, audited);
    else
      s.ui =
          dui.open(p, c, model, options, audited)
              .onClose(
                  () -> {
                    s.request++;
                    s.rewards.burstStarted = -1;
                    s.advent.back();
                    if (s.expiry != null) {
                      s.expiry.cancel();
                      s.expiry = null;
                    }
                  });
  }

  private void show(Player p, Session s) {
    switch (s.section) {
      case "shop" -> shop(p, s);
      case "rewards" -> rewards(p, s);
      case "advent" -> advent(p, s);
      case "slots" -> slots(p, s);
      case "videos" -> video(p, s);
      default -> components(p, s);
    }
  }

  private void components(Player p, Session s) {
    if (!s.display.configured && s.preview == null) {
      setup(p, s);
      return;
    }
    var c = canvas(s.kit.compact() ? "showcase-compact" : "showcase", s.kit.data(), Map.of());
    var items =
        s.kit.page.equals("media") ? ShowcaseItems.stacks(p, s.kit) : Map.<String, ItemStack>of();
    var options =
        s.preview == null
            ? DialogOptions.notice("dui / Component showcase", "Close showcase", "kit_close")
            : new DialogOptions(
                Component.text("dui / Layout preview"),
                List.of(),
                List.of(
                    new DialogOptions.Button("Use this layout", "kit_display_keep", 128),
                    new DialogOptions.Button("Change size", "kit_display_change", 128)),
                null,
                2,
                true);
    present(
        p,
        s,
        c,
        new ViewModel(Map.of(), Map.of(), items, Map.of()),
        options,
        ctx -> {
          switch (ctx.action()) {
            case "kit_close" -> {}
            case "kit_form" -> form(p, s);
            case "kit_reset" -> confirm(p, s);
            case "kit_setup", "kit_display_change" -> setup(p, s);
            case "kit_display_keep" -> {
              if (s.preview != null) {
                s.display = s.preview;
                s.preview = null;
                save("display", p.getUniqueId(), s.display);
                s.kit.notice = "Layout saved. Change it with the settings icon.";
                show(p, s);
              }
            }
            default -> {
              s.kit.apply(ctx.action(), ctx.value());
              show(p, s);
            }
          }
        });
    export(
        p,
        s,
        c,
        Map.of(
            "section",
            "showcase",
            "page",
            s.kit.page,
            "state",
            s.kit,
            "display",
            s.display,
            "preview",
            s.preview != null));
  }

  private void setup(Player p, Session s) {
    var initial = s.preview != null ? s.preview : s.display;
    s.preview = null;
    s.kit.layout = s.display.layout;
    s.kit.dropdownOpen = false;
    var c = canvas("setup", Map.of("theme", theme(s)), Map.of());
    var input =
        DialogInput.singleOption(
                "gui_scale",
                Component.text("Which GUI scale do you use?"),
                List.of("auto", "1", "2", "3", "4", "5+").stream()
                    .map(
                        v ->
                            SingleOptionDialogInput.OptionEntry.create(
                                v,
                                Component.text(v.equals("auto") ? "Auto / not sure" : v),
                                v.equals(initial.guiScale)))
                    .toList())
            .width(260)
            .build();
    var options =
        new DialogOptions(
            Component.text("dui / Display setup"),
            List.of(input),
            List.of(
                new DialogOptions.Button("Compact preview", "kit_display_compact", 128),
                new DialogOptions.Button("Spacious preview", "kit_display_spacious", 128)),
            new DialogOptions.Button("Cancel", "kit_display_cancel", 128),
            2,
            false);
    present(
        p,
        s,
        c,
        ViewModel.data(Map.of()),
        options,
        ctx -> {
          if (ctx.action().equals("kit_display_cancel")) {
            s.preview = null;
            s.kit.layout = s.display.layout;
            if (s.display.configured) show(p, s);
            return;
          }
          String scale = ctx.response() == null ? null : ctx.response().getText("gui_scale");
          if (!DisplayPreferences.SCALES.contains(scale)) {
            setup(p, s);
            return;
          }
          String layout = ctx.action().equals("kit_display_compact") ? "compact" : "spacious";
          s.preview = DisplayPreferences.selection(scale, layout);
          s.kit.layout = layout;
          s.kit.part = 0;
          show(p, s);
        });
    export(p, s, c, Map.of("section", "showcase", "page", "setup", "display", s.display));
  }

  private void form(Player p, Session s) {
    int width = s.kit.compact() ? 260 : 320;
    var state = s.kit;
    var c = canvas("form", Map.of("width", width + 24, "theme", theme(s)), Map.of());
    var inputs =
        List.<DialogInput>of(
            DialogInput.text("sample_text", Component.text("Text / sample title"))
                .initial(state.formName)
                .maxLength(24)
                .width(width)
                .build(),
            DialogInput.bool("sample_check", Component.text("Checkbox / example enabled"))
                .initial(state.formChecked)
                .build(),
            DialogInput.singleOption(
                    "sample_option",
                    Component.text("Option picker / sample variant"),
                    List.of("Option A", "Option B", "Option C").stream()
                        .map(
                            v ->
                                SingleOptionDialogInput.OptionEntry.create(
                                    v, Component.text(v), v.equals(state.formStyle)))
                        .toList())
                .width(width)
                .build(),
            DialogInput.numberRange(
                    "sample_amount", Component.text("Slider / sample amount"), 0, 100)
                .initial((float) state.formAmount)
                .step(5f)
                .width(width)
                .build());
    var options =
        new DialogOptions(
            Component.text("dui / Sample form"),
            inputs,
            List.of(new DialogOptions.Button("Save example", "kit_save")),
            new DialogOptions.Button("Cancel", "kit_cancel"),
            1,
            false);
    present(
        p,
        s,
        c,
        ViewModel.data(Map.of()),
        options,
        ctx -> {
          if (ctx.action().equals("kit_cancel")) {
            state.notice = "Edit cancelled. Previous values kept.";
            show(p, s);
            return;
          }
          var response = ctx.response();
          String name = response == null ? null : response.getText("sample_text"),
              option = response == null ? null : response.getText("sample_option");
          Boolean checked = response == null ? null : response.getBoolean("sample_check");
          Float amount = response == null ? null : response.getFloat("sample_amount");
          if (name == null
              || name.length() > 24
              || name.codePoints().anyMatch(Character::isISOControl)
              || option == null
              || !List.of("Option A", "Option B", "Option C").contains(option)
              || checked == null
              || amount == null
              || !Float.isFinite(amount)
              || amount < 0
              || amount > 100) {
            form(p, s);
            return;
          }
          state.formName = name.strip();
          state.formStyle = option;
          state.formChecked = checked;
          state.formAmount = Math.round(amount);
          state.page = "forms";
          state.notice = "Saved. Your sample values are shown on the cards.";
          show(p, s);
        });
    export(p, s, c, Map.of("section", "showcase", "page", "input", "state", s.kit));
  }

  private void confirm(Player p, Session s) {
    var c =
        canvas(
            "confirm", Map.of("width", s.kit.compact() ? 284 : 344, "theme", theme(s)), Map.of());
    present(
        p,
        s,
        c,
        ViewModel.data(Map.of()),
        DialogOptions.notice("dui / Reset examples", "Back to showcase", "kit_cancel"),
        ctx -> {
          if (ctx.action().equals("kit_reset_yes")) {
            String page = s.kit.page, layout = s.kit.layout;
            int part = s.kit.part;
            boolean dark = s.kit.dark;
            s.kit = new ShowcaseState();
            s.kit.page = page;
            s.kit.layout = layout;
            s.kit.part = part;
            s.kit.dark = dark;
          }
          show(p, s);
        });
    export(p, s, c, Map.of("section", "showcase", "page", "confirm", "state", s.kit));
  }

  private void shop(Player p, Session s) {
    var images = ShopView.images(s.shop);
    var c = canvas("shop", ShopView.data(s.shop, p.getName()), images);
    var links =
        s.shop.checkout
            ? Map.of("shop_link", URI.create(ShopState.DEMO_URL))
            : Map.<String, URI>of();
    present(
        p,
        s,
        c,
        new ViewModel(Map.of(), images, ShopItems.stacks(p), links),
        DialogOptions.notice("dui / Demo store", "Close store", "shop_close"),
        ctx -> {
          if (ctx.action().equals("shop_close")) return;
          s.shop.apply(ctx.action(), ctx.value());
          show(p, s);
        });
    var extra = new HashMap<String, Object>();
    extra.put("section", "shop");
    extra.put("state", s.shop);
    extra.put("total", s.shop.total());
    if (s.shop.checkout) {
      var i = c.images.getFirst();
      extra.put("qr", QrCode.region(ShopState.DEMO_URL, i.x(), i.y(), i.width(), i.pixelSize()));
    }
    export(p, s, c, extra);
  }

  private void rewards(Player p, Session s) {
    if (s.expiry != null) s.expiry.cancel();
    long age = p.getWorld().getGameTime() - s.rewards.burstStarted;
    if (age < 0 || age >= ItemTransport.BURST_TICKS) s.rewards.burstStarted = -1;
    var date = LocalDate.now(ZoneOffset.UTC);
    var c = canvas("rewards", RewardView.data(s.rewards, date), Map.of());
    present(
        p,
        s,
        c,
        new ViewModel(Map.of(), Map.of(), RewardItems.stacks(s.rewards, date), Map.of()),
        DialogOptions.notice("dui / Daily rewards", "Close rewards", "reward_close"),
        ctx -> {
          if (ctx.action().equals("reward_close")) return;
          long before = s.rewards.stars;
          s.rewards.apply(ctx.action(), ctx.value(), LocalDate.now(ZoneOffset.UTC));
          if (ctx.action().equals("reward_claim") && s.rewards.stars > before && s.rewards.motion)
            s.rewards.burstStarted = p.getWorld().getGameTime();
          save("rewards", p.getUniqueId(), s.rewards);
          show(p, s);
        });
    if (c.confetti != null) {
      long revision = s.ui.revision();
      s.expiry =
          getServer()
              .getScheduler()
              .runTaskLater(
                  this,
                  () -> {
                    if (visible(p, s, "rewards") && s.ui.revision() == revision) {
                      s.rewards.burstStarted = -1;
                      show(p, s);
                    }
                  },
                  Math.max(1, ItemTransport.BURST_TICKS + 40 - age));
    }
    var extra = new HashMap<String, Object>();
    extra.put("section", "rewards");
    extra.put("state", s.rewards);
    extra.put("celebrating", s.rewards.celebrating);
    extra.put("selected", s.rewards.selection(date));
    extra.put("today", s.rewards.today(date).toString());
    extra.put("confetti", c.confetti);
    export(p, s, c, extra);
  }

  private void advent(Player p, Session s) {
    if (s.expiry != null) {
      s.expiry.cancel();
      s.expiry = null;
    }
    long tick = p.getWorld().getGameTime();
    var state = s.advent;
    if (state.phase == AdventState.Phase.OPENING) state.reveal(state.generation, tick);
    if (state.phase == AdventState.Phase.REVEALED && tick - state.startedAt >= 120)
      state.effectsFinished = true;
    var c = canvas("advent", AdventView.data(state, tick), AdventView.images(state));
    present(
        p,
        s,
        c,
        new ViewModel(Map.of(), Map.of(), AdventItems.stacks(state), Map.of()),
        DialogOptions.notice("dui / Gift drop", "Close calendar", "advent_close"),
        ctx -> {
          if (ctx.action().equals("advent_close")) return;
          long now = p.getWorld().getGameTime();
          switch (ctx.action()) {
            case "advent_open" -> state.open(Integer.parseInt(ctx.value()), now);
            case "advent_back" -> state.back();
            case "advent_again" -> {
              if (state.phase == AdventState.Phase.REVEALED) state.open(state.selected, now);
            }
            case "advent_size" -> state.compact = !state.compact;
            case "advent_motion" -> {
              state.motion = !state.motion;
              if (!state.motion && state.phase == AdventState.Phase.OPENING) {
                state.phase = AdventState.Phase.REVEALED;
                state.rewardAt = now;
              }
            }
            default -> {
              return;
            }
          }
          show(p, s);
        });
    if (state.phase == AdventState.Phase.OPENING) {
      long token = state.generation;
      s.expiry =
          getServer()
              .getScheduler()
              .runTaskLater(
                  this,
                  () -> {
                    if (visible(p, s, "advent") && state.reveal(token, p.getWorld().getGameTime()))
                      show(p, s);
                  },
                  Math.max(1, AdventState.OPEN_TICKS - (tick - state.startedAt)));
    } else if (state.phase == AdventState.Phase.REVEALED
        && state.motion
        && !state.effectsFinished) {
      long token = state.generation;
      s.expiry =
          getServer()
              .getScheduler()
              .runTaskLater(
                  this,
                  () -> {
                    if (visible(p, s, "advent") && state.generation == token) {
                      state.effectsFinished = true;
                      show(p, s);
                    }
                  },
                  Math.max(1, 120 - (tick - state.startedAt)));
    }
    var extra = new HashMap<String, Object>();
    extra.put("section", "advent");
    extra.put("state", state);
    extra.put("phase", state.phase);
    extra.put("selected", state.selected);
    extra.put("effects", c.effects);
    export(p, s, c, extra);
  }

  private SlotState copySlots(SlotState value) {
    var next = JSON.fromJson(JSON.toJson(value), SlotState.class);
    next.compact = value.compact;
    next.paytable = value.paytable;
    next.startedAt = value.startedAt;
    return next;
  }

  private void settle(UUID id, Session s) {
    var next = copySlots(s.slots);
    if (next.settle()) {
      save("slots", id, next);
      s.slots = next;
    }
  }

  private void finishSlots(UUID id, Session s) {
    if (s.settlement != null) s.settlement.cancel();
    if (s.expiry != null) s.expiry.cancel();
    settle(id, s);
  }

  private boolean visible(Player p, Session s, String section) {
    return p.isOnline()
        && sessions.get(p.getUniqueId()) == s
        && s.section.equals(section)
        && s.ui != null
        && s.ui.isActive()
        && dui.packLoaded(p);
  }

  private void slots(Player p, Session s) {
    if (s.expiry != null) s.expiry.cancel();
    long age = p.getWorld().getGameTime() - s.slots.startedAt;
    var c = canvas("slots", SlotView.data(s.slots), Map.of());
    if (!s.slots.pending
        && c.animation != null
        && (age < 0 || age >= c.animation.durationTicks() + 20)) {
      s.slots.startedAt = -1;
      c = canvas("slots", SlotView.data(s.slots), Map.of());
    }
    present(
        p,
        s,
        c,
        ViewModel.data(Map.of()),
        DialogOptions.notice("dui / Demo arcade", "Leave arcade", "slot_close"),
        ctx -> {
          if (ctx.action().equals("slot_close")) return;
          var next = copySlots(s.slots);
          boolean spin = ctx.action().equals("slot_spin");
          if (spin) {
            if (!next.spin(p.getWorld().getGameTime(), random)) {
              show(p, s);
              return;
            }
          } else next.apply(ctx.action());
          save("slots", p.getUniqueId(), next);
          s.slots = next;
          show(p, s);
          if (spin)
            s.settlement =
                getServer()
                    .getScheduler()
                    .runTaskLater(
                        this,
                        () -> {
                          settle(p.getUniqueId(), s);
                          s.settlement = null;
                          if (visible(p, s, "slots")) show(p, s);
                        },
                        next.motion ? SlotState.SPIN_TICKS : 1);
        });
    if (!s.slots.pending && s.slots.startedAt >= 0 && c.animation != null) {
      long revision = s.ui.revision();
      s.expiry =
          getServer()
              .getScheduler()
              .runTaskLater(
                  this,
                  () -> {
                    if (visible(p, s, "slots") && s.ui.revision() == revision) {
                      s.slots.startedAt = -1;
                      show(p, s);
                    }
                  },
                  Math.max(1, c.animation.durationTicks() + 20 - age));
    }
    var extra = new HashMap<String, Object>();
    extra.put("section", "slots");
    extra.put("state", s.slots);
    extra.put("effect", c.animation);
    extra.put("effects", c.effects);
    extra.put("focusOutlineHidden", c.hideFocusOutline);
    export(p, s, c, extra);
  }

  private void requestVideos(Player p, Session s, boolean force) {
    s.videoLoading = true;
    long guard = ++s.request;
    int page = s.videoPage;
    boolean compact = s.videoCompact;
    show(p, s);
    videos
        .refresh(force)
        .thenCompose(
            feed ->
                java.util.concurrent.CompletableFuture.allOf(
                    VideoView.model(feed, page, compact, true, "", Map.of()).visible().stream()
                        .map(v -> videos.thumbnail(v).handle((image, error) -> null))
                        .toArray(java.util.concurrent.CompletableFuture[]::new)))
        .whenComplete(
            (value, error) -> {
              if (!isEnabled()) return;
              getServer()
                  .getScheduler()
                  .runTask(
                      this,
                      () -> {
                        if (visible(p, s, "videos") && s.request == guard) {
                          s.videoLoading = false;
                          show(p, s);
                        }
                      });
            });
  }

  private void video(Player p, Session s) {
    var images = new HashMap<String, RasterImage>();
    for (var v : videos.feed().videos()) {
      var image = videos.cached(v);
      if (image != null) images.put(v.id(), image);
    }
    var model =
        VideoView.model(
            videos.feed(), s.videoPage, s.videoCompact, s.videoLoading, videos.error(), images);
    s.videoPage = model.page();
    var c = canvas("videos", model.data(), model.images());
    var links = new HashMap<String, URI>();
    for (var v : model.visible()) {
      links.put("watch_" + v.id(), URI.create(v.watchUrl()));
      links.put("thumb_" + v.id(), URI.create(v.watchUrl()));
    }
    present(
        p,
        s,
        c,
        new ViewModel(Map.of(), model.images(), Map.of(), links),
        DialogOptions.notice("dui / YouTube uploads", "Close videos", "video_close"),
        ctx -> {
          switch (ctx.action()) {
            case "video_close" -> {
              return;
            }
            case "video_previous" -> s.videoPage = Math.max(0, s.videoPage - 1);
            case "video_next" -> s.videoPage++;
            case "video_size" -> {
              s.videoCompact = !s.videoCompact;
              s.videoPage = 0;
            }
            case "video_refresh" -> {}
            default -> throw new IllegalArgumentException("Unknown video action");
          }
          requestVideos(p, s, ctx.action().equals("video_refresh"));
        });
    var extra = new HashMap<String, Object>();
    extra.put("section", "videos");
    extra.put("page", model.page());
    extra.put("pages", model.pages());
    extra.put("loading", s.videoLoading);
    extra.put("error", videos.error());
    extra.put("checkedAt", videos.feed().checkedAt().toString());
    extra.put(
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
    export(p, s, c, extra);
  }

  private void export(Player p, Session s, Canvas c, Map<String, Object> extra) {
    try {
      var data = new LinkedHashMap<String, Object>(extra);
      data.put("width", c.width);
      data.put("height", c.height);
      data.put("hits", c.hits);
      data.put("items", c.items);
      data.put("transitions", c.transitions);
      data.put("heads", c.heads);
      data.put("packSha1", metadata.sha1());
      var images = new ArrayList<Map<String, Object>>();
      for (var i : c.images) {
        var rgb = new ArrayList<Integer>();
        for (int y = 0; y < i.raster().height; y++)
          for (int x = 0; x < i.raster().width; x++) rgb.add(i.raster().rgb(x, y));
        images.add(
            Map.of(
                "id",
                i.id(),
                "x",
                i.x(),
                "y",
                i.y(),
                "width",
                i.width(),
                "height",
                i.height(),
                "pixelSize",
                i.pixelSize(),
                "columns",
                i.raster().width,
                "rows",
                i.raster().height,
                "rgb",
                rgb));
      }
      data.put("images", images);
      if (s.ui.component() != null)
        data.put(
            "componentBytes",
            net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
                .serialize(s.ui.component())
                .getBytes(StandardCharsets.UTF_8)
                .length);
      var target = directory.resolve("layouts/" + p.getName() + ".json");
      var staging = target.resolveSibling(target.getFileName() + ".tmp");
      Files.writeString(staging, JSON.toJson(data));
      Files.move(
          staging, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      getLogger().info("DUI_DEMO_SHOW player=" + p.getName() + " section=" + extra.get("section"));
    } catch (Exception e) {
      getLogger().warning("Demo layout export failed: " + e.getMessage());
    }
  }

  private void audit(Player p, Session s, String action) {
    try {
      Files.writeString(
          directory.resolve("actions.jsonl"),
          JSON.toJson(
                  Map.of(
                      "time",
                      Instant.now().toString(),
                      "player",
                      p.getName(),
                      "action",
                      action,
                      "display",
                      s.display,
                      "showcase",
                      s.kit,
                      "shop",
                      s.shop,
                      "rewards",
                      s.rewards,
                      "advent",
                      s.advent,
                      "slots",
                      s.slots))
              + "\n",
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (Exception e) {
      getLogger().warning("Demo action report failed: " + e.getMessage());
    }
  }

  @EventHandler
  public void join(PlayerJoinEvent e) {
    getServer()
        .getScheduler()
        .runTaskLater(
            this,
            () -> {
              if (e.getPlayer().isOnline()) show(e.getPlayer(), session(e.getPlayer()));
            },
            20);
  }

  @EventHandler
  public void pack(PlayerResourcePackStatusEvent e) {
    if (e.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED
        && dui.packLoaded(e.getPlayer())) {
      getLogger().info("DUI_PACK_LOADED " + e.getPlayer().getName());
      show(e.getPlayer(), session(e.getPlayer()));
    }
  }

  @EventHandler
  public void quit(PlayerQuitEvent e) {
    var s = sessions.remove(e.getPlayer().getUniqueId());
    if (s != null) finishSlots(e.getPlayer().getUniqueId(), s);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String name = command.getName();
    String section =
        switch (name) {
          case "uishop" -> "shop";
          case "dailyrewards" -> "rewards";
          case "advent" -> "advent";
          case "slots" -> "slots";
          case "uivideos" -> "videos";
          default -> "components";
        };
    var arguments = new ArrayList<>(List.of(args));
    if (name.equals("dui") && !arguments.isEmpty())
      section = arguments.removeFirst().toLowerCase(Locale.ROOT);
    if (section.equals("reload")) {
      if (!sender.hasPermission("dui-demo.reload")) {
        sender.sendMessage("You cannot reload the demo templates.");
        return true;
      }
      try {
        reloadTemplates();
        sender.sendMessage("dui / Templates reloaded.");
        for (var p : getServer().getOnlinePlayers()) {
          var s = sessions.get(p.getUniqueId());
          if (s != null && s.ui != null && s.ui.isActive()) show(p, s);
        }
      } catch (Exception e) {
        sender.sendMessage("Templates unchanged: " + e.getMessage());
      }
      return true;
    }
    if (!(sender instanceof Player p)) {
      sender.sendMessage("Run /dui as a player.");
      return true;
    }
    if (!Set.of("components", "setup", "shop", "rewards", "advent", "slots", "videos")
        .contains(section)) {
      p.sendMessage("/dui [components|setup|shop|rewards|advent|slots|videos|reload]");
      return true;
    }
    var s = session(p);
    s.request++;
    if (s.expiry != null) s.expiry.cancel();
    s.advent.back();
    s.section = section.equals("setup") ? "components" : section;
    s.preview = null;
    String option = arguments.isEmpty() ? "" : arguments.getFirst().toLowerCase(Locale.ROOT);
    boolean compact =
        option.isEmpty() ? s.display.layout.equals("compact") : option.equals("compact");
    switch (section) {
      case "setup" -> {
        setup(p, s);
        return true;
      }
      case "advent" -> s.advent.compact = compact;
      case "shop" -> {
        s.shop.compact = compact;
        s.shop.checkout = false;
        s.shop.cartPage = 0;
      }
      case "rewards" -> {
        s.rewards.compact = compact;
        s.rewards.selected = -1;
        s.rewards.celebrating = false;
        s.rewards.burstStarted = -1;
      }
      case "slots" -> {
        s.slots.compact = compact;
        s.slots.paytable = false;
      }
      case "videos" -> {
        s.videoCompact = compact;
        s.videoPage = 0;
        requestVideos(p, s, option.equals("refresh"));
        return true;
      }
      default -> {
        if (name.equals("uikit") && option.equals("setup")) {
          setup(p, s);
          return true;
        }
        s.kit.page = option.isEmpty() ? "basics" : option;
        s.kit.part = 0;
        s.kit.dropdownOpen = false;
        if (ShowcaseState.PAGES.stream().noneMatch(page -> page.id().equals(s.kit.page)))
          s.kit.page = "basics";
      }
    }
    show(p, s);
    return true;
  }
}
