package anpilot.client.features.ai.utils

import net.minecraft.core.BlockPos
import org.slf4j.LoggerFactory

object BaritoneHelper {
    private val logger = LoggerFactory.getLogger("ANPilotBaritone")
    private var warnedMissing = false

    fun configure() {
        runCatching {
            val settings = baritoneApi().getMethod("getSettings").invoke(null)
            setSetting(settings, "allowBreak", false)
            setSetting(settings, "allowPlace", true)
            setSetting(settings, "allowInventory", true)
            setSetting(settings, "allowParkourPlace", true)
            setSetting(settings, "failureTimeoutMS", 5000L)
            setSetting(settings, "allowParkour", true)
            setSetting(settings, "avoidance", true)
            setSetting(settings, "chatDebug", false)
        }.onFailure { warnMissing(it) }
    }

    fun restore() {
        runCatching {
            val settings = baritoneApi().getMethod("getSettings").invoke(null)
            resetSettingToDefault(settings, "allowBreak")
            resetSettingToDefault(settings, "allowPlace")
            resetSettingToDefault(settings, "allowInventory")
            resetSettingToDefault(settings, "allowParkourPlace")
            resetSettingToDefault(settings, "failureTimeoutMS")
            resetSettingToDefault(settings, "allowParkour")
            resetSettingToDefault(settings, "avoidance")
            resetSettingToDefault(settings, "chatDebug")
        }.onFailure { warnMissing(it) }
    }

    fun pathTo(pos: BlockPos): Boolean {
        return runCatching {
            val goal = Class.forName("baritone.api.pathing.goals.GoalBlock")
                .getConstructor(BlockPos::class.java)
                .newInstance(pos)
            val customGoalProcess = primaryBaritone().javaClass.getMethod("getCustomGoalProcess").invoke(primaryBaritone())
            customGoalProcess.javaClass.methods
                .first { it.name == "setGoalAndPath" && it.parameterTypes.size == 1 }
                .invoke(customGoalProcess, goal)
            true
        }.getOrElse { exception ->
            warnMissing(exception)
            false
        }
    }

    fun pathNear(pos: BlockPos, range: Int): Boolean {
        return runCatching {
            val goal = Class.forName("baritone.api.pathing.goals.GoalNear")
                .getConstructor(BlockPos::class.java, Int::class.javaPrimitiveType)
                .newInstance(pos, range)
            val customGoalProcess = primaryBaritone().javaClass.getMethod("getCustomGoalProcess").invoke(primaryBaritone())
            customGoalProcess.javaClass.methods
                .first { it.name == "setGoalAndPath" && it.parameterTypes.size == 1 }
                .invoke(customGoalProcess, goal)
            true
        }.getOrElse { exception ->
            warnMissing(exception)
            false
        }
    }

    fun cancel() {
        runCatching {
            val pathingBehavior = primaryBaritone().javaClass.getMethod("getPathingBehavior").invoke(primaryBaritone())
            pathingBehavior.javaClass.getMethod("cancelEverything").invoke(pathingBehavior)
        }.onFailure { warnMissing(it) }
    }

    fun isPathing(): Boolean {
        return runCatching {
            val pathingBehavior = primaryBaritone().javaClass.getMethod("getPathingBehavior").invoke(primaryBaritone())
            pathingBehavior.javaClass.getMethod("isPathing").invoke(pathingBehavior) as Boolean
        }.getOrDefault(false)
    }

    fun requestPause() {
        runCatching {
            val commandManager = primaryBaritone().javaClass.getMethod("getCommandManager").invoke(primaryBaritone())
            commandManager.javaClass.getMethod("execute", String::class.java).invoke(commandManager, "pause")
        }.onFailure { warnMissing(it) }
    }

    fun requestResume() {
        runCatching {
            val commandManager = primaryBaritone().javaClass.getMethod("getCommandManager").invoke(primaryBaritone())
            commandManager.javaClass.getMethod("execute", String::class.java).invoke(commandManager, "resume")
        }.onFailure { warnMissing(it) }
    }

    private fun primaryBaritone(): Any {
        val provider = baritoneApi().getMethod("getProvider").invoke(null)
        return provider.javaClass.getMethod("getPrimaryBaritone").invoke(provider)
    }

    private fun baritoneApi(): Class<*> = Class.forName("baritone.api.BaritoneAPI")

    private fun setSetting(settings: Any, name: String, value: Any) {
        val setting = settings.javaClass.getField(name).get(settings)
        setting.javaClass.getField("value").set(setting, value)
    }

    private fun resetSettingToDefault(settings: Any, name: String) {
        val setting = settings.javaClass.getField(name).get(settings)
        val defaultValue = setting.javaClass.getField("defaultValue").get(setting)
        setting.javaClass.getField("value").set(setting, defaultValue)
    }

    private fun warnMissing(exception: Throwable) {
        if (!warnedMissing) {
            warnedMissing = true
            AgentUtils.sendMessage("§cBaritone 不可用，船内寻路/自动拾取会暂停。请确认已安装兼容 Baritone。")
            logger.warn("Baritone integration unavailable", exception)
        }
    }
}
