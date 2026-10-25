package dev.durahud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * HUD 位置编辑界面：按 F10 打开，用鼠标把 HUD 拖到想放的地方，ESC 结束。
 *
 * <p><b>为什么不做成"游戏里直接拖"</b>：没有界面时鼠标是拿来转视角的，
 * 想在游戏里拖就必须拦截鼠标，会跟操作打架。做成一个透明界面最省事：视角不动、
 * 鼠标变成指针，拖完按 ESC 就回到游戏，位置当场写进配置文件。</p>
 *
 * <p>界面本身不画背景，能看到游戏画面，方便对着实际界面调位置。</p>
 */
public final class HudEditorScreen extends Screen {

    private static final int HINT_BG = 0xA0000000;
    private static final int HINT_COLOR = 0xFFEAEAEA;
    private static final int HINT_SUB = 0xFF9FC4FF;

    private boolean dragging;
    /** 按下时鼠标相对 HUD 左上角的偏移，拖动时保持这个偏移，手感才跟手。 */
    private int grabDx;
    private int grabDy;

    /** 从哪个界面进来的；关掉时回到它（null 表示直接从游戏里按 F10 打开，回游戏）。 */
    private final Screen parent;

    public HudEditorScreen() {
        this(null);
    }

    public HudEditorScreen(Screen parent) {
        super(Component.literal("耐久显示 HUD 位置"));
        this.parent = parent;
    }

    // ---------------------------------------------------------------- 绘制

    @Override
    public void extractRenderState(GuiGraphicsExtractor g) {
        // 刻意不调用 super：不要那层把世界压暗的底，方便对着真实画面调位置
        clampToScreen();
        Config cfg = Config.get();
        DurabilityHud.render(g, cfg.getX(), cfg.getY(), true);

        String title = "拖动到想放的位置　·　ESC 完成";
        String sub = "当前位置  x=" + cfg.getX() + "  y=" + cfg.getY() + "　（方向键微调）";
        int tw = this.font.width(title);
        int sw = this.font.width(sub);
        int w = Math.max(tw, sw) + 12;
        int x = (this.width - w) / 2;
        int y = 8;
        g.fill(x, y, x + w, y + 28, HINT_BG);
        g.fill(x, y, x + w, y + 1, 0x50FFFFFF);
        g.centeredText(this.font, title, this.width / 2, y + 5, HINT_COLOR);
        g.centeredText(this.font, sub, this.width / 2, y + 16, HINT_SUB);
    }

    /** 当前 HUD 在屏幕上的矩形：{x, y, w, h}。 */
    private int[] rect() {
        Minecraft mc = Minecraft.getInstance();
        List<DurabilityHud.Entry> es = DurabilityHud.entries(mc);
        int w = es.isEmpty() ? DurabilityHud.PLACEHOLDER_W : DurabilityHud.hudWidth(mc, es);
        int h = DurabilityHud.hudHeight(es);
        return new int[]{Config.get().getX(), Config.get().getY(), w, h};
    }

    /**
     * 把存下来的位置收进当前界面。
     *
     * <p>位置是按 GUI 坐标存的，换窗口大小 / 界面缩放后可能落在屏幕外 ——
     * 那样进编辑界面会看不到、也抓不到。这里只保证左上角在屏幕内、右下角留
     * {@link DurabilityHud#MIN_VISIBLE} 像素可抓；<b>不</b>按整块大小算边界，
     * 否则装备件数一变（HUD 变宽变高）就会把位置推走并写盘，看起来像"自己挪了"。</p>
     */
    private void clampToScreen() {
        int[] pos = DurabilityHud.clampAnchor(Config.get().getX(), Config.get().getY(),
                this.width, this.height);
        if (pos[0] != Config.get().getX() || pos[1] != Config.get().getY()) {
            Config.get().setX(pos[0]);
            Config.get().setY(pos[1]);
            Config.get().save();
        }
    }

    // ---------------------------------------------------------------- 交互

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (!event.isLeftMouse()) {
            return super.mouseClicked(event, doubled);
        }
        int[] r = rect();
        if (event.x() >= r[0] && event.x() <= r[0] + r[2]
                && event.y() >= r[1] && event.y() <= r[1] + r[3]) {
            dragging = true;
            grabDx = (int) Math.round(event.x()) - r[0];
            grabDy = (int) Math.round(event.y()) - r[1];
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!dragging) {
            return super.mouseDragged(event, deltaX, deltaY);
        }
        int[] r = rect();
        int nx = (int) Math.round(event.x()) - grabDx;
        int ny = (int) Math.round(event.y()) - grabDy;
        // 别拖出屏幕，否则就找不回来了
        nx = Math.max(0, Math.min(nx, Math.max(0, this.width - r[2])));
        ny = Math.max(0, Math.min(ny, Math.max(0, this.height - r[3])));
        Config.get().setX(nx);
        Config.get().setY(ny);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            Config.get().save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        Config cfg = Config.get();
        return switch (event.key()) {
            case InputConstants.KEY_ESCAPE -> {
                Config.get().save();
                if (this.minecraft != null) {
                    // 从背包进来的就还回背包，按 F10 进来的就回游戏
                    this.minecraft.setScreenAndShow(this.parent);
                }
                yield true;
            }
            case InputConstants.KEY_LEFT -> {
                cfg.setX(Math.max(0, cfg.getX() - 1));
                Config.get().save();
                yield true;
            }
            case InputConstants.KEY_RIGHT -> {
                cfg.setX(cfg.getX() + 1);
                Config.get().save();
                yield true;
            }
            case InputConstants.KEY_UP -> {
                cfg.setY(Math.max(0, cfg.getY() - 1));
                Config.get().save();
                yield true;
            }
            case InputConstants.KEY_DOWN -> {
                cfg.setY(cfg.getY() + 1);
                Config.get().save();
                yield true;
            }
            default -> super.keyPressed(event);
        };
    }

    /** 打开这个界面时把游戏暂停（单人存档里更安全，联机时服务端本来就不会停）。 */
    @Override
    public boolean isPauseScreen() {
        return true;
    }
}