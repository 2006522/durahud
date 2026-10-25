package dev.durahud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

/**
 * 在背包界面左侧加一个「HUD」按钮，点一下开位置编辑界面。
 *
 * <p>逻辑放在这里而不是 mixin 里，是因为 mixin 包（{@code dev.durahud.mixin}）
 * 访问不到 {@code Screen} 的 {@code protected} 成员，也不该去碰包内可见的 {@link Log}。
 * mixin 只负责"在 init 末尾报一下面板坐标"，剩下的都在这儿。</p>
 */
public final class InventoryHudButton {

    private static final int WIDTH = 26;
    private static final int HEIGHT = 20;
    /** 按钮与面板之间留的缝。 */
    private static final int GAP = 4;

    private InventoryHudButton() {
    }

    /**
     * @param screen    当前的容器界面
     * @param panelLeft 面板左上角的屏幕坐标（{@code leftPos}）
     * @param panelTop  面板顶边的屏幕坐标（{@code topPos}）
     */
    public static void addTo(Screen screen, int panelLeft, int panelTop) {
        // 放面板左侧外面，不压任何原版格子；界面太窄就贴住左边距
        int x = panelLeft - WIDTH - GAP;
        if (x < 2) {
            x = 2;
        }
        int y = panelTop + GAP;

        Button button = Button.builder(Component.literal("HUD"), b -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null) {
                        // 把背包当成"父界面"传过去，关掉设置后回到背包而不是直接退回游戏
                        mc.setScreenAndShow(new HudSettingsScreen(screen));
                    }
                })
                .bounds(x, y, WIDTH, HEIGHT)
                .tooltip(Component.translatable("durahud.button.tip"))
                .build();

        try {
            // addRenderableWidget 是 protected，mixin 里没法直接调，反射拿一次最省事；
            // 拿不到就静默跳过——按钮不出现总比崩游戏好
            Method m = Screen.class.getDeclaredMethod("addRenderableWidget", GuiEventListener.class);
            m.setAccessible(true);
            m.invoke(screen, button);
        } catch (Exception e) {
            Log.warn("背包按钮添加失败: " + e.getMessage());
        }
    }
}