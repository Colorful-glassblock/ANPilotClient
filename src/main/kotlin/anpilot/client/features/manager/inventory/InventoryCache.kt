package anpilot.client.features.manager.inventory

import net.minecraft.client.Minecraft
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.Item
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments

object InventoryCache {

    private val mc = Minecraft.getInstance()

    private var sharpness: Holder.Reference<Enchantment>? = null
    private var efficiency: Holder.Reference<Enchantment>? = null

    val itemCache =
        HashMap<String, Item>()

    fun init() {

        val level = mc.level ?: return

        val registry =
            level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)

        sharpness =
            registry.get(
                Enchantments.SHARPNESS
            ).orElse(null)

        efficiency =
            registry.get(
                Enchantments.EFFICIENCY
            ).orElse(null)
    }

    fun sharpness() = sharpness
    fun efficiency() = efficiency
}
