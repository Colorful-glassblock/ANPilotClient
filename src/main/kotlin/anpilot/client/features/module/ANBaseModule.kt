package anpilot.client.features.module

import anpilot.client.api.module.ANModule
import anpilot.client.api.module.ANModuleCategory
import anpilot.client.api.module.ANModuleState
import anpilot.client.bootstrap.ANServiceRegistry
import anpilot.client.features.module.anpilot.ANPilotGuiEditor
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.Bind
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

open class ANBaseModule(
    override val name: String,
    override val description: String,
    override val category: ANModuleCategory,
    private val chineseName: String? = null,
    defaultState: ANModuleState = ANModuleState.DISABLED
) : ANModule {
    val mc: Minecraft = Minecraft.getInstance()

    private val bind = ANSetting("Keybind", Bind(-1, false))
    private val open = ANSetting("IsOpen", false)
    private val itemSelectOpen = ANSetting("ItemSelectOpen", false)
    private val settings = mutableListOf<ANSetting<*>>(bind, open, itemSelectOpen)
    private var customDisableMessage: String? = null

    override var state: ANModuleState = defaultState
        set(value) {
            if (field == value) return
            field = value
            if (value == ANModuleState.ENABLED) {
                if (!fullNullCheck()) {
                    sendClientMessage("Enable")
                    onEnable()
                }
                if (ANServiceRegistry.isInitialized) {
                    ANServiceRegistry.runtime.eventBus.subscribe(this)
                }
            } else {
                if (ANServiceRegistry.isInitialized) {
                    ANServiceRegistry.runtime.eventBus.unsubscribe(this)
                }
                if (!fullNullCheck()) {
                    val msg = customDisableMessage ?: "Disable"
                    sendClientMessage(msg)
                    customDisableMessage = null
                    onDisable()
                }
            }
        }

    val isOpen: Boolean
        get() = open.value

    fun setOpen(value: Boolean) {
        open.setValue(value)
    }

    fun setOpenSilent(value: Boolean) {
        open.setValueSilent(value)
    }

    fun Is_itemSelect_open(): Boolean = itemSelectOpen.value

    fun Set_itemSelect_open(value: Boolean) {
        itemSelectOpen.setValue(value)
    }

    fun toggle() {
        if (!isToggleable()) return
        state = if (enabled) ANModuleState.DISABLED else ANModuleState.ENABLED
    }

    fun enable() {
        state = ANModuleState.ENABLED
    }

    fun disable() {
        state = ANModuleState.DISABLED
    }

    fun disable(message: String) {
        customDisableMessage = "Disable: $message"
        state = ANModuleState.DISABLED
    }

    fun isOn(): Boolean = enabled

    fun isOff(): Boolean = !enabled

    fun setEnabled(enabled: Boolean) {
        state = if (enabled) ANModuleState.ENABLED else ANModuleState.DISABLED
    }

    fun listening(): Boolean = enabled

    fun getDisplayName(): String = name

    open fun getDisplayHudName(): String {
        return if (ANPilotGuiEditor.useChineseNames() && chineseName != null) chineseName else name
    }

    open fun isToggleable(): Boolean = true

    fun getFullArrayString(): String = getDisplayHudName()

    fun getBind(): Bind = bind.value

    fun setBind(key: Int, mouse: Boolean) {
        bind.setValue(Bind(key, mouse))
    }

    open fun onBindPressed(setting: ANSetting<Bind>, key: Int, mouse: Boolean) {
    }

    open fun onMousePressed(button: Int) {
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ANSetting<*>> addSetting(setting: T): T {
        if (!settings.contains(setting)) {
            settings += setting
        }
        setting.module = this
        return setting
    }

    open fun getSettings(): List<ANSetting<*>> {
        collectDeclaredSettings()
        return settings
    }

    fun sendChatMessage(message: String) {
        if (fullNullCheck()) return
        Minecraft.getInstance().connection?.sendChat(message)
    }

    fun sendChatCommand(command: String) {
        if (fullNullCheck()) return
        Minecraft.getInstance().connection?.sendCommand(command.removePrefix("/"))
    }

    fun sendClientMessage(message: String) {
        if (fullNullCheck()) return
        Minecraft.getInstance().player?.sendSystemMessage(
            Component.literal("[$name]").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" "))
                .append(Component.literal(message).withStyle(ChatFormatting.LIGHT_PURPLE))
        )
    }

    protected fun fullNullCheck(): Boolean {
        val minecraft = Minecraft.getInstance()
        return minecraft.player == null || minecraft.level == null
    }

    private fun collectDeclaredSettings() {
        generateSequence<Class<*>>(javaClass) { it.superclass }
            .takeWhile { it != ANBaseModule::class.java.superclass }
            .flatMap { it.declaredFields.asSequence() }
            .filter { ANSetting::class.java.isAssignableFrom(it.type) }
            .forEach { field ->
                field.isAccessible = true
                val setting = field.get(this) as? ANSetting<*> ?: return@forEach
                addSetting(setting)
            }
    }
}
