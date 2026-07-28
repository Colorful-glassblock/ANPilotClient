package anpilot.client.renderer

import kotlin.math.roundToInt

data class ANColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255
) {
    val argb: Int
        get() = ((alpha and 255) shl 24) or
            ((red and 255) shl 16) or
            ((green and 255) shl 8) or
            (blue and 255)

    fun withAlpha(alpha: Int): ANColor = copy(alpha = alpha.coerceIn(0, 255))

    fun withAlpha(alpha: Float): ANColor = withAlpha((alpha.coerceIn(0f, 1f) * 255f).roundToInt())

    companion object {
        val WHITE = ANColor(255, 255, 255)
        val BLACK = ANColor(0, 0, 0)
        val TRANSPARENT = ANColor(0, 0, 0, 0)

        fun rgb(red: Int, green: Int, blue: Int): ANColor = ANColor(red, green, blue)

        fun rgba(red: Int, green: Int, blue: Int, alpha: Int): ANColor = ANColor(red, green, blue, alpha)

        fun fromArgb(argb: Int): ANColor = ANColor(
            red = (argb ushr 16) and 255,
            green = (argb ushr 8) and 255,
            blue = argb and 255,
            alpha = (argb ushr 24) and 255
        )
    }
}