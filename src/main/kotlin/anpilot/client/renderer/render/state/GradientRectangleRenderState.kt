package anpilot.client.renderer.render.state

import anpilot.client.renderer.render.pipeline.ANRenderPipelines
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2fc
import kotlin.math.roundToInt

class GradientRectangleRenderState(
    private val pose: Matrix3x2fc,
    private val x: Float,
    private val y: Float,
    private val width: Float,
    private val height: Float,
    private val topLeftColor: Int,
    private val topRightColor: Int,
    private val bottomRightColor: Int,
    private val bottomLeftColor: Int,
    private val scissorArea: ScreenRectangle?
) : GuiElementRenderState {
    private val bounds = ScreenRectangle(x.roundToInt(), y.roundToInt(), width.roundToInt(), height.roundToInt())

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertex(vertexConsumer, x, y, topLeftColor)
        vertex(vertexConsumer, x, y + height, bottomLeftColor)
        vertex(vertexConsumer, x + width, y + height, bottomRightColor)
        vertex(vertexConsumer, x + width, y, topRightColor)
    }

    override fun pipeline(): RenderPipeline = ANRenderPipelines.GRADIENT_RECTANGLE

    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds

    private fun vertex(vertexConsumer: VertexConsumer, vertexX: Float, vertexY: Float, color: Int) {
        vertexConsumer.addVertexWith2DPose(pose, vertexX, vertexY).setColor(color)
    }
}
