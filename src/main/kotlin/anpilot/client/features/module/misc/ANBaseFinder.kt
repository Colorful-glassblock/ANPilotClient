package anpilot.client.features.module.misc

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.basefinder.BaseFinderTask
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting

class ANBaseFinder : ANBaseModule(
    name = "BaseFinder",
    description = "乘鞘翅飞行巡航时自动扫描并定位附近的玩家基地",
    category = ANModuleCategory.MISC,
    chineseName = "基地扫描"
) {
    val page = addSetting(ANSetting("Page", Page.MAIN))

    val resume = addSetting(ANSetting("Resume", true) { isPage(Page.MAIN) })
    val chunkRange = addSetting(ANSetting("ChunkRange", 5f, 1f, 20f) { isPage(Page.MAIN) })
    val step = addSetting(ANSetting("ChunkStep", 512f, 128f, 1024f) { isPage(Page.MAIN) })
    val altitude = addSetting(ANSetting("FlyAltitude", 200f, 100f, 320f) { isPage(Page.MAIN) })
    val chunkScanRadius = addSetting(ANSetting("ScanChunks", 6f, 1f, 12f) { isPage(Page.MAIN) })
    val chunksPerTick = addSetting(ANSetting("ChunksPerTick", 2f, 1f, 8f) { isPage(Page.MAIN) })
    val minFireworks = addSetting(ANSetting("MinFireworks", 20f, 1f, 128f) { isPage(Page.MAIN) })
    val checkpointSeconds = addSetting(ANSetting("CheckpointSec", 5f, 1f, 60f) { isPage(Page.MAIN) })
    val disconnectAfterLanding = addSetting(ANSetting("LandingLeave", false) { isPage(Page.MAIN) })
    val xaeroWaypoint = addSetting(ANSetting("XaeroWaypoint", true) { isPage(Page.MAIN) })

    val singleChest = addSetting(ANSetting("单箱", 0, 0, 100) { isPage(Page.CONTAINER) })
    val doubleChest = addSetting(ANSetting("大箱", 5, 0, 100) { isPage(Page.CONTAINER) })
    val barrel = addSetting(ANSetting("木桶", 5, 0, 100) { isPage(Page.CONTAINER) })
    val shulkerBox = addSetting(ANSetting("潜影盒", 5, 0, 100) { isPage(Page.CONTAINER) })
    val enderChest = addSetting(ANSetting("末影箱", 5, 0, 100) { isPage(Page.CONTAINER) })
    val hopper = addSetting(ANSetting("漏斗", 5, 0, 100) { isPage(Page.CONTAINER) })
    val furnace = addSetting(ANSetting("熔炉", 5, 0, 100) { isPage(Page.CONTAINER) })
    val brewingStand = addSetting(ANSetting("酿造台", 5, 0, 100) { isPage(Page.CONTAINER) })

    val beacon = addSetting(ANSetting("信标", 5, 0, 20) { isPage(Page.BLOCKS) })
    val conduit = addSetting(ANSetting("潮涌核心", 5, 0, 20) { isPage(Page.BLOCKS) })
    val bed = addSetting(ANSetting("床", 5, 0, 100) { isPage(Page.BLOCKS) })

    private var agent: ANAgent? = null

    override fun onEnable() {
        if (fullNullCheck()) return
        val nextAgent = ANAgent(this)
        agent = nextAgent
        nextAgent.scheduler.push(BaseFinderTask(nextAgent, this))
        nextAgent.start()
    }

    override fun onDisable() {
        agent?.stop()
        agent = null
    }

    override fun onUnload() {
        onDisable()
    }

    override fun onTick() {
        if (fullNullCheck()) return
        agent?.tick()
    }

    fun searchRadiusBlocks(): Int {
        return (chunkRange.value * 10_000f).toInt().coerceAtLeast(step.value.toInt())
    }

    fun thresholdFor(category: String, key: String): Int {
        return when (category) {
            "containers" -> containerThreshold(key)
            "blocks" -> blockThreshold(key)
            else -> 0
        }
    }

    private fun containerThreshold(key: String): Int {
        return when (key) {
            "chest" -> singleChest.value
            "double_chest" -> doubleChest.value
            "barrel" -> barrel.value
            "shulker_box" -> shulkerBox.value
            "ender_chest" -> enderChest.value
            "hopper" -> hopper.value
            "furnace" -> furnace.value
            "brewing_stand" -> brewingStand.value
            else -> 0
        }
    }

    private fun blockThreshold(key: String): Int {
        return when (key) {
            "beacon" -> beacon.value
            "conduit" -> conduit.value
            "bed" -> bed.value
            else -> 0
        }
    }

    private fun isPage(target: Page): Boolean = page.value == target

    enum class Page {
        MAIN,
        CONTAINER,
        BLOCKS
    }
}
