package anpilot.client.features.module.anpilot

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.bootstrap.ANServiceRegistry
import anpilot.client.features.manager.rotation.MovementFix
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.Minecraft

@Suppress("unused")
class ANPilotClient : ANBaseModule(
    name = "PilotSetting",
    description = "Global ANPilot client behavior settings.",
    category = ANModuleCategory.CLIENT,
    chineseName = "客户端设置"
) {

    val PilotUser = addSetting(ANSetting("Pilot", currentPlayerName()))
    val rotationMovementFix = addSetting(ANSetting("MovementFix", MovementFix.OFF))

    override fun isToggleable(): Boolean = false

    override fun onTick() {
        val playerName = currentPlayerName()
        if (PilotUser.value != playerName) {
            PilotUser.setValueSilent(playerName)
        }
    }

    private fun currentPlayerName(): String {
        val minecraft = Minecraft.getInstance()
        return minecraft.player?.name?.string ?: minecraft.user.name
    }

    companion object {
        fun current(): ANPilotClient? {
            if (!ANServiceRegistry.isInitialized) return null
            return ANServiceRegistry.runtime.moduleManager.get("PilotSetting") as? ANPilotClient
        }

        fun globalMovementFix(): MovementFix {
            return current()?.rotationMovementFix?.value ?: MovementFix.OFF
        }
    }
}
