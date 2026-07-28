package anpilot.client.features.manager.rotation

import net.minecraft.client.player.LocalPlayer

object RotationApplier {
    fun apply(player: LocalPlayer, rotation: Rotation) {
        player.yRot = rotation.yaw
        player.xRot = rotation.pitch
    }

    fun restore(player: LocalPlayer, rotation: Rotation) {
        player.yRot = rotation.yaw
        player.xRot = rotation.pitch
    }
}
