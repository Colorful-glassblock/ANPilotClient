package anpilot.client.features.module.movement

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3

class ANBoost : ANBaseModule(
    name = "Boost",
    description = "开启瞬间赋予玩家朝看向方向的爆发位移加速度(位移冲刺)",
    category = ANModuleCategory.MOVEMENT,
    chineseName = "速度爆发"
) {
    val strength = addSetting(ANSetting("Boost", 4.0, 0.1, 10.0))

    override fun onEnable() {
        if (mc.player != null) {
            boost()
        }
        toggle()
    }

    private fun boost() {
        val player = mc.player ?: return
        val forward = player.forward
        val v = Vec3(forward.x * strength.value, forward.y * strength.value, forward.z * strength.value)
        player.push(v.x, v.y, v.z)
    }
}
