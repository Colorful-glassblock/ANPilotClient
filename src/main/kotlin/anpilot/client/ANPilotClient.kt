package anpilot.client

import anpilot.client.bootstrap.ANClientBootstrap
import net.fabricmc.api.ModInitializer

object ANPilotClient : ModInitializer {
    override fun onInitialize() {
        ANClientBootstrap.initialize()
    }
}
