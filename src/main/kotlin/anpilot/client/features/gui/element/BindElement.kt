package anpilot.client.features.gui.element

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.gui.component.ANElement
import anpilot.client.features.module.anpilot.ANTheme
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.Bind
import org.lwjgl.glfw.GLFW

class BindElement(
    private val setting: ANSetting<Bind>
) : ANElement(height = 12f) {
    private var listening = false

    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderBounds(context, mouseX, mouseY)
        val label = if (listening) "Press..." else setting.value.displayName
        val pillWidth = context.textWidth(label, 0.68f) + 10f
        context.text(setting.name, x, y + 2f, ANTheme.SetText.rgb, 0.68f)
        context.roundedRect(x + width - pillWidth, y + 1f, pillWidth, 10f, 4f, ANTheme.SetCtrlFill)
        context.text(label, x + width - pillWidth + 2f, y + 2f, ANTheme.SetText.rgb, 0.68f)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) return false
        if (listening) {
            setting.setValue(Bind(button, true))
            listening = false
            return true
        }

        when (button) {
            0 -> listening = true
            1 -> clearBind()
        }
        return true
    }

    override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!listening) return false
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
            clearBind()
        } else {
            setting.setValue(Bind(key, false))
        }
        listening = false
        return true
    }

    private fun clearBind() {
        setting.setValue(Bind(-1, false))
    }
}

