package anpilot.client.features.ai.subsystem

import anpilot.client.bootstrap.ANServiceRegistry
import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.Render3DEvent
import anpilot.client.renderer.ANColor
import anpilot.client.renderer.render.ANRender3DEngine
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB

class RenderSystem(private val agent: ANAgent) {
    private val renderPositions = mutableListOf<BlockPos>()
    private var scaffoldTargetPos: BlockPos? = null
    private val scaffoldPositions = mutableListOf<BlockPos>()

    init {
        if (ANServiceRegistry.isInitialized) {
            ANServiceRegistry.runtime.eventBus.subscribe(this)
        }
    }

    fun show(vararg positions: BlockPos) {
        synchronized(renderPositions) {
            renderPositions.clear()
            renderPositions.addAll(positions)
        }
    }

    fun showScaffold(targetPos: BlockPos, path: Collection<BlockPos>) {
        synchronized(renderPositions) {
            scaffoldTargetPos = targetPos
            scaffoldPositions.clear()
            scaffoldPositions.addAll(path)
        }
    }

    fun clearScaffold() {
        synchronized(renderPositions) {
            scaffoldTargetPos = null
            scaffoldPositions.clear()
        }
    }

    fun clear() {
        synchronized(renderPositions) {
            renderPositions.clear()
        }
        clearScaffold()
    }

    fun stop() {
        if (ANServiceRegistry.isInitialized) {
            ANServiceRegistry.runtime.eventBus.unsubscribe(this)
        }
        clear()
    }

    @ANEventHandler
    fun onRender3D(event: Render3DEvent) {
        val list = synchronized(renderPositions) { ArrayList(renderPositions) }
        for (i in list.indices) {
            val pos = list[i] ?: continue
            val box = AABB(pos)
            val color = when (i) {
                0 -> ANColor.rgb(255, 0, 255) 
                1 -> ANColor.rgb(0, 255, 255) 
                else -> ANColor.rgb(255, 255, 0) 
            }
            val line = color.withAlpha(255)
            val fill = color.withAlpha(45)
            ANRender3DEngine.box(event.context, box, line, fill)
        }

        val (target, path) = synchronized(renderPositions) {
            scaffoldTargetPos to ArrayList(scaffoldPositions)
        }

        target?.let { pos ->
            val box = AABB(pos)
            val color = ANColor.rgb(255, 0, 0) 
            val line = color.withAlpha(255)
            val fill = color.withAlpha(45)
            ANRender3DEngine.box(event.context, box, line, fill)
        }

        for (pos in path) {
            val box = AABB(pos)
            val color = ANColor.rgb(0, 255, 0) 
            val line = color.withAlpha(255)
            val fill = color.withAlpha(45)
            ANRender3DEngine.box(event.context, box, line, fill)
        }
    }
}
