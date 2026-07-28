package anpilot.client.features.manager.rotation

import anpilot.client.bootstrap.ANServiceRegistry
import net.minecraft.client.player.LocalPlayer

class RotationHandler {
    var cachedRotation: Rotation? = null
        private set

    fun applyRotations(player: LocalPlayer) {
        cachedRotation = Rotation(player)
        ANServiceRegistry.runtime.rotationManager.clientRotation?.apply(player)
    }

    fun revertRotations(player: LocalPlayer?) {
        if (player == null || cachedRotation == null) return

        cachedRotation?.apply(player)
        cachedRotation = null
    }

    fun resetRotations(playerRotation: Rotation, speed: Float) {
        if (!ANServiceRegistry.runtime.rotationManager.hasClientRotation()) return
        ANServiceRegistry.runtime.rotationManager.clearClientRotation()
    }
}
