package anpilot.client.features.module.movement

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.ANTickEvent
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen

class ANGuiMove : ANBaseModule(
    name = "GuiMove",
    description = "允许在打开背包、箱子或菜单界面时自由控制角色的行走与转向",
    category = ANModuleCategory.MOVEMENT,
    chineseName = "界面移动"
) {
    val sneak = addSetting(ANSetting("Sneak", false))

    @ANEventHandler
    fun onTick(event: ANTickEvent) {
        val player = mc.player ?: return
        if (mc.level == null) return
        if (mc.screen == null || mc.screen is ChatScreen) return

        KeyMapping.setAll()

        val options = mc.options
        if (!sneak.value) {
            options.keyShift.isDown = false
        }
    }
}
