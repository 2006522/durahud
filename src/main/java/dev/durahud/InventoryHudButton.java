package dev.durahud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

/**
 * 背包界面左侧那个「HUD」按钮：轻点开设置，按住拖动可以换位置。
 *
 * <p>位置存的是**相对面板左上角的偏移**（{@code button_dx}/{@code button_dy}），
 * 不是绝对屏幕坐标 —— 换 GUI 缩放或分辨率后按钮还贴在面板左边，不会跑丢。</p>
 *
 * <p>逻辑放在这里而不是 mixin 里，是因为 mixin 包（{@code dev.durahud.mixin}）
 * 访问不到 {@code Screen} 的 {@code protected} 成员，也不该碰包内可见的 {@link Log}。
 * mixin 只负责把鼠标事件转过来。</p>
 */
public final class InventoryHudButton {

    private static final int WIDTH = 26;
    private static final int HEIGHT = 20;

    private InventoryHudButton() {
    }

    /**
     * 在容器界面里加按钮。
     *
     * @return 加好的按钮；反射失败时返回 {@code null}（按钮不出现，但绝不崩游戏）
     */
    public static Button addTo(Screen screen, int panelLeft, int panelTop) {
        Config cfg = Config.get();
        int wantX = panelLeft + cfg.getButtonDx();
        int wantY = panelTop + cfg.getButtonDy();

        Button button = Button.builder(Component.literal("HUD"), b -> openSettings(screen))
                .bounds(wantX, wantY, WIDTH, HEIGHT)
                .tooltip(Component.translatable("durahud.button.tip"))
                .build();
        clamp(button, screen);

        // 位置存的是相对面板的偏移，一般不会被收；真被屏幕边缘收掉了就把结果写回去，
        // 免得每次进来都在"存的位置"和"实际显示位置"之间来回跳
        if (button.getX() != wantX || button.getY() != wantY) {
            cfg.setButtonDx(button.getX() - panelLeft);
            cfg.setButtonDy(button.getY() - panelTop);
            cfg.save();
        }

        try {
            // addRenderableWidget 是 protected，mixin 里没法直接调，反射拿一次最省事
            Method m = Screen.class.getDeclaredMethod("addRenderableWidget", GuiEventListener.class);
            m.setAccessible(true);
            m.invoke(screen, button);
            return button;
        } catch (Exception e) {
            Log.warn("背包按钮添加失败: " + e.getMessage());
            return null;
        }
    }

    /** 打开设置界面，把当前界面当成父界面（关掉时回到它）。 */
    public static void openSettings(Screen from) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreenAndShow(new HudSettingsScreen(from));
        }
    }

    /** 别让按钮被拖出屏幕，否则就找不回来了。 */
    public static void clamp(Button button, Screen screen) {
        int maxX = Math.max(0, screen.width - WIDTH);
        int maxY = Math.max(0, screen.height - HEIGHT);
        button.setX(Math.max(0, Math.min(maxX, button.getX())));
        button.setY(Math.max(0, Math.min(maxY, button.getY())));
    }

    /** 拖完把「相对面板的偏移」写回配置。 */
    public static void persist(Button button, int panelLeft, int panelTop) {
        Config cfg = Config.get();
        cfg.setButtonDx(button.getX() - panelLeft);
        cfg.setButtonDy(button.getY() - panelTop);
        cfg.save();
    }
}