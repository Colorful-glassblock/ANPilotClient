package anpilot.client.renderer.rotation

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

class Rotation(val yaw: Float, val pitch: Float) {

    constructor(entity: Entity) : this(entity.yRot, entity.xRot)

    fun add(other: Rotation): Rotation = Rotation(this.yaw + other.yaw, this.pitch + other.pitch)

    fun subtract(other: Rotation): Rotation = Rotation(this.yaw - other.yaw, this.pitch - other.pitch)

    fun multiply(scale: Float): Rotation = Rotation(this.yaw * scale, this.pitch * scale)

    fun apply(entity: Entity) {
        entity.yRot = yaw
        entity.xRot = pitch
    }

    fun applyToPlayer() {
        Minecraft.getInstance().player?.let { apply(it) }
    }

    fun getComponents(): FloatArray = floatArrayOf(yaw, pitch)

    fun fov(other: Rotation): Float {
        val delta = this.closestDelta(other)
        return Math.sqrt((delta.yaw * delta.yaw + delta.pitch * delta.pitch).toDouble()).toFloat()
    }

    fun closestDelta(other: Rotation): Rotation {
        val dyaw = Mth.wrapDegrees(other.yaw - this.yaw)
        val dpitch = other.pitch - this.pitch
        return Rotation(dyaw, dpitch)
    }

    fun toForwardVector(): Vec3 {
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        return Vec3(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
        )
    }

    fun withYaw(newYaw: Float): Rotation = Rotation(newYaw, this.pitch)

    fun withPitch(newPitch: Float): Rotation = Rotation(this.yaw, newPitch)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rotation) return false
        return yaw == other.yaw && pitch == other.pitch
    }

    override fun hashCode(): Int = 31 * yaw.hashCode() + pitch.hashCode()

    companion object {
        private fun getGcd(): Double {
            val mc = Minecraft.getInstance()
            val sensitivity = mc.options.sensitivity().get().toDouble() * 0.6 + 0.2
            val scaled = sensitivity * sensitivity * sensitivity
            return if (mc.options.cameraType.isFirstPerson && mc.player != null && mc.player!!.isScoping) {
                scaled
            } else {
                scaled * 8.0
            }
        }
        
        fun calculateNewRotation(prev: Rotation, dx: Double, dy: Double): Rotation {
            val gcd = getGcd()
            val delta = Rotation((dx * gcd * 0.15).toFloat(), (dy * gcd * 0.15).toFloat())
            val newRot = prev.add(delta)
            return newRot.withPitch(Mth.clamp(newRot.pitch, -90f, 90f))
        }
    }
}
