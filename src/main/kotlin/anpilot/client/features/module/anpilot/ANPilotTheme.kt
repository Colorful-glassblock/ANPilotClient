package anpilot.client.features.module.anpilot

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.ThemeGroupSetting

class ANPilotTheme : ANBaseModule(
    name = "PilotTheme",
    description = "Client theme selector.",
    category = ANModuleCategory.CLIENT,
    chineseName = "主题管理"
) {
    val theme = addSetting(ANSetting("ANPilotTheme", ThemeGroupSetting("Default")))


    override fun isToggleable(): Boolean = false
}
