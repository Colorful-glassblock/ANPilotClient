package anpilot.client.minecraft.gui

import anpilot.client.bootstrap.ANServiceRegistry
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import anpilot.client.features.gui.component.activeEditingElement
import anpilot.client.features.manager.ANConfigManager
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.GuiEventListener

class ANClickGuiScreen : Screen(Component.literal("ANPilot ClickGui")) {
    private val dummyEditBox by lazy {
        EditBox(
            Minecraft.getInstance().font,
            0, 0, 0, 0,
            Component.literal("Dummy")
        ).apply {
            setFocused(true)
        }
    }

    override fun getFocused(): GuiEventListener? {
        if (activeEditingElement != null) {
            return dummyEditBox
        }
        return super.getFocused()
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        ANServiceRegistry.runtime.clickGui.render(
            MinecraftGuiRenderContext(context, font, width, height),
            mouseX,
            mouseY,
            deltaTicks
        )
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        return ANServiceRegistry.runtime.clickGui.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        return ANServiceRegistry.runtime.clickGui.mouseReleased(event.x(), event.y(), event.button()) || super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        return ANServiceRegistry.runtime.clickGui.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose()
            return true
        }

        return ANServiceRegistry.runtime.clickGui.keyPressed(event.key(), event.scancode(), event.modifiers()) || super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        val text = event.codepointAsString()
        return text.any { ANServiceRegistry.runtime.clickGui.charTyped(it, 0) } || super.charTyped(event)
    }

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        if (Minecraft.getInstance().level == null) {
            super.extractBackground(context, mouseX, mouseY, deltaTicks)
        }
    }

    override fun extractTransparentBackground(context: GuiGraphicsExtractor) {
        if (Minecraft.getInstance().level == null) {
            super.extractTransparentBackground(context)
        }
    }

    override fun removed() {
        super.removed()
        activeEditingElement = null
        ANConfigManager.saveCurrent()
    }

    override fun isInGameUi(): Boolean = Minecraft.getInstance().level != null

    override fun isPauseScreen(): Boolean = false
}
