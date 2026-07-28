package anpilot.client.features.module.anpilot

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.ConfigGroupSetting

class ANPilotConfig : ANBaseModule(
    name = "PilotConfig",
    description = "Client configuration selector.",
    category = ANModuleCategory.CLIENT,
    chineseName = "配置管理"
) {
    val config = addSetting(ANSetting("ANPilotConfig", ConfigGroupSetting(0)))


    override fun isToggleable(): Boolean = false
}
