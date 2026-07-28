package anpilot.client.renderer.render

import anpilot.client.renderer.ANColor
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.ANPilotRenderTypes
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

object ANRender3DEngine {
    fun line(context: LevelRenderContext, from: Vec3, to: Vec3, color: ANColor) {
        val start = toCameraSpace(context, from)
        val end = toCameraSpace(context, to)
        val normal = lineNormal(start, end)
        val identityPose = context.poseStack().last().copy().apply { setIdentity() }

        context.submitNodeCollector().submitCustomGeometry(context.poseStack(), ANPilotRenderTypes.TRACER_LINES) { _, vertexConsumer ->
            vertexConsumer.addVertex(identityPose, start.x.toFloat(), start.y.toFloat(), start.z.toFloat())
                .setColor(color.red, color.green, color.blue, color.alpha)
                .setNormal(identityPose, normal.x, normal.y, normal.z)
                .setLineWidth(2f)
            vertexConsumer.addVertex(identityPose, end.x.toFloat(), end.y.toFloat(), end.z.toFloat())
                .setColor(color.red, color.green, color.blue, color.alpha)
                .setNormal(identityPose, normal.x, normal.y, normal.z)
                .setLineWidth(2f)
        }
    }

    fun box(context: LevelRenderContext, box: AABB, lineColor: ANColor, fillColor: ANColor? = null, alwaysPass: Boolean = false) {
        val min = toCameraSpace(context, Vec3(box.minX, box.minY, box.minZ))
        val max = toCameraSpace(context, Vec3(box.maxX, box.maxY, box.maxZ))
        fillColor?.takeIf { it.alpha > 0 }?.let { submitBoxFill(context, min.x, min.y, min.z, max.x, max.y, max.z, it) }
        submitBoxLines(context, min.x, min.y, min.z, max.x, max.y, max.z, lineColor, alwaysPass)
    }

    fun cube(context: LevelRenderContext, center: Vec3, size: Double, color: ANColor) {
        box(context, AABB.ofSize(center, size, size, size), color)
    }

    fun crosshairWorldPos(context: LevelRenderContext): Vec3 {
        return ANRender3DCenter.center ?: context.levelState().cameraRenderState.pos
    }

    private fun toCameraSpace(context: LevelRenderContext, pos: Vec3): Vec3 {
        return pos.subtract(context.levelState().cameraRenderState.pos)
    }

    private fun submitBoxLines(context: LevelRenderContext, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, color: ANColor, alwaysPass: Boolean = false) {
        val identityPose = context.poseStack().last().copy().apply { setIdentity() }
        
        fun drawPass(renderType: RenderType, alpha: Int, width: Float) {
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), renderType) { _, vertexConsumer ->
                fun vertex(x: Double, y: Double, z: Double, normal: Vector3f) {
                    vertexConsumer.addVertex(identityPose, x.toFloat(), y.toFloat(), z.toFloat())
                        .setColor(color.red, color.green, color.blue, alpha)
                        .setNormal(identityPose, normal.x, normal.y, normal.z)
                        .setLineWidth(width)
                }

                fun line(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double) {
                    val normal = lineNormal(x1, y1, z1, x2, y2, z2)
                    vertex(x1, y1, z1, normal)
                    vertex(x2, y2, z2, normal)
                }

                line(x1, y1, z1, x2, y1, z1)
                line(x2, y1, z1, x2, y1, z2)
                line(x2, y1, z2, x1, y1, z2)
                line(x1, y1, z2, x1, y1, z1)

                line(x1, y2, z1, x2, y2, z1)
                line(x2, y2, z1, x2, y2, z2)
                line(x2, y2, z2, x1, y2, z2)
                line(x1, y2, z2, x1, y2, z1)

                line(x1, y1, z1, x1, y2, z1)
                line(x2, y1, z1, x2, y2, z1)
                line(x2, y1, z2, x2, y2, z2)
                line(x1, y1, z2, x1, y2, z2)
            }
        }

        if (alwaysPass) {
            drawPass(ANPilotRenderTypes.XRAY_LINES, color.alpha, 2f)
        } else {
            
            drawPass(ANPilotRenderTypes.XRAY_LINES_HIDDEN, (color.alpha * 0.3f).toInt(), 2f)

            
            drawPass(ANPilotRenderTypes.XRAY_LINES_VISIBLE, color.alpha, 2f)
        }
    }

    private fun submitBoxFill(context: LevelRenderContext, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, color: ANColor) {
        val identityPose = context.poseStack().last().copy().apply { setIdentity() }
        context.submitNodeCollector().submitCustomGeometry(context.poseStack(), ANPilotRenderTypes.XRAY_FILLED_BOX) { _, vertexConsumer ->
            fun vertex(x: Double, y: Double, z: Double) {
                vertexConsumer.addVertex(identityPose, x.toFloat(), y.toFloat(), z.toFloat())
                    .setColor(color.red, color.green, color.blue, color.alpha)
            }

            fun quad(ax: Double, ay: Double, az: Double, bx: Double, by: Double, bz: Double, cx: Double, cy: Double, cz: Double, dx: Double, dy: Double, dz: Double) {
                vertex(ax, ay, az)
                vertex(bx, by, bz)
                vertex(cx, cy, cz)
                vertex(dx, dy, dz)
            }

            quad(x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1)
            quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2)
            quad(x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2)
            quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1)
            quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1)
            quad(x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2)
        }
    }

    private fun lineNormal(start: Vec3, end: Vec3): Vector3f = lineNormal(start.x, start.y, start.z, end.x, end.y, end.z)

    private fun lineNormal(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Vector3f {
        val normal = Vector3f((x2 - x1).toFloat(), (y2 - y1).toFloat(), (z2 - z1).toFloat())
        return if (normal.lengthSquared() > 0f) normal.normalize() else Vector3f(0f, 1f, 0f)
    }
}
