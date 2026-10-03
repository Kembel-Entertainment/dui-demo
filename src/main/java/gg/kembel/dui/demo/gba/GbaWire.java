package gg.kembel.dui.demo.gba;

import java.io.*;
import java.util.zip.CRC32;

/** Versioned, length-limited IPC. Protocol payloads use DataInput/DataOutput big endian. */
public final class GbaWire {
  public static final int VERSION=1, HELLO=1, FRAME=2, SAVED=3, ERROR=4,
      KEYS=10, PULSE=11, PAUSE=12, SAVE=13, STOP=14;
  public static final int MAXIMUM=2*1024*1024;
  public record Message(int type, byte[] bytes) {
    public DataInputStream input() { return new DataInputStream(new ByteArrayInputStream(bytes)); }
  }
  @FunctionalInterface public interface Payload { void write(DataOutputStream output) throws IOException; }
  public static byte[] payload(Payload writer) throws IOException {
    var bytes=new ByteArrayOutputStream();
    writer.write(new DataOutputStream(bytes)); return bytes.toByteArray();
  }
  public static void write(DataOutputStream output,int type,byte[] bytes) throws IOException {
    if(bytes.length>MAXIMUM)throw new IOException("IPC message too large");
    var crc=new CRC32();crc.update(type);crc.update(bytes);
    synchronized(output) {
      output.writeInt(bytes.length);output.writeByte(type);output.write(bytes);
      output.writeInt((int)crc.getValue());output.flush();
    }
  }
  public static Message read(DataInputStream input) throws IOException {
    int length=input.readInt();
    if(length<0||length>MAXIMUM)throw new IOException("Invalid IPC length");
    int type=input.readUnsignedByte();byte[] bytes=input.readNBytes(length);
    if(bytes.length!=length)throw new EOFException();
    var crc=new CRC32();crc.update(type);crc.update(bytes);
    if(input.readInt()!=(int)crc.getValue())throw new IOException("IPC checksum mismatch");
    return new Message(type,bytes);
  }
}
