package anpilot.client.renderer.rotation

import net.minecraft.client.player.LocalPlayer

class RotationHandler {
    private var cachedRotation: Rotation? = null

    fun applyRotations(player: LocalPlayer) {
        cachedRotation = Rotation(player)
        
        
    }

    fun revertRotations(player: LocalPlayer) {
        if (player == null || cachedRotation == null) return
        cachedRotation?.apply(player)
        cachedRotation = null
    }

    fun resetRotations(playerRotation: Rotation) {
        
    }
}
