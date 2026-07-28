package anpilot.client.features.module.misc

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.manager.ANFriendManager
import anpilot.client.features.module.ANBaseModule
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.EntityHitResult

class ANFriendAdd : ANBaseModule(
    name = "FriendAdd",
    description = "准星对准玩家开启即可快速将该玩家添加至好友白名单列表中",
    category = ANModuleCategory.MISC,
    chineseName = "添加好友"
) {

    override fun onTick() {
        val minecraft = Minecraft.getInstance()
        val player = (minecraft.hitResult as? EntityHitResult)?.entity as? Player ?: return
        ANFriendManager.addFriend(player.name.string)
        disable()
    }
}
