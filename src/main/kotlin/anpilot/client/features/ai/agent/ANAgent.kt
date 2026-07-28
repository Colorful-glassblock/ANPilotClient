package anpilot.client.features.ai.agent

import anpilot.client.features.ai.scheduler.ANTaskScheduler
import anpilot.client.features.ai.subsystem.FlightSystem
import anpilot.client.features.ai.subsystem.MovementSystem
import anpilot.client.features.ai.subsystem.NavigationSystem
import anpilot.client.features.ai.subsystem.RotationSystem
import anpilot.client.features.ai.subsystem.SafetySystem
import anpilot.client.features.ai.subsystem.RenderSystem
import anpilot.client.features.ai.task.elytrapilot.TakeOffTask
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.module.misc.ANElytraPilotPlus
import anpilot.client.features.module.misc.ANAutoBuild
import anpilot.client.features.ai.task.autobuild.AutoBuildBootTask
import anpilot.client.features.ai.task.autoenchant.BootTask
import anpilot.client.features.module.player.ANAutoEnchant
import net.minecraft.client.Minecraft

class ANAgent(val module: ANBaseModule) {
    val scheduler = ANTaskScheduler(this)
    val navigation = NavigationSystem(this)
    val flight = FlightSystem(this)
    val safety = SafetySystem(this)
    val rotation = RotationSystem(this)
    val movement = MovementSystem(this)
    val render = RenderSystem(this)

    fun start() {
        if (module is ANElytraPilotPlus) {
            scheduler.push(TakeOffTask(this))
        } else if (module is ANAutoBuild) {
            scheduler.push(AutoBuildBootTask(this))
        } else if (module is ANAutoEnchant) {
            scheduler.push(BootTask(this))
        }
    }

    fun tick() {
        scheduler.tick()
        navigation.tick()
        rotation.tick()
        movement.tick()
    }

    fun stop() {
        scheduler.stop()
        navigation.stop()
        flight.stop()
        rotation.stop()
        movement.stop()
        render.stop()
    }

    companion object {
        val minecraft: Minecraft
            get() = Minecraft.getInstance()
    }
}
