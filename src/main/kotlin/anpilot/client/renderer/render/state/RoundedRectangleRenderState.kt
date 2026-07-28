package anpilot.client.renderer.render.state

import anpilot.client.renderer.render.pipeline.ANRenderPipelines
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2fc
import kotlin.math.roundToInt

class RoundedRectangleRenderState(
    private val pose: Matrix3x2fc,
    private val x: Float,
    private val y: Float,
    private val width: Float,
    private val height: Float,
    private val radius: Float,
    private val borderWidth: Float,
    private val fillColor: Int,
    private val borderColor: Int,
    private val glowRadius: Float,
    private val glowColor: Int,
    private val scissorArea: ScreenRectangle?
) : GuiElementRenderState {
    private val expandedX = x - glowRadius
    private val expandedY = y - glowRadius
    private val expandedWidth = width + glowRadius * 2f
    private val expandedHeight = height + glowRadius * 2f
    private val bounds = ScreenRectangle(expandedX.roundToInt(), expandedY.roundToInt(), expandedWidth.roundToInt(), expandedHeight.roundToInt())

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertex(vertexConsumer, expandedX, expandedY, -glowRadius, -glowRadius)
        vertex(vertexConsumer, expandedX, expandedY + expandedHeight, -glowRadius, height + glowRadius)
        vertex(vertexConsumer, expandedX + expandedWidth, expandedY + expandedHeight, width + glowRadius, height + glowRadius)
        vertex(vertexConsumer, expandedX + expandedWidth, expandedY, width + glowRadius, -glowRadius)
    }

    override fun pipeline(): RenderPipeline = ANRenderPipelines.ROUNDED_RECTANGLE

    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds

    private fun vertex(vertexConsumer: VertexConsumer, screenX: Float, screenY: Float, localX: Float, localY: Float) {
        vertexConsumer.addVertexWith2DPose(pose, screenX, screenY)
            .setUv(localX, localY)
            .setUv1(packRadiusAndBorder(), packBorderRedGreen())
            .setUv2(packWidth(), packHeight())
            .setColor(fillColor)
    }

    private fun packRadiusAndBorder(): Int {
        val radiusValue = packRadius(radius)
        val borderValue = packBorderWidth(borderWidth)
        return radiusValue or (borderValue shl 8)
    }

    private fun packRadius(value: Float): Int = (value * 8f).roundToInt().coerceIn(0, 255)

    private fun packBorderWidth(value: Float): Int = (value * 16f).roundToInt().coerceIn(0, 255)

    private fun packBorderRedGreen(): Int {
        val redValue = colorRed(borderColor).coerceIn(0, 255)
        val greenValue = colorGreen(borderColor).coerceIn(0, 255)
        return redValue or (greenValue shl 8)
    }

    private fun packWidth(): Int = width.roundToInt().coerceIn(0, 65535) or (colorBlue(borderColor).coerceIn(0, 255) shl 16)

    private fun packHeight(): Int = height.roundToInt().coerceIn(0, 65535) or (colorAlpha(borderColor).coerceIn(0, 255) shl 16)

    private fun colorRed(color: Int): Int = (color ushr 16) and 255

    private fun colorGreen(color: Int): Int = (color ushr 8) and 255

    private fun colorBlue(color: Int): Int = color and 255

    private fun colorAlpha(color: Int): Int = (color ushr 24) and 255
}
