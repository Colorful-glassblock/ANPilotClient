package anpilot.client.features.ai.task.autobuild

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.BaritoneHelper
import anpilot.client.features.module.misc.ANAutoBuild
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import anpilot.client.features.utility.ANTimer

class AutoBuildBootTask(agent: ANAgent) : AITask(agent) {

    private var targetPos: BlockPos? = null
    private var lastPos: BlockPos? = null
    private val stuckTimer = ANTimer()
    private var isStuck = false

    override fun start() {
        val module = agent.module as? ANAutoBuild ?: return
        val p = player ?: return
        val pPos = p.blockPosition()

        if (module.isSchematicFullyBuilt()) {
            module.building = false
            AgentUtils.sendMessage("所有图纸方块均已建造完毕！")
            finished = true
            return
        }

        if (module.layerBuild.value > 0) {
            while (module.currentBuildLayer < (module.maxWorldY - module.minWorldY)) {
                module.updateUnbuiltLayerBlocks()
                if (module.unbuiltLayerBlocks.isNotEmpty()) {
                    break
                }
                module.currentBuildLayer++
                AgentUtils.sendMessage("Layer${module.currentBuildLayer}完成! 级进到Layer${module.currentBuildLayer + 1}")
                module.updateFilteredProjection()
            }
        }

        module.updateUnbuiltLayerBlocks()



        if (checkInventoryAndRefill(module)) {
            return
        }

        val closest = module.unbuiltLayerBlocks.minByOrNull { it.pos.distSqr(pPos) }
        if (closest == null) {
            finished = true
            return
        }

        val dest = closest.pos
        targetPos = dest

        if (!BaritoneHelper.pathTo(dest)) {
            finished = true
        }
    }

    override fun tick() {
        val module = agent.module as? ANAutoBuild ?: return
        val dest = targetPos ?: return
        val player = player ?: return

        if (checkInventoryAndRefill(module)) {
            BaritoneHelper.cancel()
            return
        }

        val pPos = player.blockPosition()
        if (lastPos != pPos) {
            lastPos = pPos
            if (!isStuck) {
                stuckTimer.reset()
            }
        }

        if (!isStuck && stuckTimer.passedMs(5000L)) {
            isStuck = true
            stuckTimer.reset()
            AgentUtils.sendMessage("CHECK:寻路冗余校验")
            BaritoneHelper.pathTo(dest.above())
        }

        val pathTarget = if (isStuck) dest.above() else dest

        if (isStuck) {
            if (AgentUtils.horizontalDistance(pathTarget) <= 1.5 || stuckTimer.passedMs(10000L)) {
                isStuck = false
                stuckTimer.reset()
                BaritoneHelper.cancel()
            } else {
                if (!BaritoneHelper.isPathing()) {
                    BaritoneHelper.pathTo(pathTarget)
                }
            }
        } else {
            if (AgentUtils.horizontalDistance(dest) <= 1.5) {
                BaritoneHelper.cancel()
                agent.movement.jump()
                agent.scheduler.push(AutoBuildPathTask(agent))
                finished = true
            } else {
                if (!BaritoneHelper.isPathing()) {
                    BaritoneHelper.pathTo(dest)
                }
            }
        }
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
                    module.sendClientMessage("缺少材料: ${BuiltInRegistries.BLOCK.getKey(block).path} 且未绑定物料箱,已暂停自动建造")
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
}
