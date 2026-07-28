package anpilot.client.features.module.anpilot

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.HudGroupSetting
import anpilot.client.minecraft.gui.ANHudEditorScreen
import net.minecraft.client.Minecraft

class ANPilotHud : ANBaseModule(
    name = "PilotHud",
    description = "Client HUD editor entry.",
    category = ANModuleCategory.CLIENT,
    chineseName = "HUD编辑器"
) {
    val hud = addSetting(ANSetting("ANPilotHud", HudGroupSetting(0f, 0f)))

    override fun onEnable() {
        Minecraft.getInstance().setScreen(ANHudEditorScreen())
        disable()
    }
}
