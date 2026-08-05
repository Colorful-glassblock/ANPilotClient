package anpilot.client.bootstrap

import anpilot.client.api.event.ANEventBus
import anpilot.client.api.gui.ANClickGui
import anpilot.client.api.module.ANModuleRegistry
import anpilot.client.features.event.ANEventBusImpl
import anpilot.client.features.gui.ANClickGuiImpl
import anpilot.client.features.manager.ANConfigManager
import anpilot.client.features.manager.ANRotationManager
import anpilot.client.features.module.ANModuleManager

class ANClientRuntime(
    val moduleManager: ANModuleManager,
    val moduleRegistry: ANModuleRegistry,
    val clickGui: ANClickGui,
    val eventBus: ANEventBus,
    val rotationManager: ANRotationManager
) {
    companion object {
        fun createDefault(): ANClientRuntime {
            val moduleManager = ANModuleManager()
            moduleManager.register()
            ANConfigManager.initialize(moduleManager)
            return ANClientRuntime(
                moduleManager = moduleManager,
                moduleRegistry = moduleManager,
                clickGui = ANClickGuiImpl(moduleManager),
                eventBus = ANEventBusImpl(),
                rotationManager = ANRotationManager()
            )
        }
    }
}
