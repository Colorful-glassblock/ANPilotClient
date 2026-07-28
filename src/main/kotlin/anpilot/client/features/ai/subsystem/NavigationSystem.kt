package anpilot.client.features.ai.subsystem

import anpilot.client.features.ai.agent.ANAgent
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

class NavigationSystem(private val agent: ANAgent) {
    private var target: Vec3? = null

    fun tick() {
        val current = target ?: return
        agent.rotation.request(getYaw(current), getPitch(current))
    }

    fun moveTo(pos: BlockPos) {
        target = Vec3.atCenterOf(pos)
    }

    fun stop() {
        target = null
    }

    private fun getYaw(target: Vec3): Float {
        val player = ANAgent.minecraft.player ?: return 0f
        val dx = target.x - player.x
        val dz = target.z - player.z
        return (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
    }

    private fun getPitch(target: Vec3): Float {
        val player = ANAgent.minecraft.player ?: return 0f
        val dx = target.x - player.x
        val dz = target.z - player.z
        val dy = target.y - player.eyeY
        val horizontal = sqrt(dx * dx + dz * dz)
        return (-Math.toDegrees(atan2(dy, horizontal))).toFloat()
    }
}
