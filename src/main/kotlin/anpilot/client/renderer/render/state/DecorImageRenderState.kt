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
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class DecorImageRenderState(
    private val pose: Matrix3x2fc,
    private val texture: Identifier,
    private val centerX: Float,
    private val centerY: Float,
    private val width: Float,
    private val height: Float,
    private val rotationDegrees: Float,
    private val u0: Float,
    private val v0: Float,
    private val u1: Float,
    private val v1: Float,
    private val color: Int,
    private val scissorArea: ScreenRectangle?
) : GuiElementRenderState {
    private val corners = buildCorners()
    private val bounds = buildBounds()

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertex(vertexConsumer, corners[0], u0, v0)
        vertex(vertexConsumer, corners[1], u0, v1)
        vertex(vertexConsumer, corners[2], u1, v1)
        vertex(vertexConsumer, corners[3], u1, v0)
    }

    override fun pipeline(): RenderPipeline = ANRenderPipelines.DECOR_IMAGE

    override fun textureSetup(): TextureSetup {
        val textureView = Minecraft.getInstance().textureManager.getTexture(texture).textureView
        return TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
    }

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds

    private fun buildCorners(): Array<Point> {
        val halfWidth = width * 0.5f
        val halfHeight = height * 0.5f
        val radians = rotationDegrees * (PI.toFloat() / 180.0f)
        val cos = cos(radians)
        val sin = sin(radians)
        return arrayOf(
            rotate(-halfWidth, -halfHeight, cos, sin),
            rotate(-halfWidth, halfHeight, cos, sin),
            rotate(halfWidth, halfHeight, cos, sin),
            rotate(halfWidth, -halfHeight, cos, sin)
        )
    }

    private fun rotate(localX: Float, localY: Float, cos: Float, sin: Float): Point {
        return Point(
            centerX + localX * cos - localY * sin,
            centerY + localX * sin + localY * cos
        )
    }

    private fun buildBounds(): ScreenRectangle {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (corner in corners) {
            minX = min(minX, corner.x)
            minY = min(minY, corner.y)
            maxX = max(maxX, corner.x)
            maxY = max(maxY, corner.y)
        }
        val left = floor(minX).toInt()
        val top = floor(minY).toInt()
        return ScreenRectangle(left, top, ceil(maxX - minX).toInt(), ceil(maxY - minY).toInt())
    }

    private fun vertex(vertexConsumer: VertexConsumer, point: Point, u: Float, v: Float) {
        vertexConsumer.addVertexWith2DPose(pose, point.x, point.y)
            .setUv(u, v)
            .setColor(color)
    }

    private data class Point(val x: Float, val y: Float)
}
