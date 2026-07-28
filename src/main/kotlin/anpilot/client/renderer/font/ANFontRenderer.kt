package anpilot.client.renderer.font

import anpilot.client.renderer.render.state.FontTextRenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

class ANFontRenderer(private val fallbackFont: Font) {
    private val atlas: ANStbFontAtlas? by lazy { loadAtlas() }

    fun draw(context: GuiGraphicsExtractor, text: String, x: Float, y: Float, color: Int, scale: Float = 1f, scissorArea: ScreenRectangle? = null) {
        if (x.isNaN() || y.isNaN() || scale.isNaN()) return
        val fontAtlas = atlas
        if (fontAtlas == null) {
            context.text(fallbackFont, text, x.roundToInt(), y.roundToInt(), color)
            return
        }
        context.guiRenderState.addGlyphToCurrentLayer(FontTextRenderState(context.pose(), fontAtlas, text, x.roundToInt().toFloat(), y.roundToInt().toFloat(), color, scissorArea, scale))
    }

    fun drawCentered(context: GuiGraphicsExtractor, text: String, x: Int, y: Int, color: Int, scissorArea: ScreenRectangle? = null) {
        draw(context, text, (x - width(text) / 2f).roundToInt().toFloat(), y.toFloat(), color, 1f, scissorArea)
    }

    fun drawCentered(context: GuiGraphicsExtractor, text: String, x: Int, y: Int, color: Int, scale: Float = 1f, scissorArea: ScreenRectangle? = null) {
        draw(context, text, (x - width(text, scale) / 2f).roundToInt().toFloat(), y.toFloat(), color, scale, scissorArea)
    }

    fun width(text: String, scale: Float = 1f): Int = atlas?.width(text, scale) ?: fallbackFont.width(text)

    fun height(scale: Float = 1f): Int = atlas?.height(scale) ?: fallbackFont.lineHeight

    fun customFontLoaded(): Boolean = atlas != null

    private fun loadAtlas(): ANStbFontAtlas? {
        return runCatching {
            val minecraft = Minecraft.getInstance()
            val resources = minecraft.resourceManager.listResources("fonts") { it.getPath().endsWith(".ttf") }
            val resource = resources[Identifier.fromNamespaceAndPath("anpilotclient", "fonts/an.ttf")]
                ?: resources.values.firstOrNull()
                ?: return null
            ANGlyphCache.create(resource.open())
        }.getOrNull()
    }
}
