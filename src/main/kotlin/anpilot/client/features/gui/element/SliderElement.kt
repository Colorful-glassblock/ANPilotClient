package anpilot.client.features.gui.element

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.gui.component.ANElement
import anpilot.client.features.module.anpilot.ANPilotGuiEditor
import anpilot.client.features.module.anpilot.ANTheme
import anpilot.client.features.setting.ANSetting
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class SliderElement(
    private val setting: ANSetting<Number>
) : ANElement(height = 18f) {
    private var dragging = false
    private var editing = false
    private var inputText = ""
    private var valueTextX = 0f
    private var valueTextY = 0f
    private var valueTextWidth = 0f
    private var valueTextHeight = 9f

    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderBounds(context, mouseX, mouseY)
        if (dragging && !editing) updateValue(mouseX.toDouble())

        val min = setting.getMin()?.toDouble() ?: 0.0
        val max = setting.getMax()?.toDouble() ?: 1.0
        val value = setting.value.toDouble().coerceIn(min, max)
        val progress = if (max == min) 0f else ((value - min) / (max - min)).toFloat()
        val barX = x
        val barY = y + 12f
        val barWidth = width-5f
        val valueText = if (editing) inputText else format(value)
        valueTextWidth = context.textWidth(valueText, 0.68f).toFloat()
        valueTextX = x + width - valueTextWidth
        valueTextY = y

        context.text(setting.name, x, y, ANTheme.SetText.rgb, 0.68f)
        if (editing) {
            context.roundedRect(valueTextX - 3f, valueTextY - 1f, valueTextWidth + 6f, valueTextHeight + 2f, 3f, Color(0x22FFFFFF, true))
        }
        context.text(if (editing) "${valueText}_" else valueText, valueTextX, valueTextY, if (editing) ANTheme.SetAccent.rgb else ANTheme.SetText.rgb, 0.68f)
        context.roundedRect(barX, barY, barWidth, 4f, 2f, ANTheme.SetCtrlFill)
        context.roundedRect(barX, barY, barWidth * progress, 4f, 2f, ANTheme.SetAccent)
        context.roundedRect(barX + barWidth * progress - 3f, barY - 2f, 8f, 8f, 4f, ANTheme.SetText)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false
        if (isValueHovered(mouseX, mouseY)) {
            editing = true
            dragging = false
            inputText = format(setting.value.toDouble())
            return true
        }
        editing = false
        if (!isHovered(mouseX, mouseY)) return false
        dragging = true
        updateValue(mouseX)
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val wasDragging = dragging
        dragging = false
        return wasDragging
    }

    override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!editing) return false
        when (key) {
            GLFW.GLFW_KEY_ESCAPE -> {
                editing = false
                inputText = ""
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> commitInput()
            GLFW.GLFW_KEY_BACKSPACE -> if (inputText.isNotEmpty()) inputText = inputText.dropLast(1)
            GLFW.GLFW_KEY_DELETE -> inputText = ""
            else -> return false
        }
        return true
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (!editing || !isAllowedInputChar(chr)) return false
        inputText += chr
        return true
    }

    private fun updateValue(mouseX: Double) {
        val min = setting.getMin() ?: return
        val max = setting.getMax() ?: return
        val minValue = min.toDouble()
        val maxValue = max.toDouble()
        val progress = ((mouseX - x) / width).coerceIn(0.0, 1.0)
        val value = minValue + (maxValue - minValue) * progress
        val next = coerceNumberType(value)
        setting.setValue(next)
        (setting.module as? ANPilotGuiEditor)?.syncToTheme()
    }

    private fun commitInput() {
        val value = inputText.toDoubleOrNull()
        if (value != null) {
            setting.setValue(coerceNumberType(value))
            (setting.module as? ANPilotGuiEditor)?.syncToTheme()
        }
        editing = false
        inputText = ""
    }

    private fun coerceNumberType(value: Double): Number = when (setting.value) {
        is Byte -> value.roundToInt().toByte()
        is Short -> value.roundToInt().toShort()
        is Int -> value.roundToInt()
        is Long -> value.roundToLong()
        is Float -> value.toFloat()
        else -> value
    }

    private fun isValueHovered(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= valueTextX - 3f &&
            mouseX <= valueTextX + valueTextWidth + 3f &&
            mouseY >= valueTextY - 1f &&
            mouseY <= valueTextY + valueTextHeight + 1f

    private fun isAllowedInputChar(chr: Char): Boolean =
        chr.isDigit() || chr == '-' || chr == '+' || chr == '.'

    private fun format(value: Double): String = if (setting.value is Int || setting.value is Long) value.roundToInt().toString() else String.format("%.2f", value)
}

