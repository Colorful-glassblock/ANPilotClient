package anpilot.client.features.ai.task.elytrapilot

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.BaritoneHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks

class FindTakeoffPositionTask(agent: ANAgent) : AITask(agent) {
    private var takeoffPos: BlockPos? = null
    private var pathing = false

    override fun start() {
        if (AgentUtils.agentNullCheck()) {
            finished = true
            return
        }
        takeoffPos = findDragonHeadAttachedBlock(40)?.above(5)
        if (takeoffPos == null) {
            AgentUtils.sendMessage("没找到可起飞出口，尝试原地起飞")
            localTakeoff()
            finished = true
            return
        }
        agent.rotation.pause()
        pathing = BaritoneHelper.pathTo(takeoffPos!!)
        if (!pathing) {
            agent.rotation.resume()
            AgentUtils.sendMessage("寻路至起飞点失败，尝试原地起飞")
            localTakeoff()
            finished = true
        }
    }

    override fun tick() {
        val player = player ?: run {
            finished = true
            return
        }
        val pos = takeoffPos ?: run {
            finished = true
            return
        }
        BaritoneHelper.pathTo(pos)
        if (player.blockPosition().closerThan(pos, 1.5)) {
            agent.rotation.resume()
            agent.scheduler.push(TakeOffTask(agent))
            finished = true
        }
    }

    override fun stop() {
        if (pathing) BaritoneHelper.cancel()
        agent.rotation.resume()
    }

    private fun findDragonHeadAttachedBlock(radius: Int): BlockPos? {
        val level = ANAgent.minecraft.level ?: return null
        val player = ANAgent.minecraft.player ?: return null
        val origin = player.blockPosition()
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val check = origin.offset(x, y, z)
                    val block = level.getBlockState(check).block
                    if (block == Blocks.DRAGON_HEAD || block == Blocks.DRAGON_WALL_HEAD) {
                        return getStartFly(check)
                    }
                }
            }
        }
        return null
    }

    private fun getStartFly(dragonHeadPos: BlockPos): BlockPos? {
        val level = ANAgent.minecraft.level ?: return null
        for (dir in Direction.Plane.HORIZONTAL) {
            val offset = dragonHeadPos.relative(dir)
            if (!level.getBlockState(offset).isAir) return offset
        }
        return null
    }

    private fun localTakeoff() {

        agent.scheduler.push(TakeOffTask(agent))
    }

}
