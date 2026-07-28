package anpilot.client.features.ai.utils

import anpilot.client.features.ai.agent.ANAgent
import net.minecraft.core.BlockPos

object SnakeExplorer {
    private var initialized = false
    private var startPos: BlockPos? = null
    private var xDir = 1
    private var zDir = 1
    private var spacing = 256
    private var width = 10000
    private var length = 10000
    private var limitLength = true
    private var currentLayer = 0
    private var state = 0
    private var currentTarget: BlockPos? = null

    fun init(spacingValue: Int, widthValue: Int, lengthValue: Int, limit: Boolean, keepPath: Boolean) {
        val player = ANAgent.minecraft.player ?: return
        if (keepPath && initialized && spacing == spacingValue && width == widthValue && length == lengthValue && limitLength == limit) {
            AgentUtils.sendMessage("继续原蛇形路径：起点: $startPos 目标: $currentTarget")
            return
        }

        startPos = player.blockPosition()
        initialized = true
        spacing = spacingValue
        width = widthValue
        length = lengthValue
        limitLength = limit
        xDir = if ((startPos?.x ?: 0) >= 0) 1 else -1
        zDir = if ((startPos?.z ?: 0) >= 0) 1 else -1
        currentLayer = 0
        state = 0
        currentTarget = null
        AgentUtils.sendMessage("探索初始化：方向 X($xDir) Z($zDir) 起点: $startPos")
    }

    fun isInitialized(): Boolean = initialized

    fun target(): BlockPos? {
        if (!initialized) return null
        if (currentTarget == null) currentTarget = next()
        return currentTarget
    }

    fun advance(): BlockPos? {
        currentTarget = next()
        return currentTarget
    }

    private fun next(): BlockPos? {
        val start = startPos ?: return null
        var targetZ = start.z + currentLayer * spacing * zDir
        val targetX = when (state) {
            0 -> {
                state = 1
                start.x + width * xDir
            }
            1 -> {
                currentLayer++
                targetZ = start.z + currentLayer * spacing * zDir
                state = 2
                start.x + width * xDir
            }
            2 -> {
                state = 3
                start.x
            }
            3 -> {
                currentLayer++
                targetZ = start.z + currentLayer * spacing * zDir
                state = 0
                start.x
            }
            else -> start.x
        }

        if (limitLength && currentLayer * spacing >= length) {
            AgentUtils.sendMessage("扫描范围已覆盖完毕")
            return null
        }

        val next = BlockPos(targetX, 180, targetZ)
        AgentUtils.sendMessage("下个搜索点: $next")
        return next
    }
}
