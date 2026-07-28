package anpilot.client.features.module.hud

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.EquipmentSlot

class ANPlayerArmor : ANDraggableHudModule("PlayerArmor", "自身装备的护甲与手持物品耐久度", "装备盔甲", 250f, 180f) {

    override fun renderHudContent(context: ANGuiRenderContext, editor: Boolean) {
        val player = Minecraft.getInstance().player
        val itemScale = hudScale*1.2f
        setHudBounds(80f * itemScale, scaled(25f), -scaled(10f), -scaled(5f))
        context.borderedRoundedRect(x - scaled(5f), y - scaled(2f), hudWidth , hudHeight, scaled(10f), scaled(1f), HudColors.panelFillColor, HudColors.panelBorderColor)
        val armor = listOf(
            player?.getItemBySlot(EquipmentSlot.HEAD),
            player?.getItemBySlot(EquipmentSlot.CHEST),
            player?.getItemBySlot(EquipmentSlot.LEGS),
            player?.getItemBySlot(EquipmentSlot.FEET)
        )
        armor.forEachIndexed { index, stack ->
            if (stack != null) context.item(stack, x + index * 18f * itemScale, y, itemScale, true)
        }
    }
}
