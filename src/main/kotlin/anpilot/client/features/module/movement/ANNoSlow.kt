package anpilot.client.features.module.movement

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.Minecraft

class ANNoSlow : ANBaseModule(
    name = "NoSlow",
    description = "取消吃食物、使用物品、举盾、潜行以及踩灵魂沙/蜂蜜块时的减速限制",
    category = ANModuleCategory.MOVEMENT,
    chineseName = "防减速"
) {
    val items = addSetting(ANSetting("Items", true))
    val blocking = addSetting(ANSetting("Shielding", true))
    val sneaking = addSetting(ANSetting("Sneaking", false))
    val crawling = addSetting(ANSetting("Crawling", false))
    val soulSand = addSetting(ANSetting("SoulSand", false))
    val honey = addSetting(ANSetting("Honey", false))

    fun shouldCancelSlowedDown(): Boolean {
        val player = mc.player ?: return false
        
        if (player.isBlocking) {
            return blocking.value
        }
        
        if (player.isUsingItem) {
            return items.value
        }
        
        return false
    }
}
