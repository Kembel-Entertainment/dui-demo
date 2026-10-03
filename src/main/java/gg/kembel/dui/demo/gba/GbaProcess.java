package gg.kembel.dui.demo.gba;

import gg.kembel.dui.core.video.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.LockSupport;
import java.util.function.*;

/** Blocking IPC lives on daemon threads; Paper handlers only update small bounded mailboxes. */
final class GbaProcess implements AutoCloseable {
  private final Process process;
  private final DataOutputStream output;
  private final AtomicInteger keys=new AtomicInteger(),pulse=new AtomicInteger();
  private final AtomicReference<Boolean> pause=new AtomicReference<>();
  private final AtomicBoolean save=new AtomicBoolean(),closed=new AtomicBoolean();
  private final CompletableFuture<Void> stopped=new CompletableFuture<>();
  private volatile String core="starting";
  private volatile double fps;
  private final AtomicLong frames=new AtomicLong(),saved=new AtomicLong();
  GbaProcess(Path library,Path rom,Path directory,Consumer<VideoFrame> frame,Consumer<String> failure,Consumer<String> log) throws Exception {
    String java=Path.of(System.getProperty("java.home"),"bin","java").toString();
    String classpath=Path.of(GbaWorker.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    process=new ProcessBuilder(java,"--enable-native-access=ALL-UNNAMED","-Xms16m","-Xmx128m","-cp",classpath,
        GbaWorker.class.getName(),library.toString(),rom.toString(),directory.toString()).start();
    output=new DataOutputStream(new BufferedOutputStream(process.getOutputStream()));
    Thread.ofPlatform().daemon().name("gba-worker-stderr").start(()-> {
      try(var reader=process.errorReader()) { String line;while((line=reader.readLine())!=null)log.accept(line); }
      catch(IOException ignored) {}
    });
    Thread.ofPlatform().daemon().name("gba-worker-reader").start(()-> {
      try(var input=new DataInputStream(new BufferedInputStream(process.getInputStream()))) {
        while(true) {
          var message=GbaWire.read(input);var data=message.input();
          switch(message.type()) {
            case GbaWire.HELLO -> {
              if(data.readInt()!=GbaWire.VERSION)throw new IOException("Worker protocol mismatch");
              core=data.readUTF();data.readUTF();fps=data.readDouble();
            }
            case GbaWire.FRAME -> {
              long sequence=data.readLong();int width=data.readUnsignedShort(),height=data.readUnsignedShort();
              if(width!=240||height!=160||message.bytes().length!=12+width*height*4)throw new IOException("Worker frame dimensions");
              int[] pixels=new int[width*height];for(int i=0;i<pixels.length;i++)pixels[i]=data.readInt();
              frames.incrementAndGet();if(!closed.get())frame.accept(new VideoFrame(width,height,PixelFormat.RGB888,sequence,pixels));
            }
            case GbaWire.SAVED -> saved.incrementAndGet();
            case GbaWire.ERROR -> throw new IOException(data.readUTF());
            default -> throw new IOException("Unknown worker response");
          }
        }
      } catch(Exception e) { if(!closed.get())failure.accept(e.toString()); }
      finally { close(); }
    });
    Thread.ofPlatform().daemon().name("gba-worker-writer").start(()-> {
      int last=-1;
      try {
        while(!closed.get()) {
          int mask=keys.get();if(last!=mask){writeMask(GbaWire.KEYS,mask);last=mask;}
          int press=pulse.getAndSet(0);if(press!=0)writeMask(GbaWire.PULSE,press);
          Boolean paused=pause.getAndSet(null);if(paused!=null)GbaWire.write(output,GbaWire.PAUSE,GbaWire.payload(out->out.writeBoolean(paused)));
          if(save.getAndSet(false))GbaWire.write(output,GbaWire.SAVE,new byte[0]);
          LockSupport.parkNanos(1_000_000);
        }
        GbaWire.write(output,GbaWire.STOP,new byte[0]);
      } catch(IOException e) { if(!closed.get())failure.accept(e.toString());close(); }
    });
  }
  private void writeMask(int type,int mask) throws IOException { GbaWire.write(output,type,GbaWire.payload(out->out.writeInt(mask))); }
  void keys(int mask) { keys.set(mask); }
  void pulse(int mask) { pulse.getAndUpdate(previous->previous|mask); }
  void pause(boolean value) { keys.set(0);pulse.set(0);pause.set(value); }
  void save() { save.set(true); }
  String statistics() { return core+" / "+String.format(java.util.Locale.ROOT,"%.2f Hz",fps)+" / frames "+frames.get()+" / keys "+keys.get()+" / checkpoints "+saved.get(); }
  CompletableFuture<Void> stopped() { return stopped; }
  @Override public void close() {
    if(!closed.compareAndSet(false,true))return;
    Thread.ofPlatform().daemon().name("gba-worker-stop").start(()-> {
      try { if(!process.waitFor(3,TimeUnit.SECONDS)) { process.destroyForcibly();process.waitFor(); } }
      catch(InterruptedException e){process.destroyForcibly();Thread.currentThread().interrupt();}
      finally { stopped.complete(null); }
    });
  }
}
