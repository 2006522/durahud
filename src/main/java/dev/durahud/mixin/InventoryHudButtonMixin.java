package dev.durahud.mixin;

import dev.durahud.InventoryHudButton;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 给背包界面（生存背包 / 创造物品栏）挂一个 HUD 按钮。
 *
 * <p>只在 {@code init} 末尾动手：这时候 {@code leftPos} / {@code topPos} 已经算好，
 * 面板位置是最新的。其它界面（箱子、熔炉……）一律不管，保持原版干净。</p>
 */
@Mixin(AbstractContainerScreen.class)
public abstract class InventoryHudButtonMixin {

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "init", at = @At("TAIL"))
    private void durahud$addHudButton(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (!(self instanceof InventoryScreen) && !(self instanceof CreativeModeInventoryScreen)) {
            return;
        }
        InventoryHudButton.addTo(self, this.leftPos, this.topPos);
    }
}