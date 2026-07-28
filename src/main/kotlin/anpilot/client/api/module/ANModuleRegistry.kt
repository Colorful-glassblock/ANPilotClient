package anpilot.client.api.module

interface ANModuleRegistry {
    fun categories(): List<ANModuleCategory>

    fun modules(category: ANModuleCategory): List<ANModule>

    fun allModules(): List<ANModule> = categories().flatMap(::modules)
}
