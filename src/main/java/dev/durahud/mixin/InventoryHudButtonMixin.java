package dev.durahud.mixin;

import dev.durahud.InventoryHudButton;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 给背包界面（生存背包 / 创造物品栏）挂 HUD 按钮，并支持**按住拖动**换位置。
 *
 * <p>点和拖的区分：按下后移动超过阈值才算拖，否则松开时当点击处理。这样不用等长按计时，
 * 手感即时，也不会因为手抖误判。拖动中的鼠标事件全部截住，避免同时移动物品或转视角。</p>
 *
 * <p>状态用 mixin 加的实例字段，不用 static —— 万一同时存在两个容器界面也不会串。</p>
 */
@Mixin(AbstractContainerScreen.class)
public abstract class InventoryHudButtonMixin {

    /** 移动超过这么多像素才算拖动，避免点按时手抖被当成拖。 */
    @Unique
    private static final int DURAHUD_DRAG_THRESHOLD = 3;

    @Unique
    private Button durahud$button;
    @Unique
    private boolean durahud$dragging;
    @Unique
    private boolean durahud$moved;
    @Unique
    private double durahud$grabX;
    @Unique
    private double durahud$grabY;
    @Unique
    private double durahud$startX;
    @Unique
    private double durahud$startY;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "init", at = @At("TAIL"))
    private void durahud$addHudButton(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        this.durahud$button = null;
        this.durahud$dragging = false;
        this.durahud$moved = false;
        if (!(self instanceof InventoryScreen) && !(self instanceof CreativeModeInventoryScreen)) {
            return;
        }
        this.durahud$button = InventoryHudButton.addTo(self, this.leftPos, this.topPos);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void durahud$onMouseClicked(MouseButtonEvent event, boolean doubled,
                                        CallbackInfoReturnable<Boolean> cir) {
        Button button = this.durahud$button;
        if (button == null || !event.isLeftMouse() || !button.isMouseOver(event.x(), event.y())) {
            return;
        }
        this.durahud$dragging = true;
        this.durahud$moved = false;
        // 记住"按下点相对按钮左上角的偏移"，拖动时保持它，手感才跟手
        this.durahud$grabX = event.x() - button.getX();
        this.durahud$grabY = event.y() - button.getY();
        this.durahud$startX = event.x();
        this.durahud$startY = event.y();
        cir.setReturnValue(Boolean.TRUE);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void durahud$onMouseDragged(MouseButtonEvent event, double deltaX, double deltaY,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!this.durahud$dragging) {
            return;
        }
        Button button = this.durahud$button;
        if (button == null) {
            return;
        }
        if (Math.abs(event.x() - this.durahud$startX) > DURAHUD_DRAG_THRESHOLD
                || Math.abs(event.y() - this.durahud$startY) > DURAHUD_DRAG_THRESHOLD) {
            this.durahud$moved = true;
        }
        button.setX((int) Math.round(event.x() - this.durahud$grabX));
        button.setY((int) Math.round(event.y() - this.durahud$grabY));
        InventoryHudButton.clamp(button, (Screen) (Object) this);
        cir.setReturnValue(Boolean.TRUE);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void durahud$onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!this.durahud$dragging) {
            return;
        }
        this.durahud$dragging = false;
        Button button = this.durahud$button;
        if (button != null) {
            if (this.durahud$moved) {
                // 拖动结束：记住新位置，不打开设置
                InventoryHudButton.persist(button, this.leftPos, this.topPos);
            } else {
                // 没动过：当成点击
                InventoryHudButton.openSettings((Screen) (Object) this);
            }
        }
        cir.setReturnValue(Boolean.TRUE);
    }
}