package anpilot.client.features.ai.utils

import anpilot.client.features.manager.ANConfigManager
import net.minecraft.core.BlockPos
import org.slf4j.LoggerFactory

object FoundLocationStore {
    private val logger = LoggerFactory.getLogger("ANPilotFoundLocations")
    private val file get() = ANConfigManager.foundElytraFile()

    fun contains(pos: BlockPos?): Boolean {
        if (pos == null || !file.exists()) return false
        return runCatching { file.readLines().any { parsePosition(it)?.let { saved -> sameLocation(saved, pos) } == true } }
            .getOrElse { exception ->
                logger.warn(exception.message)
                false
            }
    }

    fun save(pos: BlockPos?) {
        if (pos == null || contains(pos)) return
        runCatching {
            file.parentFile?.mkdirs()
            file.appendText(locationKey(pos) + System.lineSeparator())
        }.onFailure { logger.warn(it.message) }
    }

    fun all(): Set<BlockPos> {
        if (!file.exists()) return emptySet()
        return runCatching { file.readLines().mapNotNull(::parsePosition).toSet() }.getOrElse { emptySet() }
    }

    private fun sameLocation(a: BlockPos, b: BlockPos): Boolean = a.x == b.x && a.z == b.z

    private fun locationKey(pos: BlockPos): String = "${pos.x} ${pos.y} ${pos.z}"

    private fun parsePosition(line: String): BlockPos? {
        val clean = line.substringBefore('#').trim()
        if (clean.isEmpty()) return null

        val legacyLong = clean.toLongOrNull()
        if (legacyLong != null) return BlockPos.of(legacyLong)

        val parts = clean.split(Regex("[,;\\s]+")).filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val x = parts[0].toIntOrNull() ?: return null
        val y = if (parts.size >= 3) parts[1].toIntOrNull() ?: return null else 180
        val z = if (parts.size >= 3) parts[2].toIntOrNull() ?: return null else parts[1].toIntOrNull() ?: return null
        return BlockPos(x, y, z)
    }
}
