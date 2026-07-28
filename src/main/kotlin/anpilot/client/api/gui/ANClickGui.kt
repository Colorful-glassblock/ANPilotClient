package anpilot.client.api.gui

interface ANClickGui {
    fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float)

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false

    fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean = false

    fun charTyped(chr: Char, modifiers: Int): Boolean = false

    fun resetView() {}
}
