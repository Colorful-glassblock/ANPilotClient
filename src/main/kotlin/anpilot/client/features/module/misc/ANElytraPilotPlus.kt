package anpilot.client.features.module.misc

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.api.module.ANModuleState
import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.utils.BaritoneHelper
import anpilot.client.features.ai.utils.ElytraFileTargets
import anpilot.client.features.ai.utils.SnakeExplorer
import anpilot.client.features.manager.ANConfigManager
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.bootstrap.ANServiceRegistry
import anpilot.client.features.setting.impl.FileSelectSetting

class ANElytraPilotPlus : ANBaseModule(
    name = "ElytraPilotPlus",
    description = "末地寻找鞘翅，自动控制鞘翅长途巡航飞行与避障",
    category = ANModuleCategory.MISC,
    chineseName = "鞘翅飞行员"
) {
    enum class FinderMode { FILE, SCAN }

    val finderMode = addSetting(ANSetting("Mode", FinderMode.SCAN))
    val file = addSetting(ANSetting("File", FileSelectSetting(ANConfigManager::elytraFileNames)) { finderMode.value == FinderMode.FILE })
    val highGlideY = addSetting(ANSetting("H_GlideY", 180f, 100f, 500f))
    val lowGlideY = addSetting(ANSetting("L_GlideY", 150f, 100f, 500f))
    val step = addSetting(ANSetting("ChunkStep", 256f, 256f, 512f) { finderMode.value == FinderMode.SCAN })
    val xWidth = addSetting(ANSetting("X_Width", 10000f, 2000f, 20000f) { finderMode.value == FinderMode.SCAN })
    val zWidthLimit = addSetting(ANSetting("ZLimit", true) { finderMode.value == FinderMode.SCAN })
    val zWidth = addSetting(ANSetting("Z_Width", 10000f, 2000f, 20000f) { finderMode.value == FinderMode.SCAN })
    

    private var agent: ANAgent? = null

    override fun onEnable() {
        if (fullNullCheck()) return

        val moduleManager = ANServiceRegistry.runtime.moduleManager
        val autoLog = moduleManager.get("AutoLog")
        if (autoLog != null && !autoLog.enabled) {
            autoLog.state = ANModuleState.ENABLED
            sendClientMessage("安全校验，已自动开启AutoLog模块")
        }

        val elytraReplace = moduleManager.get("ElytraReplace")
        if (elytraReplace != null && !elytraReplace.enabled) {
            elytraReplace.state = ANModuleState.ENABLED
            sendClientMessage("安全校验，已自动开启 ElytraReplace模块")
        }

        val nextAgent = ANAgent(this)
        agent = nextAgent
        initFinder()
        BaritoneHelper.configure()
        nextAgent.start()
    }

    override fun onDisable() {
        agent?.stop()
        agent = null
        BaritoneHelper.cancel()
    }

    override fun onUnload() {
        onDisable()
    }

    override fun onTick() {
        if (fullNullCheck()){
            disable()
            return
        }
        agent?.tick()
    }

    private fun initFinder() {
        when (finderMode.value) {
            FinderMode.SCAN -> SnakeExplorer.init(
                step.value.toInt(),
                xWidth.value.toInt(),
                zWidth.value.toInt(),
                zWidthLimit.value,
                false
            )

            FinderMode.FILE -> ElytraFileTargets.init(file.value.currentFileName())
        }
    }
}
