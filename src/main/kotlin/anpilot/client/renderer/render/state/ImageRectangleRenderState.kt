package anpilot.client.renderer.render.state

import anpilot.client.renderer.render.pipeline.ANRenderPipelines
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2fc
import kotlin.math.roundToInt
import com.mojang.blaze3d.systems.RenderSystem

class ImageRectangleRenderState(
    private val pose: Matrix3x2fc,
    private val texture: Identifier,
    private val x: Float,
    private val y: Float,
    private val width: Float,
    private val height: Float,
    private val u0: Float,
    private val v0: Float,
    private val u1: Float,
    private val v1: Float,
    private val color: Int,
    private val scissorArea: ScreenRectangle?
) : GuiElementRenderState {
    private val bounds = ScreenRectangle(x.roundToInt(), y.roundToInt(), width.roundToInt(), height.roundToInt())

    constructor(
        pose: Matrix3x2fc,
        texture: Identifier,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Int,
        scissorArea: ScreenRectangle?
    ) : this(pose, texture, x, y, width, height, 0f, 0f, 1f, 1f, color, scissorArea)

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertex(vertexConsumer, x, y, u0, v0)
        vertex(vertexConsumer, x, y + height, u0, v1)
        vertex(vertexConsumer, x + width, y + height, u1, v1)
        vertex(vertexConsumer, x + width, y, u1, v0)
    }

    override fun pipeline(): RenderPipeline = ANRenderPipelines.IMAGE_RECTANGLE

    override fun textureSetup(): TextureSetup {
        val textureView = Minecraft.getInstance().textureManager.getTexture(texture).textureView
        return TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
    }

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds

    private fun vertex(vertexConsumer: VertexConsumer, vertexX: Float, vertexY: Float, u: Float, v: Float) {
        vertexConsumer.addVertexWith2DPose(pose, vertexX, vertexY)
            .setUv(u, v)
            .setColor(color)
    }
}
