package anpilot.client.features.manager

import anpilot.client.features.manager.inventory.Inventory
import anpilot.client.features.manager.inventory.InventoryCache
import anpilot.client.features.manager.inventory.InventoryCombat
import anpilot.client.features.manager.inventory.InventoryElytra
import anpilot.client.features.manager.inventory.InventorySearch
import anpilot.client.features.manager.inventory.InventorySlot

object InventoryManager {

    val core
        get() = Inventory

    val search
        get() = InventorySearch

    val slot
        get() = InventorySlot

    val combat
        get() = InventoryCombat

    val elytra
        get() = InventoryElytra

    val cache
        get() = InventoryCache
}
