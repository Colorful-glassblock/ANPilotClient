package anpilot.client.features.gui.element

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.gui.component.ANElement
import anpilot.client.features.module.anpilot.ANTheme
import anpilot.client.features.setting.ANSetting
import java.awt.Color
import anpilot.client.features.gui.component.activeEditingElement
import net.minecraft.client.Minecraft

class StringElement(
    private val setting: ANSetting<String>
) : ANElement(height = 12f) {
    private var editing = false

    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderBounds(context, mouseX, mouseY)
        context.text(setting.name, x, y + 2f, ANTheme.SetText.rgb, 0.68f)

        val value = setting.value.ifEmpty { if (editing) "" else "empty" }
        val valueText = if (editing) "${value}_" else value
        val nameWidth = context.textWidth(setting.name, 0.68f)
        val boxX = x + nameWidth + 5f
        val boxWidth = (width - nameWidth - 5f).coerceAtLeast(40f)

        context.borderedRoundedRect(boxX, y, boxWidth, height, 4f, 1f, ANTheme.SetCtrlFill, ANTheme.SetCtrlBorder)
        context.text(valueText, boxX + 5f, y + 2f, ANTheme.SetText.rgb, 0.68f)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val wasEditing = editing
        editing = button == 0 && isHovered(mouseX, mouseY)
        if (editing) {
            activeEditingElement = this
        } else if (wasEditing && activeEditingElement === this) {
            activeEditingElement = null
        }
        return editing
    }

    override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!editing) return false


        if (key == 257 || key == 335) {
            editing = false
            if (activeEditingElement === this) {
                activeEditingElement = null
            }
            return true
        }

        val isCtrlDown = (modifiers and 2) != 0 || (modifiers and 8) != 0


        if (isCtrlDown && key == 86) {
            val clipText = try {
                Minecraft.getInstance().keyboardHandler.clipboard
            } catch (_: Exception) {
                ""
            }
            if (clipText.isNotEmpty()) {
                val cleanText = clipText.replace("\r", "").replace("\n", "")
                setting.setValue(setting.value + cleanText)
            }
            return true
        }


        if (isCtrlDown && key == 67) {
            try {
                Minecraft.getInstance().keyboardHandler.clipboard = setting.value
            } catch (_: Exception) {}
            return true
        }


        if (key == 259 && setting.value.isNotEmpty()) {
            if (isCtrlDown) {
                setting.setValue("")
            } else {
                setting.setValue(setting.value.dropLast(1))
            }
            return true
        }

        return true
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (!editing || chr.isISOControl()) return false
        val isCtrlDown = (modifiers and 2) != 0 || (modifiers and 8) != 0
        if (isCtrlDown) return false
        setting.setValue(setting.value + chr)
        return true
    }

    override fun isEditingText(): Boolean = editing
}

