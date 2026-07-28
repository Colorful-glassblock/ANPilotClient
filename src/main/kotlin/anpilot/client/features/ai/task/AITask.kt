package anpilot.client.features.ai.task

import anpilot.client.features.ai.agent.ANAgent
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer

abstract class AITask(protected val agent: ANAgent) {
    protected val player: LocalPlayer?
        get() = Minecraft.getInstance().player

    var finished: Boolean = false
        protected set

    open fun start() {}

    abstract fun tick()

    open fun stop() {}
}
