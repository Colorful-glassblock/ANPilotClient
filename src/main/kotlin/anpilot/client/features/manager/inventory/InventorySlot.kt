package anpilot.client.features.manager.inventory

object InventorySlot {
    fun save() {
        Inventory.save()
    }

    fun restore() {
        Inventory.restore()
    }

    fun switchTo(slot: Int): Boolean =
        Inventory.switchTo(slot)

    fun switchSilent(slot: Int): Boolean =
        Inventory.switchSilent(slot)

    fun swap(slot: Int, swapBack: Boolean = true): Boolean =
        Inventory.swap(slot, swapBack)

    fun swapBack(): Boolean =
        Inventory.swapBack()

    fun startSwap(slot: Int, type: SilentSwapType = SilentSwapType.HOTBAR): Boolean =
        Inventory.startSwap(slot, type)

    fun endSwap(type: SilentSwapType? = null): Boolean =
        Inventory.endSwap(type)
}
