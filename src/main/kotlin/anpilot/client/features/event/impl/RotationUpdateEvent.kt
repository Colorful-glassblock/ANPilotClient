package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable


open class RotationUpdateEvent(val yaw: Float, val pitch: Float) : Cancellable() {

    class Pre : RotationUpdateEvent(0.0f, 0.0f)
    class PrePacket : RotationUpdateEvent(0.0f, 0.0f)
}