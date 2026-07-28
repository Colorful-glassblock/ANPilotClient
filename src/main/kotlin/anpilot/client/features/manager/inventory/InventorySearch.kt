package anpilot.client.features.manager.inventory

import net.minecraft.tags.ItemTags
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block

object InventorySearch {
    fun find(
        range: IntRange,
        predicate: (ItemStack) -> Boolean
    ): SearchInvResult =
        Inventory.find(range, predicate)

    fun findBest(
        range: IntRange,
        predicate: (ItemStack) -> Boolean,
        score: (ItemStack) -> Float
    ): SearchInvResult =
        Inventory.findBest(range, predicate, score)

    fun findInHotbar(predicate: (ItemStack) -> Boolean): SearchInvResult =
        Inventory.findInHotbar(predicate)

    fun findInInventory(predicate: (ItemStack) -> Boolean): SearchInvResult =
        Inventory.findInInventory(predicate)

    fun findItemInHotbar(items: Collection<Item>): SearchInvResult =
        Inventory.findItemInHotbar(items)

    fun findItemInHotbar(vararg items: Item): SearchInvResult =
        Inventory.findItemInHotbar(*items)

    fun findItemInInventory(items: Collection<Item>): SearchInvResult =
        Inventory.findItemInInventory(items)

    fun findItemInInventory(vararg items: Item): SearchInvResult =
        Inventory.findItemInInventory(*items)

    fun findBlockInHotbar(blocks: Collection<Block>): SearchInvResult =
        Inventory.findBlockInHotbar(blocks)

    fun findBlockInHotbar(vararg blocks: Block): SearchInvResult =
        Inventory.findBlockInHotbar(*blocks)

    fun findBlockInInventory(blocks: Collection<Block>): SearchInvResult =
        Inventory.findBlockInInventory(blocks)

    fun findBlockInInventory(vararg blocks: Block): SearchInvResult =
        Inventory.findBlockInInventory(*blocks)

    fun sword(): SearchInvResult =
        findBest(
            0 until Inventory.MAIN_SIZE,
            { it.`is`(ItemTags.SWORDS) },
            InventoryCombat::swordScore
        )

    fun axe(): SearchInvResult =
        findBest(
            0 until Inventory.MAIN_SIZE,
            { it.item is AxeItem },
            InventoryCombat::axeScore
        )

    fun pickaxe(): SearchInvResult =
        findBest(
            0 until Inventory.MAIN_SIZE,
            { it.`is`(ItemTags.PICKAXES) },
            InventoryCombat::pickaxeScore
        )
}
