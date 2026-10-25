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
 * <p>位置存的是<b>绝对 GUI 坐标</b>（{@code button_x}/{@code button_y}），跟 HUD 本身一样。
 * 之前存的是「相对面板左上角的偏移」，看着更讲究，实际会出问题：背包面板的原点在
 * 生存背包（176 宽，开配方书还会整体左移）和创造物品栏（195 宽、面板更高）里不一样，
 * 同一个偏移量算出来的屏幕位置差十几像素，玩家看到的就是
 * 「我拖好了、也保存了，怎么换个界面又是另一个地方」。绝对坐标不受面板位置影响。</p>
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
     * @param panelLeft 面板左边缘，只在第一次落位（老配置迁移 / 恢复默认位置）时用来算绝对坐标
     * @param panelTop  面板上边缘，同上
     * @return 加好的按钮；反射失败时返回 {@code null}（按钮不出现，但绝不崩游戏）
     */
    public static Button addTo(Screen screen, int panelLeft, int panelTop) {
        Config cfg = Config.get();
        // 第一次用（或刚恢复默认位置）：按面板左边的默认偏移落一次位，之后一律用绝对坐标
        cfg.ensureButtonPos(panelLeft, panelTop);

        int wantX = cfg.getButtonX();
        int wantY = cfg.getButtonY();

        Button button = Button.builder(Component.literal("HUD"), b -> openSettings(screen))
                .bounds(wantX, wantY, WIDTH, HEIGHT)
                .tooltip(Component.translatable("durahud.button.tip"))
                .build();
        clamp(button, screen);

        // 换分辨率 / 界面缩放后绝对坐标可能落到屏幕外，收回来并写回配置，
        // 否则每次进来都在「存的位置」和「实际显示位置」之间来回跳
        if (button.getX() != wantX || button.getY() != wantY) {
            cfg.setButtonX(button.getX());
            cfg.setButtonY(button.getY());
            cfg.save();
            Log.info("按钮超出屏幕，已收回并保存到 " + button.getX() + "," + button.getY());
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

    /**
     * 把按钮摆到「鼠标位置 - 抓取偏移」，并收进屏幕。
     *
     * <p>拖动过程和松手都走这一个方法：{@code mouseReleased} 与最后一次
     * {@code mouseDragged} 的坐标未必一致（松手前最后一段移动可能没派发拖动事件），
     * 松手时再摆一次，才能做到「掉在哪就存哪」。</p>
     */
    public static void moveTo(Button button, Screen screen, double mouseX, double mouseY,
                              double grabX, double grabY) {
        button.setX((int) Math.round(mouseX - grabX));
        button.setY((int) Math.round(mouseY - grabY));
        clamp(button, screen);
    }

    /** 别让按钮被拖出屏幕，否则就找不回来了。 */
    public static void clamp(Button button, Screen screen) {
        int maxX = Math.max(0, screen.width - WIDTH);
        int maxY = Math.max(0, screen.height - HEIGHT);
        button.setX(Math.max(0, Math.min(maxX, button.getX())));
        button.setY(Math.max(0, Math.min(maxY, button.getY())));
    }

    /** 拖完把绝对坐标写回配置。 */
    public static void persist(Button button) {
        Config cfg = Config.get();
        cfg.setButtonX(button.getX());
        cfg.setButtonY(button.getY());
        cfg.save();
        Log.info("HUD 按钮位置已保存：" + button.getX() + "," + button.getY());
    }
}
