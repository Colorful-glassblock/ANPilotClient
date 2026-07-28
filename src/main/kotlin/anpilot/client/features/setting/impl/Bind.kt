package anpilot.client.features.setting.impl

import org.lwjgl.glfw.GLFW

class Bind(
    val key: Int,
    val mouse: Boolean
) {
    var hold: Boolean = false

    val displayName: String
        get() {
            if (mouse) return "M$key"
            if (key == -1) return ""

            keyAlias(key)?.let { return it }

            val glfwName = GLFW.glfwGetKeyName(key, 0)
            if (glfwName != null) return glfwName.uppercase()

            return GLFW::class.java.declaredFields
                .firstOrNull { field ->
                    field.name.startsWith("GLFW_KEY_") && field.getInt(null) == key
                }
                ?.name
                ?.removePrefix("GLFW_KEY_")
                ?.replace('_', ' ')
                ?.lowercase()
                ?.split(' ')
                ?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                ?: "UNKNOWN.$key"
        }

    private fun keyAlias(key: Int): String? = when (key) {
        GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT"
        GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT"
        GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL"
        GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL"
        GLFW.GLFW_KEY_LEFT_ALT -> "LALT"
        GLFW.GLFW_KEY_RIGHT_ALT -> "RALT"
        GLFW.GLFW_KEY_ESCAPE -> "ESC"
        GLFW.GLFW_KEY_SPACE -> "SPACE"
        GLFW.GLFW_KEY_ENTER -> "ENTER"
        GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE"
        GLFW.GLFW_KEY_DELETE -> "DELETE"
        else -> null
    }
}
