package anpilot.client.features.ai.task.flyto

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.FireworkUtils
import anpilot.client.features.module.misc.ANFlyTo
import net.minecraft.core.BlockPos
import kotlin.math.atan2
import kotlin.math.sqrt

class FlyToGlideTask(agent: ANAgent) : AITask(agent) {
    private var stage = GlideStage.ASCEND
    private var subStage = GlideSubStage.NONE
    private var lastFirework = 0L

    override fun tick() {
        val player = player ?: run {
            finished = true
            return
        }
        val module = agent.module as? ANFlyTo ?: run {
            finished = true
            return
        }
        if (ANAgent.minecraft.level == null) {
            finished = true
            return
        }

        if (!player.isFallFlying) {
            AgentUtils.sendMessage("Glide Lost")
            module.complete("Glide Lost")
            finished = true
            return
        }

        val target = BlockPos(module.targetX.value, player.blockY, module.targetZ.value)
        if (horizontalDistance(target) <= ANFlyTo.REACH_RANGE) {
            agent.scheduler.push(FlyToLandingTask(agent))
            finished = true
            return
        }

        flyTo(module, target)
    }

    private fun flyTo(module: ANFlyTo, target: BlockPos) {
        val player = player ?: return
        val hGlideY = module.highGlideY.value.toInt()
        val lGlideY = module.lowGlideY.value.toInt()
        val flyYaw = yawTo(target)
        val horizontalDist = horizontalDistance(target)

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
                val pitch = approachPitch(module, target)
                agent.rotation.request(AgentUtils.lerpYaw(flyYaw, 0.2f), AgentUtils.lerpPitch(pitch, 0.2f))
                if (pitch <= -10f) boostIfReady()
            }
        }
    }

    private fun approachPitch(module: ANFlyTo, target: BlockPos): Float {
        val player = player ?: return 0f
        val dx = target.x + 0.5 - player.x
        val dz = target.z + 0.5 - player.z
        val horizontalDist = sqrt(dx * dx + dz * dz)
        val desiredY = module.lowGlideY.value + (module.highGlideY.value - module.lowGlideY.value) *
            (horizontalDist.toFloat() / 500f).coerceIn(0f, 1f)
        val error = player.y.toFloat() - desiredY
        return (error * 0.6f + 5f).coerceIn(-15f, 18f)
    }

    private fun boostIfReady() {
        val now = System.currentTimeMillis()
        if (now - lastFirework >= 4000L && FireworkUtils.useFirework()) {
            lastFirework = now
        }
    }

    private fun horizontalDistance(pos: BlockPos): Double {
        val player = player ?: return 0.0
        val dx = pos.x + 0.5 - player.x
        val dz = pos.z + 0.5 - player.z
        return sqrt(dx * dx + dz * dz)
    }

    private fun yawTo(pos: BlockPos): Float {
        val player = player ?: return 0f
        val dx = pos.x + 0.5 - player.x
        val dz = pos.z + 0.5 - player.z
        return (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
    }

    private enum class GlideStage { ASCEND, CRUISE, APPROACH }

    private enum class GlideSubStage { NONE, ASCEND, DESCEND }
}
