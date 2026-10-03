package gg.kembel.dui.demo.e2e;

import com.google.gson.*;
import java.nio.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import com.mojang.blaze3d.opengl.GlTexture;
import org.lwjgl.opengl.*;

/** Observes ordinary vanilla rendering: the fixture adds no video renderer or custom input packets. */
public final class GbaClient {
  public static GbaClient instance;
  private int ticks,stage,changed,fbo,last=-1,samples,visible;
  private long start,measured;
  private String inventory;
  private final ByteBuffer pixel=org.lwjgl.BufferUtils.createByteBuffer(4);
  void initialize(){instance=this;ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
  public void rendered(Minecraft mc) {
    if(stage!=2||mc.player==null||!mc.player.isPassenger()||mc.gui.screen()!=null)return;
    var target=mc.gameRenderer.mainRenderTarget();
    if(!(target.getColorTexture() instanceof GlTexture texture))return;
    int previous=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    if(fbo==0)fbo=GL30.glGenFramebuffers();
    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,fbo);
    GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture.glId(),0);
    pixel.clear();GL11.glReadPixels(target.width/2,target.height/2+30,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
    int value=(Byte.toUnsignedInt(pixel.get(0))<<16)|(Byte.toUnsignedInt(pixel.get(1))<<8)|Byte.toUnsignedInt(pixel.get(2));
    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,previous);
    samples++;if(value!=last){visible++;last=value;}
    if(start==0)start=System.nanoTime();
  }
  private void step(){stage++;changed=ticks;System.out.println("GBA_TEST_STAGE "+stage);}
  private void shot(Minecraft mc,String name) {
    Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(Paths.output().resolve(name+".png"));}catch(Exception e){throw new RuntimeException(e);}});
  }
  private static String inventory(Minecraft mc) {
    StringBuilder s=new StringBuilder();for(int i=0;i<mc.player.getInventory().getContainerSize();i++)s.append(mc.player.getInventory().getItem(i)).append(';');return s.toString();
  }
  private void tick(Minecraft mc) {
    ticks++;mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
    try {
      if(stage==2&&(ticks-changed)%80==0){
        System.out.println("GBA_DEBUG screen="+mc.gui.screen()+" vehicle="+(mc.player==null?null:mc.player.getVehicle())+" start="+start);
        shot(mc,"debug");mc.player.connection.sendCommand("gba stats");
      }
      if(ticks>800)throw new IllegalStateException("GBA test timeout, stage "+stage);
      if(mc.gui.overlay()!=null||ticks-changed<10)return;
      if(stage==0&&ticks>60) {
        mc.options.pauseOnLostFocus=false;mc.options.framerateLimit().set(120);mc.options.enableVsync().set(false);
        mc.getWindow().setWindowed(1280,900);mc.options.guiScale().set(2);mc.resizeGui();
        var server=new ServerData("GBA fixture",Paths.server(),ServerData.Type.OTHER);
        server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(server.ip),server,false,null);step();
      }else if(stage==1&&mc.player!=null&&mc.gui.screen() instanceof DialogScreen<?>) {
        inventory=inventory(mc);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.player.connection.sendCommand("gba play color-controls");step();
      }else if(stage==2&&start!=0&&System.nanoTime()-start>6_000_000_000L) {
        measured=System.nanoTime()-start;shot(mc,"fullscreen");
        if(visible<60)throw new IllegalStateException("No moving video image: "+visible+" changes / "+samples+" samples");
        mc.options.keyJump.setDown(true);step();
      }else if(stage==3&&ticks-changed>30) {
        mc.player.connection.sendCommand("gba stats");mc.options.keyJump.setDown(false);shot(mc,"input-a");
        mc.player.setYRot(mc.player.getYRot()+180);mc.player.setXRot(70);step();
      }else if(stage==4&&ticks-changed>30) {
        shot(mc,"camera-turned");mc.player.connection.sendCommand("gba menu");step();
      }else if(stage==5&&mc.gui.screen() instanceof DialogScreen<?>) {
        if(mc.player.isPassenger())throw new IllegalStateException("Camera not restored in menu");
        mc.player.connection.sendCommand("gba resume");step();
      }else if(stage==6&&ticks-changed>35&&mc.player.isPassenger()) {
        mc.options.guiScale().set(0);mc.resizeGui();shot(mc,"auto-scale");mc.player.connection.sendCommand("gba close");step();
      }else if(stage==7&&ticks-changed>40) {
        if(mc.getCameraEntity()!=mc.player||mc.player.isPassenger())throw new IllegalStateException("Seat or camera leaked");
        if(!inventory.equals(inventory(mc)))throw new IllegalStateException("Inventory changed");
        var result=new JsonObject();result.addProperty("passed",true);result.addProperty("steps",8);result.addProperty("muted",true);
        result.addProperty("visibleChanges",visible);result.addProperty("renderSamples",samples);
        result.addProperty("visibleFps",visible/(measured/1_000_000_000.0));result.addProperty("inventoryUnchanged",true);
        Files.writeString(Paths.output().resolve("client-result.json"),result.toString());
        if(fbo!=0)GL30.glDeleteFramebuffers(fbo);System.out.println("GBA_TEST_COMPLETE "+result);mc.stop();stage=8;
      }
    }catch(Exception e){try{Files.writeString(Paths.output().resolve("failure.txt"),e.toString());}catch(Exception ignored){}e.printStackTrace();mc.stop();stage=8;}
  }
}
