package anpilot.client.features.module.hud

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import java.awt.Color
import net.minecraft.client.Minecraft

abstract class ANDraggableHudModule(
    name: String,
    description: String,
    chineseName: String,
    defaultX: Float,
    defaultY: Float
) : ANBaseModule(name, description, ANModuleCategory.HUD, chineseName) {

    companion object {
        private const val SCREEN_PADDING = 5f
        private const val EDITOR_HIT_PADDING = 5.0
        private const val EDITOR_LABEL_SCALE = 0.45f
        private val EDITOR_FILL = Color(0x2200FFFF, true)
        private val EDITOR_BORDER = Color(0xCC00FFFF.toInt(), true)
        private val EDITOR_TEXT = Color(0xEEFFFFFF.toInt(), true)
    }

    private var currentScreenWidth = 1920f
    private var currentScreenHeight = 1080f

    val posX = addSetting(
        ANSetting(
            "X",
            defaultX / (runCatching { Minecraft.getInstance().window.guiScaledWidth.toFloat() }.getOrNull()?.takeIf { it > 0 } ?: 1920f),
            0f,
            1f
        )
    )
    val posY = addSetting(
        ANSetting(
            "Y",
            defaultY / (runCatching { Minecraft.getInstance().window.guiScaledHeight.toFloat() }.getOrNull()?.takeIf { it > 0 } ?: 1080f),
            0f,
            1f
        )
    )
    val uiScale = addSetting(ANSetting("UIScale", 0.8f, 0.25f, 1.0f))
    var hudWidth: Float = 0f
        private set
    var hudHeight: Float = 0f
        private set
    private var hudOffsetX: Float = 0f
    private var hudOffsetY: Float = 0f
    protected open val showXSetting: Boolean = true

    val visualX: Float get() = x + hudOffsetX
    val visualY: Float get() = y + hudOffsetY
    val hudScale: Float get() = uiScale.value

    fun scaled(value: Float): Float = value * hudScale

    fun renderHud(context: ANGuiRenderContext, editor: Boolean) {
        currentScreenWidth = context.width.toFloat()
        currentScreenHeight = context.height.toFloat()

        
        if (posX.value > 1.0f) {
            posX.setValueSilent((posX.value / currentScreenWidth).coerceIn(0f, 1f))
        }
        if (posY.value > 1.0f) {
            posY.setValueSilent((posY.value / currentScreenHeight).coerceIn(0f, 1f))
        }

        renderHudContent(context, editor)
        if (editor) {
            context.borderedRoundedRect(visualX, visualY, hudWidth.coerceAtLeast(scaled(8f)), hudHeight.coerceAtLeast(scaled(8f)), scaled(6f), scaled(1f), EDITOR_FILL, EDITOR_BORDER)
            context.text(getDisplayHudName(), visualX + 3f, (visualY - 7f).coerceAtLeast(1f), EDITOR_TEXT.rgb, EDITOR_LABEL_SCALE)
        }
    }

    abstract fun renderHudContent(context: ANGuiRenderContext, editor: Boolean)

    fun setHudBounds(width: Float, height: Float, offsetX: Float = 0f, offsetY: Float = 0f) {
        hudWidth = width.coerceAtLeast(1f)
        hudHeight = height.coerceAtLeast(1f)
        hudOffsetX = offsetX
        hudOffsetY = offsetY
    }

    fun contains(mouseX: Double, mouseY: Double): Boolean {
        val padding = EDITOR_HIT_PADDING
        return mouseX >= visualX - padding &&
            mouseX <= visualX + hudWidth + padding &&
            mouseY >= visualY - padding &&
            mouseY <= visualY + hudHeight + padding
    }

    fun setPosition(context: ANGuiRenderContext, nextX: Float, nextY: Float) {
        currentScreenWidth = context.width.toFloat()
        currentScreenHeight = context.height.toFloat()

        val minX = SCREEN_PADDING - hudOffsetX
        val maxX = (currentScreenWidth - SCREEN_PADDING - hudWidth - hudOffsetX).coerceAtLeast(minX)
        val clampedDragX = nextX.coerceIn(minX, maxX)

        val minY = SCREEN_PADDING - hudOffsetY
        val maxY = (currentScreenHeight - SCREEN_PADDING - hudHeight - hudOffsetY).coerceAtLeast(minY)
        val clampedDragY = nextY.coerceIn(minY, maxY)

        posX.setValueSilent(clampedDragX / currentScreenWidth)
        posY.setValueSilent(clampedDragY / currentScreenHeight)
    }

    fun moveTo(context: ANGuiRenderContext, nextX: Float, nextY: Float) {
        setPosition(context, nextX, nextY)
    }



    override fun getSettings(): List<ANSetting<*>> {
        val settings = super.getSettings()
        return settings
    }

    val x: Float get() {
        val rawX = posX.value * currentScreenWidth
        val minX = SCREEN_PADDING - hudOffsetX
        val maxX = (currentScreenWidth - SCREEN_PADDING - hudWidth - hudOffsetX).coerceAtLeast(minX)
        return rawX.coerceIn(minX, maxX)
    }

    val y: Float get() {
        val rawY = posY.value * currentScreenHeight
        val minY = SCREEN_PADDING - hudOffsetY
        val maxY = (currentScreenHeight - SCREEN_PADDING - hudHeight - hudOffsetY).coerceAtLeast(minY)
        return rawY.coerceIn(minY, maxY)
    }

}
