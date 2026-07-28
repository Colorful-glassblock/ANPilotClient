package anpilot.client.bootstrap

import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.GameJoinedEvent
import anpilot.client.features.event.impl.GameLeftEvent
import anpilot.client.features.manager.ANConfigManager
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.module.anpilot.ANPilotGuiEditor
import anpilot.client.features.event.impl.ANMinecraftEvents
import anpilot.client.minecraft.gui.ANClickGuiScreen
import anpilot.client.features.manager.ANSoundManager
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import org.slf4j.LoggerFactory

object ANClientBootstrap {
    private val logger = LoggerFactory.getLogger("ANPilotClient")
    private var clickGuiBindWasDown = false

    fun initialize() {
        logger.info("Initializing ANPilotClient")
        val runtime = ANClientRuntime.createDefault()
        ANServiceRegistry.initialize(runtime)
        ANSoundManager.initialize()
        runtime.moduleManager.allModules()
            .filterIsInstance<ANBaseModule>()
            .filter { it.enabled }
            .forEach { runtime.eventBus.subscribe(it) }
        runtime.eventBus.subscribe(runtime.rotationManager)
        runtime.eventBus.subscribe(ConfigLifecycleListener)

        ClientLifecycleEvents.CLIENT_STOPPING.register(ClientLifecycleEvents.ClientStopping {
            ANConfigManager.saveCurrent()
        })

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            handleClickGuiBind(client)
            ANMinecraftEvents.tick()
            ANConfigManager.autoSaveIfNeeded()
        })

        LevelRenderEvents.COLLECT_SUBMITS.register(LevelRenderEvents.CollectSubmits { context ->
            ANMinecraftEvents.renderWorld(context)
        })
    }

    private fun handleClickGuiBind(client: Minecraft) {
        val guiEditor = ANServiceRegistry.runtime.moduleManager.allModules()
            .filterIsInstance<ANPilotGuiEditor>()
            .firstOrNull()
        val bind = guiEditor?.getBind()
        val isDown = bind != null && !bind.mouse && bind.key != -1 && bind.key != 0 && InputConstants.isKeyDown(client.window, bind.key)
        if (isDown && !clickGuiBindWasDown && client.screen !is ChatScreen) {
            toggleClickGui(client)
        }
        clickGuiBindWasDown = isDown
    }

    private fun toggleClickGui(client: Minecraft) {
        if (client.screen is ANClickGuiScreen) {
            client.setScreen(null)
        } else {
            ANServiceRegistry.runtime.clickGui.resetView()
            client.setScreen(ANClickGuiScreen())
        }
    }

    private object ConfigLifecycleListener {
        @ANEventHandler
        fun onGameJoined(event: GameJoinedEvent) {
            ANConfigManager.loadOnGameJoin()
        }

        @ANEventHandler
        fun onGameLeft(event: GameLeftEvent) {
            ANConfigManager.saveOnGameLeave()
        }
    }
}
