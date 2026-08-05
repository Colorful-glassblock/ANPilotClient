package anpilot.client.features.gui.component

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.gui.ANDecorTextureManager
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.module.anpilot.ANTheme
import anpilot.client.renderer.render.ANProceduralDecorRenderer
import net.minecraft.client.Minecraft
import java.awt.Color

class ANCategoryPanel(
    private val category: ANModuleCategory,
    modules: List<ANBaseModule>,
    x: Float,
    y: Float,
    width: Float,
    private val onModulePrimaryClick: (ANBaseModule) -> Boolean = { false }
) : ANElement(x, y, width, 36f) {
    private companion object {
        private const val CLOSED_HEIGHT = 28f
        private const val HEADER_HEIGHT = 28f
        private const val SIDE_PADDING = 7f
        private const val MODULE_GAP = 2.6f
        private const val PLAYER_MODEL_HEIGHT = 100f
        private const val PLAYER_MODEL_PADDING = 8f
    }

    var maxHeight: Float = Float.MAX_VALUE

    private val buttons = modules
        .onEach { it.setOpenSilent(false) }
        .map { ANModuleButton(it, onModulePrimaryClick) }
        .toMutableList()
    private var open = true
    private var scroll = 0f

    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        val panelHeight = maxHeight
        val contentTop = y + HEADER_HEIGHT
        val contentBottom = y + panelHeight - extraBottomHeight()
        scroll = scroll.coerceIn(-maxScroll(), 0f)

        val title = category.displayName
        if (!open) {
            context.pushScissor(x, y, width, HEADER_HEIGHT+5f)
            try {
                drawPanel(context, x, y, width, CLOSED_HEIGHT)
                context.text(title, x + width / 2f - context.textWidth(title) / 2f, y + 9f, ANTheme.PanelText.rgb)
            } finally {
                context.popScissor()
            }
            return
        }

        drawPanel(context, x, y, width, panelHeight)
        context.text(title, x + width / 2f - context.textWidth(title) / 2f, y + 9f, ANTheme.PanelText.rgb)

        if (hasPlayerModel()) renderPlayerModel(context, mouseX, mouseY, panelHeight)

        context.pushScissor(0F, contentTop, context.width.toFloat(), (contentBottom - contentTop).coerceAtLeast(0f))
        try {
            var currentY = contentTop + scroll
            buttons.forEach { button ->
                val buttonHeight = button.totalHeight()
                if (currentY >= contentBottom) return@forEach
                button.x = x + SIDE_PADDING
                button.y = currentY
                button.width = width - SIDE_PADDING * 2f
                button.maxVisibleY = contentBottom
                button.render(context, mouseX, mouseY, deltaTicks)
                currentY += buttonHeight + MODULE_GAP
            }
        } finally {
            context.popScissor()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + CLOSED_HEIGHT) {
            if (button == 1) open = !open
            return true
        }
        if (open) {
            buttons.forEach {
                if (it.y < contentBottom() && it.mouseClicked(mouseX, mouseY, button)) return true
            }
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (open) {
            buttons.forEach {
                if (it.y < contentBottom() && it.mouseReleased(mouseX, mouseY, button)) return true
            }
        }
        return false
    }

    override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        if (open) {
            buttons.forEach {
                if (it.y < contentBottom() && it.keyPressed(key, scanCode, modifiers)) return true
            }
        }
        return false
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (open) {
            buttons.forEach {
                if (it.y < contentBottom() && it.charTyped(chr, modifiers)) return true
            }
        }
        return false
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!open || mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + maxHeight) return false
        val maxScroll = maxScroll()
        if (maxScroll <= 0f) return false
        scroll = (scroll + amount.toFloat() * 18f).coerceIn(-maxScroll, 0f)
        return true
    }

    fun totalHeight(): Float {
        if (!open) return CLOSED_HEIGHT
        if (buttons.isEmpty()) return HEADER_HEIGHT + extraBottomHeight()
        return HEADER_HEIGHT + buttons.sumOf { (it.totalHeight() + MODULE_GAP).toDouble() }.toFloat() + extraBottomHeight()
    }

    private fun maxScroll(): Float = (totalHeight() - maxHeight).coerceAtLeast(0f)

    private fun renderPlayerModel(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, panelHeight: Float) {
        val player = Minecraft.getInstance().player ?: return
        val modelTop = y + panelHeight - PLAYER_MODEL_HEIGHT
        val modelBottom = y + panelHeight - PLAYER_MODEL_PADDING
        val modelWidth = (width - SIDE_PADDING * 2f).toInt()
        context.playerModel(
            (x + SIDE_PADDING).toInt(),
            modelTop.toInt(),
            (x + SIDE_PADDING).toInt() + modelWidth,
            modelBottom.toInt(),
            40,
            mouseX.toFloat(),
            mouseY.toFloat(),
            player
        )
    }

    private fun contentBottom(): Float = y + maxHeight - extraBottomHeight()

    private fun extraBottomHeight(): Float = if (hasPlayerModel()) PLAYER_MODEL_HEIGHT else 0f

    private fun hasPlayerModel(): Boolean = category == ANModuleCategory.CLIENT

    private fun drawPanel(context: ANGuiRenderContext, x: Float, y: Float, width: Float, height: Float) {
        context.borderedRoundedRect(x, y, width, height, ANTheme.PanelRadius, ANTheme.PanelBorderWidth, ANTheme.PanelFill, ANTheme.PanelBorder)
        if (!ANTheme.DecorEnabled) return
        context.roundedBorderDecor(
            ANDecorTextureManager.texture(ANTheme.DecorFile),
            x,
            y,
            width,
            height,
            ANTheme.PanelRadius,
            ANProceduralDecorRenderer.RoundedBorderDecorOptions(
                baseSize = ANTheme.DecorSize,
                density = ANTheme.DecorDensity,
                minScale = minOf(ANTheme.DecorMinScale, ANTheme.DecorMaxScale),
                maxScale = maxOf(ANTheme.DecorMinScale, ANTheme.DecorMaxScale),
                rotationRandomDegrees = ANTheme.DecorRotation,
                offset = ANTheme.DecorOffset,
                seed = ANTheme.DecorSeed + category.ordinal,
                color = ANTheme.DecorColor.rgb,
                maxInstances = 64
            )
        )
    }

    private val ANModuleCategory.displayName: String
        get() = when (this) {
            ANModuleCategory.COMBAT -> "Combat"
            ANModuleCategory.RENDER -> "Render"
            ANModuleCategory.MOVEMENT -> "Movement"
            ANModuleCategory.PLAYER -> "Player"
            ANModuleCategory.MISC -> "Tool"
            ANModuleCategory.HUD -> "Hud"
            ANModuleCategory.CLIENT -> "ANPilot"
        }
}
