package gg.kembel.dui.demo.gba;

import java.io.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GbaWireTest {
  @Test void messagesRoundtripAndRejectCorruptionAndTruncation() throws Exception {
    var bytes=new ByteArrayOutputStream();
    GbaWire.write(new DataOutputStream(bytes),GbaWire.KEYS,GbaWire.payload(out->out.writeInt(0x105)));
    var message=GbaWire.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    assertEquals(GbaWire.KEYS,message.type());assertEquals(0x105,message.input().readInt());
    byte[] broken=bytes.toByteArray();broken[6]^=1;
    assertThrows(IOException.class,()->GbaWire.read(new DataInputStream(new ByteArrayInputStream(broken))));
    assertThrows(IOException.class,()->GbaWire.read(new DataInputStream(new ByteArrayInputStream(new byte[]{0,32,0,1}))));
  }
}
