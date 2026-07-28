package anpilot.client.renderer.render.state

import anpilot.client.renderer.render.pipeline.ANRenderPipelines
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2fc
import kotlin.math.roundToInt

class RoundedGradientRectangleRenderState(
    private val pose: Matrix3x2fc,
    private val x: Float,
    private val y: Float,
    private val width: Float,
    private val height: Float,
    private val radius: Float,
    private val topLeftColor: Int,
    private val topRightColor: Int,
    private val bottomRightColor: Int,
    private val bottomLeftColor: Int,
    private val scissorArea: ScreenRectangle?
) : GuiElementRenderState {
    private val bounds = ScreenRectangle(x.roundToInt(), y.roundToInt(), width.roundToInt(), height.roundToInt())

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertex(vertexConsumer, x, y, 0f, 0f, topLeftColor)
        vertex(vertexConsumer, x, y + height, 0f, height, bottomLeftColor)
        vertex(vertexConsumer, x + width, y + height, width, height, bottomRightColor)
        vertex(vertexConsumer, x + width, y, width, 0f, topRightColor)
    }

    override fun pipeline(): RenderPipeline = ANRenderPipelines.ROUNDED_GRADIENT_RECTANGLE

    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds

    private fun vertex(vertexConsumer: VertexConsumer, vertexX: Float, vertexY: Float, localX: Float, localY: Float, color: Int) {
        vertexConsumer.addVertexWith2DPose(pose, vertexX, vertexY)
            .setUv(localX, localY)
            .setUv1(radius.roundToInt().coerceIn(0, 255), width.roundToInt().coerceIn(0, 255) or (height.roundToInt().coerceIn(0, 255) shl 8))
            .setColor(color)
    }
}
