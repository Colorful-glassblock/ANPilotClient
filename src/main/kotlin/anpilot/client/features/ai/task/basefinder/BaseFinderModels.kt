package anpilot.client.features.ai.task.basefinder

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos

data class BaseFinderSession(
    val centerX: Int = 0,
    val centerY: Int = 0,
    val centerZ: Int = 0,
    val targetIndex: Int = 0,
    val scannedChunks: MutableSet<String> = mutableSetOf(),
    val detectionKeys: MutableSet<String> = mutableSetOf()
) {
    fun center(): BlockPos = BlockPos(centerX, centerY, centerZ)
}

data class BaseFinderDetection(
    val Coordinate: String,
    val Detected: String,
    val Time: String
)

data class BaseFinderScanResult(
    val chunkPos: ChunkPos,
    val containers: MutableMap<String, Int> = mutableMapOf(),
    val blocks: MutableMap<String, Int> = mutableMapOf()
) {
    fun totalContainers(): Int = containers.values.sum()
    fun totalBlocks(): Int = blocks.values.sum()

    fun categories(): Map<String, Map<String, Int>> = mapOf(
        "containers" to containers.filterValues { it > 0 }.toSortedMap(),
        "blocks" to blocks.filterValues { it > 0 }.toSortedMap()
    )
}

fun MutableMap<String, Int>.increment(key: String, amount: Int = 1) {
    if (amount <= 0) return
    this[key] = (this[key] ?: 0) + amount
}
