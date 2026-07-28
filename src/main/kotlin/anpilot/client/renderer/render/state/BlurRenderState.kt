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
import org.joml.Matrix3x2fc
import kotlin.math.roundToInt

class BlurRenderState(
    private val pose: Matrix3x2fc,
    private val x: Float,
    private val y: Float,
    private val width: Float,
    private val height: Float,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val cornerRadius: Float,
    private val blurRadius: Float,
    private val tintColor: Int,
    private val scissorArea: ScreenRectangle?
) : GuiElementRenderState {
    private val bounds = ScreenRectangle(x.roundToInt(), y.roundToInt(), width.roundToInt(), height.roundToInt())

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertex(vertexConsumer, x, y, x / screenWidth, y / screenHeight, 0, 0)
        vertex(vertexConsumer, x, y + height, x / screenWidth, (y + height) / screenHeight, 0, height.roundToInt())
        vertex(vertexConsumer, x + width, y + height, (x + width) / screenWidth, (y + height) / screenHeight, width.roundToInt(), height.roundToInt())
        vertex(vertexConsumer, x + width, y, (x + width) / screenWidth, y / screenHeight, width.roundToInt(), 0)
    }

    override fun pipeline(): RenderPipeline = ANRenderPipelines.BLUR

    override fun textureSetup(): TextureSetup = TextureSetup.singleTexture(
        Minecraft.getInstance().mainRenderTarget.colorTextureView!!,
        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
    )

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds

    private fun vertex(vertexConsumer: VertexConsumer, screenX: Float, screenY: Float, u: Float, v: Float, localX: Int, localY: Int) {
        vertexConsumer.addVertexWith2DPose(pose, screenX, screenY)
            .setUv(u, v)
            .setUv1(blurRadius.roundToInt().coerceIn(0, 255), packRadiusAndWidth())
            .setUv2(localX.coerceIn(0, 32767), localY.coerceIn(0, 32767))
            .setColor(tintColor)
    }

    private fun packRadiusAndWidth(): Int {
        val radiusValue = cornerRadius.roundToInt().coerceIn(0, 255)
        val widthValue = width.roundToInt().coerceIn(0, 255)
        return radiusValue or (widthValue shl 8)
    }
}
