package anpilot.client.features.event.impl 

import anpilot.client.features.event.Cancellable
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack


open class InteractItemEvent(val itemStack: ItemStack) : Cancellable() {


    val item: Item get() = itemStack.item

    class Pre(item: ItemStack) : InteractItemEvent(item)
    class Post(item: ItemStack) : InteractItemEvent(item)
}