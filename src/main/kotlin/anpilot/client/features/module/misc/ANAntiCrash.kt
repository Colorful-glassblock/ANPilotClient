package anpilot.client.features.module.misc

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.api.module.ANModuleState
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.PacketEvent
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.world.phys.Vec3

class ANAntiCrash : ANBaseModule(
    name = "AntiCrash",
    description = "防止恶意玩家炸客户端",
    category = ANModuleCategory.MISC,
    chineseName = "防崩服",
    defaultState = ANModuleState.ENABLED
) {

    @ANEventHandler
    fun onReceivePacket(event: PacketEvent.Receive) {
        when (val packet = event.packet) {
            is ClientboundExplodePacket -> {
                val explodePos = packet.center
                var playerKnockback = Vec3.ZERO
                if (packet.playerKnockback.isPresent) {
                    playerKnockback = packet.playerKnockback.get()
                }
                if (explodePos.x() > 30_000_000 || explodePos.y() > 30_000_000 || explodePos.z() > 30_000_000 ||
                    explodePos.x() < -30_000_000 || explodePos.y() < -30_000_000 || explodePos.z() < -30_000_000 ||
                    playerKnockback.x > 30_000_000 || playerKnockback.y > 30_000_000 || playerKnockback.z > 30_000_000 ||
                    playerKnockback.x < -30_000_000 || playerKnockback.y < -30_000_000 || playerKnockback.z < -30_000_000
                ) {
                    cancelPacket(event)
                }
            }
            is ClientboundLevelParticlesPacket -> {
                if (packet.count > 100_000) {
                    cancelPacket(event)
                }
            }
            is ClientboundPlayerPositionPacket -> {
                val playerPos = packet.change.position()
                if (playerPos.x > 30_000_000 || playerPos.y > 30_000_000 || playerPos.z > 30_000_000 ||
                    playerPos.x < -30_000_000 || playerPos.y < -30_000_000 || playerPos.z < -30_000_000
                ) {
                    cancelPacket(event)
                }
            }
            is ClientboundSetEntityMotionPacket -> {
                val movement = packet.movement
                if (movement.x > 1000 || movement.y > 1000 || movement.z > 1000 ||
                    movement.x < -1000 || movement.y < -1000 || movement.z < -1000
                ) {
                    cancelPacket(event)
                }
            }
        }
    }

    private fun cancelPacket(event: PacketEvent.Receive) {
        sendClientMessage("有坏蛋正在崩服！！！")
        event.cancel()
    }
}
