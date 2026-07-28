package anpilot.client.features.module.render

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting

class ANWeather : ANBaseModule(
    name = "Weather",
    description = "自定义修改客户端本地显示的天气状态",
    category = ANModuleCategory.RENDER,
    chineseName = "自定义天气"
) {
    val weatherMode = addSetting(ANSetting("Weather", Weather.CLEAR))

    enum class Weather {
        CLEAR,
        RAIN,
        THUNDER
    }
}
