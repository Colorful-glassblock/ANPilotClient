package anpilot.client.features.ai.subsystem

import anpilot.client.features.ai.agent.ANAgent

class MovementSystem(private val agent: ANAgent) {
    var paused = false

    fun tick() {
        if (paused) return
        
    }

    fun stop() {
        paused = true
        val options = ANAgent.minecraft.options
        options.keyUp.setDown(false)
        options.keyDown.setDown(false)
        options.keyLeft.setDown(false)
        options.keyRight.setDown(false)
        options.keyJump.setDown(false)
        options.keyShift.setDown(false)
    }

    fun jump() {
        ANAgent.minecraft.player?.jumpFromGround()
    }
}
