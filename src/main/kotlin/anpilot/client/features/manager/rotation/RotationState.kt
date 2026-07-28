package anpilot.client.features.manager.rotation

class RotationState {
    var visualRotation: Rotation = Rotation(0f, 0f)
    var serverRotation: Rotation = Rotation(0f, 0f)
    var actualServerRotation: Rotation = Rotation(0f, 0f)
    var appliedRotation: Rotation = Rotation(0f, 0f)
    var rotationActive: Boolean = false

    fun updateActualServerRotation(rotation: Rotation) {
        actualServerRotation = rotation.copy()
    }
}
