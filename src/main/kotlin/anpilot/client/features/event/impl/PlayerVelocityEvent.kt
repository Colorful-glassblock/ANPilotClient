package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable


class PlayerVelocityEvent : Cancellable() {

    var yaw: Float = 0.0f
}