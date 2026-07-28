package anpilot.client.features.ai.utils

import anpilot.client.features.ai.agent.ANAgent
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.item.Items
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import net.minecraft.world.level.block.Blocks

object AgentUtils {
    fun sendMessage(message: String) {
        ANAgent.minecraft.player?.sendSystemMessage(
            Component.literal("[ANPilot:Agent]").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(" "))
                .append(
                    Component.literal(message)
                        .withStyle(ChatFormatting.GREEN)
                )
        )
    }

    fun hasFirework(): Boolean {
        val inventory = ANAgent.minecraft.player?.inventory ?: return false
        for (slot in 0 until inventory.containerSize) {
            if (inventory.getItem(slot).item == Items.FIREWORK_ROCKET) return true
        }
        return false
    }

    fun reachedToPos(target: BlockPos?, range: Int): Boolean {
        val player = ANAgent.minecraft.player ?: return false
        if (target == null) return false
        val playerPos = player.blockPosition()
        return abs(target.x - playerPos.x) <= range && abs(target.z - playerPos.z) <= range
    }

    fun scanThroughChunks(radius: Int): BlockPos? {
        val minecraft = ANAgent.minecraft
        val player = minecraft.player ?: return null
        val level = minecraft.level ?: return null
        val chunkPos = player.chunkPosition()
        val chunkRadius = radius shr 4
        for (x in -chunkRadius..chunkRadius) {
            for (z in -chunkRadius..chunkRadius) {
                val chunk = level.getChunk(chunkPos.x + x, chunkPos.z + z)
                for (pos in chunk.blockEntitiesPos) {
                    if (level.getBlockState(pos).`is`(Blocks.BREWING_STAND)) {
                        return pos.offset(0, 20, 0)
                    }
                }
            }
        }
        return null
    }

    fun lerpYaw(targetYaw: Float, t: Float): Float {
        val player = ANAgent.minecraft.player ?: return targetYaw
        return player.yRot + Mth.wrapDegrees(targetYaw - player.yRot) * t
    }

    fun lerpPitch(targetPitch: Float, t: Float): Float {
        val player = ANAgent.minecraft.player ?: return targetPitch
        return player.xRot + (targetPitch - player.xRot) * t
    }

    fun getDynamicPitch(glideStartY: Float, maxGlideDist: Float, targetPos: BlockPos): Float {
        val player = ANAgent.minecraft.player ?: return 0f
        val dx = targetPos.x + 0.5 - player.x
        val dz = targetPos.z + 0.5 - player.z
        val horizontalDist = sqrt(dx * dx + dz * dz)
        val currentY = player.y.toFloat()
        var desiredY = targetPos.y + (glideStartY - targetPos.y) * (horizontalDist.toFloat() / maxGlideDist)
        desiredY = desiredY.coerceAtMost(glideStartY)
        val error = currentY - desiredY
        val pitch = error * 0.6f + 5f
        return Mth.clamp(pitch, -15f, 10f)
    }

    fun agentNullCheck(): Boolean = ANAgent.minecraft.player == null || ANAgent.minecraft.level == null

    fun yawTo(pos: BlockPos): Float {
        val player = ANAgent.minecraft.player ?: return 0f
        val dx = pos.x + 0.5 - player.x
        val dz = pos.z + 0.5 - player.z
        return (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
    }

    fun horizontalDistance(pos: BlockPos): Double {
        val player = ANAgent.minecraft.player ?: return 0.0
        val dx = pos.x + 0.5 - player.x
        val dz = pos.z + 0.5 - player.z
        return sqrt(dx * dx + dz * dz)
    }


}
