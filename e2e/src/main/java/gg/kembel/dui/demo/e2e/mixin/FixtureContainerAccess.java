package gg.kembel.dui.demo.e2e.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface FixtureContainerAccess {
  @Accessor("leftPos")
  int dui$left();

  @Accessor("topPos")
  int dui$top();
}
