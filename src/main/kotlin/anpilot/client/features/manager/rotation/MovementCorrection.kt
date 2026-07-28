package anpilot.client.features.manager.rotation

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3

object MovementCorrection {
    fun correctMovement(deltaYaw: Float, forward: Float, sideways: Float): Vec2 {
        val delta = Math.toRadians(Mth.wrapDegrees(deltaYaw).toDouble())
        val cos = Math.cos(delta).toFloat()
        val sin = Math.sin(delta).toFloat()

        val correctedForward = forward * cos + sideways * sin
        val correctedSideways = sideways * cos - forward * sin

        return Vec2(correctedSideways, correctedForward)
    }

    fun correct(forward: Float, sideways: Float, visualYaw: Float, serverYaw: Float): Vec2 {
        return correctMovement(visualYaw - serverYaw, forward, sideways)
    }

    fun correct(input: Vec3, visualYaw: Float, serverYaw: Float): Vec3 {
        val corrected = correct(input.z.toFloat(), input.x.toFloat(), visualYaw, serverYaw)
        return Vec3(corrected.x.toDouble(), input.y, corrected.y.toDouble())
    }
}
