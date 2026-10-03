package gg.kembel.dui.demo.gba;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.LockSupport;

/** Isolated worker entry point: a native crash never executes in the Paper process. */
public final class GbaWorker {
  private static void atomic(Path file,byte[] bytes) throws IOException {
    Path tmp=file.resolveSibling(file.getFileName()+".tmp");
    try(var out=FileChannel.open(tmp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)) {
      var buffer=java.nio.ByteBuffer.wrap(bytes);while(buffer.hasRemaining())out.write(buffer);out.force(true);
    }
    if(Files.exists(file))Files.copy(file,file.resolveSibling(file.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);
    try { Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
    catch(AtomicMoveNotSupportedException e) { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING); }
  }
  private static void save(LibretroCore core,Path directory,String identity) throws Throwable {
    for(int id=0;id<=1;id++) {
      byte[] memory=core.memory(id);if(memory.length>0)atomic(directory.resolve(id==0?"battery.srm":"rtc.bin"),memory);
    }
    // Version + ROM identity live inside the atomic snapshot, so interrupted writes cannot mismatch metadata.
    byte[] state=core.serialize();
    atomic(directory.resolve("resume.state"),GbaWire.payload(out->{out.writeUTF(identity);out.writeInt(state.length);out.write(state);}));
  }
  private static void restore(LibretroCore core,Path directory,String identity) throws Throwable {
    for(int id=0;id<=1;id++) {
      Path p=directory.resolve(id==0?"battery.srm":"rtc.bin");
      if(Files.exists(p)&&Files.size(p)<=16*1024*1024)core.memory(id,Files.readAllBytes(p));
    }
    for(String name:new String[]{"resume.state","resume.state.bak"}) {
      Path p=directory.resolve(name);if(!Files.exists(p)||Files.size(p)>16*1024*1024)continue;
      try(var in=new DataInputStream(Files.newInputStream(p))) {
        if(!in.readUTF().equals(identity))continue;
        int length=in.readInt();if(length<1||length>16*1024*1024)continue;
        byte[] bytes=in.readNBytes(length);
        if(bytes.length==length&&core.restore(bytes))return;
      } catch(IOException e) { System.err.println("Ignoring incomplete checkpoint: "+name); }
    }
  }
  public static void main(String[] args) throws Throwable {
    if(args.length!=3)throw new IllegalArgumentException("library rom save-directory");
    var input=new DataInputStream(new BufferedInputStream(System.in));
    var output=new DataOutputStream(new BufferedOutputStream(System.out));
    var stop=new AtomicBoolean();var paused=new AtomicBoolean();var checkpoint=new AtomicBoolean();
    var held=new AtomicInteger();var pulses=new AtomicLongArray(16);
    var latest=new AtomicReference<byte[]>();
    Thread writer=Thread.ofPlatform().daemon().name("gba-output").start(()-> {
      try { while(!stop.get()||latest.get()!=null) {
        byte[] frame=latest.getAndSet(null);
        if(frame==null)LockSupport.parkNanos(1_000_000);else GbaWire.write(output,GbaWire.FRAME,frame);
      }}catch(IOException e){stop.set(true);}
    });
    Thread.ofPlatform().daemon().name("gba-input").start(()-> {
      try { while(!stop.get()) {
        var message=GbaWire.read(input);var data=message.input();
        switch(message.type()) {
          case GbaWire.KEYS -> held.set(data.readInt()&65535);
          case GbaWire.PULSE -> { int mask=data.readInt();long until=System.nanoTime()+100_000_000;
            for(int i=0;i<16;i++)if((mask&(1<<i))!=0)pulses.set(i,until); }
          case GbaWire.PAUSE -> { paused.set(data.readBoolean());held.set(0);for(int i=0;i<16;i++)pulses.set(i,0); }
          case GbaWire.SAVE -> checkpoint.set(true);
          case GbaWire.STOP -> stop.set(true);
          default -> throw new IOException("Unknown worker command");
        }
      }}catch(IOException e){stop.set(true);}
    });
    Path rom=Path.of(args[1]),directory=Path.of(args[2]);
    String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(rom)));
    var sequence=new AtomicLong();
    try(var core=new LibretroCore(Path.of(args[0]),rom,directory,()->{
      int mask=held.get();long now=System.nanoTime();
      for(int i=0;i<16;i++)if(pulses.get(i)>now)mask|=1<<i;return mask;
    },frame->{
      try { latest.set(GbaWire.payload(out->{out.writeLong(sequence.getAndIncrement());
        out.writeShort(frame.width());out.writeShort(frame.height());for(int pixel:frame.rgb())out.writeInt(pixel);}));
      }catch(IOException impossible){throw new UncheckedIOException(impossible);}
    })) {
      String identity=hash+":"+core.version;
      restore(core,directory,identity);
      GbaWire.write(output,GbaWire.HELLO,GbaWire.payload(out->{out.writeInt(GbaWire.VERSION);out.writeUTF(core.version);out.writeUTF(hash);out.writeDouble(core.fps);}));
      long period=Math.round(1_000_000_000/core.fps),next=System.nanoTime(),lastSave=next;
      while(!stop.get()) {
        long now=System.nanoTime();
        if(checkpoint.getAndSet(false)||now-lastSave>=30_000_000_000L) {
          save(core,directory,identity);lastSave=now;
          GbaWire.write(output,GbaWire.SAVED,new byte[0]);
        }
        if(!paused.get())core.run();
        next+=period;long wait=next-System.nanoTime();
        if(wait>0)LockSupport.parkNanos(wait);else if(wait < -period*3)next=System.nanoTime();
      }
      save(core,directory,identity);GbaWire.write(output,GbaWire.SAVED,new byte[0]);
    } catch(Throwable e) {
      try { GbaWire.write(output,GbaWire.ERROR,GbaWire.payload(out->out.writeUTF(e.toString()))); } catch(IOException ignored) {}
      throw e;
    } finally { stop.set(true);writer.join(250); }
  }
}
