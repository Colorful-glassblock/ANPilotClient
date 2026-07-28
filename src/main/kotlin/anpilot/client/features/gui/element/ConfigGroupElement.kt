package anpilot.client.features.gui.element

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.gui.component.ANElement
import anpilot.client.features.manager.ANConfigManager
import anpilot.client.features.module.anpilot.ANTheme
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.ConfigGroupSetting
import org.lwjgl.glfw.GLFW
import java.awt.Color

class ConfigGroupElement(
    private val setting: ANSetting<ConfigGroupSetting>
) : ANElement(height = ROW_HEIGHT) {
    private companion object {
        private const val ROWS = 6
        private const val ROW_HEIGHT = 18f
        private const val ROW_GAP = 2f
        private const val RADIUS = 5f
        private const val BORDER = 1f
        private const val TEXT_SCALE = 0.62f
        private val ADD_FILL = Color(57, 57, 57, 197)
        private val ADD_BORDER = Color(17, 108, 100, 255)
        private val ADD_ICON_FILL = Color(27, 27, 27, 224)
        private val ADD_ICON = Color(0, 0, 255, 255)
        private val ADD_ICON_HOVER = Color(255, 0, 76, 255)
    }

    private var inputMode = false
    private var inputRow = -1
    private var inputText = ""

    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        val configs = ANConfigManager.configNames()
        val currentName = ANConfigManager.currentConfigName()
        setting.value.index = configs.indexOf(currentName).coerceAtLeast(0)

        height = dynamicHeight(configs.size)

        for (row in 0 until visibleRows(configs.size)) {
            val rowY = y + row * (ROW_HEIGHT + ROW_GAP)
            if (row < configs.size && row < ROWS - 1) {
                renderConfigRow(context, mouseX, mouseY, row, rowY, configs[row], currentName)
            } else {
                renderAddRow(context, mouseX, mouseY, row, rowY)
                break
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false
        val row = ((mouseY - y) / (ROW_HEIGHT + ROW_GAP)).toInt()
        if (row !in 0 until ROWS) return false
        val rowY = y + row * (ROW_HEIGHT + ROW_GAP)
        if (mouseY > rowY + ROW_HEIGHT) return false

        val configs = ANConfigManager.configNames()
        if (row < configs.size && row < ROWS - 1) {
            ANConfigManager.chooseConfig(configs[row])
            inputMode = false
            inputText = ""
            inputRow = -1
        } else {
            inputMode = true
            inputText = ""
            inputRow = row
        }
        return true
    }

    override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!inputMode) return false
        when (key) {
            GLFW.GLFW_KEY_ESCAPE -> cancelInput()
            GLFW.GLFW_KEY_ENTER -> {
                ANConfigManager.createConfig(inputText)
                cancelInput()
            }
            GLFW.GLFW_KEY_BACKSPACE -> if (inputText.isNotEmpty()) inputText = inputText.dropLast(1)
            GLFW.GLFW_KEY_DELETE -> inputText = ""
            else -> return false
        }
        return true
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (!inputMode || chr.isISOControl()) return false
        inputText += chr
        return true
    }

    private fun renderConfigRow(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, index: Int, rowY: Float, name: String, currentName: String) {
        val hovered = mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT
        val selected = name == currentName
        val fill = when {
            selected -> ANTheme.SelOnFill
            hovered -> ANTheme.SelHoverFill
            else -> ANTheme.SelFill
        }
        val border = when {
            selected -> ANTheme.SelOnBorder
            hovered -> ANTheme.SetAccent
            else -> ANTheme.SelBorder
        }
        context.borderedRoundedRect(x, rowY, width, ROW_HEIGHT, RADIUS, BORDER, fill, border)
        context.text(name, x+3, rowY + 5f, ANTheme.SetText.rgb, TEXT_SCALE)
        if (selected) {
            val author = ""
            val authorWidth = context.textWidth(author, TEXT_SCALE).toFloat()
            context.text(author, x + width - authorWidth, rowY + 6.5f, ANTheme.SetText.rgb, TEXT_SCALE)
        }
    }

    private fun renderAddRow(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, row: Int, rowY: Float) {
        val rowWidth = width
        val iconSize = 10f
        val iconX = x + rowWidth / 2f - iconSize / 2f
        val iconY = rowY + (ROW_HEIGHT - iconSize) / 2f
        val hovered = mouseX >= iconX && mouseX <= iconX + iconSize && mouseY >= iconY && mouseY <= iconY + iconSize
        val editing = inputMode && inputRow == row
        context.borderedRoundedRect(x, rowY, rowWidth, ROW_HEIGHT, RADIUS, BORDER, ADD_FILL, if (hovered || editing) ANTheme.SelHoverFill else ADD_BORDER)
        if (editing) {
            val text = inputText.ifEmpty { "Config name" }
            val textWidth = context.textWidth(text, TEXT_SCALE).toFloat()
            context.text(text, x + (rowWidth - textWidth) / 2f, rowY + 6.5f, if (inputText.isEmpty()) Color(180, 180, 180, 180).rgb else ANTheme.SetAccent.rgb, TEXT_SCALE)
            return
        }
        val iconColor = if (hovered) ADD_ICON_HOVER else ADD_ICON
        context.roundedRect(iconX, iconY, iconSize, iconSize, 3f, ADD_ICON_FILL)
        context.roundedRect(iconX + 4f, iconY + 2f, 2f, 6f, 1f, iconColor)
        context.roundedRect(iconX + 2f, iconY + 4f, 6f, 2f, 1f, iconColor)
    }

    private fun dynamicHeight(itemCount: Int): Float {
        val rows = visibleRows(itemCount)
        return rows * ROW_HEIGHT + (rows - 1).coerceAtLeast(0) * ROW_GAP
    }

    private fun visibleRows(itemCount: Int): Int = (itemCount + 1).coerceAtMost(ROWS)

    private fun cancelInput() {
        inputMode = false
        inputText = ""
        inputRow = -1
    }
}

