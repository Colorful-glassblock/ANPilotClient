package anpilot.client.features.event.impl 

import anpilot.client.features.event.Cancellable

open class PlayerJumpEvent : Cancellable() {

    class Pre : PlayerJumpEvent()

    class Post : PlayerJumpEvent()

    class Yaw(var yaw: Float) : PlayerJumpEvent()
}