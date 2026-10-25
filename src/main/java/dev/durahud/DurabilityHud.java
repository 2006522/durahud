package dev.durahud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 耐久 HUD 的绘制。
 *
 * <p>列出四件盔甲 + 主副手，凡是<b>带耐久</b>的才显示（火把、方块这类没耐久的直接跳过），
 * 每行是「游戏原版物品图标 + 耐久条 + 数字」。图标直接用 {@code GuiGraphicsExtractor#item}
 * 画 {@link ItemStack} 本身，所以跟背包里看到的一模一样，不需要自己准备贴图。</p>
 *
 * <p>顺序按装备意义排：头 → 胸 → 腿 → 脚，再是主手、副手。同一种东西只看「剩余耐久」，
 * 颜色分三档：> 50% 绿、> 25% 黄、否则红。</p>
 */
public final class DurabilityHud {

    /** 面板内边距。 */
    static final int PAD = 3;
    /** 图标边长（原版物品图标就是 16×16）。 */
    static final int ICON = 16;
    /** 一行的高度：图标 16 + 条与下一行之间留 2。 */
    static final int ROW_H = 18;
    /** 图标与右侧文字的间距。 */
    static final int TEXT_GAP = 4;
    /** 耐久条尺寸与相对图标左上角的纵向偏移（照抄原版物品栏里的位置）。 */
    static final int BAR_W = 13;
    static final int BAR_H = 2;
    static final int BAR_DY = 13;

    static final int COL_PANEL = 0x90000000;
    static final int COL_BORDER = 0x50FFFFFF;
    static final int COL_BAR_BG = 0xFF141414;
    static final int COL_EDIT_BORDER = 0xFF66AAFF;
    static final int COL_HINT = 0xFFEAEAEA;
    static final int COL_EMPTY = 0xFFAAAAAA;

    /** 一件可显示装备都没有时，编辑界面里占位框的宽度（保证还有东西可拖）。 */
    static final int PLACEHOLDER_W = 96;

    private DurabilityHud() {
    }

    /** 一项要显示的装备。 */
    record Entry(ItemStack stack, int remain, int max, float ratio) {
    }

    // ---------------------------------------------------------------- 数据

    static List<Entry> entries(Minecraft mc) {
        Config cfg = Config.get();
        List<Entry> out = new ArrayList<>(6);
        Player player = mc.player;
        if (player == null) {
            return out;
        }
        if (cfg.isShowArmor()) {
            add(out, player, EquipmentSlot.HEAD);
            add(out, player, EquipmentSlot.CHEST);
            add(out, player, EquipmentSlot.LEGS);
            add(out, player, EquipmentSlot.FEET);
        }
        if (cfg.isShowHeld()) {
            add(out, player, EquipmentSlot.MAINHAND);
            add(out, player, EquipmentSlot.OFFHAND);
        }
        return out;
    }

    private static void add(List<Entry> out, Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        // 空手、火把、方块……这些没有耐久概念，不占格子
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return;
        }
        int remain = Math.max(0, max - stack.getDamageValue());
        out.add(new Entry(stack, remain, max, remain / (float) max));
    }

    // ---------------------------------------------------------------- 尺寸

    static int hudWidth(Minecraft mc, List<Entry> es) {
        int textW = 0;
        if (Config.get().getText() != Config.TextMode.NONE && !es.isEmpty()) {
            for (Entry e : es) {
                textW = Math.max(textW, mc.font.width(label(e)));
            }
            textW += TEXT_GAP;
        }
        return PAD * 2 + ICON + textW;
    }

    static int hudHeight(List<Entry> es) {
        return PAD * 2 + Math.max(1, es.size()) * ROW_H;
    }

    // ---------------------------------------------------------------- 绘制

    /** HUD 元素入口：每帧由 Fabric 调。 */
    static void extract(GuiGraphicsExtractor g) {
        Config cfg = Config.get();
        if (!cfg.isEnable()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // F1 隐藏界面时跟着一起藏起来
        if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.isHidden()) {
            return;
        }
        // 编辑界面里由它自己画（带高亮框），这里再画一次会重叠
        if (mc.gui != null && mc.gui.screen() instanceof HudEditorScreen) {
            return;
        }
        render(g, cfg.getX(), cfg.getY(), false);
    }

    static void render(GuiGraphicsExtractor g, int x, int y, boolean editing) {
        Minecraft mc = Minecraft.getInstance();
        List<Entry> es = entries(mc);

        // 平时没东西可显示就整块都不画；只有编辑界面里才画个占位框，否则没东西可拖
        if (es.isEmpty() && !editing) {
            return;
        }

        int w = es.isEmpty() ? PLACEHOLDER_W : hudWidth(mc, es);
        int h = hudHeight(es);

        if (Config.get().isBackground()) {
            g.fill(x, y, x + w, y + h, COL_PANEL);
            // 细边框，让它在各种背景上都看得清
            g.fill(x, y, x + w, y + 1, COL_BORDER);
            g.fill(x, y + h - 1, x + w, y + h, COL_BORDER);
            g.fill(x, y, x + 1, y + h, COL_BORDER);
            g.fill(x + w - 1, y, x + w, y + h, COL_BORDER);
        }

        if (editing) {
            g.fill(x - 1, y - 1, x + w + 1, y, COL_EDIT_BORDER);
            g.fill(x - 1, y + h, x + w + 1, y + h + 1, COL_EDIT_BORDER);
            g.fill(x - 1, y - 1, x, y + h + 1, COL_EDIT_BORDER);
            g.fill(x + w, y - 1, x + w + 1, y + h + 1, COL_EDIT_BORDER);
        }

        if (es.isEmpty()) {
            if (editing) {
                // 没装备时也给个占位，否则没东西可拖
                g.text(mc.font, "没有带耐久的装备", x + PAD, y + PAD + 4, COL_EMPTY);
            }
            return;
        }

        boolean showText = Config.get().getText() != Config.TextMode.NONE;
        int rowY = y + PAD;
        for (Entry e : es) {
            // 图标：直接用本体的 ItemStack，画出来就是游戏原版的样子
            g.item(e.stack(), x + PAD, rowY);

            int barX = x + PAD + 2;
            int barY = rowY + BAR_DY;
            g.fill(barX, barY, barX + BAR_W, barY + BAR_H, COL_BAR_BG);
            int fillW = Math.round(BAR_W * e.ratio());
            if (fillW > 0) {
                g.fill(barX, barY, barX + fillW, barY + BAR_H, colorFor(e.ratio()));
            }

            if (showText) {
                g.text(mc.font, label(e), x + PAD + ICON + TEXT_GAP, rowY + 4, colorFor(e.ratio()));
            }
            rowY += ROW_H;
        }
    }

    // ---------------------------------------------------------------- 小工具

    static String label(Entry e) {
        return switch (Config.get().getText()) {
            case PERCENT -> Math.round(e.ratio() * 100.0F) + "%";
            case REMAIN -> Integer.toString(e.remain());
            case BOTH -> e.remain() + "/" + e.max();
            case NONE -> "";
        };
    }

    static int colorFor(float ratio) {
        if (ratio > 0.5F) {
            return 0xFF3CDE3C;
        }
        if (ratio > 0.25F) {
            return 0xFFE0C040;
        }
        return 0xFFE04040;
    }
}