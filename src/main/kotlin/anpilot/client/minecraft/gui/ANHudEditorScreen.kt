package anpilot.client.minecraft.gui

import anpilot.client.bootstrap.ANServiceRegistry
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import anpilot.client.features.manager.ANConfigManager
import com.mojang.blaze3d.platform.InputConstants

class ANHudEditorScreen : Screen(Component.literal("ANPilot HUD Editor")) {
    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        val gui = MinecraftGuiRenderContext(context, font, width, height)
        ANServiceRegistry.runtime.moduleManager.renderHud(gui, editor = true)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        
    }

    override fun extractTransparentBackground(context: GuiGraphicsExtractor) {
        
    }

    override fun removed() {
        super.removed()
        ANConfigManager.saveCurrent()
    }

    override fun isInGameUi(): Boolean = true

    override fun isPauseScreen(): Boolean = false
}
