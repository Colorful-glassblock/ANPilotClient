package anpilot.client.features.event.impl

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.gui.GuiGraphicsExtractor

class Render2DEvent(val context: GuiGraphicsExtractor, val tickDelta: Float)
class Render3DEvent(val context: LevelRenderContext, val tickDelta: Float)


