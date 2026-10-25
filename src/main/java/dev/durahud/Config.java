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
     * <p>存的是**相对量**而不是绝对坐标：这样换 GUI 缩放、换分辨率，按钮都还贴在面板
     * 左边同一个位置，不会跑到屏幕外面去。默认 {@code -30} 就是面板左边外侧
     * （按钮宽 26 + 间距 4）。</p>
     */
    private int buttonDx = -30;
    private int buttonDy = 4;

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

    /** 背包里 HUD 按钮相对面板左上角的横向偏移。 */
    public int getButtonDx() {
        return buttonDx;
    }

    public void setButtonDx(int buttonDx) {
        this.buttonDx = buttonDx;
    }

    /** 背包里 HUD 按钮相对面板左上角的纵向偏移。 */
    public int getButtonDy() {
        return buttonDy;
    }

    public void setButtonDy(int buttonDy) {
        this.buttonDy = buttonDy;
    }

    /** HUD 与按钮一起恢复默认位置（HUD 回左上角，按钮回面板左侧）。 */
    public void resetPositions() {
        this.x = 6;
        this.y = 6;
        this.buttonDx = -30;
        this.buttonDy = 4;
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

        String mode = p.getProperty("text", "").trim().toUpperCase();
        if (!mode.isEmpty()) {
            try {
                text = TextMode.valueOf(mode);
            } catch (IllegalArgumentException ignored) {
                // 写错了就用默认值，不因为一个配置项崩掉
            }
        }
        Log.info("已读取配置 " + FILE.getAbsolutePath()
                + " → HUD(" + x + "," + y + ") 按钮偏移(" + buttonDx + "," + buttonDy + ") 文本=" + text);
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