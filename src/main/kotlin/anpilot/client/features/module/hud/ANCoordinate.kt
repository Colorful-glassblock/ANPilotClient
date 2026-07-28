package anpilot.client.features.module.hud

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.module.anpilot.ANTheme
import net.minecraft.client.Minecraft
import net.minecraft.world.level.Level
import kotlin.math.round

class ANCoordinate : ANDraggableHudModule("Coordinate", "在屏幕显示玩家当前坐标与下界/主世界换算坐标", "世界坐标", 20f, 70f) {
    override fun renderHudContent(context: ANGuiRenderContext, editor: Boolean) {
        val player = Minecraft.getInstance().player
        val xPos = player?.x ?: 0.0
        val yPos = player?.y ?: 0.0
        val zPos = player?.z ?: 0.0
        val coordX = round(xPos * 10.0) / 10.0
        val coordY = round(yPos * 10.0) / 10.0
        val coordZ = round(zPos * 10.0) / 10.0
        val netherFactor = if (player?.level()?.dimension() == Level.NETHER) 8.0 else 0.125
        val hposX = (xPos * netherFactor).toInt()
        val hposZ = (zPos * netherFactor).toInt()
        val text = "XYZ[$coordX|$coordY|$coordZ] [$hposX|$hposZ]"
        val panelWidth = context.textWidth(text, hudScale).toFloat().coerceAtLeast(scaled(120f)) + scaled(20f)
        setHudBounds(panelWidth, scaled(20f))

        context.borderedRoundedRect(x, y, hudWidth, hudHeight, scaled(6f), scaled(1.5f), HudColors.panelFillColor, HudColors.panelBorderColor)
        val coordXText = coordX.toString()
        val coordYText = coordY.toString()
        val coordZText = coordZ.toString()
        val coordXWidth = context.textWidth(coordXText, hudScale).toFloat()
        val coordYWidth = context.textWidth(coordYText, hudScale).toFloat()
        val coordZWidth = context.textWidth(coordZText, hudScale).toFloat()
        context.text("XYZ", x + scaled(5f), y + scaled(5f), HudColors.text1.rgb, hudScale)
        context.text(coordXText, x + scaled(32f), y + scaled(5f), HudColors.text2.rgb, hudScale)
        context.text("|", x + scaled(35f) + coordXWidth, y + scaled(5f), ANTheme.Yellow.rgb, hudScale)
        context.text(coordYText, x + scaled(42f) + coordXWidth, y + scaled(5f), HudColors.text2.rgb, hudScale)
        context.text("|", x + scaled(45f) + coordXWidth + coordYWidth, y + scaled(5f), ANTheme.Yellow.rgb, hudScale)
        context.text(coordZText, x + scaled(52f) + coordXWidth + coordYWidth, y + scaled(5f), HudColors.text2.rgb, hudScale)
        context.text("[$hposX $hposZ]", x + scaled(57f) + coordXWidth + coordYWidth + coordZWidth, y + scaled(5f), HudColors.text3.rgb, hudScale)
    }
}
