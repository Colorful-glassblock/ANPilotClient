package anpilot.client.api.module

interface ANModule {
    val name: String
    val description: String
    val category: ANModuleCategory
    var state: ANModuleState

    val enabled: Boolean
        get() = state == ANModuleState.ENABLED

    fun onEnable() {}

    fun onDisable() {}

    fun onTick() {}

    fun onLogin() {}

    fun onLogout() {}

    fun onThread() {}

    fun onUnload() {}
}
