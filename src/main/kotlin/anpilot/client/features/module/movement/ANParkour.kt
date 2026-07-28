package anpilot.client.features.module.movement

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB

class ANParkour : ANBaseModule(
    name = "Parkour",
    description = "当移动到达方块边缘即将掉落时自动触发精准跳跃",
    category = ANModuleCategory.MOVEMENT,
    chineseName = "自动跑酷"
) {
    private var jumping = false

    override fun onDisable() {
        if (jumping) {
            mc.options.keyJump.isDown = false
            jumping = false
        }
    }

    override fun onTick() {
        val player = mc.player ?: return
        val level = mc.level ?: return

        val playerBox = player.boundingBox.move(0.0, -0.5, 0.0).inflate(-0.001, 0.0, -0.001)
        if (player.onGround() && !player.isShiftKeyDown && level.noCollision(playerBox)) {
            mc.options.keyJump.isDown = true
            jumping = true
        } else if (jumping) {
            mc.options.keyJump.isDown = false
            jumping = false
        }
    }
}
