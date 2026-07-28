package anpilot.client.features.module.render

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.ANSettingEvent
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.ItemSelectSetting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.level.block.Block

class ANXRay : ANBaseModule(
    name = "XRay",
    description = "隐藏普通泥土与石头方块，透视渲染矿石与目标方块",
    category = ANModuleCategory.RENDER,
    chineseName = "透视"
) {
    companion object {
        val ORES = listOf(
            "coal_ore", "deepslate_coal_ore",
            "iron_ore", "deepslate_iron_ore",
            "gold_ore", "deepslate_gold_ore",
            "lapis_ore", "deepslate_lapis_ore",
            "redstone_ore", "deepslate_redstone_ore",
            "diamond_ore", "deepslate_diamond_ore",
            "emerald_ore", "deepslate_emerald_ore",
            "copper_ore", "deepslate_copper_ore",
            "nether_gold_ore", "nether_quartz_ore",
            "ancient_debris"
        )
    }

    val whitelist = addSetting(ANSetting("Target", ItemSelectSetting(ORES)))
    val opacity = addSetting(ANSetting("Opacity", 0.0f, 0.0f, 255.0f))
    val exposedOnly = addSetting(ANSetting("ExposedOnly", false))
    val antiAntiXray = addSetting(ANSetting("AntiAntiXray", false))
    val antiAntiXrayRange = addSetting(ANSetting("AAXRange", 15, 1, 32) { antiAntiXray.value })
    val antiAntiXrayPackets = addSetting(ANSetting("AAXPackets", 2, 1, 64) { antiAntiXray.value })
    val antiAntiXrayStep = addSetting(ANSetting("AAXStep", 2, 1, 4) { antiAntiXray.value })
    val antiAntiXrayRescanTicks = addSetting(ANSetting("AAXRescan", 20, 1, 200) { antiAntiXray.value })

    private val EXPOSED_POS = ThreadLocal.withInitial { BlockPos.MutableBlockPos() }
    private var scanOrigin: BlockPos? = null
    private var scanLeftIndex = 0
    private var scanRightIndex = -1
    private var scanCooldownTicks = 0

    override fun onEnable() {
        resetAntiAntiXrayScan()
        mc.levelRenderer.allChanged()
    }

    override fun onDisable() {
        resetAntiAntiXrayScan()
        mc.levelRenderer.allChanged()
    }

    override fun onTick() {
        if (!antiAntiXray.value) {
            resetAntiAntiXrayScan()
            return
        }

        runAntiAntiXrayScan()
    }

    @ANEventHandler
    fun onSettingChange(event: ANSettingEvent) {
        if (event.setting == whitelist || event.setting == opacity || event.setting == exposedOnly) {
            if (enabled) {
                mc.levelRenderer.allChanged()
            }
        }

        if (event.setting == antiAntiXray || event.setting == antiAntiXrayRange || event.setting == antiAntiXrayStep) {
            resetAntiAntiXrayScan()
        }
    }

    fun isVisible(block: Block, pos: BlockPos?): Boolean {
        if (!enabled) return true
        return whitelist.value.contains(block) && (!exposedOnly.value || (pos == null || isExposed(pos)))
    }

    fun isOpacityMode(): Boolean = enabled && opacity.value > 0.0f

    fun opacityAlpha(): Int = opacity.value.toInt().coerceIn(0, 255)

    fun applyOpacity(color: Int): Int = (color and 0x00FFFFFF) or (opacityAlpha() shl 24)

    private fun runAntiAntiXrayScan() {
        val player = mc.player ?: return
        val connection = mc.connection ?: return
        val currentOrigin = player.blockPosition()
        val origin = scanOrigin

        if (origin == null || shouldRestartScan(origin, currentOrigin)) {
            beginAntiAntiXrayScan(currentOrigin)
        }

        if (scanLeftIndex > scanRightIndex) {
            if (scanCooldownTicks > 0) {
                scanCooldownTicks--
            } else {
                beginAntiAntiXrayScan(currentOrigin)
            }
            return
        }

        val activeOrigin = scanOrigin ?: return
        val step = antiAntiXrayStep.value.coerceAtLeast(1)
        val packets = antiAntiXrayPackets.value.coerceAtLeast(1)
        var sent = 0

        while (sent < packets && scanLeftIndex <= scanRightIndex) {
            sendAntiAntiXrayPacket(connection, activeOrigin, scanLeftIndex)
            scanLeftIndex += step
            sent++

            if (sent < packets && scanLeftIndex <= scanRightIndex) {
                sendAntiAntiXrayPacket(connection, activeOrigin, scanRightIndex)
                scanRightIndex -= step
                sent++
            }
        }
    }

    private fun beginAntiAntiXrayScan(origin: BlockPos) {
        val range = antiAntiXrayRange.value.coerceAtLeast(1)
        val size = range * 2 + 1
        scanOrigin = origin.immutable()
        scanLeftIndex = 0
        scanRightIndex = size * size * size - 1
        scanCooldownTicks = antiAntiXrayRescanTicks.value.coerceAtLeast(1)
    }

    private fun resetAntiAntiXrayScan() {
        scanOrigin = null
        scanLeftIndex = 0
        scanRightIndex = -1
        scanCooldownTicks = 0
    }

    private fun shouldRestartScan(origin: BlockPos, currentOrigin: BlockPos): Boolean {
        val range = antiAntiXrayRange.value.coerceAtLeast(1)
        val restartDistance = (range / 2).coerceAtLeast(2)
        return origin.distManhattan(currentOrigin) > restartDistance
    }

    private fun sendAntiAntiXrayPacket(connection: ClientPacketListener, origin: BlockPos, index: Int) {
        val range = antiAntiXrayRange.value.coerceAtLeast(1)
        val size = range * 2 + 1
        val x = index % size
        val z = (index / size) % size
        val y = index / (size * size)
        val pos = origin.offset(x - range, y - range, z - range)

        connection.send(
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                pos,
                Direction.UP
            )
        )
    }

    private fun isExposed(blockPos: BlockPos): Boolean {
        val level = mc.level ?: return false
        val mutablePos = EXPOSED_POS.get()
        for (direction in Direction.entries) {
            mutablePos.set(blockPos).move(direction)
            if (!level.getBlockState(mutablePos).canOcclude()) return true
        }
        return false
    }
}
