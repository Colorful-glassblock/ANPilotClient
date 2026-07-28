package anpilot.client.renderer.render.state

import anpilot.client.renderer.render.pipeline.ANRenderPipelines
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2fc
import kotlin.math.roundToInt

class RoundedBorderRectangleRenderState(
    private val pose: Matrix3x2fc,
    private val x: Float,
    private val y: Float,
    private val width: Float,
    private val height: Float,
    private val radius: Float,
    private val borderWidth: Float,
    private val fillColor: Int,
    private val borderColor: Int,
    private val scissorArea: ScreenRectangle?
) : GuiElementRenderState {
    private val bounds = ScreenRectangle(x.roundToInt(), y.roundToInt(), width.roundToInt(), height.roundToInt())

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertex(vertexConsumer, x, y, 0f, 0f)
        vertex(vertexConsumer, x, y + height, 0f, height)
        vertex(vertexConsumer, x + width, y + height, width, height)
        vertex(vertexConsumer, x + width, y, width, 0f)
    }

    override fun pipeline(): RenderPipeline = ANRenderPipelines.ROUNDED_BORDER_RECTANGLE

    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds

    private fun vertex(vertexConsumer: VertexConsumer, vertexX: Float, vertexY: Float, uvX: Float, uvY: Float) {
        vertexConsumer.addVertexWith2DPose(pose, vertexX, vertexY)
            .setUv(uvX, uvY)
            .setUv1(packRadiusBorder(), packBlueAlphaWidthLo())
            .setUv2(packWidthHiHeightLo(), packHeightHiGreen())
            .setColor(fillColor)
    }

    
    private fun packRadiusBorder(): Int {
        val r = packRadius(radius)
        val bw = packBorderWidth(borderWidth)
        return r or (bw shl 8)
    }

    private fun packRadius(value: Float): Int = (value * 8f).roundToInt().coerceIn(0, 255)

    private fun packBorderWidth(value: Float): Int = (value * 16f).roundToInt().coerceIn(0, 255)

    
    private fun packBlueAlphaWidthLo(): Int {
        val blue = (colorBlue(borderColor) * 15 / 255).coerceIn(0, 15)
        val alpha = (colorAlpha(borderColor) * 15 / 255).coerceIn(0, 15)
        val wLo = width.roundToInt().coerceIn(0, 65535) and 0xFF
        return blue or (alpha shl 4) or (wLo shl 8)
    }

    
    private fun packWidthHiHeightLo(): Int {
        val wHi = (width.roundToInt().coerceIn(0, 65535) shr 8) and 0xFF
        val hLo = height.roundToInt().coerceIn(0, 65535) and 0xFF
        return wHi or (hLo shl 8)
    }

    
    private fun packHeightHiGreen(): Int {
        val hHi = (height.roundToInt().coerceIn(0, 65535) shr 8) and 0xFF
        val green = (colorGreen(borderColor) * 15 / 255).coerceIn(0, 15)
        val red = (colorRed(borderColor) * 15 / 255).coerceIn(0, 15)
        return hHi or (green shl 8) or (red shl 12)
    }

    private fun colorRed(color: Int): Int = (color ushr 16) and 255

    private fun colorGreen(color: Int): Int = (color ushr 8) and 255

    private fun colorBlue(color: Int): Int = color and 255

    private fun colorAlpha(color: Int): Int = (color ushr 24) and 255
}
