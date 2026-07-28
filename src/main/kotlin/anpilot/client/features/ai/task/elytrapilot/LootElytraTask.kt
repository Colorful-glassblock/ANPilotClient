package anpilot.client.features.ai.task.elytrapilot

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.BaritoneHelper
import anpilot.client.features.ai.utils.FoundLocationStore
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class LootElytraTask(agent: ANAgent, private val shipPos: BlockPos, private val elytraPos: BlockPos) : AITask(agent) {
    private val startElytraCount = countElytra()
    private var lootPos: BlockPos = elytraPos
    private var pathing = false
    private var attackCooldown = 0
    private var elytraSpawnTicks = 0

    override fun start() {
        lootPos = getLootPos(elytraPos)
        agent.rotation.pause()
        pathing = BaritoneHelper.pathTo(lootPos)
        if (!pathing) agent.rotation.resume()
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

        val dist = player.position().distanceTo(Vec3.atCenterOf(lootPos))
        if (dist > 1 && pathing) {
            BaritoneHelper.pathTo(lootPos)
            return
        }

        if (pathing) {
            BaritoneHelper.cancel()
            pathing = false
        }
        agent.rotation.resume()
        lookAt(Vec3.atCenterOf(elytraPos))
        val frame = findElytraFrame()
        if (frame != null && attackCooldown-- <= 0) {
            ANAgent.minecraft.gameMode?.attack(player, frame)
            player.swing(InteractionHand.MAIN_HAND)
            attackCooldown = 10
        }

        
        val droppedElytra = findDroppedElytra()
        if (droppedElytra != null) {
            elytraSpawnTicks++
            if (elytraSpawnTicks > 100) { 
                lookAt(droppedElytra.position())
                ANAgent.minecraft.options.keyUp.setDown(true)
            } else {
                ANAgent.minecraft.options.keyUp.setDown(false)
            }
        }

        if (countElytra() > startElytraCount) {
            FoundLocationStore.save(shipPos)
            AgentUtils.sendMessage("§b[ANElytraPilotPlus] 已收集鞘翅: ${countElytra()}")
            ANAgent.minecraft.options.keyUp.setDown(false)
            if (emptyInventorySlots() < 2) {
                agent.scheduler.push(StoreElytraTask(agent, elytraPos))
            } else {
                agent.scheduler.push(FindTakeoffPositionTask(agent))
            }
            finished = true
        }
    }

    override fun stop() {
        BaritoneHelper.cancel()
        agent.rotation.resume()
        ANAgent.minecraft.options.keyUp.setDown(false)
    }

    private fun getLootPos(framePos: BlockPos): BlockPos {
        val frame = findElytraFrame()
        val facing = frame?.direction ?: getDirectionFromPlayer(framePos)
        val inside = framePos.relative(facing.opposite)
        if (canStandAt(inside)) return inside
        for (direction in Direction.Plane.HORIZONTAL) {
            val pos = framePos.relative(direction)
            if (canStandAt(pos)) return pos
        }
        return inside
    }

    private fun getDirectionFromPlayer(pos: BlockPos): Direction {
        val player = player ?: return Direction.NORTH
        val dx = player.x - (pos.x + 0.5)
        val dz = player.z - (pos.z + 0.5)
        return if (abs(dx) > abs(dz)) {
            if (dx > 0) Direction.EAST else Direction.WEST
        } else {
            if (dz > 0) Direction.SOUTH else Direction.NORTH
        }
    }

    private fun canStandAt(pos: BlockPos): Boolean {
        val level = ANAgent.minecraft.level ?: return false
        return !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty &&
            level.getBlockState(pos).getCollisionShape(level, pos).isEmpty &&
            level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty
    }

    private fun countElytra(): Int {
        val player = ANAgent.minecraft.player ?: return 0
        var count = 0
        val inventory = player.inventory
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (stack.item == Items.ELYTRA) count += stack.count
        }
        return count
    }

    private fun emptyInventorySlots(): Int {
        val player = ANAgent.minecraft.player ?: return 0
        var count = 0
        for (slot in 0 until 36) {
            if (player.inventory.getItem(slot).isEmpty) count++
        }
        return count
    }

    private fun findElytraFrame(): ItemFrame? {
        val level = ANAgent.minecraft.level ?: return null
        for (entity in level.entitiesForRendering()) {
            val frame = entity as? ItemFrame ?: continue
            if (frame.blockPosition() == elytraPos && frame.item.item == Items.ELYTRA) return frame
        }
        return null
    }

    private fun lookAt(pos: Vec3) {
        val player = player ?: return
        val dx = pos.x - player.x
        val dz = pos.z - player.z
        val dy = pos.y - player.eyeY
        val horizontal = sqrt(dx * dx + dz * dz)
        val yaw = (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
        val pitch = (-Math.toDegrees(atan2(dy, horizontal))).toFloat()
        agent.rotation.request(yaw, pitch)
    }

    private fun findDroppedElytra(): ItemEntity? {
        val level = ANAgent.minecraft.level ?: return null
        val player = player ?: return null
        var bestEntity: ItemEntity? = null
        var bestDistance = Double.MAX_VALUE
        for (entity in level.entitiesForRendering()) {
            val itemEntity = entity as? ItemEntity ?: continue
            if (itemEntity.item.item == Items.ELYTRA) {
                val dist = itemEntity.distanceToSqr(player)
                if (dist < bestDistance) {
                    bestDistance = dist
                    bestEntity = itemEntity
                }
            }
        }
        return bestEntity
    }
}
