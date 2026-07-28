package anpilot.client.features.setting.impl

import java.awt.Color

class ColorGroupSetting(color: Int) {
    private var colorValue: Int = color
    var saturation: Float = 0f
    var brightness: Float = 0f
    var alpha: Float = 0f

    val awtColor: Color
        get() = getColorRGB()

    fun getColor(): Int = colorValue

    fun setColor(color: Int) {
        colorValue = color
    }

    fun getColorRGB(): Color = Color(colorValue, true)

    fun getColor_Saturation(): Float = saturation

    fun setColor_Saturation(colorSaturation: Float) {
        saturation = colorSaturation
    }

    fun getColor_Bright(): Float = brightness

    fun setColor_Bright(colorBright: Float) {
        brightness = colorBright
    }

    fun getColor_Alpha(): Float = alpha

    fun setColor_Alpha(colorAlpha: Float) {
        alpha = colorAlpha
    }
}
