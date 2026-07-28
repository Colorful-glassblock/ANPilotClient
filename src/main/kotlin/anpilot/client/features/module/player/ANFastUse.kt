package anpilot.client.features.module.player

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class ANFastUse : ANBaseModule(
    name = "FastUse",
    description = "取消或缩短食物、药水及经验瓶等物品的使用冷却时间",
    category = ANModuleCategory.PLAYER,
    chineseName = "快速使用"
) {
    val delay = addSetting(ANSetting("Delay", 1f, 0f, 5f))

    fun getItemUseCooldown(itemStack: ItemStack): Int {
        return if (shouldWorkSome(itemStack)) delay.value.toInt() else 4
    }


    private fun shouldWorkSome(itemStack: ItemStack): Boolean {
        return itemStack.item is BlockItem || itemStack.item == Items.EXPERIENCE_BOTTLE
    }
}
