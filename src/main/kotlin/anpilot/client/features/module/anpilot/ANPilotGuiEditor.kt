package anpilot.client.features.module.anpilot

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.module.misc.ANBotTask.TriggerMode
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.ColorGroupSetting
import com.mojang.blaze3d.platform.InputConstants
import java.awt.Color

class ANPilotGuiEditor : ANBaseModule(
    name = "ClickGui",
    description = "Client GUI editor entry.",
    category = ANModuleCategory.CLIENT,
    chineseName = "界面编辑器"
) {
    enum class Language {
        English,
        Chinese
    }

    enum class Group {
        FILL,
        BORDER,
        RADIUS
    }

    val language = addSetting(ANSetting("Language", Language.English))
    val animations = addSetting(ANSetting("Animations", false))
    val groupSelect = addSetting(ANSetting("Pages", Group.FILL))

    val bgTint = addSetting(ANSetting("BgTint", ColorGroupSetting(Color(0xFFFFFFFF.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val panelFill = addSetting(ANSetting("PanelFill", ColorGroupSetting(Color(0xD9182434.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val panelBorder = addSetting(ANSetting("PanelBorder", ColorGroupSetting(Color(0xCC2F8CFF.toInt(), true).rgb)) { groupSelect.value == Group.BORDER })
    val panelText = addSetting(ANSetting("PanelText", ColorGroupSetting(Color(0xFF5CFFB3.toInt(), true).rgb)) { groupSelect.value == Group.FILL })

    val btnFill = addSetting(ANSetting("BtnFill", ColorGroupSetting(Color(0xCC1B324D.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val btnBorder = addSetting(ANSetting("BtnBorder", ColorGroupSetting(Color(0xCC2F8CFF.toInt(), true).rgb)) { groupSelect.value == Group.BORDER })
    val btnHoverFill = addSetting(ANSetting("BtnHoverFill", ColorGroupSetting(Color(0xE02A4F73.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val btnOnFill = addSetting(ANSetting("BtnOnFill", ColorGroupSetting(Color(0xE025725A.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val btnOnBorder = addSetting(ANSetting("BtnOnBorder", ColorGroupSetting(Color(0xFF4EF2A7.toInt(), true).rgb)) { groupSelect.value == Group.BORDER })
    val btnText = addSetting(ANSetting("BtnText", ColorGroupSetting(Color(0xFFE6F6FF.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val btnOffText = addSetting(ANSetting("BtnOffText", ColorGroupSetting(Color(0xB8E6F6FF.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val btnDot = addSetting(ANSetting("BtnDot", ColorGroupSetting(Color(0xFF4C7DAB.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val btnOnDot = addSetting(ANSetting("BtnOnDot", ColorGroupSetting(Color(0xFF4EF2A7.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val descFill = addSetting(ANSetting("DescFill", ColorGroupSetting(Color(0xE6101824.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val descText = addSetting(ANSetting("DescText", ColorGroupSetting(Color(0xFFD7E8F8.toInt(), true).rgb)) { groupSelect.value == Group.FILL })

    val setText = addSetting(ANSetting("SetText", ColorGroupSetting(Color(0xFFD7E8F8.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val setCtrlFill = addSetting(ANSetting("SetCtrlFill", ColorGroupSetting(Color(0xCC16263A.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val setCtrlBorder = addSetting(ANSetting("SetCtrlBorder", ColorGroupSetting(Color(0xCC2F8CFF.toInt(), true).rgb)) { groupSelect.value == Group.BORDER })
    val setAccent = addSetting(ANSetting("SetAccent", ColorGroupSetting(Color(0xFF4EF2A7.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val setMutedText = addSetting(ANSetting("SetMutedText", ColorGroupSetting(Color(0x99D7E8F8.toInt(), true).rgb)) { groupSelect.value == Group.FILL })

    val selFill = addSetting(ANSetting("SelFill", ColorGroupSetting(Color(0xCC16263A.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val selBorder = addSetting(ANSetting("SelBorder", ColorGroupSetting(Color(0xCC2F8CFF.toInt(), true).rgb)) { groupSelect.value == Group.BORDER })
    val selHoverFill = addSetting(ANSetting("SelHoverFill", ColorGroupSetting(Color(0xE02A4F73.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val selOnFill = addSetting(ANSetting("SelOnFill", ColorGroupSetting(Color(0xE025725A.toInt(), true).rgb)) { groupSelect.value == Group.FILL })
    val selOnBorder = addSetting(ANSetting("SelOnBorder", ColorGroupSetting(Color(0xFF4EF2A7.toInt(), true).rgb)) { groupSelect.value == Group.BORDER })

    val panelRadius = addSetting(ANSetting("PanelRadius", 13f, 1f, 20f) { groupSelect.value == Group.RADIUS })
    val btnRadius = addSetting(ANSetting("BtnRadius", 8f, 1f, 10f) { groupSelect.value == Group.RADIUS })
    val panelBorderWidth = addSetting(ANSetting("PanelBorderW", 1.0f, 0.0f, 4.0f) { groupSelect.value == Group.RADIUS })
    val btnBorderWidth = addSetting(ANSetting("BtnBorderW", 1f, 0.0f, 2.0f) { groupSelect.value == Group.RADIUS })

    init {
        setBind(InputConstants.KEY_RSHIFT, false)
        activeLanguageSetting = language
        activeAnimationsSetting = animations
        syncToTheme()
    }

    override fun isToggleable(): Boolean = false

    fun ensureDefaultBind() {
        if (getBind().key == -1) {
            setBind(InputConstants.KEY_RSHIFT, false)
        }
    }

    fun syncToTheme() {
        ensureDefaultBind()
        getSettings().forEach { setting ->
            when (val v = setting.value) {
                is ColorGroupSetting -> updateTheme(setting)
                is Int -> ANTheme.updateFloatFromSetting(setting.name, v.toFloat())
                is Float -> ANTheme.updateFloatFromSetting(setting.name, v)
                is Double -> ANTheme.updateFloatFromSetting(setting.name, v.toFloat())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateTheme(setting: ANSetting<*>) {
        ANTheme.updateFromSetting(setting.name, (setting as ANSetting<ColorGroupSetting>).value.getColor())
    }

    companion object {
        private var activeLanguageSetting: ANSetting<Language>? = null
        private var activeAnimationsSetting: ANSetting<Boolean>? = null

        fun useChineseNames(): Boolean = activeLanguageSetting?.value == Language.Chinese

        fun animationsEnabled(): Boolean = activeAnimationsSetting?.value != false
    }
}
