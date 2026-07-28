package anpilot.client.features.setting.impl

class FileSelectSetting(
    private val filesProvider: () -> List<String>,
    defaultFile: String = ""
) {
    var fileName: String = defaultFile

    fun files(): List<String> = filesProvider()

    fun currentFileName(): String {
        val files = files()
        if (fileName.isBlank() || fileName !in files) {
            fileName = files.firstOrNull().orEmpty()
        }
        return fileName
    }

    fun setFile(name: String) {
        fileName = name
    }
}
