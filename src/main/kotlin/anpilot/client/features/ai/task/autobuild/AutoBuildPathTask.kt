package anpilot.client.features.ai.task.autobuild

import anpilot.client.bootstrap.ANServiceRegistry
import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.BaritoneHelper
import anpilot.client.features.manager.rotation.Rotation
import anpilot.client.features.module.misc.ANAutoBuild
import anpilot.client.features.module.misc.BlockPlacer
import anpilot.client.features.utility.ANTimer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.state.BlockState

class AutoBuildPathTask(agent: ANAgent) : AITask(agent) {

    private var currentTarget: BlockPos? = null
    private var lastPos: BlockPos? = null
    private val stuckTimer = ANTimer()
    private var randomTarget: BlockPos? = null
    private var breakingPos: BlockPos? = null

    override fun start() {
        val module = agent.module as? ANAutoBuild ?: return
        module.updateUnbuiltLayerBlocks()

        if (checkInventoryAndRefill(module)) {
            return
        }

        updatePathTarget()
    }

    override fun tick() {
        val module = agent.module as? ANAutoBuild ?: return
        val player = player ?: return

        module.updateUnbuiltLayerBlocks()

        if (module.isSchematicFullyBuilt()) {
            BaritoneHelper.cancel()
            module.building = false
            AgentUtils.sendMessage("整个投影图纸已建造完毕！")
            finished = true
            return
        }

        if (checkInventoryAndRefill(module)) {
            BaritoneHelper.cancel()
            return
        }

        if (module.unbuiltLayerBlocks.isEmpty()) {
            BaritoneHelper.cancel()
            agent.movement.jump()
            return
        }

        val pPos = player.blockPosition()
        if (lastPos != pPos) {
            lastPos = pPos
            if (randomTarget == null) {
                stuckTimer.reset()
            }
        }

        if (randomTarget == null && stuckTimer.passedMs(5000L)) {
            val currentLayerY = module.minWorldY + module.currentBuildLayer
            var builtBlocks = module.placeBlocks.filter { it.pos.y == currentLayerY && module.isBuildTargetBuilt(it) }
            if (builtBlocks.isEmpty()) {
                builtBlocks = module.placeBlocks.filter { module.isBuildTargetBuilt(it) }
            }
            if (builtBlocks.isNotEmpty()) {
                val randomBlock = builtBlocks.random()
                randomTarget = randomBlock.pos.above()
                AgentUtils.sendMessage("CHECK校验，寻路冗余执行")
            } else {
                stuckTimer.reset()
            }
        }

        if (randomTarget != null) {
            val dest = randomTarget!!
            if (AgentUtils.horizontalDistance(dest) <= 1.5 || stuckTimer.passedMs(10000L)) {
                randomTarget = null
                stuckTimer.reset()
                BaritoneHelper.cancel()
            } else {
                if (!BaritoneHelper.isPathing() || currentTarget != dest) {
                    currentTarget = dest
                    if (!BaritoneHelper.pathNear(dest, 1)) {
                        BaritoneHelper.pathTo(dest)
                    }
                }
            }
            return
        }

        updatePathTarget()
    }

    private fun updatePathTarget() {
        val module = agent.module as? ANAutoBuild ?: return
        val player = player ?: return

        val targetBlock = module.unbuiltLayerBlocks.firstOrNull() ?: return
        
        val level = ANAgent.minecraft.level ?: return
        val actualState = level.getBlockState(targetBlock.pos)
        val isWrongOrientation = !actualState.isAir && !actualState.canBeReplaced() && !module.placer.isBlockBuilt(actualState, targetBlock.state)

        if (shouldUseAuxiliaryBlock(targetBlock, actualState)) {
            agent.scheduler.push(AuxiliaryBlockTask(agent, targetBlock.pos))
            return
        }

        val facingProp = targetBlock.state.properties.find {
            it.name == "facing" || it.name == "axis" || it.name == "rotation" || it.name == "orientation"
        }
        val isDirectional = facingProp != null

        val dest = targetBlock.pos.above()
        currentTarget = dest

        val pPos = player.blockPosition()

        if (isDirectional && isWrongOrientation) {
            if (pPos.x == dest.x && pPos.z == dest.z && pPos.y == dest.y) {
                BaritoneHelper.cancel()
                val v = targetBlock.state.getValue(facingProp).toString().lowercase()
                var neededYaw = player.yRot
                val neededPitch = 90f
                when (v) {
                    "north" -> neededYaw = 180f
                    "south" -> neededYaw = 0f
                    "west" -> neededYaw = 90f
                    "east" -> neededYaw = -90f
                }
                val rotation = Rotation(neededYaw, neededPitch)
                ANServiceRegistry.runtime.rotationManager.setSilentRotation(rotation)
                
                val currentLayerY = module.minWorldY + module.currentBuildLayer
                if (pPos.y == currentLayerY) {
                    agent.movement.jump()
                }
                
                val gameMode = ANAgent.minecraft.gameMode
                if (gameMode != null) {
                    if (breakingPos != targetBlock.pos) {
                        gameMode.startDestroyBlock(targetBlock.pos, Direction.UP)
                        breakingPos = targetBlock.pos
                    } else {
                        gameMode.continueDestroyBlock(targetBlock.pos, Direction.UP)
                    }
                    player.swing(InteractionHand.MAIN_HAND)
                }
                return
            }
            
            breakingPos = null
            
            if (!BaritoneHelper.isPathing() || currentTarget != dest) {
                BaritoneHelper.pathTo(dest)
            }
            return
        }

        if (pPos.x == targetBlock.pos.x && pPos.z == targetBlock.pos.z && pPos.y == targetBlock.pos.y) {
            agent.movement.jump()
        }

        if (!BaritoneHelper.isPathing()) {
            if (!BaritoneHelper.pathNear(dest, 1)) {
                BaritoneHelper.pathTo(dest)
            }
        }
    }

    private fun shouldUseAuxiliaryBlock(
        targetBlock: BlockPlacer.PlaceBlock,
        actualState: BlockState
    ): Boolean {
        if (!actualState.isAir) return false
        if (!allowsAuxiliarySupport(targetBlock.state)) return false

        val level = ANAgent.minecraft.level ?: return false
        for (dir in Direction.values()) {
            val neighborState = level.getBlockState(targetBlock.pos.relative(dir))
            if (neighborState.canAnchorAuxiliaryCheck()) return false
        }

        return true
    }

    private fun allowsAuxiliarySupport(state: BlockState): Boolean {
        val blockName = BuiltInRegistries.BLOCK.getKey(state.block).path
        return blockName !in AUXILIARY_DENIED_TARGET_BLOCKS &&
                AUXILIARY_DENIED_TARGET_KEYWORDS.none { blockName.contains(it) }
    }

    private fun BlockState.canAnchorAuxiliaryCheck(): Boolean {
        return !isAir && !canBeReplaced()
    }

    private fun checkInventoryAndRefill(module: ANAutoBuild): Boolean {
        val layerNeededBlocks = module.unbuiltLayerBlocks.map { module.getCanonicalBlock(it.state.block) }.distinct()

        for (block in layerNeededBlocks) {
            val count = module.getInventoryItemCount(block)
            if (count < 5) {
                val refillType = if (count == 0) RefillType.MISSING else RefillType.LOW
                val chests = module.materialChestMap[block]
                if (!chests.isNullOrEmpty()) {
                    val firstChest = chests.first()
                    agent.scheduler.push(
                        MaterialRefillTask(
                            agent,
                            firstChest.pos1,
                            block,
                            chests.map { it.pos1 },
                            refillType
                        )
                    )
                    finished = true
                    return true
                } else if (count == 0) {
                    module.building = false
                    module.sendClientMessage("缺少材料: ${BuiltInRegistries.BLOCK.getKey(block).path} 且未绑定物料箱，已暂停自动建造。")
                    finished = true
                    return true
                }
            }
        }
        return false
    }

    override fun stop() {
        BaritoneHelper.cancel()
    }

    companion object {
        private val AUXILIARY_DENIED_TARGET_BLOCKS = setOf(
            "cobweb",
            "vine",
            "twisting_vines",
            "twisting_vines_plant",
            "weeping_vines",
            "weeping_vines_plant"
        )

        private val AUXILIARY_DENIED_TARGET_KEYWORDS = setOf(
            "vine",
            "web"
        )
    }
}
