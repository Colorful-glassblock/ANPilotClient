package anpilot.client.features.gui.component

import anpilot.client.api.gui.ANGuiRenderContext
import java.awt.Color

var activeEditingElement: ANElement? = null

abstract class ANElement(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f
) {
    open fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {}

    open fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    open fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    open fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean = false

    open fun charTyped(chr: Char, modifiers: Int): Boolean = false

    open fun isEditingText(): Boolean = false

    protected fun renderBounds(context: ANGuiRenderContext, mouseX: Int, mouseY: Int) {
        if (isHovered(mouseX.toDouble(), mouseY.toDouble())) {
            context.roundedRect(x - 2, y - 2, width + 4, height + 4, 5f, HOVER_BOUNDS_COLOR)
        }
    }

    fun isHovered(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height

    private companion object {
        private val HOVER_BOUNDS_COLOR = Color(0x33FFFFFF, true)
    }
}
