package anpilot.client.features.gui.element

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.gui.component.ANElement
import anpilot.client.features.module.anpilot.ANTheme
import anpilot.client.features.setting.ANSetting
import java.awt.Color

class ModeElement(
    private val setting: ANSetting<Enum<*>>
) : ANElement(height = 12f) {
    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderBounds(context, mouseX, mouseY)
        context.text(setting.name, x, y+2f, ANTheme.SetText.rgb, 0.68f)
        val mode = setting.currentEnumName()
        val pillWidth = context.textWidth(mode, 0.68f) + 10f
        context.borderedRoundedRect(x + width - pillWidth, y, pillWidth, 12f, 6f, 1f, ANTheme.SetCtrlFill, ANTheme.SetCtrlBorder)
        context.text(mode, x + width - pillWidth + 5f, y + 2f, ANTheme.SetText.rgb, 0.68f)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) return false
        if (button == 0) {
            setting.increaseEnum()
            return true
        }
        if (button == 1) {
            val modes = setting.getModes()
            val currentIndex = modes.indexOfFirst { it.equals(setting.currentEnumName(), ignoreCase = true) }
            setting.setEnumByNumber(if (currentIndex <= 0) modes.lastIndex else currentIndex - 1)
            return true
        }
        return false
    }
}

