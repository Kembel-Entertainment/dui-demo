package gg.kembel.dui.demo.gba;

import gg.kembel.dui.core.*;
import gg.kembel.dui.core.video.*;
import gg.kembel.dui.paper.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Consumer-owned emulator, ROM catalogue, controls, design and per-player save policy. */
public final class GbaDemo implements Listener,AutoCloseable {
  private final JavaPlugin plugin;
  private final Dui dui;
  private final Path directory,romDirectory;
  private final MenuTemplate menu;
  private final VideoSurfaceTemplate screen;
  private final Map<UUID,Session> sessions=new HashMap<>();
  private final Map<UUID,CompletableFuture<Void>> stopping=new HashMap<>();
  private final ExecutorService io=Executors.newFixedThreadPool(2,r->{var t=new Thread(r,"gba-launch");t.setDaemon(true);return t;});
  private volatile boolean active=true;
  private static final class Session {
    volatile boolean closed;
    volatile GbaProcess process;
    volatile VideoSurfaceSession surface;
    DialogSession menu;
    String title;
    boolean changingView;
    final CompletableFuture<Void> termination = new CompletableFuture<>();
  }
  public GbaDemo(JavaPlugin plugin,Dui dui,Path directory) throws Exception {
    this.plugin=plugin;this.dui=dui;this.directory=directory.resolve("gba");
    romDirectory=this.directory.resolve("roms");Files.createDirectories(romDirectory);
    Path demo=romDirectory.resolve("color-controls.gba");
    if(!Files.exists(demo))try(var input=plugin.getResource("gba/color-controls.gba")){Files.copy(Objects.requireNonNull(input),demo);}
    for(String name:List.of("gba","gba-screen")) {
      Path p=directory.resolve("ui/"+name+".html");
      if(!Files.exists(p))try(var in=plugin.getResource("ui/"+name+".html")){Files.copy(Objects.requireNonNull(in),p);}
    }
    menu=dui.compile("ui/gba.html",Files.readString(directory.resolve("ui/gba.html")),ComponentRegistry.EMPTY,gg.kembel.dui.demo.DemoTemplates.environment(gg.kembel.dui.demo.DemoTemplates.font()));
    screen=dui.compileVideoSurface("ui/gba-screen.html",Files.readString(directory.resolve("ui/gba-screen.html")),ComponentRegistry.EMPTY);
    screen.render(screenData("GBA",60));
    plugin.getServer().getPluginManager().registerEvents(this,plugin);
  }
  private Map<String,Object> screenData(String title,double fps) {
    return Map.of("title",title,"fps",Math.min(60,fps),"bytes",plugin.getConfig().getLong("gba.bytes-per-second",16*1024*1024));
  }
  private void main(Runnable action) {
    if(active)try { plugin.getServer().getScheduler().runTask(plugin,()->{if(active)action.run();}); }
    catch(IllegalStateException ignored) {}
  }
  private List<Path> roms() throws Exception {
    try(var paths=Files.list(romDirectory)) { return paths.filter(Files::isRegularFile)
        .filter(p->p.getFileName().toString().matches("[A-Za-z0-9_.-]+\\.gba"))
        .sorted(Comparator.comparing(p->p.getFileName().toString())).toList(); }
  }
  public boolean command(Player player,List<String> args) {
    try {
      String action=args.isEmpty()?"menu":args.getFirst().toLowerCase(Locale.ROOT);
      switch(action) {
        case "play" -> { if(args.size()<2)showMenu(player);else play(player,args.get(1)); }
        case "close" -> stop(player.getUniqueId());
        case "save" -> { var s=sessions.get(player.getUniqueId());if(s!=null&&s.process!=null)s.process.save(); }
        case "stats" -> {
          var s=sessions.get(player.getUniqueId());
          player.sendMessage(s==null?"No emulator running.":s.process==null?"Emulator starting.":s.process.statistics());
          if(s!=null&&s.surface!=null)player.sendMessage("Transport: "+s.surface.statistics());
        }
        case "resume" -> { var s=sessions.get(player.getUniqueId());if(s==null)showMenu(player);else showScreen(player,s); }
        default -> showMenu(player);
      }
    }catch(Exception e){player.sendMessage("GBA: "+e.getMessage());plugin.getLogger().log(java.util.logging.Level.WARNING,"GBA command failed",e);}
    return true;
  }
  private void showMenu(Player player) throws Exception {
    Session s=sessions.get(player.getUniqueId());
    if(s!=null) {
      s.changingView=true;
      if(s.process!=null)s.process.pause(true);
      if(s.surface!=null){s.surface.close();s.surface=null;}
    }
    var entries=new ArrayList<Map<String,Object>>();int y=54;
    for(Path rom:roms().stream().limit(5).toList()) {
      String id=rom.getFileName().toString();entries.add(Map.of("id",id,"title",id.substring(0,id.length()-4),"y",y));y+=27;
    }
    Session existing=s;
    var view=dui.open(player,menu,ViewModel.data(Map.of("roms",entries,"running",s!=null,"status",s==null?"Choose a cartridge. Saves belong to you.":"Paused. Your current session is safe.")),
        DialogOptions.notice("DUI / Pocket Arcade","Close","close"),context->{
          if(context.action().equals("play")){context.session().close();command(player,List.of("play",context.value()));}
          else if(context.action().equals("resume")&&existing!=null)showScreen(player,existing);
          else context.session().close();
        });
    if(s!=null){s.menu=view;s.changingView=false;view.onClose(()->{if(!existing.changingView)stop(player.getUniqueId());});}
  }
  private void play(Player player,String alias) throws Exception {
    Path rom=roms().stream().filter(p->p.getFileName().toString().equals(alias)||p.getFileName().toString().equals(alias+".gba")).findFirst()
        .orElseThrow(()->new IllegalArgumentException("Unknown local cartridge"));
    UUID id=player.getUniqueId();
    if(stopping.containsKey(id)&&!stopping.get(id).isDone()){player.sendMessage("Finishing your previous save. Try again shortly.");return;}
    if(sessions.containsKey(id)){stop(id);player.sendMessage("Saved the previous session. Select the cartridge again in a moment.");return;}
    if(sessions.size()+stopping.values().stream().filter(f->!f.isDone()).count()>=plugin.getConfig().getInt("gba.maximum-sessions",2)) {
      player.sendMessage("All emulator slots are occupied.");return;
    }
    var s=new Session();s.title=rom.getFileName().toString().replaceFirst("\\.gba$","");sessions.put(id,s);
    try { showScreen(player,s); }
    catch(RuntimeException error) { s.termination.complete(null);stop(id);throw error; }
    if(s.closed || s.surface == null || !s.surface.isActive()) { s.termination.complete(null);stop(id);return; }
    io.execute(()-> {
      try {
        Path core=NativeCore.install(directory.resolve("native"));
        String hash=NativeCore.hash(Files.readAllBytes(rom));
        if(s.closed){s.termination.complete(null);return;}
        var process=new GbaProcess(core,rom,directory.resolve("saves").resolve(id.toString()).resolve(hash),
            frame->{var surface=s.surface;if(surface!=null&&surface.isActive())surface.submit(frame);},
            error->main(()->{player.sendMessage("GBA worker stopped: "+error);stop(id);}),
            line->plugin.getLogger().fine("GBA "+id+": "+line));
        process.stopped().whenComplete((ignored,error)->s.termination.complete(null));
        s.process=process;if(s.closed)process.close();else if(s.surface==null)process.pause(true);
      }catch(Exception e){s.termination.complete(null);main(()->{player.sendMessage("Could not launch GBA: "+e.getMessage());stop(id);});}
    });
  }
  private void showScreen(Player player,Session s) {
    s.changingView=true;
    if(s.menu!=null){s.menu.close();s.menu=null;}
    var presentation=screen.render(screenData(s.title,60));
    s.surface=dui.openVideoSurface(player,presentation.specification(),new VideoSurfaceOptions(8,false),event->{
      var worker=s.process;
      if(event.type()==SurfaceInput.Type.SLOT) {
        if(event.slot()==7){try{showMenu(player);}catch(Exception e){stop(player.getUniqueId());}return;}
        if(worker!=null) {
          int bit=switch(event.slot()){case 0->3;case 1->2;case 2->10;case 3->11;default->-1;};
          if(bit>=0)worker.pulse(1<<bit);
        }
      }else if(event.type()==SurfaceInput.Type.STATE) {
        if(event.sneak()){stop(player.getUniqueId());return;}
        if(worker!=null)worker.keys((event.forward()?1<<4:0)|(event.backward()?1<<5:0)|(event.left()?1<<6:0)|(event.right()?1<<7:0)
            |(event.jump()?1<<8:0)|(event.sprint()?1:0));
      }
    }).onClose(()->{if(!s.changingView&&!s.closed)stop(player.getUniqueId());});
    s.surface.hud(presentation.hud());s.changingView=false;
    if(s.process!=null)s.process.pause(false);
  }
  private void stop(UUID id) {
    Session s=sessions.remove(id);if(s==null)return;s.closed=true;
    if(s.surface!=null)s.surface.close();if(s.menu!=null)s.menu.close();
    stopping.put(id,s.termination);
    s.termination.whenComplete((ignored,error)->main(()->stopping.remove(id,s.termination)));
    if(s.process!=null)s.process.close();
  }
  @EventHandler public void quit(PlayerQuitEvent e){stop(e.getPlayer().getUniqueId());}
  @Override public void close(){for(UUID id:List.copyOf(sessions.keySet()))stop(id);active=false;io.shutdown();HandlerList.unregisterAll(this);}
}
