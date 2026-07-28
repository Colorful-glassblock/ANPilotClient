package anpilot.client.features.module.render

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting

class ANModels : ANBaseModule(
    name = "Models",
    description = "自定义修改世界中渲染的实体外观尺寸与旋转缩放",
    category = ANModuleCategory.RENDER,
    chineseName = "模型"
) {
    val crystalScale = addSetting(ANSetting("CrystalScale", 1.0f, 0.1f, 1.5f))
    val crystalSpin = addSetting(ANSetting("CrystalSpin", 1.0f, 0.0f, 10.0f))
    val crystalBounce = addSetting(ANSetting("CrystalBounce", true))
}
