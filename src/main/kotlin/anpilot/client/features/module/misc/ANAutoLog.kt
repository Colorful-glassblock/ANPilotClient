package anpilot.client.features.module.misc

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

class ANAutoLog : ANBaseModule(
    name = "AutoLog",
    description = "当血量低于设定阈值、图腾耗尽或遇到危险威胁时自动秒断开服务器连接保命",
    category = ANModuleCategory.MISC,
    chineseName = "自动Log"
) {
    val autoDisable = addSetting(ANSetting("AutoDisable", true))
    val lowHp = addSetting(ANSetting("LowHp", false))
    val leaveHp = addSetting(ANSetting("HP", 4f, 0f, 10f) { lowHp.value })
    val totems = addSetting(ANSetting("Totems", false))
    val totemsCount = addSetting(ANSetting("TotemsCount", 2f, 0f, 10f) { totems.value })
    val lowY = addSetting(ANSetting("LowY", false))
    val yLog = addSetting(ANSetting("Height", 64f, -64f, 300f) { lowY.value })
    val ping = addSetting(ANSetting("Ping", false))
    val leavePing = addSetting(ANSetting("PingLimit", 500f, 20f, 1000f) { ping.value })
    val falling = addSetting(ANSetting("Falling", false))
    val fallingDown = addSetting(ANSetting("FDistance", 5f, 3f, 200f) { falling.value })
    val players = addSetting(ANSetting("Players", false))
    val distance = addSetting(ANSetting("PDistance", 256f, 4f, 256f) { players.value })


    override fun onTick() {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val level = minecraft.level ?: return

        if (players.value) {
            val maxDistanceSq = distance.value * distance.value
            level.players().firstOrNull { it !== player && player.distanceToSqr(it) <= maxDistanceSq }?.let {
                leave("有玩家出现: ${it.name.string}")
                return
            }
        }

        if (totems.value) {
            val count = itemCount(player, Items.TOTEM_OF_UNDYING)
            if (count <= totemsCount.value) {
                leave("图腾数量不足: ${count}个剩余")
                return
            }
        }

        if (lowY.value && player.y <= yLog.value) {
            leave("玩家低高度: ${player.y}")
            return
        }

        val latency = minecraft.connection?.getPlayerInfo(player.uuid)?.latency ?: 0
        if (ping.value && latency >= leavePing.value) {
            leave("游戏延迟过大: ${latency}ms")
            return
        }

        if (falling.value && player.fallDistance > fallingDown.value) {
            leave("玩家自由落体: 高度 ${player.y.toInt()}")
            return
        }

        if (lowHp.value && player.health < leaveHp.value * 2f) {
            leave("血量低: ${(player.health / 2f).toInt()}")
        }
    }

    private fun leave(message: String) {
        val minecraft = Minecraft.getInstance()
        if (autoDisable.value) {
            disable()
        }
        minecraft.connection?.connection?.disconnect(Component.literal("[AutoLog] $message"))
    }

    private fun itemCount(player: Player, item: Item): Int {
        val inventory = player.inventory
        var count = 0
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (stack.item == item) {
                count += stack.count
            }
        }
        return count
    }
}
