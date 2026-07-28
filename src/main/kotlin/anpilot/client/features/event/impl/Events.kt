package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable
import anpilot.client.features.manager.rotation.Rotation
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.world.phys.Vec3


class EventPostSync : Cancellable()
class EventPreSync(val yaw: Float, val pitch: Float) : Cancellable() {
    var postAction: Runnable? = null
}
class TickMovementEvent
class InteractSneakEvent : Cancellable()
class EventEntitySpawnPost(val entity: Entity) : Cancellable()
class RenderAfterWorldEvent(val context: Any?)

class EventAttack(val entity: Entity?) : Cancellable()
class EventPostTick
class EventPreTick
class ResourcePacksReloadedEvent

class ServerConnectBeginEvent(val address: ServerAddress)

class EventBreakBlock(val pos: BlockPos) : Cancellable()
