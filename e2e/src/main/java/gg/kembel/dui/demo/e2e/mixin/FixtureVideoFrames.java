package gg.kembel.dui.demo.e2e.mixin;

import gg.kembel.dui.demo.e2e.GbaClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class FixtureVideoFrames {
  @Inject(method="runTick",at=@At("RETURN"))
  private void dui$observeFrame(boolean render,CallbackInfo ci) {
    if(render&&GbaClient.instance!=null)GbaClient.instance.rendered((Minecraft)(Object)this);
  }
}
