package anpilot.client.features.manager.inventory

import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items

object InventoryElytra {

    private val mc =
        Minecraft.getInstance()

    fun find(): Int {

        val player =
            mc.player ?: return -1

        
        val chest =
            player.getItemBySlot(EquipmentSlot.CHEST)

        if (
            chest.item ==
            Items.ELYTRA
        ) {
            return -2
        }

        for (slot in 0..35) {

            val stack =
                player.inventory.getItem(slot)

            
            val damage = stack.get(DataComponents.DAMAGE) ?: 0
            val maxDamage = stack.get(DataComponents.MAX_DAMAGE) ?: 0

            if (
                stack.item ==
                Items.ELYTRA &&
                (maxDamage - damage) > 1
            ) {
                return slot
            }
        }

        return -1
    }
}