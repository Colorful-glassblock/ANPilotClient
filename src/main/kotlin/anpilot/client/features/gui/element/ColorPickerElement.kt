package anpilot.client.features.gui.element

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.gui.component.ANElement
import anpilot.client.features.module.anpilot.ANPilotGuiEditor
import anpilot.client.features.module.anpilot.ANTheme
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.ColorGroupSetting
import java.awt.Color
import kotlin.math.roundToInt

class ColorPickerElement(
    private val setting: ANSetting<ColorGroupSetting>
) : ANElement(height = CLOSED_HEIGHT) {
    private companion object {
        private const val CLOSED_HEIGHT = 12f
        private const val PICKER_GAP = 4f
        private const val PICKER_HEIGHT = 14f
        private const val EXPANDED_HEIGHT = CLOSED_HEIGHT + PICKER_GAP + PICKER_HEIGHT
        private const val TEXT_SCALE = 0.68f
        private const val PREVIEW_SIZE = 12f
        private const val GAP = 5f
        private const val BAR_HEIGHT = 5f
    }

    private enum class DragTarget {
        HUE,
        ALPHA,
        SHADE
    }

    private var expanded = false
    private var dragTarget: DragTarget? = null
    private var lastColorValue: Int? = null
    private var hue = 0f
    private var saturation = 1f
    private var brightness = 1f
    private var alpha = 255f

    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderBounds(context, mouseX, mouseY)
        syncFromSetting()
        dragTarget?.let { updateDrag(it, mouseX.toFloat(), mouseY.toFloat()) }
        height = if (expanded) EXPANDED_HEIGHT else CLOSED_HEIGHT

        val currentColor = currentColor()
        val preview = previewRect()
        context.text(setting.name, x, y + 2f, ANTheme.SetText.rgb, TEXT_SCALE)
        context.borderedRoundedRect(preview.x, preview.y, preview.width, preview.height, 4f, 1f, currentColor, ANTheme.SetCtrlBorder)

        if (!expanded) return

        val layout = pickerLayout()
        drawHueBar(context, layout.controlsX, layout.hueY, layout.controlWidth)
        drawAlphaBar(context, layout.controlsX, layout.alphaY, layout.controlWidth, currentColor)
        drawShadeBox(context, layout.shadeX, layout.shadeY, layout.shadeSize)

        context.roundedRect(layout.controlsX + hue / 360f * layout.controlWidth - 1.5f, layout.hueY - 2f, 3f, BAR_HEIGHT + 4f, 1.5f, Color(0xEEFFFFFF.toInt(), true))
        context.roundedRect(layout.controlsX + alpha / 255f * layout.controlWidth - 1.5f, layout.alphaY - 2f, 3f, BAR_HEIGHT + 4f, 1.5f, Color(0xEEFFFFFF.toInt(), true))
        context.roundedRect(layout.shadeX + saturation * layout.shadeSize - 2f, layout.shadeY + (1f - brightness) * layout.shadeSize - 2f, 4f, 4f, 2f, Color(0xEEFFFFFF.toInt(), true))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false

        val preview = previewRect()
        if (preview.contains(mouseX, mouseY)) {
            expanded = !expanded
            height = if (expanded) EXPANDED_HEIGHT else CLOSED_HEIGHT
            return true
        }

        if (!expanded) return false

        val layout = pickerLayout()
        dragTarget = when {
            layout.alphaBounds.contains(mouseX, mouseY) -> DragTarget.ALPHA
            layout.hueBounds.contains(mouseX, mouseY) -> DragTarget.HUE
            layout.shadeBounds.contains(mouseX, mouseY) -> DragTarget.SHADE
            else -> null
        }
        dragTarget?.let { updateDrag(it, mouseX.toFloat(), mouseY.toFloat()) }
        return dragTarget != null
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val handled = dragTarget != null
        dragTarget = null
        return handled
    }

    private fun updateDrag(target: DragTarget, mouseX: Float, mouseY: Float) {
        val layout = pickerLayout()
        when (target) {
            DragTarget.HUE -> hue = ((mouseX - layout.controlsX) / layout.controlWidth * 360f).coerceIn(0f, 360f)
            DragTarget.ALPHA -> alpha = ((mouseX - layout.controlsX) / layout.controlWidth * 255f).coerceIn(0f, 255f)
            DragTarget.SHADE -> {
                saturation = ((mouseX - layout.shadeX) / layout.shadeSize).coerceIn(0f, 1f)
                brightness = (1f - (mouseY - layout.shadeY) / layout.shadeSize).coerceIn(0f, 1f)
            }
        }
        applyColor()
    }

    private fun drawHueBar(context: ANGuiRenderContext, barX: Float, barY: Float, barWidth: Float) {
        val colors = listOf(
            Color(0xFFFF0000.toInt(), true),
            Color(0xFFFFFF00.toInt(), true),
            Color(0xFF00FF00.toInt(), true),
            Color(0xFF00FFFF.toInt(), true),
            Color(0xFF0000FF.toInt(), true),
            Color(0xFFFF00FF.toInt(), true),
            Color(0xFFFF0000.toInt(), true)
        )
        val segmentWidth = barWidth / (colors.size - 1)
        for (i in 0 until colors.lastIndex) {
            context.gradientRect(barX + i * segmentWidth, barY, segmentWidth + 0.5f, BAR_HEIGHT, colors[i], colors[i + 1], colors[i + 1], colors[i])
        }
    }

    private fun drawAlphaBar(context: ANGuiRenderContext, barX: Float, barY: Float, barWidth: Float, color: Color) {
        val transparent = Color(color.red, color.green, color.blue, 0)
        val opaque = Color(color.red, color.green, color.blue, 255)
        context.gradientRect(barX, barY, barWidth, BAR_HEIGHT, transparent, opaque, opaque, transparent)
    }

    private fun drawShadeBox(context: ANGuiRenderContext, boxX: Float, boxY: Float, size: Float) {
        val hueColor = Color(Color.HSBtoRGB(hue / 360f, 1f, 1f))
        context.gradientRect(boxX, boxY, size, size, Color.WHITE, hueColor, hueColor, Color.WHITE)
        context.gradientRect(boxX, boxY, size, size, Color(0x00000000, true), Color(0x00000000, true), Color(0xFF000000.toInt(), true), Color(0xFF000000.toInt(), true))
    }

    private fun syncFromSetting() {
        if (dragTarget != null) return
        val colorValue = setting.value.getColor()
        if (lastColorValue == colorValue) return
        lastColorValue = colorValue
        val color = Color(colorValue, true)
        val hsv = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        hue = hsv[0] * 360f
        saturation = hsv[1]
        brightness = hsv[2]
        alpha = color.alpha.toFloat()
        setting.value.setColor_Saturation(saturation)
        setting.value.setColor_Bright(brightness)
        setting.value.setColor_Alpha(alpha)
    }

    private fun currentColor(): Color {
        val rgb = Color.HSBtoRGB(hue / 360f, saturation, brightness) and 0x00FFFFFF
        val argb = rgb or (alpha.roundToInt().coerceIn(0, 255) shl 24)
        return Color(argb, true)
    }

    private fun applyColor() {
        val value = setting.value
        val nextColor = currentColor()
        value.setColor(nextColor.rgb)
        value.setColor_Saturation(saturation)
        value.setColor_Bright(brightness)
        value.setColor_Alpha(alpha)
        lastColorValue = nextColor.rgb
        setting.setValue(value)
        (setting.module as? ANPilotGuiEditor)?.syncToTheme()
    }

    private fun previewRect(): Rect = Rect(x + width - PREVIEW_SIZE, y, PREVIEW_SIZE, PREVIEW_SIZE)

    private fun pickerLayout(): PickerLayout {
        val controlsX = x
        val shadeSize = PICKER_HEIGHT
        val shadeX = x + width - shadeSize - 2f
        val controlWidth = (width * 3f / 4f).coerceAtMost(shadeX - controlsX - GAP).coerceAtLeast(28f)
        val hueY = y + CLOSED_HEIGHT + PICKER_GAP
        val alphaY = hueY + 8f
        val shadeY = hueY
        return PickerLayout(
            controlsX = controlsX,
            controlWidth = controlWidth,
            hueY = hueY,
            alphaY = alphaY,
            shadeX = shadeX,
            shadeY = shadeY,
            shadeSize = shadeSize
        )
    }

    private data class PickerLayout(
        val controlsX: Float,
        val controlWidth: Float,
        val hueY: Float,
        val alphaY: Float,
        val shadeX: Float,
        val shadeY: Float,
        val shadeSize: Float
    ) {
        val hueBounds: Rect = Rect(controlsX, hueY - 3f, controlWidth, 13f)
        val alphaBounds: Rect = Rect(controlsX, alphaY - 3f, controlWidth, 13f)
        val shadeBounds: Rect = Rect(shadeX, shadeY, shadeSize, shadeSize)
    }

    private data class Rect(val x: Float, val y: Float, val width: Float, val height: Float) {
        fun contains(mouseX: Double, mouseY: Double): Boolean = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }
}

