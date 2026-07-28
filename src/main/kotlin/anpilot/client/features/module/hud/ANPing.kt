package anpilot.client.features.module.hud

import anpilot.client.api.gui.ANGuiRenderContext
import net.minecraft.client.Minecraft

class ANPing : ANDraggableHudModule("Ping", "当前客户端与服务器之间的网络延迟(Ping)", "服务器延迟", 500f, 55f) {
    override fun renderHudContent(context: ANGuiRenderContext, editor: Boolean) {
        val ping = getPing().toString()
        val text = "Ping$ping"
        val width = context.textWidth(text, hudScale).toFloat() + scaled(20f)
        setHudBounds(width, scaled(20f))
        context.borderedRoundedRect(x, y, hudWidth, hudHeight, scaled(8f), scaled(1.5f), HudColors.panelFillColor, HudColors.panelBorderColor)
        context.text("Ping", x + scaled(5f), y + scaled(5f), HudColors.text1.rgb, hudScale)
        context.text(ping, x + scaled(35f), y + scaled(5f), HudColors.text2.rgb, hudScale)
    }

    private fun getPing(): Int {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return 0
        return minecraft.connection?.getPlayerInfo(player.uuid)?.latency ?: 0
    }
}
