package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable

class PlayerInputEvent(
    var movementForward: Float, 
    var movementSideways: Float
) : Cancellable()