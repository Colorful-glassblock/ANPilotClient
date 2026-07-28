package anpilot.client.features.ai.task.elytrapilot

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.ElytraFileTargets
import anpilot.client.features.ai.utils.FoundLocationStore
import anpilot.client.features.ai.utils.SnakeExplorer
import anpilot.client.features.module.hud.ANInventory
import anpilot.client.features.module.misc.ANElytraPilotPlus
import anpilot.client.features.ai.utils.FireworkUtils
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.world.item.Items
import net.minecraft.util.Mth
import net.minecraft.world.level.levelgen.Heightmap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3

class EndGlideTask(agent: ANAgent) : AITask(agent) {
    private var target: BlockPos? = null
    private var heightCheckPassPos: BlockPos? = null
    private var lastClimbFireworkAt = 0L

    override fun tick() {
        val player = player ?: run {
            finished = true
            return
        }
        if (ANAgent.minecraft.level == null) {
            finished = true
            return
        }

        
        val shipPos = AgentUtils.scanThroughChunks(256)
        if (shipPos != null && !FoundLocationStore.contains(shipPos)) {
            AgentUtils.sendMessage("扫描到末地船，停止巡航与避障，执行降落")
            agent.scheduler.push(LandingTask(agent, shipPos))
            finished = true
            return
        }

        
        val fireworks = countFireworks()
        if (fireworks < 5) {
            val safePos = findSafeLandingPos()
            if (safePos != null) {
                AgentUtils.sendMessage("烟花不足，执行冗余补给")
                agent.scheduler.push(EmergencyLandingTask(agent, safePos))
                finished = true
                return
            }
        }
        val elytraModule = agent.module as? ANElytraPilotPlus ?: run {
            finished = true
            return
        }

        if (target == null) {
            target = nextTarget(initial = true)
            agent.flight.setTarget(target)
        }
        val currentTarget = target ?: run {
            finished = true
            return
        }

        
        agent.flight.glideFlyTo()

        
        val highestPos = getForwardHighest(20)
        if (highestPos != null) {
            val playerY = player.y
            if (heightCheckPassPos != null) {
                if (playerY - 10 > heightCheckPassPos!!.y) {
                    heightCheckPassPos = null
                } else {
                    val safePitch = getSafePitch(heightCheckPassPos!!)
                    val flyYaw = getYawTo(currentTarget)
                    agent.rotation.request(AgentUtils.lerpYaw(flyYaw, 0.2f), AgentUtils.lerpPitch(safePitch, 0.2f))
                    boostIfReady()
                }
            } else {
                if (playerY - 15 < highestPos.y) {
                    heightCheckPassPos = highestPos.above(5)
                    val safePitch = getSafePitch(heightCheckPassPos!!)
                    val flyYaw = getYawTo(currentTarget)
                    agent.rotation.request(AgentUtils.lerpYaw(flyYaw, 0.2f), AgentUtils.lerpPitch(safePitch, 0.2f))
                    boostIfReady()
                }
            }
        }

        if (AgentUtils.reachedToPos(currentTarget, 5)) {
            if (elytraModule.finderMode.value == ANElytraPilotPlus.FinderMode.FILE) {
                agent.scheduler.push(LandingTask(agent, currentTarget))
                finished = true
                return
            }

            target = nextTarget(initial = false)
            agent.flight.setTarget(target)
            if (target == null) {
                finished = true
                return
            }
        }

        if (!player.isFallFlying) {
            AgentUtils.sendMessage("Glide Lost")
        }

        if(ElytraStorageSupport.countInventoryElytra() <= 1) {
            val minecraft = Minecraft.getInstance()
            minecraft.connection?.connection?.disconnect(Component.literal("[ANElytraPilotPlus] 鞘翅不足，保护离开"))
        }
    }

    private fun getForwardHighest(distance: Int): BlockPos? {
        val player = ANAgent.minecraft.player ?: return null
        val level = ANAgent.minecraft.level ?: return null

        val playerPos = player.position()
        val lookVec = player.lookAngle.normalize()

        val forwardCenter = playerPos.add(lookVec.scale(distance.toDouble()))
        val center = BlockPos(forwardCenter.x.toInt(), forwardCenter.y.toInt(), forwardCenter.z.toInt())

        val leftVec = Vec3(-lookVec.z, 0.0, lookVec.x).normalize()

        val left = center.offset(leftVec.x.toInt(), 0, leftVec.z.toInt())
        val right = center.offset(-leftVec.x.toInt(), 0, -leftVec.z.toInt())

        val topCenter = BlockPos(center.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, center.x, center.z), center.z)
        val topLeft = BlockPos(left.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, left.x, left.z), left.z)
        val topRight = BlockPos(right.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, right.x, right.z), right.z)

        var maxPos = topCenter
        if (topLeft.y > maxPos.y) maxPos = topLeft
        if (topRight.y > maxPos.y) maxPos = topRight

        return maxPos
    }

    private fun getSafePitch(targetPos: BlockPos): Float {
        val player = ANAgent.minecraft.player ?: return 0f
        val dx = targetPos.x + 0.5 - player.x
        val dz = targetPos.z + 0.5 - player.z
        val horizontalDist = sqrt(dx * dx + dz * dz)
        val expectedY = max(targetPos.y.toDouble(), targetPos.y.toDouble() + 20)
        val error = (expectedY - player.y).toFloat()
        val pitch = -error / horizontalDist.toFloat() * 45f
        return Mth.clamp(pitch, -80f, 5f)
    }

    private fun getYawTo(pos: BlockPos): Float {
        val player = ANAgent.minecraft.player ?: return 0f
        val dx = pos.x + 0.5 - player.x
        val dz = pos.z + 0.5 - player.z
        return (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
    }

    private fun boostIfReady() {
        val now = System.currentTimeMillis()
        if (now - lastClimbFireworkAt >= 4000L && FireworkUtils.useFirework()) {
            lastClimbFireworkAt = now
        }
    }

    private fun nextTarget(initial: Boolean): BlockPos? {
        val elytraModule = agent.module as? ANElytraPilotPlus ?: return null
        return when (elytraModule.finderMode.value) {
            ANElytraPilotPlus.FinderMode.SCAN -> if (initial) SnakeExplorer.target() else SnakeExplorer.advance()
            ANElytraPilotPlus.FinderMode.FILE -> if (initial) ElytraFileTargets.target() else ElytraFileTargets.advance()
        }
    }

    private fun countFireworks(): Int {
        val player = ANAgent.minecraft.player ?: return 0
        var count = 0
        val inventory = player.inventory
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (stack.item == Items.FIREWORK_ROCKET) count += stack.count
        }
        return count
    }

    private fun findSafeLandingPos(): BlockPos? {
        val level = ANAgent.minecraft.level ?: return null
        val player = ANAgent.minecraft.player ?: return null
        val pos = player.blockPosition()
        val dir = player.lookAngle
        
        
        for (dist in 0..120 step 4) {
            val checkX = (pos.x + dir.x * dist).toInt()
            val checkZ = (pos.z + dir.z * dist).toInt()
            val surfaceY = getSurfaceY(checkX, checkZ)
            if (surfaceY > 0 && isAreaSafe(checkX, checkZ, surfaceY)) {
                return BlockPos(checkX, surfaceY + 1, checkZ)
            }
        }
        
        
        for (r in 8..80 step 8) {
            for (dx in -r..r step 8) {
                for (dz in -r..r step 8) {
                    val checkX = pos.x + dx
                    val checkZ = pos.z + dz
                    val surfaceY = getSurfaceY(checkX, checkZ)
                    if (surfaceY > 0 && isAreaSafe(checkX, checkZ, surfaceY)) {
                        return BlockPos(checkX, surfaceY + 1, checkZ)
                    }
                }
            }
        }
        return null
    }

    private fun isAreaSafe(x: Int, z: Int, centerY: Int): Boolean {
        
        
        for (dx in -3..3) {
            for (dz in -3..3) {
                if (dx == 0 && dz == 0) continue
                val y = getSurfaceYWindow(x + dx, z + dz, centerY)
                if (y <= 0 || abs(y - centerY) > 3) {
                    return false
                }
            }
        }
        return true
    }

    private fun getSurfaceY(x: Int, z: Int): Int {
        val level = ANAgent.minecraft.level ?: return -1
        for (y in 255 downTo 1) {
            val blockPos = BlockPos(x, y, z)
            val state = level.getBlockState(blockPos)
            if (!state.isAir) {
                if (state.`is`(Blocks.CHORUS_PLANT) ||
                    state.`is`(Blocks.CHORUS_FLOWER)) {
                    continue
                }
                if (!state.getCollisionShape(level, blockPos).isEmpty) {
                    return y
                }
            }
        }
        return -1
    }

    private fun getSurfaceYWindow(x: Int, z: Int, centerY: Int): Int {
        val level = ANAgent.minecraft.level ?: return -1
        
        val startY = (centerY + 5).coerceAtMost(255)
        val endY = (centerY - 5).coerceAtLeast(1)
        for (y in startY downTo endY) {
            val blockPos = BlockPos(x, y, z)
            val state = level.getBlockState(blockPos)
            if (!state.isAir) {
                if (state.`is`(Blocks.CHORUS_PLANT) ||
                    state.`is`(Blocks.CHORUS_FLOWER)) {
                    continue
                }
                if (!state.getCollisionShape(level, blockPos).isEmpty) {
                    return y
                }
            }
        }
        return -1
    }

}
