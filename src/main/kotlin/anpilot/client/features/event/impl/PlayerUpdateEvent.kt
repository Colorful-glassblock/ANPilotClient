package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable


open class PlayerUpdateEvent : Cancellable() {

    class Pre : PlayerUpdateEvent()
    class Peri : PlayerUpdateEvent()
    class PrePacket : PlayerUpdateEvent()
    class Post : PlayerUpdateEvent()
}