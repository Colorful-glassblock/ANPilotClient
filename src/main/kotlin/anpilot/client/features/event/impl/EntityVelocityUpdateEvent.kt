package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

class EntityVelocityUpdateEvent(
    var entity: Entity?,
    var x: Double,
    var y: Double,
    var z: Double,
    var isExplosion: Boolean = false
) : Cancellable() {
    val clientVelocity: Vec3
        get() = Vec3(x, y, z)

    companion object {
        fun get(entity: Entity?, vec3: Vec3, isExplosion: Boolean = false): EntityVelocityUpdateEvent {
            return EntityVelocityUpdateEvent(entity, vec3.x, vec3.y, vec3.z, isExplosion)
        }
    }
}
