package anpilot.client.renderer.render.state

import anpilot.client.renderer.font.ANStbFontAtlas
import anpilot.client.renderer.render.pipeline.ANRenderPipelines
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2fc
import kotlin.math.roundToInt

class FontTextRenderState(
    private val pose: Matrix3x2fc,
    private val atlas: ANStbFontAtlas,
    private val text: String,
    private val x: Float,
    private val y: Float,
    private val color: Int,
    private val scissorArea: ScreenRectangle?,
    private val scale: Float = 1f
) : GuiElementRenderState {
    private val bounds = ScreenRectangle(x.roundToInt(), y.roundToInt(), atlas.width(text, scale), atlas.height(scale))

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        atlas.uploadIfNeeded()
        var currentX = x.roundToInt().toFloat()
        val effectiveScale = atlas.renderScale * scale
        val baselineY = atlas.baselineY(y.roundToInt().toFloat(), scale).roundToInt().toFloat()
        text.codePoints().forEach { codepoint ->
            val glyph = atlas.glyph(codepoint) ?: return@forEach
            val x0 = currentX + glyph.x0 * effectiveScale
            val y0 = baselineY + glyph.y0 * effectiveScale
            val x1 = currentX + glyph.x1 * effectiveScale
            val y1 = baselineY + glyph.y1 * effectiveScale
            vertex(vertexConsumer, x0, y0, glyph.u0, glyph.v0)
            vertex(vertexConsumer, x0, y1, glyph.u0, glyph.v1)
            vertex(vertexConsumer, x1, y1, glyph.u1, glyph.v1)
            vertex(vertexConsumer, x1, y0, glyph.u1, glyph.v0)
            currentX += glyph.xAdvance * effectiveScale
        }
    }

    override fun pipeline(): RenderPipeline = ANRenderPipelines.FONT_TEXT

    override fun textureSetup(): TextureSetup = TextureSetup.singleTexture(atlas.textureView, atlas.sampler)

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds

    private fun vertex(vertexConsumer: VertexConsumer, x: Float, y: Float, u: Float, v: Float) {
        vertexConsumer.addVertexWith2DPose(pose, x, y)
            .setUv(u, v)
            .setColor(color)
    }
}
