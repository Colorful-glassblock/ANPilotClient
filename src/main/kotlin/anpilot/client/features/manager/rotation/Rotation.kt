package anpilot.client.features.manager.rotation

import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import java.util.ArrayList
import java.util.Comparator

class Rotation(var yaw: Float, var pitch: Float) {

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

    fun copy(): Rotation = Rotation(yaw, pitch)

    fun wrap(): Rotation {
        yaw = Mth.wrapDegrees(yaw)
        pitch = Mth.clamp(pitch, -90.0f, 90.0f)
        return this
    }

    fun correctSensitivity(prev: Rotation): Rotation {
        val delta = closestDelta(prev)
        val options = approximateCursorDeltas(delta)

        return options.stream()
            .min(Comparator.comparingDouble { this.fov(it).toDouble() })
            .orElse(this)
    }

    fun smoothedTurn(target: Rotation, smoothness: Double): Rotation {
        val delta = target.closestDelta(this).multiply(smoothness.toFloat())
        return this.add(delta)
    }

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
            Math.sin(-yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(-yawRad) * Math.cos(pitchRad)
        )
    }

    fun withYaw(newYaw: Float): Rotation = Rotation(newYaw, this.pitch)

    fun withPitch(newPitch: Float): Rotation = Rotation(this.yaw, newPitch)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val rotation = other as Rotation
        return rotation.yaw.compareTo(yaw) == 0 && rotation.pitch.compareTo(pitch) == 0
    }

    override fun hashCode(): Int {
        var result = yaw.hashCode()
        result = 31 * result + pitch.hashCode()
        return result
    }

    companion object {
        fun calculateNewRotation(prev: Rotation, dx: Double, dy: Double): Rotation {
            val gcd = getGcd()
            val delta = Rotation((dx * gcd * 0.15f).toFloat(), (dy * gcd * 0.15f).toFloat())
            var newRot = prev.add(delta)

            newRot = newRot.withPitch(Mth.clamp(newRot.pitch, -90.0f, 90.0f))
            return newRot
        }

        fun approximateCursorDeltas(deltaRotation: Rotation): List<Rotation> {
            val gcd = getGcd() * 0.15f
            val tx = -deltaRotation.yaw / gcd
            val ty = -deltaRotation.pitch / gcd

            val possibilities = ArrayList<Rotation>()
            possibilities.add(calculateNewRotation(Rotation(0f, 0f), Math.floor(tx), Math.floor(ty)))
            possibilities.add(calculateNewRotation(Rotation(0f, 0f), Math.ceil(tx), Math.floor(ty)))
            possibilities.add(calculateNewRotation(Rotation(0f, 0f), Math.ceil(tx), Math.ceil(ty)))
            possibilities.add(calculateNewRotation(Rotation(0f, 0f), Math.floor(tx), Math.ceil(ty)))
            return possibilities
        }

        private fun getGcd(): Double {
            val mc = Minecraft.getInstance()

            val sensitivity = mc.options.sensitivity().get() * 0.6 + 0.2
            val scaled = sensitivity * sensitivity * sensitivity

            val isSpyglass = mc.player?.isScoping == true
            return if (mc.options.cameraType.isFirstPerson && mc.player != null && isSpyglass) scaled else scaled * 8.0
        }
    }
}
