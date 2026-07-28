package anpilot.client.features.utility

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.core.Holder
import net.minecraft.world.item.enchantment.Enchantment

object EnchantUtils {
    fun getLevel(enchantment: Holder<Enchantment>, stack: ItemStack): Int {
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack)
    }

    fun getDensityLevel(stack: ItemStack): Int {
        
        return 0
    }
}
