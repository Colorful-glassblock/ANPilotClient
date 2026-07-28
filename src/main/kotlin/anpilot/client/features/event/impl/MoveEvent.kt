package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable

class MoveEvent(var x: Double, var y: Double, var z: Double) : Cancellable() {
    var modify = false
    companion object {
        fun get(x: Double, y: Double, z: Double): MoveEvent = MoveEvent(x, y, z)
    }
}