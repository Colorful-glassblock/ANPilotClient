package anpilot.client.features.ai.subsystem

import anpilot.client.features.ai.agent.ANAgent

class RotationSystem(private val agent: ANAgent) {
    private var yaw = 0f
    private var pitch = 0f
    private var paused = false
    private var active = false

    fun request(yaw: Float, pitch: Float) {
        this.yaw = yaw
        this.pitch = pitch
        active = true
    }

    fun tick() {
        if (paused || !active) return
        val player = ANAgent.minecraft.player ?: return
        player.yRot = yaw
        player.xRot = pitch
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun stop() {
        pause()
        active = false
    }
}
