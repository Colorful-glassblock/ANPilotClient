package anpilot.client.features.module.misc

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.flyto.FlyToTakeOffTask
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.network.chat.Component

class ANFlyTo : ANBaseModule(
    name = "FlyTo",
    description = "设定目标 X/Z 坐标，自动使用鞘翅滑翔飞行并平稳降落到附近安全点",
    category = ANModuleCategory.MISC,
    chineseName = "自动飞向坐标"
) {
    val targetX = addSetting(ANSetting("TargetX", 0, -30_000_000, 30_000_000))
    val targetZ = addSetting(ANSetting("TargetZ", 0, -30_000_000, 30_000_000))
    val highGlideY = addSetting(ANSetting("H_GlideY", 180f, 80f, 500f))
    val lowGlideY = addSetting(ANSetting("L_GlideY", 150f, 60f, 500f))
    val leaveOnNoLanding = addSetting(ANSetting("NoLandingLeave", false))

    private var agent: ANAgent? = null
    private var completing = false

    override fun onEnable() {
        if (fullNullCheck()) return
        completing = false
        val nextAgent = ANAgent(this)
        agent = nextAgent
        nextAgent.scheduler.push(FlyToTakeOffTask(nextAgent))
    }

    override fun onDisable() {
        agent?.stop()
        agent = null
        completing = false
    }

    override fun onUnload() {
        onDisable()
    }

    override fun onTick() {
        if (fullNullCheck()) return
        agent?.tick()
    }

    fun complete(message: String) {
        if (completing) return
        completing = true
        ANAgent.minecraft.player?.sendSystemMessage(Component.literal("[ANFlyTo] $message"))
        disable()
    }

    fun noLandingFound() {
        if (leaveOnNoLanding.value) {
            ANAgent.minecraft.connection?.connection?.disconnect(Component.literal("[ANFlyTo] No landing block found"))
        }
        complete("No landing block found near current position")
    }

    companion object {
        const val REACH_RANGE = 10
        const val LANDING_SEARCH = 50
    }
}
