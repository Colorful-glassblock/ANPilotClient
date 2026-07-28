package anpilot.client.features.ai.task.basefinder

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.FireworkUtils
import anpilot.client.features.module.misc.ANBaseFinder
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.util.Mth
import kotlin.math.max
import kotlin.math.sqrt

class BaseFinderLandingTask(
    agent: ANAgent,
    private val module: ANBaseFinder,
    private val target: BlockPos
) : AITask(agent) {
    private var stage = LandingStage.APPROACH_HIGH
    private var lowRecoverBoosted = false
    private var recoveryAttempts = 0
    private var lastRecoverFireworkAt = 0L

    override fun start() {
        AgentUtils.sendMessage("BaseFinder 扫描完成，开始降落")
    }

    override fun tick() {
        val player = player ?: run {
            finished = true
            return
        }
        if (ANAgent.minecraft.level == null) {
            finished = true
            return
        }

        if (horizontalDistance(target) > LANDING_RADIUS && player.onGround()) {
            if (recoveryAttempts >= MAX_RECOVERY_ATTEMPTS) {
                finishLanding("降落恢复次数过多")
                return
            }
            recoveryAttempts++
            AgentUtils.sendMessage("BaseFinder 在非预定区域落地，重新起飞修正降落")
            restartFallFlying()
            return
        }

        val next = chooseStage()
        if (next != stage) {
            stage = next
            lowRecoverBoosted = false
        }

        when (stage) {
            LandingStage.APPROACH_HIGH -> approachHigh()
            LandingStage.LOW_RECOVER -> lowRecover()
        }

        if (player.onGround() && horizontalDistance(target) < LANDING_COMPLETE_RADIUS) {
            finishLanding("已完成降落")
        }
    }

    override fun stop() {
        agent.movement.stop()
    }

    private fun chooseStage(): LandingStage {
        val player = player ?: return LandingStage.APPROACH_HIGH
        if (stage == LandingStage.LOW_RECOVER && player.y < target.y) return LandingStage.LOW_RECOVER
        if (horizontalDistance(target) > LANDING_RADIUS && player.y < target.y) return LandingStage.LOW_RECOVER
        return LandingStage.APPROACH_HIGH
    }

    private fun approachHigh() {
        lowRecoverBoosted = false
        flyToward(target, getPitchToTarget(module.altitude.value, GLIDE_DISTANCE, target), 0.18f)
        restartFallFlyingIfNeeded()
    }

    private fun lowRecover() {
        val pitch = if (hasClearVerticalPathTo(target.y)) -80f else getPitchToTarget(module.altitude.value, GLIDE_DISTANCE, target)
        flyToward(target, pitch, 0.35f)
        if (!lowRecoverBoosted && pitch < 0f && boostIfReady()) {
            lowRecoverBoosted = true
        }
        restartFallFlyingIfNeeded()
    }

    private fun restartFallFlying() {
        val player = player ?: return
        val pos = player.blockPosition()
        val facing = player.direction
        val directions = listOf(facing, facing.clockWise, facing.opposite, facing.counterClockWise)
        val bestDir = directions.maxByOrNull { getClearDistance(pos, it) } ?: facing
        val yaw = bestDir.toYRot()
        player.yRot = yaw
        agent.rotation.request(yaw, -12f)
        player.jumpFromGround()
        restartFallFlyingIfNeeded(force = true)
    }

    private fun restartFallFlyingIfNeeded(force: Boolean = false) {
        val player = player ?: return
        if (player.isFallFlying) return
        if (!force && player.onGround()) return
        ANAgent.minecraft.connection?.send(
            ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING)
        )
    }

    private fun boostIfReady(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRecoverFireworkAt < RECOVER_FIREWORK_INTERVAL_MS) return false
        if (!FireworkUtils.useFirework()) return false
        lastRecoverFireworkAt = now
        return true
    }

    private fun hasClearVerticalPathTo(y: Int): Boolean {
        val level = ANAgent.minecraft.level ?: return false
        val player = player ?: return false
        val base = player.blockPosition()
        val top = max(base.y, y)
        for (checkY in base.y + 1..top) {
            val pos = BlockPos(base.x, checkY, base.z)
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty) return false
        }
        return true
    }

    private fun flyToward(pos: BlockPos, pitch: Float, smoothing: Float) {
        val yaw = AgentUtils.yawTo(pos)
        agent.rotation.request(AgentUtils.lerpYaw(yaw, smoothing), AgentUtils.lerpPitch(pitch, smoothing))
    }

    private fun getPitchToTarget(glideStartY: Float, maxGlideDist: Float, targetPos: BlockPos): Float {
        val player = player ?: return 0f
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

    private fun horizontalDistance(pos: BlockPos): Double {
        val player = player ?: return Double.MAX_VALUE
        val dx = pos.x + 0.5 - player.x
        val dz = pos.z + 0.5 - player.z
        return sqrt(dx * dx + dz * dz)
    }

    private fun getClearDistance(startPos: BlockPos, dir: Direction): Int {
        val level = ANAgent.minecraft.level ?: return 0
        var dist = 0
        var checkPos = startPos
        for (i in 1..8) {
            checkPos = checkPos.relative(dir)
            if (!level.getBlockState(checkPos).isAir ||
                !level.getBlockState(checkPos.above()).isAir ||
                !level.getBlockState(checkPos.above(2)).isAir
            ) {
                break
            }
            dist++
        }
        return dist
    }

    private fun finishLanding(message: String) {
        AgentUtils.sendMessage("BaseFinder $message")
        finished = true
        if (module.disconnectAfterLanding.value) {
            Minecraft.getInstance().connection?.connection?.disconnect(Component.literal("[BaseFinder] $message"))
        }
    }

    private enum class LandingStage { APPROACH_HIGH, LOW_RECOVER }

    private companion object {
        private const val GLIDE_DISTANCE = 700f
        private const val LANDING_RADIUS = 24.0
        private const val LANDING_COMPLETE_RADIUS = 20.0
        private const val RECOVER_FIREWORK_INTERVAL_MS = 3_000L
        private const val MAX_RECOVERY_ATTEMPTS = 3
    }
}
