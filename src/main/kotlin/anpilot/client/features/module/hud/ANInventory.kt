package anpilot.client.features.module.hud

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.Minecraft

class ANInventory : ANDraggableHudModule("Inventory", "在屏幕HUD上实时展示当前玩家背包", "背包物品", 300f, 250f) {

    override fun renderHudContent(context: ANGuiRenderContext, editor: Boolean) {
        val player = Minecraft.getInstance().player
        val itemScale = hudScale*1.2f
        val borderWidth = 9f * 18f * itemScale
        val borderHeight = 3f * 18f * itemScale
        setHudBounds(borderWidth + scaled(10f), borderHeight + scaled(10f), -scaled(10f), -scaled(10f))
        context.borderedRoundedRect(x - scaled(5f), y - scaled(5f), hudWidth, hudHeight, scaled(12f), scaled(1f), HudColors.panelFillColor, HudColors.panelBorderColor)
        if (player == null) return
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val slot = row * 9 + col + 9
                context.item(player.inventory.getItem(slot), x + col * 18f * itemScale, y + row * 18f * itemScale, itemScale, true)
            }
        }
    }
}
