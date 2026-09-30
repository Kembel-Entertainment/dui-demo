package gg.kembel.dui.demo.e2e.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface FixtureKeyboardAccess {
  @Invoker("charTyped")
  void dui$character(long window, CharacterEvent event);

  @Invoker("keyPress")
  void dui$key(long window, int action, KeyEvent event);
}
