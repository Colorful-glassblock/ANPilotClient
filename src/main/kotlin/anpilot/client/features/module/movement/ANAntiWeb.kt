package anpilot.client.features.module.movement

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3

class ANAntiWeb : ANBaseModule(
    name = "AntiWeb",
    description = "减弱或完全无视蜘蛛网陷阱对移动速度的严重滞缓约束",
    category = ANModuleCategory.MOVEMENT,
    chineseName = "反蜘蛛网"
) {
    enum class Mode {
        NORMAL,
        GRIM
    }

    val mode = addSetting(ANSetting("Mode", Mode.NORMAL))
    val range = addSetting(ANSetting("Range", 1.0f, 0.0f, 3.0f) { mode.value == Mode.GRIM })
    val horizontalFactor = addSetting(ANSetting("H-Factor", 1.0f, 0.0f, 1.0f))
    val verticalFactor = addSetting(ANSetting("V-Factor", 1.0f, 0.0f, 1.0f))

    override fun onTick() {
        val player = mc.player ?: return
        val level = mc.level ?: return
        val connection = mc.connection ?: return
        if (mode.value != Mode.GRIM) return

        val box = player.boundingBox.inflate(range.value.toDouble())
        BlockPos.betweenClosedStream(box)
            .map(BlockPos::immutable)
            .filter { level.getBlockState(it).`is`(Blocks.COBWEB) }
            .forEach { pos ->
                connection.send(
                    ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        pos,
                        Direction.DOWN
                    )
                )
            }
    }

    fun webMultiplier(): Vec3 {
        val horizontal = horizontalFactor.value.toDouble()
        val vertical = verticalFactor.value.toDouble()
        return if (horizontal == 1.0 && vertical == 1.0) {
            Vec3.ZERO
        } else {
            Vec3(horizontal, vertical, horizontal)
        }
    }
}
