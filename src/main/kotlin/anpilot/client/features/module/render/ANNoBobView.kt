package anpilot.client.features.module.render

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting


class ANNoBobView : ANBaseModule(
    name = "NoBobView",
    description = "禁用玩家行走与跑动时的画面镜头及手臂摇晃动画",
    category = ANModuleCategory.RENDER,
    chineseName = "取消摇晃"
) {
    val handBob = addSetting(ANSetting("HandBob", 0.25f, 0.0f, 1.0f))
}
