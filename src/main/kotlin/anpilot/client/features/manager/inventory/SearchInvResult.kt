package anpilot.client.features.manager.inventory

import net.minecraft.world.item.ItemStack

data class SearchInvResult(
    val slot: Int,
    val found: Boolean,
    val stack: ItemStack = ItemStack.EMPTY
) {
    companion object {
        fun notFound() =
            SearchInvResult(-1, false)
    }
}