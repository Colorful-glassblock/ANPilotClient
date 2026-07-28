package anpilot.client.features.module.combat

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.PacketEvent
import anpilot.client.features.module.ANBaseModule
import anpilot.client.minecraft.mixin.accessor.ANServerboundInteractPacketAccessor
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

class ANKnockback : ANBaseModule(
    name = "Knockback",
    description = "攻击敌人时造成更大的击退效果",
    category = ANModuleCategory.COMBAT,
    chineseName = "更多击退"
) {
    @ANEventHandler
    fun onSendPacket(event: PacketEvent.Send) {
        val packet = event.packet
        if (packet is ServerboundInteractPacket) {
            val accessor = (packet as Any) as? ANServerboundInteractPacketAccessor
            val actionStr = accessor?.action?.javaClass?.simpleName ?: ""
            val actionFullStr = accessor?.action?.toString() ?: ""
            if (actionStr.contains("Attack", ignoreCase = true) || actionFullStr.contains("ATTACK", ignoreCase = true)) {
                val player = mc.player ?: return
                player.connection.send(
                    ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING)
                )
            }
        }
    }
}
