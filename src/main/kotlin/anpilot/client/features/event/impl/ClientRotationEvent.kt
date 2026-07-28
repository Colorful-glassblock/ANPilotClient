package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable
import anpilot.client.features.manager.rotation.Rotation

class ClientRotationEvent(var rotation: Rotation) : Cancellable(){

    var yaw: Float
        get() = rotation.yaw
        set(value) {
            rotation.yaw = value
        }


    var pitch: Float
        get() = rotation.pitch
        set(value) {
            rotation.pitch = value
        }
}