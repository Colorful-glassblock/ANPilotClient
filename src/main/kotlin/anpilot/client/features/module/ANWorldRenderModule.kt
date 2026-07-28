package anpilot.client.features.module

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext

interface ANWorldRenderModule {
    fun renderWorld(context: LevelRenderContext)
}
