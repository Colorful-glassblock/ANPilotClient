package anpilot.client.features.module.anpilot

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.FriendGroupSetting

class ANPilotFriend : ANBaseModule(
    name = "PilotFriend",
    description = "Client friend list UI entry.",
    category = ANModuleCategory.CLIENT,
    chineseName = "好友管理"
) {
    val friend = addSetting(ANSetting("ANPilotFriend", FriendGroupSetting()))


    override fun isToggleable(): Boolean = false
}
