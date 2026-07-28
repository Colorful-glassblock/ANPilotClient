package anpilot.client.features.manager.inventory

import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.*
import net.minecraft.world.item.enchantment.EnchantmentHelper

object InventoryCombat {

    private val mc =
        Minecraft.getInstance()

    fun swordScore(
        stack: ItemStack
    ): Float {

        if (!stack.`is`(ItemTags.SWORDS)) return 0f

        val sharpness =
            InventoryCache.sharpness()

        
        val baseDamage = stack.get(DataComponents.ATTRIBUTE_MODIFIERS)?.modifiers()?.sumOf {
            if (it.attribute().toString().contains("generic.attack_damage")) it.modifier().amount() else 0.0
        }?.toFloat() ?: 0f

        return baseDamage +
                if (sharpness != null)
                    EnchantmentHelper
                        .getItemEnchantmentLevel(
                            sharpness,
                            stack
                        ).toFloat()
                else 0f
    }

    fun axeScore(
        stack: ItemStack
    ): Float {

        if (stack.item !is AxeItem) return 0f

        val sharpness =
            InventoryCache.sharpness()

        
        val baseDamage = stack.get(DataComponents.ATTRIBUTE_MODIFIERS)?.modifiers()?.sumOf {
            if (it.attribute().toString().contains("generic.attack_damage")) it.modifier().amount() else 0.0
        }?.toFloat() ?: 0f

        return baseDamage +
                if (sharpness != null)
                    EnchantmentHelper
                        .getItemEnchantmentLevel(
                            sharpness,
                            stack
                        ).toFloat()
                else 0f
    }

    fun pickaxeScore(
        stack: ItemStack
    ): Float {

        val efficiency =
            InventoryCache.efficiency()

        return if (efficiency != null)
            EnchantmentHelper
                .getItemEnchantmentLevel(
                    efficiency,
                    stack
                ).toFloat()
        else 0f
    }
}
