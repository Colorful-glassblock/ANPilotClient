package anpilot.client.features.ai.task.elytrapilot

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.FireworkUtils
import anpilot.client.features.module.misc.ANElytraPilotPlus
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import kotlin.math.max
import kotlin.math.sqrt
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity

class EmergencyLandingTask(agent: ANAgent, private val target: BlockPos) : AITask(agent) {
    private var stage = LandingStage.APPROACH_HIGH
    private var lowRecoverBoosted = false
    private var recoveryAttempts = 0

    override fun start() {
        
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
            if (recoveryAttempts >= 3) {
                fail("迫降恢复次数过多, 保护离开")
                finished = true
                return
            }
            recoveryAttempts++
            AgentUtils.sendMessage("非预定区域落地，重新起飞")
            
            val pos = player.blockPosition()
            val facing = player.direction
            val directions = listOf(facing, facing.clockWise, facing.opposite, facing.counterClockWise)
            val bestDir = directions.maxByOrNull { getClearDistance(pos, it) } ?: facing
            
            val yaw = bestDir.toYRot()
            player.yRot = yaw
            agent.rotation.request(yaw, -12f)
            
            agent.scheduler.push(TakeOffTask(agent))
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

        if (player.onGround() && horizontalDistance(target) < 10.0) {
            AgentUtils.sendMessage("寻找位置放置烟花潜影盒")
            val positions = findRefillPlacePositions(player.blockPosition())
            if (positions != null) {
                agent.scheduler.push(EmergencyRefillTask(agent, positions.first, positions.second))
            } else {
                AgentUtils.sendMessage("没有合适的方块空间用于补给，放弃避险")
            }
            finished = true
        }
    }

    private fun chooseStage(): LandingStage {
        val player = player ?: return LandingStage.APPROACH_HIGH
        if (stage == LandingStage.LOW_RECOVER && player.y < target.y) return LandingStage.LOW_RECOVER
        if (horizontalDistance(target) > LANDING_RADIUS && player.y < target.y) return LandingStage.LOW_RECOVER
        return LandingStage.APPROACH_HIGH
    }

    private fun approachHigh() {
        val elytraModule = agent.module as? ANElytraPilotPlus ?: run {
            finished = true
            return
        }
        lowRecoverBoosted = false
        flyToward(target, getPitchToTarget(elytraModule.highGlideY.value, GLIDE_DISTANCE, target), 0.18f)
    }

    private fun lowRecover() {
        val elytraModule = agent.module as? ANElytraPilotPlus ?: run {
            finished = true
            return
        }
        val pitch = if (hasClearVerticalPathTo(target.y)) -90f else getPitchToTarget(elytraModule.highGlideY.value, GLIDE_DISTANCE, target)
        flyToward(target, pitch, 0.35f)
        if (!lowRecoverBoosted && pitch < 0f) {
            FireworkUtils.useFirework()
            lowRecoverBoosted = true
        }
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

    private fun findRefillPlacePositions(base: BlockPos): Pair<BlockPos, BlockPos>? {
        val level = ANAgent.minecraft.level ?: return null
        for (x in -2..2) {
            for (z in -2..2) {
                
                if (x * x < 3 && z * z < 3) continue
                
                val p1 = base.offset(x, 0, z)
                if (level.getBlockState(p1).isAir && ElytraStorageSupport.supportHitResult(p1) != null) {
                    val p2 = p1.above()
                    if (level.getBlockState(p2).isAir) {
                        
                        if (!hasEntityCollision(p1)) {
                            return Pair(p1, p2)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun hasEntityCollision(pos: BlockPos): Boolean {
        val level = ANAgent.minecraft.level ?: return false
        val box = AABB(pos)
        for (entity in level.entitiesForRendering()) {
            if (entity.isSpectator) continue
            if (entity is ItemEntity) continue
            if (entity is ExperienceOrb) continue
            if (entity.boundingBox.intersects(box)) {
                return true
            }
        }
        return false
    }

    private fun fail(message: String) {
        stop()
        if (!finished) {
            ElytraStorageSupport.disablePilot(agent)
            finished = true
            val minecraft = Minecraft.getInstance()
            minecraft.connection?.connection?.disconnect(Component.literal("[EmergencyLanding] $message"))
        }
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
        val player = player ?: return 0.0
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

    private enum class LandingStage { APPROACH_HIGH, LOW_RECOVER }

    private companion object {
        private const val GLIDE_DISTANCE = 700f
        private const val LANDING_RADIUS = 24
    }
}
