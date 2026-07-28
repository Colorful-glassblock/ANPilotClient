package anpilot.client.features.ai.task.basefinder

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import java.io.File

object BaseFinderXaeroWaypoints {
    private const val HEADER = "#\n#waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination\n#\n"

    fun append(name: String, pos: BlockPos, dimension: String): Boolean {
        val minecraft = Minecraft.getInstance()
        val root = File(minecraft.gameDirectory, "xaero/minimap")
        if (!root.isDirectory) return false

        val dimFolderName = dimensionFolder(dimension) ?: return false
        val worldFolder = resolveWorldFolder(root, dimFolderName) ?: return false
        val dimFolder = File(worldFolder, dimFolderName)
        if (!dimFolder.isDirectory) return false

        val file = File(dimFolder, "waypoints.txt")
        if (!file.exists()) file.writeText(HEADER)
        val safeName = cleanWaypointPart(name).ifBlank { "ANBaseFinder" }
        val line = "waypoint:$safeName:BF:${pos.x}:${pos.y}:${pos.z}:5:false:0:gui.xaero_default:false:0:1:true"
        val existing = runCatching { file.readLines() }.getOrDefault(emptyList())
        if (line in existing) return true
        file.appendText(System.lineSeparator() + line)
        return true
    }

    private fun resolveWorldFolder(root: File, dimFolderName: String): File? {
        val candidates = currentFolderCandidates()
        for (candidate in candidates) {
            val folder = File(root, candidate)
            if (File(folder, dimFolderName).isDirectory) return folder
        }

        return root.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && it.name != "backup" }
            ?.filter { File(it, dimFolderName).isDirectory }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun currentFolderCandidates(): List<String> {
        val minecraft = Minecraft.getInstance()
        val result = linkedSetOf<String>()

        runCatching { minecraft.singleplayerServer?.worldData?.levelName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { result += cleanFolderPart(it) }

        val serverData = runCatching { minecraft.currentServer }.getOrNull()
        listOfNotNull(
            runCatching { serverData?.ip }.getOrNull(),
            runCatching { serverData?.name }.getOrNull()
        ).filter { it.isNotBlank() }.forEach { value ->
            val clean = cleanFolderPart(value)
            result += "Multiplayer_$clean"
            result += "Multiplayer_${clean.substringBefore(':')}"
            result += "Multiplayer_${clean.replace(':', '_')}"
        }

        return result.filter { it.isNotBlank() }
    }

    private fun dimensionFolder(dimension: String): String? {
        return when (dimension) {
            "minecraft:overworld" -> "dim%0"
            "minecraft:the_nether" -> "dim%-1"
            "minecraft:the_end" -> "dim%1"
            else -> null
        }
    }

    private fun cleanWaypointPart(value: String): String {
        return value.trim().replace(Regex("[:\\r\\n]"), " ")
    }

    private fun cleanFolderPart(value: String): String {
        return value.trim().replace(Regex("[\\\\/*?\"<>|]"), "_")
    }
}
