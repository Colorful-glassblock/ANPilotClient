package anpilot.client.features.gui.element

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.gui.component.ANElement
import anpilot.client.features.module.anpilot.ANPilotGuiEditor
import anpilot.client.features.module.anpilot.ANTheme
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.FileSelectSetting

class FileSelectElement(
    private val setting: ANSetting<FileSelectSetting>
) : ANElement(height = COLLAPSED_HEIGHT) {
    private companion object {
        private const val ROWS = 6
        private const val ROW_HEIGHT = 18f
        private const val ROW_GAP = 2f
        private const val COLLAPSED_HEIGHT = 14f
        private const val RADIUS = 5f
        private const val BORDER = 1f
        private const val TEXT_SCALE = 0.62f
    }

    private var expanded = false

    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderBounds(context, mouseX, mouseY)
        val fileSetting = setting.value
        val files = fileSetting.files()
        val current = fileSetting.currentFileName().substringBeforeLast(".").ifEmpty { "No files" }
        val arrow = if (expanded) "v" else ">"

        context.text(setting.name, x, y + 3f, ANTheme.SetText.rgb, 0.68f)
        val label = "$arrow $current"
        val pillWidth = (context.textWidth(label, 0.62f).toFloat() + 10f).coerceAtMost(width)
        context.borderedRoundedRect(x + width - pillWidth, y, pillWidth, 14f, 4f, 1f, ANTheme.SetCtrlFill, ANTheme.SetCtrlBorder)
        context.text(label, x + width - pillWidth + 5f, y + 3f, ANTheme.SetText.rgb, TEXT_SCALE)

        height = if (expanded) COLLAPSED_HEIGHT + ROW_GAP + dynamicListHeight(files.size) else COLLAPSED_HEIGHT
        if (!expanded) return

        val listY = y + COLLAPSED_HEIGHT + ROW_GAP
        val rows = visibleRows(files.size)
        if (files.isEmpty()) {
            context.borderedRoundedRect(x, listY, width, ROW_HEIGHT, RADIUS, BORDER, ANTheme.SelFill, ANTheme.SelBorder)
            context.text("Put files in folder", x + 5f, listY + 6.5f, ANTheme.SetText.rgb, TEXT_SCALE)
            return
        }

        for (row in 0 until rows) {
            val rowY = listY + row * (ROW_HEIGHT + ROW_GAP)
            val name = files[row]
            val displayName = name.substringBeforeLast(".")
            val hovered = mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT
            val selected = name == fileSetting.fileName
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
            context.text(displayName, x + 5f, rowY + 5f, ANTheme.SetText.rgb, TEXT_SCALE)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false
        if (mouseY <= y + COLLAPSED_HEIGHT) {
            expanded = !expanded
            return true
        }
        if (!expanded) return false

        val files = setting.value.files()
        val row = ((mouseY - (y + COLLAPSED_HEIGHT + ROW_GAP)) / (ROW_HEIGHT + ROW_GAP)).toInt()
        if (row in 0 until visibleRows(files.size)) {
            setting.value.setFile(files[row])
            setting.setValue(setting.value)
            (setting.module as? ANPilotGuiEditor)?.syncToTheme()
            expanded = false
            return true
        }
        return false
    }

    private fun visibleRows(itemCount: Int): Int = itemCount.coerceAtMost(ROWS)

    private fun dynamicListHeight(itemCount: Int): Float {
        val rows = visibleRows(itemCount).coerceAtLeast(1)
        return rows * ROW_HEIGHT + (rows - 1).coerceAtLeast(0) * ROW_GAP
    }
}

