package anpilot.client.features.ai.subsystem

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.FireworkUtils
import anpilot.client.features.module.misc.ANElytraPilotPlus
import net.minecraft.core.BlockPos
import kotlin.math.atan2
import kotlin.math.sqrt

class FlightSystem(private val agent: ANAgent) {
    private var target: BlockPos? = null
    private var stage = GlideStage.ASCEND
    private var subStage = GlideSubStage.NONE
    private var lastFirework = 0L

    fun setTarget(target: BlockPos?) {
        this.target = target
        stage = GlideStage.ASCEND
        subStage = GlideSubStage.NONE
    }

    fun glideFlyTo() {
        val player = ANAgent.minecraft.player ?: return
        val currentTarget = target ?: return
        if (!player.isFallFlying) return
        val elytraModule = agent.module as? ANElytraPilotPlus ?: return

        val hGlideY = elytraModule.highGlideY.value.toInt()
        val lGlideY = elytraModule.lowGlideY.value.toInt()
        val flyYaw = getYawTo(currentTarget)
        val horizontalDist = horizontalDistance(currentTarget)

        when (stage) {
            GlideStage.ASCEND -> {
                agent.rotation.request(AgentUtils.lerpYaw(flyYaw, 0.1f), AgentUtils.lerpPitch(-15f, 0.1f))
                boostIfReady()
                if (player.y >= hGlideY) stage = GlideStage.CRUISE
            }
            GlideStage.CRUISE -> {
                agent.rotation.request(AgentUtils.lerpYaw(flyYaw, 0.1f), player.xRot)
                if (horizontalDist > 500.0) {
                    if (player.y > hGlideY) subStage = GlideSubStage.DESCEND
                    if (player.y < lGlideY) subStage = GlideSubStage.ASCEND
                    when (subStage) {
                        GlideSubStage.DESCEND -> agent.rotation.request(flyYaw, AgentUtils.lerpPitch(20f, 0.1f))
                        GlideSubStage.ASCEND -> {
                            agent.rotation.request(flyYaw, AgentUtils.lerpPitch(-15f, 0.1f))
                            if (player.y < hGlideY) boostIfReady()
                        }
                        GlideSubStage.NONE -> Unit
                    }
                }
                if (horizontalDist < 500.0) stage = GlideStage.APPROACH
            }
            GlideStage.APPROACH -> {
                val dynamicPitch = AgentUtils.getDynamicPitch(hGlideY.toFloat(), 600f, currentTarget)
                agent.rotation.request(AgentUtils.lerpYaw(flyYaw, 0.2f), AgentUtils.lerpPitch(dynamicPitch, 0.2f))
                if (dynamicPitch <= -10f) boostIfReady()
            }
        }
    }

    fun stop() {
        target = null
        stage = GlideStage.ASCEND
        subStage = GlideSubStage.NONE
    }

    private fun boostIfReady() {
        val now = System.currentTimeMillis()
        if (now - lastFirework >= 4000L && FireworkUtils.useFirework()) {
            lastFirework = now
        }
    }

    private fun horizontalDistance(pos: BlockPos): Double {
        val player = ANAgent.minecraft.player ?: return 0.0
        val dx = pos.x - player.x
        val dz = pos.z - player.z
        return sqrt(dx * dx + dz * dz)
    }

    private fun getYawTo(pos: BlockPos): Float {
        val player = ANAgent.minecraft.player ?: return 0f
        val dx = pos.x - player.x
        val dz = pos.z - player.z
        return (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
    }

    private enum class GlideStage { ASCEND, CRUISE, APPROACH }

    private enum class GlideSubStage { NONE, ASCEND, DESCEND }
}
