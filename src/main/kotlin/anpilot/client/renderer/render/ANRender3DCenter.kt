package anpilot.client.renderer.render

import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector4f

object ANRender3DCenter {
    var center: Vec3? = null
        private set

    fun update(projection: Matrix4f, view: Matrix4f, camera: Vec3) {
        val center4 = Vector4f(0f, 0f, 0f, 1f)
            .mul(Matrix4f(projection).invert())
            .mul(Matrix4f(view).invert())
        center4.div(center4.w)
        center = camera.add(center4.x.toDouble(), center4.y.toDouble(), center4.z.toDouble())
    }
}
