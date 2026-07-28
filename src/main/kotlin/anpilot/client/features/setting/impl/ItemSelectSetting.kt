package anpilot.client.features.setting.impl

import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block

class ItemSelectSetting(
    itemsById: List<String>
) {
    private val itemsById: MutableList<String> = itemsById.toMutableList()

    fun getItemsById(): MutableList<String> = itemsById

    fun add(id: String) {
        itemsById.add(id)
    }

    fun remove(id: String) {
        itemsById.remove(id)
    }

    fun contains(id: String): Boolean = itemsById.contains(id)

    fun add(block: Block) {
        add(normalizeDescriptionId(block.descriptionId, "block.minecraft."))
    }

    fun add(item: Item) {
        add(normalizeDescriptionId(item.descriptionId, "item.minecraft."))
    }

    fun remove(block: Block) {
        remove(normalizeDescriptionId(block.descriptionId, "block.minecraft."))
    }

    fun remove(item: Item) {
        remove(normalizeDescriptionId(item.descriptionId, "item.minecraft."))
    }

    fun contains(block: Block): Boolean = contains(normalizeDescriptionId(block.descriptionId, "block.minecraft."))

    fun contains(item: Item): Boolean = contains(normalizeDescriptionId(item.descriptionId, "item.minecraft."))

    fun clear() {
        itemsById.clear()
    }

    private fun normalizeDescriptionId(descriptionId: String, prefix: String): String = descriptionId.removePrefix(prefix)
}
