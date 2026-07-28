package anpilot.client.features.module.player

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting

class ANAutoSign : ANBaseModule(
    name = "AutoSign",
    description = "自动填写预设的四行自定义文本",
    category = ANModuleCategory.PLAYER,
    chineseName = "自动告示牌"
) {
    val line1 = addSetting(ANSetting("L1", ""))
    val line2 = addSetting(ANSetting("L2", ""))
    val line3 = addSetting(ANSetting("L3", ""))
    val line4 = addSetting(ANSetting("L4", ""))
}
