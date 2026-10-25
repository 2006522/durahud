package dev.durahud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * 配置读写，文件在 {@code config/durahud.properties}。
 *
 * <p>位置（x/y）也记在这里：在编辑界面里拖完即写盘，下次进游戏就还在原地。</p>
 */
public final class Config {

    private static final File FILE = new File(new File("config"), "durahud.properties");

    private static final Config INSTANCE = new Config();

    /** 文本显示方式。 */
    public enum TextMode {
        /** 只显示百分比，如 {@code 62%}。 */
        PERCENT,
        /** 只显示剩余耐久，如 {@code 156}。 */
        REMAIN,
        /** 剩余 / 上限，如 {@code 156/250}。 */
        BOTH,
        /** 不显示文字，只有图标和耐久条。 */
        NONE
    }

    private boolean enable = true;

    /** HUD 左上角在 GUI 坐标系里的位置。 */
    private int x = 6;
    private int y = 6;

    /** 是否显示盔甲四件。 */
    private boolean showArmor = true;
    /** 是否显示主手/副手。 */
    private boolean showHeld = true;

    private TextMode text = TextMode.PERCENT;

    /** 是否画半透明底板，关掉就只剩图标和条。 */
    private boolean background = true;

    /**
     * 背包里那个 HUD 按钮相对面板左上角的偏移。
     *
     * <p><b>只用于老配置迁移和"恢复默认位置"</b>：按钮真正的位置现在是绝对 GUI 坐标
     * （{@link #buttonX}/{@link #buttonY}）。原来存相对偏移的时候，背包面板在
     * 生存背包（176 宽、含配方书还会左移）、创造物品栏（195 宽）里原点都不一样，
     * 同一个偏移量在两种界面算出来的屏幕位置差十几像素 —— 玩家看到的就是
     * "我明明拖好了，怎么又跑别的地方去了"。</p>
     */
    private int buttonDx = -30;
    private int buttonDy = 4;

    /** 按钮左上角在 GUI 坐标系里的绝对位置；{@link #UNSET} 表示还没落位。 */
    private int buttonX = UNSET;
    private int buttonY = UNSET;

    /** 还没算过绝对位置的哨兵值。 */
    public static final int UNSET = Integer.MIN_VALUE;

    private Config() {
    }

    public static Config get() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------- 取值

    public boolean isEnable() {
        return enable;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isShowArmor() {
        return showArmor;
    }

    public boolean isShowHeld() {
        return showHeld;
    }

    public TextMode getText() {
        return text;
    }

    public void setText(TextMode text) {
        this.text = text;
    }

    public boolean isBackground() {
        return background;
    }

    /** 背包里 HUD 按钮相对面板左上角的横向偏移（仅用于落位和恢复默认）。 */
    public int getButtonDx() {
        return buttonDx;
    }

    public void setButtonDx(int buttonDx) {
        this.buttonDx = buttonDx;
    }

    /** 背包里 HUD 按钮相对面板左上角的纵向偏移（仅用于落位和恢复默认）。 */
    public int getButtonDy() {
        return buttonDy;
    }

    public void setButtonDy(int buttonDy) {
        this.buttonDy = buttonDy;
    }

    /** 按钮是否已经有绝对坐标（false 表示还要按面板偏移落一次位）。 */
    public boolean hasButtonPos() {
        return buttonX != UNSET && buttonY != UNSET;
    }

    public int getButtonX() {
        return buttonX;
    }

    public void setButtonX(int buttonX) {
        this.buttonX = buttonX;
    }

    public int getButtonY() {
        return buttonY;
    }

    public void setButtonY(int buttonY) {
        this.buttonY = buttonY;
    }

    /**
     * 第一次用（或刚恢复默认位置）时，按「面板左上角 + 偏移」算一次绝对坐标并落盘。
     *
     * <p>之后再打开任何界面都不再重算，按钮就钉在同一个屏幕位置上。</p>
     */
    public void ensureButtonPos(int panelLeft, int panelTop) {
        if (hasButtonPos()) {
            return;
        }
        buttonX = panelLeft + buttonDx;
        buttonY = panelTop + buttonDy;
        save();
        Log.info("HUD 按钮位置落位为绝对坐标 " + buttonX + "," + buttonY);
    }

    /** HUD 与按钮一起恢复默认位置（HUD 回左上角，按钮回面板左侧）。 */
    public void resetPositions() {
        this.x = 6;
        this.y = 6;
        this.buttonDx = -30;
        this.buttonDy = 4;
        // 清成"未落位"，下次打开背包时按上面的偏移重新摆到面板左边
        this.buttonX = UNSET;
        this.buttonY = UNSET;
        save();
    }

    // ---------------------------------------------------------------- 读写

    public void load() {
        Properties p = new Properties();
        if (!FILE.isFile()) {
            Log.info("没有配置文件，先用默认值: " + FILE.getAbsolutePath());
            return;
        }
        try (FileInputStream in = new FileInputStream(FILE)) {
            p.load(in);
        } catch (Exception e) {
            Log.warn("读取配置失败，使用默认值: " + e.getMessage());
            return;
        }
        enable = readBool(p, "enable", enable);
        x = readInt(p, "x", x);
        y = readInt(p, "y", y);
        showArmor = readBool(p, "show_armor", showArmor);
        showHeld = readBool(p, "show_held", showHeld);
        background = readBool(p, "background", background);
        buttonDx = readInt(p, "button_dx", buttonDx);
        buttonDy = readInt(p, "button_dy", buttonDy);
        buttonX = readInt(p, "button_x", buttonX);
        buttonY = readInt(p, "button_y", buttonY);

        String mode = p.getProperty("text", "").trim().toUpperCase();
        if (!mode.isEmpty()) {
            try {
                text = TextMode.valueOf(mode);
            } catch (IllegalArgumentException ignored) {
                // 写错了就用默认值，不因为一个配置项崩掉
            }
        }
        Log.info("已读取配置 " + FILE.getAbsolutePath()
                + " → HUD(" + x + "," + y + ")"
                + " 按钮绝对(" + (hasButtonPos() ? buttonX + "," + buttonY : "未落位")
                + ") 按钮偏移(" + buttonDx + "," + buttonDy + ") 文本=" + text);
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("enable", Boolean.toString(enable));
        p.setProperty("x", Integer.toString(x));
        p.setProperty("y", Integer.toString(y));
        p.setProperty("show_armor", Boolean.toString(showArmor));
        p.setProperty("show_held", Boolean.toString(showHeld));
        p.setProperty("text", text.name().toLowerCase());
        p.setProperty("background", Boolean.toString(background));
        p.setProperty("button_dx", Integer.toString(buttonDx));
        p.setProperty("button_dy", Integer.toString(buttonDy));
        p.setProperty("button_x", Integer.toString(buttonX));
        p.setProperty("button_y", Integer.toString(buttonY));

        File dir = FILE.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            Log.warn("创建配置目录失败: " + dir);
            return;
        }
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            p.store(out, "耐久显示(DuraHUD) 配置。位置可以按 F10 进编辑界面直接拖。");
        } catch (Exception e) {
            Log.warn("写入配置失败: " + e.getMessage());
        }
    }

    private static boolean readBool(Properties p, String key, boolean fallback) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        raw = raw.trim();
        return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "yes".equalsIgnoreCase(raw);
    }

    private static int readInt(Properties p, String key, int fallback) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}