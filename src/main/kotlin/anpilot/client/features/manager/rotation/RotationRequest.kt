package anpilot.client.features.manager.rotation

data class RotationRequest(
    val rotation: Rotation,
    val priority: RotationPriority,
    val owner: String,
    val yawStep: Float = 360.0f,
    val pitchStep: Float = 180.0f,
    val movementFix: MovementFix = MovementFix.OFF,
    val mouseSensitivityFix: Boolean = true
)
