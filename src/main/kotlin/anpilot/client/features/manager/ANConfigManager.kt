package anpilot.client.features.manager

import anpilot.client.api.module.ANModule
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.module.ANModuleManager
import anpilot.client.features.module.anpilot.ANPilotGuiEditor
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.Bind
import anpilot.client.features.setting.impl.ColorGroupSetting
import anpilot.client.features.setting.impl.ConfigGroupSetting
import anpilot.client.features.setting.impl.FileSelectSetting
import anpilot.client.features.setting.impl.HudGroupSetting
import anpilot.client.features.setting.impl.ItemSelectSetting
import anpilot.client.features.setting.impl.ThemeGroupSetting
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import java.io.File
import anpilot.client.features.module.anpilot.ANPilotConfig
import anpilot.client.features.module.anpilot.ANPilotTheme
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object ANConfigManager {
    private const val CONFIG_FOLDER_NAME = "ANPilotClient"
    private const val DEFAULT_CONFIG = "DefaultConfig"
    private const val DEFAULT_THEME = "DefaultTheme"
    private const val AUTHOR = "AN-G"

    private val logger = LoggerFactory.getLogger("ANPilotConfig")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var moduleManager: ANModuleManager? = null
    private var loading = false
    private var loadedForCurrentGame = false
    private var lastAutoSaveTime = 0L

    private const val AUTO_SAVE_INTERVAL_MS = 30L * 60L * 1000L

    private val mainFolder: File get() = File(Minecraft.getInstance().gameDirectory, CONFIG_FOLDER_NAME)
    private val configsFolder: File get() = File(mainFolder, "Configs")
    private val themeFolder: File get() = File(mainFolder, "Theme")
    private val customFolder: File get() = File(mainFolder, "Custom")
    private val customSoundsFolder: File get() = File(customFolder, "Sounds")
    private val customBackgroundFolder: File get() = File(customFolder, "BackGround")
    private val filesFolder: File get() = File(mainFolder, "Files")
    private val elytraFilesFolder: File get() = File(filesFolder, "Elytra")
    private val baseFinderFolder: File get() = File(filesFolder, "BaseFinder")
    private val mapArtFolder: File get() = File(filesFolder, "MapArt")
    private val autoBuildFolder: File get() = File(filesFolder, "AutoBuild")
    private val fastBuildFolder: File get() = File(filesFolder, "FastBuild")
    private val otherFolder: File get() = File(mainFolder, "Other")
    private val configIndex: File get() = File(otherFolder, "ConfigsIndex.txt")
    private val themeIndex: File get() = File(otherFolder, "ThemesIndex.txt")

    fun initialize(moduleManager: ANModuleManager) {
        this.moduleManager = moduleManager
        createDirs()
        if (!configFile(DEFAULT_CONFIG).exists()) saveConfig(DEFAULT_CONFIG)
        if (!themeFile(DEFAULT_THEME).exists()) saveTheme(DEFAULT_THEME)
    }

    fun configNames(): List<String> {
        createDirs()
        return configsFolder.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".ANPilot") }
            ?.map { it.name.removeSuffix(".ANPilot") }
            ?.sorted()
            ?: emptyList()
    }

    fun themeNames(): List<String> {
        createDirs()
        return themeFolder.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".ANTheme") }
            ?.map { it.name.removeSuffix(".ANTheme") }
            ?.sorted()
            ?: emptyList()
    }

    fun elytraFileNames(): List<String> {
        createDirs()
        return elytraFilesFolder.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".txt", ignoreCase = true) }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    fun elytraFile(name: String): File {
        createDirs()
        val clean = cleanName(name).ifBlank { elytraFileNames().firstOrNull().orEmpty() }
        return File(elytraFilesFolder, clean)
    }

    fun foundElytraFile(): File {
        createDirs()
        return File(otherFolder, "FoundEytra.txt")
    }

    fun customSoundsFolder(): File {
        createDirs()
        return customSoundsFolder
    }

    fun customBackgroundFolder(): File {
        createDirs()
        return customBackgroundFolder
    }

    fun baseFinderSessionFile(): File {
        createDirs()
        return File(baseFinderFolder, "session.json")
    }

    fun baseFinderDetectionsFile(): File {
        createDirs()
        return File(baseFinderFolder, "detections.json")
    }

    fun mapArtFileNames(): List<String> {
        createDirs()
        return projectionFileNames(mapArtFolder)
    }

    fun mapArtFile(name: String): File {
        createDirs()
        val clean = cleanName(name).ifBlank { mapArtFileNames().firstOrNull().orEmpty() }
        return File(mapArtFolder, clean)
    }

    fun autoBuildFileNames(): List<String> {
        createDirs()
        return projectionFileNames(autoBuildFolder)
    }

    fun autoBuildFile(name: String): File {
        createDirs()
        val clean = cleanName(name).ifBlank { autoBuildFileNames().firstOrNull().orEmpty() }
        return File(autoBuildFolder, clean)
    }

    fun fastBuildFileNames(): List<String> {
        createDirs()
        return projectionFileNames(fastBuildFolder)
    }

    fun fastBuildFile(name: String): File {
        createDirs()
        val clean = cleanName(name).ifBlank { fastBuildFileNames().firstOrNull().orEmpty() }
        return File(fastBuildFolder, clean)
    }

    fun currentConfigName(): String = readIndex(configIndex, DEFAULT_CONFIG)

    fun currentThemeName(): String = readIndex(themeIndex, DEFAULT_THEME)

    fun chooseConfig(name: String) {
        val cleanName = cleanName(name).ifEmpty { DEFAULT_CONFIG }
        val file = configFile(cleanName)
        if (!file.exists()) saveConfig(cleanName)
        loadConfig(file)
        writeIndex(configIndex, cleanName)
        syncConfigSetting(cleanName)
    }

    fun chooseTheme(name: String) {
        val cleanName = cleanName(name).ifEmpty { DEFAULT_THEME }
        val file = themeFile(cleanName)
        if (!file.exists()) saveTheme(cleanName)
        loadTheme(file)
        writeIndex(themeIndex, cleanName)
        syncThemeSetting(cleanName)
    }

    fun createConfig(name: String) {
        val cleanName = cleanName(name)
        if (cleanName.isEmpty()) return
        saveConfig(cleanName)
        chooseConfig(cleanName)
    }

    fun createTheme(name: String) {
        val cleanName = cleanName(name)
        if (cleanName.isEmpty()) return
        saveTheme(cleanName)
        chooseTheme(cleanName)
    }

    fun loadCurrent() {
        chooseConfig(currentConfigName())
        chooseTheme(currentThemeName())
        guiEditor()?.ensureDefaultBind()
    }

    fun loadOnGameJoin() {
        if (loadedForCurrentGame) return
        loadCurrent()
        loadedForCurrentGame = true
        lastAutoSaveTime = System.currentTimeMillis()
    }

    fun saveOnGameLeave() {
        if (!loadedForCurrentGame) return
        saveCurrent()
        loadedForCurrentGame = false
    }

    fun autoSaveIfNeeded() {
        if (!loadedForCurrentGame) return
        val now = System.currentTimeMillis()
        if (now - lastAutoSaveTime < AUTO_SAVE_INTERVAL_MS) return
        saveCurrent()
        lastAutoSaveTime = now
    }

    fun saveCurrent() {
        saveConfig(currentConfigName())
        saveTheme(currentThemeName())
    }

    fun saveConfig(name: String = currentConfigName()) {
        createDirs()
        val root = JsonObject()
        root.addProperty("author", AUTHOR)
        root.add("ANPilotModules", moduleArray(includeThemeEditor = false))
        
        val file = configFile(cleanName(name).ifEmpty { DEFAULT_CONFIG })
        val content = gson.toJson(JsonArray().apply { add(root) })
        safeWrite(file, content)
    }

    fun saveTheme(name: String = currentThemeName()) {
        createDirs()
        val root = JsonObject()
        root.add("ANPilotTheme", moduleArray(includeThemeEditor = true))
        
        val file = themeFile(cleanName(name).ifEmpty { DEFAULT_THEME })
        val content = gson.toJson(JsonArray().apply { add(root) })
        safeWrite(file, content)
    }

    private fun loadConfig(file: File) {
        var root = parseRoot(file)
        if (root == null) {
            val backupFile = File(file.parentFile, "${file.name}.bak")
            if (backupFile.exists()) {
                logger.warn("Main config file ${file.name} is corrupt! Attempting to load from backup...")
                root = parseRoot(backupFile)
            }
        }
        if (root == null) return
        
        loading = true
        try {
            root.getAsJsonArray("ANPilotModules")?.forEach { parseModule(it) }
        } finally {
            loading = false
        }
    }

    private fun loadTheme(file: File) {
        var root = parseRoot(file)
        if (root == null) {
            val backupFile = File(file.parentFile, "${file.name}.bak")
            if (backupFile.exists()) {
                logger.warn("Main theme file ${file.name} is corrupt! Attempting to load from backup...")
                root = parseRoot(backupFile)
            }
        }
        if (root == null) return
        
        loading = true
        try {
            root.getAsJsonArray("ANPilotTheme")?.forEach { parseModule(it) }
            guiEditor()?.syncToTheme()
        } finally {
            loading = false
        }
    }

    private fun parseRoot(file: File): JsonObject? = try {
        JsonParser.parseString(file.readText()).asJsonArray.firstOrNull()?.asJsonObject
    } catch (exception: Exception) {
        logger.warn("Failed to parse ${file.name}", exception)
        null
    }

    private fun moduleArray(includeThemeEditor: Boolean): JsonArray {
        val array = JsonArray()
        modules()
            .filterIsInstance<ANBaseModule>()
            .filter { (it is ANPilotGuiEditor) == includeThemeEditor }
            .forEach { array.add(moduleObject(it)) }
        return array
    }

    private fun moduleObject(module: ANBaseModule): JsonObject {
        val settings = JsonObject()
        module.getSettings().forEach { setting -> settingElement(setting)?.let { settings.add(setting.name, it) } }
        settings.addProperty("Enabled", module.enabled)
        return JsonObject().apply { add(module.name, settings) }
    }

    private fun settingElement(setting: ANSetting<*>): JsonElement? {
        return when (val value = setting.value) {
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Enum<*> -> JsonPrimitive(value.ordinal)
            is Bind -> JsonArray().apply {
                add(JsonPrimitive(if (value.mouse) "M${value.key}" else value.key.toString()))
                add(JsonPrimitive(value.hold))
            }
            is ColorGroupSetting -> JsonArray().apply {
                add(JsonPrimitive(value.getColor()))
                add(JsonPrimitive(value.getColor_Saturation()))
                add(JsonPrimitive(value.getColor_Bright()))
                add(JsonPrimitive(value.getColor_Alpha()))
            }
            is HudGroupSetting -> JsonArray().apply {
                add(JsonPrimitive(value.x))
                add(JsonPrimitive(value.y))
            }
            is ItemSelectSetting -> JsonArray().apply {
                value.getItemsById().forEach { add(JsonPrimitive(it)) }
            }
            is ConfigGroupSetting -> JsonPrimitive(value.index)
            is FileSelectSetting -> JsonPrimitive(value.fileName)
            is ThemeGroupSetting -> JsonPrimitive(value.getTheme_Name())
            else -> null
        }
    }

    private fun parseModule(element: JsonElement) {
        try {
            val objectValue = element.asJsonObject
            val module = modules().filterIsInstance<ANBaseModule>().firstOrNull { objectValue.has(it.name) } ?: return
            val settings = objectValue.getAsJsonObject(module.name) ?: return
            module.getSettings().forEach { setting ->
                val value = settings.get(setting.name) ?: return@forEach
                try {
                    applySetting(setting, value)
                } catch (exception: Exception) {
                    logger.warn("Failed to load ${module.name}.${setting.name}", exception)
                }
            }
            settings.get("Enabled")?.let { enabled ->
                try {
                    module.setEnabled(enabled.asBoolean)
                } catch (exception: Exception) {
                    logger.warn("Failed to load ${module.name}.Enabled", exception)
                }
            }
        } catch (exception: Exception) {
            logger.warn("Failed to parse module configuration element: $element", exception)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applySetting(setting: ANSetting<*>, element: JsonElement) {
        when (val current = setting.value) {
            is Boolean -> (setting as ANSetting<Boolean>).setValueSilent(element.asBoolean)
            is Int -> (setting as ANSetting<Int>).setValueSilent(element.asInt)
            is Float -> (setting as ANSetting<Float>).setValueSilent(element.asFloat)
            is Double -> (setting as ANSetting<Double>).setValueSilent(element.asDouble)
            is Long -> (setting as ANSetting<Long>).setValueSilent(element.asLong)
            is String -> (setting as ANSetting<String>).setValueSilent(element.asString)
            is Enum<*> -> (setting as ANSetting<Enum<*>>).setEnumByNumber(element.asInt)
            is Bind -> {
                if (element.isJsonArray) {
                    val array = element.asJsonArray
                    if (array.size() >= 2) {
                        val bindName = array[0].asString
                        val mouse = bindName.startsWith("M")
                        val nextBind = Bind(bindName.removePrefix("M").toInt(), mouse)
                        nextBind.hold = array[1].asBoolean
                        (setting as ANSetting<Bind>).setValueSilent(nextBind)
                    }
                }
            }
            is ColorGroupSetting -> {
                if (element.isJsonArray) {
                    val array = element.asJsonArray
                    if (array.size() >= 4) {
                        current.setColor(array[0].asInt)
                        current.setColor_Saturation(array[1].asFloat)
                        current.setColor_Bright(array[2].asFloat)
                        current.setColor_Alpha(array[3].asFloat)
                    }
                }
            }
            is HudGroupSetting -> {
                if (element.isJsonArray) {
                    val array = element.asJsonArray
                    if (array.size() >= 2) {
                        current.x = array[0].asFloat
                        current.y = array[1].asFloat
                    }
                }
            }
            is ItemSelectSetting -> {
                if (element.isJsonArray) {
                    current.clear()
                    element.asJsonArray.forEach { current.add(it.asString) }
                }
            }
            is ConfigGroupSetting -> current.index = element.asInt
            is FileSelectSetting -> current.setFile(element.asString)
            is ThemeGroupSetting -> current.setTheme_Name(element.asString)
        }
    }

    private fun syncConfigSetting(name: String) {
        val names = configNames()
        modules().filterIsInstance<ANPilotConfig>().firstOrNull()?.config?.value?.index = names.indexOf(name).coerceAtLeast(0)
    }

    private fun syncThemeSetting(name: String) {
        modules().filterIsInstance<ANPilotTheme>().firstOrNull()?.theme?.value?.setTheme_Name(name)
    }

    private fun guiEditor(): ANPilotGuiEditor? = modules().filterIsInstance<ANPilotGuiEditor>().firstOrNull()

    private fun modules(): List<ANModule> = moduleManager?.allModules().orEmpty()

    private fun createDirs() {
        listOf(
            mainFolder,
            configsFolder,
            themeFolder,
            customFolder,
            customSoundsFolder,
            customBackgroundFolder,
            filesFolder,
            elytraFilesFolder,
            baseFinderFolder,
            mapArtFolder,
            autoBuildFolder,
            fastBuildFolder,
            otherFolder
        ).forEach { it.mkdirs() }
    }

    private fun configFile(name: String): File = File(configsFolder, "${cleanName(name)}.ANPilot")

    private fun themeFile(name: String): File = File(themeFolder, "${cleanName(name)}.ANTheme")

    private fun readIndex(file: File, defaultName: String): String = file.takeIf { it.exists() }?.readText()?.trim()?.ifEmpty { defaultName } ?: defaultName

    private fun writeIndex(file: File, value: String) {
        createDirs()
        safeWrite(file, value)
    }

    private fun safeWrite(file: File, content: String) {
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        val backupFile = File(file.parentFile, "${file.name}.bak")
        
        
        if (file.exists() && file.length() > 0) {
            try {
                if (file.name.endsWith(".ANPilot") || file.name.endsWith(".ANTheme")) {
                    JsonParser.parseString(file.readText())
                }
                file.copyTo(backupFile, overwrite = true)
            } catch (e: Exception) {
                
            }
        }

        
        try {
            tempFile.writeText(content)
            val source = tempFile.toPath()
            val target = file.toPath()
            try {
                Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (e: AtomicMoveNotSupportedException) {
                Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to safely write file: ${file.name}", e)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun projectionFileNames(folder: File): List<String> {
        return folder.listFiles()
            ?.filter { it.isFile && PROJECTION_EXTENSIONS.any { extension -> it.name.endsWith(extension, ignoreCase = true) } }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    private fun cleanName(name: String): String = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "")

    private val PROJECTION_EXTENSIONS = listOf(".litematic", ".txt", ".csv")
}
