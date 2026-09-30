package gg.kembel.dui.demo.e2e.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface FixtureMouseAccess {
  @Invoker("onMove")
  void dui$move(long window, double x, double y);

  @Invoker("onButton")
  void dui$button(long window, MouseButtonInfo button, int action);

  @Invoker("onScroll")
  void dui$scroll(long window, double horizontal, double vertical);
}
