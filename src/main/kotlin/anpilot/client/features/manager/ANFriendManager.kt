package anpilot.client.features.manager

import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import java.io.File

object ANFriendManager {
    val pilotFriends: MutableList<String> = mutableListOf()

    init {
        loadFriends()
    }

    fun isFriend(name: String): Boolean = pilotFriends.any { it.equals(name, ignoreCase = true) }

    fun addFriend(friend: String) {
        if (!isFriend(friend)) {
            pilotFriends += friend
            saveFriends()
        }
    }

    fun removeFriend(name: String) {
        if (pilotFriends.removeIf { it.equals(name, ignoreCase = true) }) {
            saveFriends()
        }
    }

    fun clear() {
        pilotFriends.clear()
        saveFriends()
    }

    fun getNearFriends(): List<AbstractClientPlayer> {
        return Minecraft.getInstance().level?.players()?.filter { isFriend(it.name.string) }.orEmpty()
    }

    fun saveFriends() {
        val file = File("ANPilotClient/Other/PilotFriends.txt")
        try {
            file.parentFile?.mkdirs()
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (ignored: Exception) {}

        try {
            file.bufferedWriter().use { writer ->
                for (friend in pilotFriends) {
                    writer.write(friend + "\n")
                }
            }
        } catch (ignored: Exception) {}
    }

    fun loadFriends() {
        val file = File("ANPilotClient/Other/PilotFriends.txt")
        try {
            if (file.exists()) {
                pilotFriends.clear()
                file.bufferedReader().use { reader ->
                    while (reader.ready()) {
                        val line = reader.readLine()?.trim()
                        if (!line.isNullOrEmpty() && !pilotFriends.contains(line)) {
                            pilotFriends.add(line)
                        }
                    }
                }
            }
        } catch (ignored: Exception) {}
    }
}
