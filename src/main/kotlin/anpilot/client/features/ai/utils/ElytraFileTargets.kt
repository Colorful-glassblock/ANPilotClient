package anpilot.client.features.ai.utils

import anpilot.client.features.manager.ANConfigManager
import net.minecraft.core.BlockPos
import org.slf4j.LoggerFactory
import java.io.File

object ElytraFileTargets {
    private val logger = LoggerFactory.getLogger("ANPilotElytraFiles")
    private var fileName = ""
    private var targets = emptyList<BlockPos>()
    private var index = 0

    fun init(selectedFile: String) {
        val names = ANConfigManager.elytraFileNames()
        fileName = selectedFile.takeIf { it in names } ?: names.firstOrNull().orEmpty()
        targets = loadTargets(ANConfigManager.elytraFile(fileName))
        index = 0
        AgentUtils.sendMessage("Elytra file: ${fileName.ifEmpty { "No .txt files" }} targets=${targets.size}")
    }

    fun target(): BlockPos? {
        skipFound()
        return targets.getOrNull(index)
    }

    fun advance(): BlockPos? {
        if (index < targets.size) index++
        skipFound()
        return targets.getOrNull(index)
    }

    private fun skipFound() {
        while (index < targets.size && FoundLocationStore.contains(targets[index])) {
            AgentUtils.sendMessage("Skip found elytra target: ${targets[index]}")
            index++
        }
    }

    private fun loadTargets(file: File): List<BlockPos> {
        if (!file.exists()) return emptyList()
        return runCatching {
            file.readLines().mapNotNull(::parsePosition).distinctBy { "${it.x}:${it.z}" }
        }.getOrElse { exception ->
            logger.warn("Failed to read ${file.name}", exception)
            emptyList()
        }
    }

    private fun parsePosition(line: String): BlockPos? {
        val clean = line.substringBefore('#').trim()
        if (clean.isEmpty()) return null
        val parts = clean.split(Regex("[,;\\s]+")).filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val x = parts[0].toIntOrNull() ?: return null
        val y = if (parts.size >= 3) parts[1].toIntOrNull() ?: return null else 180
        val z = if (parts.size >= 3) parts[2].toIntOrNull() ?: return null else parts[1].toIntOrNull() ?: return null
        return BlockPos(x, y, z)
    }
}
