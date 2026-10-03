package gg.kembel.dui.demo.gba;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import static java.lang.foreign.ValueLayout.*;

/** Small frontend for the existing, unmodified mGBA Libretro binary, used ONLY in a worker. */
final class LibretroCore implements AutoCloseable {
  record Frame(int width,int height,int[] rgb) {}
  private final Arena arena=Arena.ofShared();
  private final Linker linker=Linker.nativeLinker();
  private final SymbolLookup symbols;
  private final Map<String,MethodHandle> functions=new HashMap<>();
  private final Map<String,String> variables=new HashMap<>();
  private final Map<String,MemorySegment> strings=new HashMap<>();
  private final MemorySegment directory;
  private final IntSupplier keys;
  private final Consumer<Frame> frames;
  private int pixelFormat=2;
  private boolean initialized,loaded;
  private volatile Throwable callbackFailure;
  final String version;
  final double fps;
  LibretroCore(Path library,Path rom,Path saves,IntSupplier keys,Consumer<Frame> frames) throws Throwable {
    this.keys=keys;this.frames=frames;
    Files.createDirectories(saves);directory=arena.allocateFrom(saves.toAbsolutePath().toString());
    symbols=SymbolLookup.libraryLookup(library.toAbsolutePath(),arena);
    callback("retro_set_environment","environment",byte.class,FunctionDescriptor.of(JAVA_BYTE,JAVA_INT,ADDRESS),int.class,MemorySegment.class);
    callback("retro_set_video_refresh","video",void.class,FunctionDescriptor.ofVoid(ADDRESS,JAVA_INT,JAVA_INT,JAVA_LONG),MemorySegment.class,int.class,int.class,long.class);
    callback("retro_set_input_poll","poll",void.class,FunctionDescriptor.ofVoid());
    callback("retro_set_input_state","input",short.class,FunctionDescriptor.of(JAVA_SHORT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT),int.class,int.class,int.class,int.class);
    callback("retro_set_audio_sample","audio",void.class,FunctionDescriptor.ofVoid(JAVA_SHORT,JAVA_SHORT),short.class,short.class);
    callback("retro_set_audio_sample_batch","audioBatch",long.class,FunctionDescriptor.of(JAVA_LONG,ADDRESS,JAVA_LONG),MemorySegment.class,long.class);
    var info=arena.allocate(32,8);
    invoke("retro_get_system_info",FunctionDescriptor.ofVoid(ADDRESS),info);
    version=cstring(info.get(ADDRESS,0))+" "+cstring(info.get(ADDRESS,8));
    invoke("retro_init",FunctionDescriptor.ofVoid());initialized=true;
    byte[] bytes=Files.readAllBytes(rom);
    if(bytes.length<192||bytes.length>32*1024*1024)throw new IllegalArgumentException("Expected a GBA cartridge, up to 32 MiB");
    var data=arena.allocateFrom(JAVA_BYTE,bytes);
    var game=arena.allocate(32,8);
    game.set(ADDRESS,0,arena.allocateFrom(rom.toAbsolutePath().toString()));
    game.set(ADDRESS,8,data);game.set(JAVA_LONG,16,bytes.length);game.set(ADDRESS,24,MemorySegment.NULL);
    loaded=(byte)invoke("retro_load_game",FunctionDescriptor.of(JAVA_BYTE,ADDRESS),game)!=0;
    if(!loaded)throw new IllegalStateException("mGBA rejected the cartridge");
    invoke("retro_set_controller_port_device",FunctionDescriptor.ofVoid(JAVA_INT,JAVA_INT),0,1);
    var av=arena.allocate(40,8);
    invoke("retro_get_system_av_info",FunctionDescriptor.ofVoid(ADDRESS),av);
    fps=av.get(JAVA_DOUBLE,24);
    if(!Double.isFinite(fps)||fps<1||fps>240)throw new IllegalStateException("Invalid core timing");
    check();
  }
  private Object invoke(String name,FunctionDescriptor signature,Object... args) throws Throwable {
    MethodHandle handle=functions.computeIfAbsent(name,n->linker.downcallHandle(symbols.find(n).orElseThrow(),signature));
    return handle.invokeWithArguments(args);
  }
  private void callback(String setter,String method,Class<?> result,FunctionDescriptor signature,Class<?>... arguments) throws Throwable {
    var handle=MethodHandles.lookup().findVirtual(LibretroCore.class,method,MethodType.methodType(result,arguments)).bindTo(this);
    invoke(setter,FunctionDescriptor.ofVoid(ADDRESS),linker.upcallStub(handle,signature,arena));
  }
  private static String cstring(MemorySegment pointer) { return pointer.address()==0?"":pointer.reinterpret(4096).getString(0); }
  private byte environment(int command,MemorySegment raw) {
    try {
      var p=raw.address()==0?raw:raw.reinterpret(32);
      switch(command) {
        case 3 -> p.set(JAVA_BYTE,0,(byte)1); // duplicate frames supported
        case 9,31 -> p.set(ADDRESS,0,directory);
        case 10 -> { int format=p.get(JAVA_INT,0);if(format<0||format>2)return 0;pixelFormat=format; }
        case 15 -> {
          String key=cstring(p.get(ADDRESS,0));
          String value=switch(key) {
            case "mgba_use_bios","mgba_color_correction","mgba_interframe_blending" -> "OFF";
            case "mgba_frameskip" -> "disabled";
            default -> variables.get(key);
          };
          p.set(ADDRESS,8,value==null?MemorySegment.NULL:strings.computeIfAbsent(value,arena::allocateFrom));
          return (byte)(value==null?0:1);
        }
        case 16 -> {
          var list=raw.reinterpret(16*512);
          for(int i=0;i<512;i++) {
            var key=list.get(ADDRESS,16L*i);if(key.address()==0)break;
            String description=cstring(list.get(ADDRESS,16L*i+8));
            int sep=description.indexOf(';');if(sep>=0)variables.put(cstring(key),description.substring(sep+1).trim().split("\\|")[0]);
          }
        }
        case 17 -> p.set(JAVA_BYTE,0,(byte)0);
        case 52 -> p.set(JAVA_INT,0,0); // legacy options registration, supported by mGBA
        case 32,37,11,18 -> {} // core geometry/performance/input descriptions
        default -> { return 0; }
      }
      return 1;
    } catch(Throwable e) { callbackFailure=e;return 0; }
  }
  private void video(MemorySegment raw,int width,int height,long pitch) {
    try {
      if(raw.address()==0||raw.address()==-1)return;
      if(width!=240||height!=160||pitch<width*(pixelFormat==1?4:2)||pitch>65536)
        throw new IllegalArgumentException("Only native 240x160 GBA output is supported");
      var data=raw.reinterpret(pitch*height);var rgb=new int[width*height];
      for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
        int v,r,g,b;
        if(pixelFormat==1)rgb[y*width+x]=data.get(JAVA_INT,y*pitch+x*4L)&0xffffff;
        else {
          v=data.get(JAVA_SHORT,y*pitch+x*2L)&65535;
          b=v&31;r=(v>>(pixelFormat==2?11:10))&31;
          g=(v>>5)&(pixelFormat==2?63:31);
          r=(r<<3)|(r>>2);b=(b<<3)|(b>>2);
          g=pixelFormat==2?(g<<2)|(g>>4):(g<<3)|(g>>2);
          rgb[y*width+x]=(r<<16)|(g<<8)|b;
        }
      }
      frames.accept(new Frame(width,height,rgb));
    } catch(Throwable e) { callbackFailure=e; }
  }
  private void poll() {}
  private short input(int port,int device,int index,int id) {
    if(port!=0||device!=1||index!=0)return 0;
    return (short)((keys.getAsInt()>>>id)&1);
  }
  private void audio(short left,short right) {}
  private long audioBatch(MemorySegment samples,long frames) { return frames; }
  private void check() { if(callbackFailure!=null)throw new IllegalStateException("Core callback failed",callbackFailure); }
  void run() throws Throwable { invoke("retro_run",FunctionDescriptor.ofVoid());check(); }
  byte[] serialize() throws Throwable {
    long size=(long)invoke("retro_serialize_size",FunctionDescriptor.of(JAVA_LONG));
    if(size<1||size>16*1024*1024)throw new IllegalStateException("Invalid state size");
    try(var temporary=Arena.ofConfined()) {
      var memory=temporary.allocate(size);
      if((byte)invoke("retro_serialize",FunctionDescriptor.of(JAVA_BYTE,ADDRESS,JAVA_LONG),memory,size)==0)
        throw new IllegalStateException("Cannot serialize core");
      return memory.toArray(JAVA_BYTE);
    }
  }
  boolean restore(byte[] bytes) throws Throwable {
    try(var temporary=Arena.ofConfined()) {
      return (byte)invoke("retro_unserialize",FunctionDescriptor.of(JAVA_BYTE,ADDRESS,JAVA_LONG),temporary.allocateFrom(JAVA_BYTE,bytes),(long)bytes.length)!=0;
    }
  }
  byte[] memory(int id) throws Throwable {
    long size=(long)invoke("retro_get_memory_size",FunctionDescriptor.of(JAVA_LONG,JAVA_INT),id);
    if(size<1)return new byte[0];if(size>16*1024*1024)throw new IllegalStateException("Invalid save memory size");
    var address=(MemorySegment)invoke("retro_get_memory_data",FunctionDescriptor.of(ADDRESS,JAVA_INT),id);
    return address.address()==0?new byte[0]:address.reinterpret(size).toArray(JAVA_BYTE);
  }
  void memory(int id,byte[] bytes) throws Throwable {
    long size=(long)invoke("retro_get_memory_size",FunctionDescriptor.of(JAVA_LONG,JAVA_INT),id);
    if(bytes.length!=size)return;
    var address=(MemorySegment)invoke("retro_get_memory_data",FunctionDescriptor.of(ADDRESS,JAVA_INT),id);
    if(address.address()!=0)MemorySegment.copy(bytes,0,address.reinterpret(size),JAVA_BYTE,0,bytes.length);
  }
  @Override public void close() {
    try {
      if(loaded)invoke("retro_unload_game",FunctionDescriptor.ofVoid());
      if(initialized)invoke("retro_deinit",FunctionDescriptor.ofVoid());
    } catch(Throwable ignored) {} finally { arena.close(); }
  }
}
