package dev.durahud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * 客户端入口：注册 HUD 元素 + 一个打开位置编辑界面的按键。
 *
 * <p>纯客户端模组，不改存档、不发包，服务端不用装。</p>
 */
public final class DuraHud implements ClientModInitializer {

    /** 编辑 HUD 位置的按键，默认 F10，可在「控制 → 按键绑定」里改。 */
    private static KeyMapping editKey;

    @Override
    public void onInitializeClient() {
        Config.get().load();
        // 第一次启动就把默认配置写出来，用户知道该改哪个文件
        Config.get().save();

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("durahud", "durability"),
                DurabilityHud::extract);

        editKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.durahud.edit",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_F10,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("durahud", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 用 while 把队列里的点击都消费掉，避免一次按下漏掉
            while (editKey.consumeClick()) {
                try {
                    if (client.gui != null && client.gui.screen() == null) {
                        // 直接对着游戏按的，没有父界面：关掉就回游戏
                        client.setScreenAndShow(new HudSettingsScreen(null));
                    }
                } catch (Exception e) {
                    Log.error("打开耐久 HUD 设置失败", e);
                }
            }
        });

        Log.info("已加载：装备和手持工具的耐久显示在屏幕上，按 F10 或背包里的 HUD 按钮可设置。");
    }
}