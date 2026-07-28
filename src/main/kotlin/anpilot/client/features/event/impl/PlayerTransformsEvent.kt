package anpilot.client.features.event.impl 

import anpilot.client.features.event.Cancellable


class PlayerTransformsEvent(
    val tickDelta: Float, 
    var yaw: Float = 0.0f,
    var pitch: Float = 0.0f
) : Cancellable()