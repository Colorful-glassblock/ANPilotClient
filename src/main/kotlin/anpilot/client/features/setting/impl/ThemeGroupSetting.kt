package anpilot.client.features.setting.impl

class ThemeGroupSetting(
    private var themeName: String
) {
    fun getTheme_Name(): String = themeName

    fun setTheme_Name(themeName: String) {
        this.themeName = themeName
    }
}
