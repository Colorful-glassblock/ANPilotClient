package anpilot.client.features.module.hud

import anpilot.client.api.gui.ANGuiRenderContext
import net.minecraft.client.Minecraft

class ANFPS : ANDraggableHudModule("FPS", "在屏幕上实时显示客户端运行帧率(FPS)", "游戏帧率", 500f, 10f) {
    override fun renderHudContent(context: ANGuiRenderContext, editor: Boolean) {
        val fps = Minecraft.getInstance().fps.toString()
        val text = "FPS$fps"
        val width = context.textWidth(text, hudScale).toFloat() + scaled(20f)
        setHudBounds(width, scaled(20f))
        context.borderedRoundedRect(x, y, hudWidth, hudHeight, scaled(8f), scaled(1.5f), HudColors.panelFillColor, HudColors.panelBorderColor)
        context.text("FPS", x + scaled(5f), y + scaled(5f), HudColors.text1.rgb, hudScale)
        context.text(fps, x + scaled(30f), y + scaled(5f), HudColors.text2.rgb, hudScale)
    }
}
