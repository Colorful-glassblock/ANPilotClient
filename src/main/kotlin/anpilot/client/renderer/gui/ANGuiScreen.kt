package anpilot.client.renderer.gui

import anpilot.client.renderer.ANGUIRenderer
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.awt.Color

class ANGuiScreen : Screen(Component.literal("ANPilot Demo")) {
    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        val panelWidth = 300f
        val panelHeight = 220f
        val panelX = (width - panelWidth) / 2f
        val panelY = (height - panelHeight) / 2f

        ANGUIRenderer.rect(context, 0f, 0f, width.toFloat(), height.toFloat(), Color(0x88000000.toInt(), true))
        ANGUIRenderer.roundedRectWithGlow(
            context,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            14f,
            2f,
            Color(0xDD10131A.toInt(), true),
            Color(0xFFB85CFF.toInt(), true),
            14f,
            Color(0xAAFF9100.toInt(), true)
        )
        ANGUIRenderer.glowingRoundedRect(
            context,
            panelX + 18f,
            panelY + 42f,
            120f,
            34f,
            8f,
            Color(0xFF2D7DFF.toInt(), true),
            8f,
            Color(0xFFFF9100.toInt(), true)
        )
        ANGUIRenderer.roundedRect(context, panelX + 150f, panelY + 42f, 132f, 34f, 16f, Color(0xFFB85CFF.toInt(), true))
        ANGUIRenderer.roundedRectWithGlow(
            context,
            panelX + 18f,
            panelY + 92f,
            264f,
            44f,
            12f,
            2f,
            Color(0xAA1C2330.toInt(), true),
            Color(0xFFFFD166.toInt(), true),
            10f,
            Color(0x88FFD100.toInt(), true)
        )

        Minecraft.getInstance().player?.let { player ->
            ANGUIRenderer.playerModel(
                context,
                panelX.toInt() + 210,
                panelY.toInt() + 138,
                panelX.toInt() + 282,
                panelY.toInt() + 208,
                28,
                mouseX.toFloat(),
                mouseY.toFloat(),
                player
            )
        }

        context.centeredText(font, title, width / 2, panelY.toInt() + 16, 0xFFFFFFFF.toInt())
        context.text(font, "Rounded rectangle + glow", panelX.toInt() + 30, panelY.toInt() + 53, 0xFFFFFFFF.toInt())
        context.text(font, "Border + fill + glow", panelX.toInt() + 34, panelY.toInt() + 108, 0xFFFFFFFF.toInt())
        context.text(font, "Player preview", panelX.toInt() + 30, panelY.toInt() + 164, 0xFFFFFFFF.toInt())
        context.centeredText(font, "Press Right Shift or Esc to close", width / 2, panelY.toInt() + 196, 0xFFBFC7D5.toInt())

        super.extractRenderState(context, mouseX, mouseY, deltaTicks)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == InputConstants.KEY_RSHIFT || event.key() == InputConstants.KEY_ESCAPE) {
            onClose()
            return true
        }

        return super.keyPressed(event)
    }

    override fun isPauseScreen(): Boolean = false
}