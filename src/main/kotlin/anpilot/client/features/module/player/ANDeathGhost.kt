package anpilot.client.features.module.player

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.ANTickEvent
import anpilot.client.features.module.ANBaseModule
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.DeathScreen

class ANDeathGhost : ANBaseModule(
    name = "DeathGhost",
    description = "死亡后自动隐藏死亡复活屏幕，允许以幽灵模式继续在世界中自由游览",
    category = ANModuleCategory.PLAYER,
    chineseName = "死亡幽灵"
) {
    private var active = false

    override fun onDisable() {
        active = false
        val player = mc.player
        player?.respawn()
    }

    @ANEventHandler
    fun onTick(event: ANTickEvent) {
        val player = mc.player ?: return

        if (mc.screen is DeathScreen) {
            mc.setScreen(null)
            if (!active) {
                active = true
            }
        }

        if (!active) return

        if (player.health < 1f) {
            player.health = 20f
        }
        if (player.foodData.foodLevel < 20) {
            player.foodData.foodLevel = 20
        }
    }
}
