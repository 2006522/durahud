package dev.durahud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 耐久 HUD 的设置界面：选显示方式（百分比 / 数字）、去调位置。
 *
 * <p>从背包那个 {@code HUD} 按钮进来（按 F10 也开这个）。界面本身是透明的，
 * 背后的 HUD 也会立刻跟着变；面板里另外画了一行"预览"，就算 HUD 位于屏幕另一头
 * 也能马上看出改成什么样。</p>
 */
public final class HudSettingsScreen extends Screen {

    private static final int PANEL_W = 224;
    private static final int PANEL_H = 154;
    private static final int COL_PANEL = 0xD0000000;
    private static final int COL_BORDER = 0x60FFFFFF;
    private static final int COL_TITLE = 0xFFFFFFFF;
    private static final int COL_HINT = 0xFF9FC4FF;
    private static final int COL_PREVIEW = 0xFF3CDE3C;

    /** 从哪个界面进来的；关掉时回到它。 */
    private final Screen parent;

    private Button percentButton;
    private Button numberButton;

    public HudSettingsScreen(Screen parent) {
        super(Component.literal("耐久 HUD 设置"));
        this.parent = parent;
    }

    private int panelLeft() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        // 两个模式按钮平分，中间留 6px
        int btnW = (PANEL_W - 16 - 6) / 2;

        this.percentButton = addRenderableWidget(Button.builder(Component.empty(),
                        b -> setMode(Config.TextMode.PERCENT))
                .bounds(left + 8, top + 32, btnW, 20)
                .build());

        this.numberButton = addRenderableWidget(Button.builder(Component.empty(),
                        b -> setMode(Config.TextMode.BOTH))
                .bounds(left + 14 + btnW, top + 32, btnW, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("编辑 HUD 位置"), b -> openEditor())
                .bounds(left + 8, top + 76, PANEL_W - 16, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("恢复默认位置"), b -> resetPositions())
                .bounds(left + 8, top + 100, PANEL_W - 16, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("完成"), b -> close())
                .bounds(left + 8, top + 124, PANEL_W - 16, 20)
                .build());

        refreshButtons();
    }

    /** HUD 和背包按钮一起回默认位置（两边都拖乱了的时候一键复原）。 */
    private void resetPositions() {
        Config.get().resetPositions();
        refreshButtons();
    }

    private void setMode(Config.TextMode mode) {
        Config.get().setText(mode);
        Config.get().save();
        refreshButtons();
    }

    /** 把「当前是哪种」直接标在按钮上，省得再画一套选中态。 */
    private void refreshButtons() {
        Config.TextMode now = Config.get().getText();
        this.percentButton.setMessage(tag(now == Config.TextMode.PERCENT, "百分比"));
        this.numberButton.setMessage(tag(isNumberMode(now), "数字（当前/总共）"));
    }

    /** REMAIN（只显示剩余）也是数字，同样算在这边，避免按钮显示成"两个都没选"。 */
    private static boolean isNumberMode(Config.TextMode mode) {
        return mode == Config.TextMode.BOTH || mode == Config.TextMode.REMAIN;
    }

    private static Component tag(boolean active, String text) {
        // 用 ▶ / 全角空格占位，保证按钮宽度和位置不跳
        return Component.literal((active ? "▶ " : "　") + text);
    }

    /** 面板里的示例文字，一眼看出当前模式长什么样。 */
    private static String previewText() {
        return switch (Config.get().getText()) {
            case PERCENT -> "62%";
            case REMAIN -> "156";
            case BOTH -> "156/250";
            case NONE -> "（不显示数字）";
        };
    }

    private void openEditor() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(new HudEditorScreen(this));
        }
    }

    private void close() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g) {
        int left = panelLeft();
        int top = panelTop();
        g.fill(left, top, left + PANEL_W, top + PANEL_H, COL_PANEL);
        g.fill(left, top, left + PANEL_W, top + 1, COL_BORDER);
        g.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, COL_BORDER);
        g.fill(left, top, left + 1, top + PANEL_H, COL_BORDER);
        g.fill(left + PANEL_W - 1, top, left + PANEL_W, top + PANEL_H, COL_BORDER);

        super.extractRenderState(g);

        int cx = this.width / 2;
        g.centeredText(this.font, "耐久 HUD 设置", cx, top + 9, COL_TITLE);
        g.centeredText(this.font, "显示方式", cx, top + 21, COL_HINT);
        // 示例：图标占位用一个色块示意，重点是右边的文字长什么样
        g.centeredText(this.font, "预览　" + previewText(), cx, top + 58, COL_PREVIEW);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}