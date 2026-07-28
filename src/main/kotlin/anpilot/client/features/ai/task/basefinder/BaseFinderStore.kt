package anpilot.client.features.ai.task.basefinder

import anpilot.client.features.manager.ANConfigManager
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.io.File

object BaseFinderStore {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val sessionFile: File get() = ANConfigManager.baseFinderSessionFile()
    private val detectionsFile: File get() = ANConfigManager.baseFinderDetectionsFile()

    fun loadSession(): BaseFinderSession? {
        val file = sessionFile
        if (!file.exists()) return null
        return runCatching { gson.fromJson(file.readText(), BaseFinderSession::class.java) }.getOrNull()
    }

    fun saveSession(session: BaseFinderSession) {
        val file = sessionFile
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(session))
    }

    fun clearSession() {
        sessionFile.takeIf { it.exists() }?.delete()
    }

    fun appendDetection(detection: BaseFinderDetection) {
        val file = detectionsFile
        file.parentFile?.mkdirs()
        val detections = readDetections(file)
        detections.add(gson.toJsonTree(detection))
        file.writeText(gson.toJson(detections))
    }

    private fun readDetections(file: File): JsonArray {
        if (!file.exists()) return JsonArray()
        return runCatching {
            val element = JsonParser.parseString(file.readText())
            if (element.isJsonArray) element.asJsonArray else JsonArray()
        }.getOrDefault(JsonArray())
    }
}
