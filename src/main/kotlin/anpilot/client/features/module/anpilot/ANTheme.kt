package anpilot.client.features.module.anpilot

import java.awt.Color

object ANTheme {
    const val MODULE_RADIUS = 15f
    const val MODULE_BORDER = 3f
    const val BUTTON_RADIUS = 10f
    const val BUTTON_BORDER = 1.5f
    const val BUTTON_OPT_RADIUS = 6f
    const val BUTTON_COLOR_RADIUS = 8f
    const val BUTTON_OPT_BORDER = 1.5f

    val Purple: Color = Color(90, 32, 246, 249)
    val Transparent: Color = Color(255, 255, 255, 0)
    val White: Color = Color(255, 255, 255, 255)
    val Black: Color = Color(0, 0, 0, 255)
    val Red: Color = Color(255, 0, 0, 255)
    val Green: Color = Color(0, 255, 0, 255)
    val Blue: Color = Color(0, 0, 255, 255)
    val Yellow: Color = Color(255, 255, 0, 255)
    val Cyan: Color = Color(0, 255, 255, 255)
    val Magenta: Color = Color(255, 0, 255, 255)
    val Gray: Color = Color(44, 44, 44, 255)

    var BgTint: Color = White
        private set
    var PanelFill: Color = Color(0xD9182434.toInt(), true)
        private set
    var PanelBorder: Color = Color(0xCC2F8CFF.toInt(), true)
        private set
    var PanelText: Color = Color(0xFF5CFFB3.toInt(), true)
        private set

    var BtnFill: Color = Color(0xCC1B324D.toInt(), true)
        private set
    var BtnBorder: Color = Color(0xCC2F8CFF.toInt(), true)
        private set
    var BtnHoverFill: Color = Color(0xE02A4F73.toInt(), true)
        private set
    var BtnOnFill: Color = Color(0xE025725A.toInt(), true)
        private set
    var BtnOnBorder: Color = Color(0xFF4EF2A7.toInt(), true)
        private set
    var BtnText: Color = Color(0xFFE6F6FF.toInt(), true)
        private set
    var BtnOffText: Color = Color(0xB8E6F6FF.toInt(), true)
        private set
    var BtnDot: Color = Color(0xFF4C7DAB.toInt(), true)
        private set
    var BtnOnDot: Color = Color(0xFF4EF2A7.toInt(), true)
        private set
    var DescFill: Color = Color(0xE6101824.toInt(), true)
        private set
    var DescText: Color = Color(0xFFD7E8F8.toInt(), true)
        private set

    var SetText: Color = Color(0xFFD7E8F8.toInt(), true)
        private set
    var SetCtrlFill: Color = Color(0xCC16263A.toInt(), true)
        private set
    var SetCtrlBorder: Color = Color(0xCC2F8CFF.toInt(), true)
        private set
    var SetAccent: Color = Color(0xFF4EF2A7.toInt(), true)
        private set
    var SetMutedText: Color = Color(0x99D7E8F8.toInt(), true)
        private set

    var SelFill: Color = Color(0xCC16263A.toInt(), true)
        private set
    var SelBorder: Color = Color(0xCC2F8CFF.toInt(), true)
        private set
    var SelHoverFill: Color = Color(0xE02A4F73.toInt(), true)
        private set
    var SelOnFill: Color = Color(0xE025725A.toInt(), true)
        private set
    var SelOnBorder: Color = Color(0xFF4EF2A7.toInt(), true)
        private set

    var PanelRadius: Float = 15f
        private set
    var BtnRadius: Float = 10f
        private set

    var PanelBorderWidth: Float = 3f
        private set
    var BtnBorderWidth: Float = 1.5f
        private set

    private val colorMap = mutableMapOf<String, Int>()
    private val floatMap = mutableMapOf<String, Float>()

    fun updateFromSetting(themeKey: String?, colorValue: Int) {
        if (themeKey == null) return
        colorMap[themeKey] = colorValue
        updateAllReferences()
    }

    fun updateFloatFromSetting(themeKey: String?, floatValue: Float) {
        if (themeKey == null) return
        floatMap[themeKey] = floatValue
        updateAllReferences()
    }

    fun getColor(themeKey: String): Color = Color(colorMap[themeKey] ?: defaultColor(themeKey).rgb, true)

    private fun updateAllReferences() {
        PanelRadius = floatMap["PanelRadius"] ?: floatMap["panelRadius"] ?: 15f
        BtnRadius = floatMap["BtnRadius"] ?: floatMap["btnRadius"] ?: 10f
        PanelBorderWidth = floatMap["PanelBorderW"] ?: floatMap["panelBorderW"] ?: 3f
        BtnBorderWidth = floatMap["BtnBorderW"] ?: floatMap["btnBorderW"] ?: 1.5f

        BgTint = getColor("BgTint")
        PanelFill = getColor("PanelFill")
        PanelBorder = getColor("PanelBorder")
        PanelText = getColor("PanelText")

        BtnFill = getColor("BtnFill")
        BtnBorder = getColor("BtnBorder")
        BtnHoverFill = getColor("BtnHoverFill")
        BtnOnFill = getColor("BtnOnFill")
        BtnOnBorder = getColor("BtnOnBorder")
        BtnText = getColor("BtnText")
        BtnOffText = getColor("BtnOffText")
        BtnDot = getColor("BtnDot")
        BtnOnDot = getColor("BtnOnDot")
        DescFill = getColor("DescFill")
        DescText = getColor("DescText")

        SetText = getColor("SetText")
        SetCtrlFill = getColor("SetCtrlFill")
        SetCtrlBorder = getColor("SetCtrlBorder")
        SetAccent = getColor("SetAccent")
        SetMutedText = getColor("SetMutedText")

        SelFill = getColor("SelFill")
        SelBorder = getColor("SelBorder")
        SelHoverFill = getColor("SelHoverFill")
        SelOnFill = getColor("SelOnFill")
        SelOnBorder = getColor("SelOnBorder")
    }

    private fun defaultColor(themeKey: String): Color = when (themeKey) {
        "BgTint" -> White
        "PanelFill" -> Color(0xD9182434.toInt(), true)
        "PanelBorder" -> Color(0xCC2F8CFF.toInt(), true)
        "PanelText" -> Color(0xFF5CFFB3.toInt(), true)
        "BtnFill" -> Color(0xCC1B324D.toInt(), true)
        "BtnBorder" -> Color(0xCC2F8CFF.toInt(), true)
        "BtnHoverFill" -> Color(0xE02A4F73.toInt(), true)
        "BtnOnFill" -> Color(0xE025725A.toInt(), true)
        "BtnOnBorder" -> Color(0xFF4EF2A7.toInt(), true)
        "BtnText" -> Color(0xFFE6F6FF.toInt(), true)
        "BtnOffText" -> Color(0xB8E6F6FF.toInt(), true)
        "BtnDot" -> Color(0xFF4C7DAB.toInt(), true)
        "BtnOnDot" -> Color(0xFF4EF2A7.toInt(), true)
        "DescFill" -> Color(0xE6101824.toInt(), true)
        "DescText" -> Color(0xFFD7E8F8.toInt(), true)
        "SetText" -> Color(0xFFD7E8F8.toInt(), true)
        "SetCtrlFill" -> Color(0xCC16263A.toInt(), true)
        "SetCtrlBorder" -> Color(0xCC2F8CFF.toInt(), true)
        "SetAccent" -> Color(0xFF4EF2A7.toInt(), true)
        "SetMutedText" -> Color(0x99D7E8F8.toInt(), true)
        "SelFill" -> Color(0xCC16263A.toInt(), true)
        "SelBorder" -> Color(0xCC2F8CFF.toInt(), true)
        "SelHoverFill" -> Color(0xE02A4F73.toInt(), true)
        "SelOnFill" -> Color(0xE025725A.toInt(), true)
        "SelOnBorder" -> Color(0xFF4EF2A7.toInt(), true)
        else -> White
    }
}
