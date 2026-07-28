package anpilot.client.features.module.render

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule

class ANFullbright : ANBaseModule(
    name = "Fullbright",
    description = "提供永久满级夜视照明效果，消除矿洞与夜间的黑暗视野盲区",
    category = ANModuleCategory.RENDER,
    chineseName = "夜视"
) {
}
