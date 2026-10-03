package gg.kembel.dui.demo.e2e.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Supplies the same focus callback GLFW emits, solely in the automated graphical client. */
@Mixin(Window.class)
public interface FixtureWindowAccess {
  @Invoker("onFocus") void dui$focus(long window, boolean focused);
}
