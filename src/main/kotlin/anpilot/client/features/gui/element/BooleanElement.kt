package anpilot.client.features.gui.element

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.gui.component.ANElement
import anpilot.client.features.module.anpilot.ANPilotGuiEditor
import anpilot.client.features.module.anpilot.ANTheme
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.utility.Animation
import java.awt.Color
import kotlin.math.roundToInt

class BooleanElement(
    private val setting: ANSetting<Boolean>
) : ANElement(height = 12f) {
    private val toggleAnimation = Animation(setting.value, 160f) { Animation.easeOutCubic(it) }

    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderBounds(context, mouseX, mouseY)
        if (toggleAnimation.state != setting.value) {
            toggleAnimation.state = setting.value
        }

        context.text(setting.name, x, y + 2f, ANTheme.SetText.rgb, 0.68f)
        val switchWidth = 20f
        val switchHeight = 12f
        val switchX = x + width - switchWidth
        val progress = animationProgress()
        val fill = blend(ANTheme.SetCtrlFill, ANTheme.SetAccent, progress)
        val knobSize = 8f
        val knobPadding = 2f
        val knobTravel = switchWidth - knobSize - knobPadding * 2f
        val knobX = switchX + knobPadding + knobTravel * progress

        context.borderedRoundedRect(switchX, y, switchWidth, switchHeight, switchHeight / 2f, 1f, fill, ANTheme.SetCtrlBorder)
        context.roundedRect(knobX, y + knobPadding, knobSize, knobSize, knobSize / 2f, ANTheme.SetText)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false
        setting.setValue(!setting.value)
        return true
    }

    private fun animationProgress(): Float {
        return if (ANPilotGuiEditor.animationsEnabled()) {
            toggleAnimation.getFactor().toFloat().coerceIn(0f, 1f)
        } else if (setting.value) {
            1f
        } else {
            0f
        }
    }

    private fun blend(from: Color, to: Color, factor: Float): Color {
        val progress = factor.coerceIn(0f, 1f)
        return Color(
            lerp(from.red, to.red, progress),
            lerp(from.green, to.green, progress),
            lerp(from.blue, to.blue, progress),
            lerp(from.alpha, to.alpha, progress)
        )
    }

    private fun lerp(from: Int, to: Int, factor: Float): Int {
        return (from + (to - from) * factor).roundToInt().coerceIn(0, 255)
    }
}

