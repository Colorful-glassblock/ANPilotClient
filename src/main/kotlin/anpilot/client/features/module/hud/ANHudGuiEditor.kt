package anpilot.client.features.module.hud

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.ColorGroupSetting
import java.awt.Color

class ANHudGuiEditor : ANBaseModule("HudGuiEditor", "配置全局HUD界面面板颜色、半径、边框与文本样式", ANModuleCategory.HUD, chineseName = "HUD个性化") {
    val panelFill = addSetting(HudColors.panelFill)
    val panelBorder = addSetting(HudColors.panelBorder)
    val panelRadius = addSetting(HudColors.panelRadius)
    val borderWidth = addSetting(HudColors.borderWidth)
    val textColor1 = addSetting(HudColors.textColor1)
    val textColor2 = addSetting(HudColors.textColor2)
    val textColor3 = addSetting(HudColors.textColor3)

    override fun isToggleable(): Boolean = false
}

object HudColors {
    val panelFill = ANSetting("PanelFill", ColorGroupSetting(Color(0xDB1642DC.toInt(), true).rgb))
    val panelBorder = ANSetting("PanelBorder", ColorGroupSetting(Color(0xFF21F1F8.toInt(), true).rgb))
    val panelRadius = ANSetting("PanelRadius", 10.0f, 0.0f, 30.0f)
    val borderWidth = ANSetting("BorderWidth", 1.0f, 0.0f, 10.0f)
    val textColor1 = ANSetting("TextColor1", ColorGroupSetting(Color(0xFF35FA1F.toInt(), true).rgb))
    val textColor2 = ANSetting("TextColor2", ColorGroupSetting(Color(0xFF1FF1D8.toInt(), true).rgb))
    val textColor3 = ANSetting("TextColor3", ColorGroupSetting(Color(0xFFD128EA.toInt(), true).rgb))

    val panelFillColor: Color get() = panelFill.value.getColorRGB()
    val panelBorderColor: Color get() = panelBorder.value.getColorRGB()
    val radius: Float get() = panelRadius.value
    val width: Float get() = borderWidth.value
    val text1: Color get() = textColor1.value.getColorRGB()
    val text2: Color get() = textColor2.value.getColorRGB()
    val text3: Color get() = textColor3.value.getColorRGB()
}
