package anpilot.client.renderer.render.state

import anpilot.client.renderer.render.pipeline.ANRenderPipelines
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2fc
import kotlin.math.min
import kotlin.math.roundToInt

class RoundedImageRectangleRenderState(
    private val pose: Matrix3x2fc,
    private val texture: Identifier,
    private val x: Float,
    private val y: Float,
    private val width: Float,
    private val height: Float,
    private val radius: Float,
    private val color: Int,
    private val scissorArea: ScreenRectangle?
) : GuiElementRenderState {
    private val safeRadius = min(radius, min(width, height) / 2f)
    private val bounds = ScreenRectangle(x.roundToInt(), y.roundToInt(), width.roundToInt(), height.roundToInt())

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertex(vertexConsumer, x, y, 0f, 0f)
        vertex(vertexConsumer, x, y + height, 0f, height)
        vertex(vertexConsumer, x + width, y + height, width, height)
        vertex(vertexConsumer, x + width, y, width, 0f)
    }

    override fun pipeline(): RenderPipeline = ANRenderPipelines.ROUNDED_IMAGE_RECTANGLE

    override fun textureSetup(): TextureSetup {
        val textureView = Minecraft.getInstance().textureManager.getTexture(texture).textureView
        return TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
    }

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds

    private fun vertex(vertexConsumer: VertexConsumer, screenX: Float, screenY: Float, localX: Float, localY: Float) {
        vertexConsumer.addVertexWith2DPose(pose, screenX, screenY)
            .setUv(localX, localY)
            .setUv1(packRadius(), 0)
            .setUv2(packWidth(), packHeight())
            .setColor(color)
    }

    private fun packRadius(): Int = (safeRadius * 8f).roundToInt().coerceIn(0, 255)

    private fun packWidth(): Int = width.roundToInt().coerceIn(0, 65535)

    private fun packHeight(): Int = height.roundToInt().coerceIn(0, 65535)
}
