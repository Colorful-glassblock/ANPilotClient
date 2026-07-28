package anpilot.client.features.module.misc.oreminer

import anpilot.client.features.setting.ANSetting
import net.minecraft.client.Minecraft
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.data.worldgen.placement.OrePlacements
import net.minecraft.resources.ResourceKey
import net.minecraft.util.valueproviders.ConstantInt
import net.minecraft.util.valueproviders.IntProvider
import net.minecraft.world.level.LevelHeightAccessor
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.FeatureSorter
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.dimension.LevelStem
import net.minecraft.world.level.levelgen.WorldGenerationContext
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider
import net.minecraft.world.level.levelgen.placement.CountPlacement
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.placement.RarityFilter
import net.minecraft.world.level.levelgen.presets.WorldPresets

enum class OreDimension {
    Overworld, Nether, End
}

class ANOreSetting(
    val name: String,
    val defaultState: Boolean,
    val color: Int 
) {
    val setting = ANSetting(name, defaultState)
}

object ANOres {
    val COAL = ANOreSetting("Coal", false, 0xFF2F2C36.toInt())
    val IRON = ANOreSetting("Iron", false, 0xFFECAD77.toInt())
    val GOLD = ANOreSetting("Gold", false, 0xFFF7E51E.toInt())
    val REDSTONE = ANOreSetting("Redstone", false, 0xFFF50717.toInt())
    val DIAMOND = ANOreSetting("Diamond", true, 0xFF21F4FF.toInt())
    val LAPIS = ANOreSetting("Lapis", false, 0xFF081ABD.toInt())
    val COPPER = ANOreSetting("Copper", false, 0xFFEF9700.toInt())
    val EMERALD = ANOreSetting("Emerald", false, 0xFF1BD12D.toInt())
    val QUARTZ = ANOreSetting("Quartz", false, 0xFFCDCDCD.toInt())
    val DEBRIS = ANOreSetting("Ancient Debris", true, 0xFFD11BF5.toInt())

    val values = listOf(COAL, IRON, GOLD, REDSTONE, DIAMOND, LAPIS, COPPER, EMERALD, QUARTZ, DEBRIS)
}

class ANOreConfig(
    val feature: PlacedFeature,
    val step: Int,
    val index: Int,
    val active: ANSetting<Boolean>,
    val color: Int,
    val generator: ChunkGenerator
) {
    var count: IntProvider = ConstantInt.of(1)
    var heightProvider: HeightProvider? = null
    var heightContext: WorldGenerationContext
    var rarity: Float = 1f
    var discardOnAirChance: Float = 0f
    var size: Int = 0
    var scattered: Boolean = false

    init {
        val bottom = Minecraft.getInstance().level?.minY ?: -64
        val height = Minecraft.getInstance().level?.dimensionType()?.logicalHeight() ?: 384
        this.heightContext = WorldGenerationContext(generator, LevelHeightAccessor.create(bottom, height))

        for (modifier in feature.placement()) {
            if (modifier is CountPlacement) {
                count = getFieldValue(modifier, IntProvider::class.java) as? IntProvider ?: count
            } else if (modifier is HeightRangePlacement) {
                heightProvider = getFieldValue(modifier, HeightProvider::class.java) as? HeightProvider
            } else if (modifier is RarityFilter) {
                val r = getFieldValue(modifier, Int::class.javaPrimitiveType!!) as? Int ?: 1
                rarity = r.toFloat()
            }
        }

        val featureConfig = feature.feature().value().config()
        if (featureConfig is OreConfiguration) {
            this.discardOnAirChance = featureConfig.discardChanceOnAirExposure
            this.size = featureConfig.size
        }

        if (feature.feature().value().feature() is ScatteredOreFeature) {
            this.scattered = true
        }
    }

    private fun getFieldValue(obj: Any, type: Class<*>): Any? {
        val field = obj.javaClass.declaredFields.firstOrNull { type.isAssignableFrom(it.type) || it.type == type } ?: return null
        field.isAccessible = true
        return field.get(obj)
    }

    companion object {
        fun getRegistry(dimension: OreDimension): Map<ResourceKey<Biome>, List<ANOreConfig>> {
            val registry: HolderLookup.Provider = VanillaRegistries.createLookup()
            val features = registry.lookupOrThrow(Registries.PLACED_FEATURE)
            val reg = registry.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value().createWorldDimensions().dimensions()

            val dim = when (dimension) {
                OreDimension.Overworld -> reg.get(LevelStem.OVERWORLD)
                OreDimension.Nether -> reg.get(LevelStem.NETHER)
                OreDimension.End -> reg.get(LevelStem.END)
            } ?: return emptyMap()

            val biomes = dim.generator().biomeSource.possibleBiomes().toList()

            val indexer = FeatureSorter.buildFeaturesPerStep(biomes, { it.value().generationSettings.features() }, true)

            val featureToOre = mutableMapOf<PlacedFeature, ANOreConfig>()

            fun register(key: ResourceKey<PlacedFeature>, step: Int, setting: ANOreSetting) {
                val placement = features.getOrThrow(key).value()
                val index = indexer[step].indexMapping().applyAsInt(placement)
                featureToOre[placement] = ANOreConfig(placement, step, index, setting.setting, setting.color, dim.generator())
            }

            register(OrePlacements.ORE_COAL_LOWER, 6, ANOres.COAL)
            register(OrePlacements.ORE_COAL_UPPER, 6, ANOres.COAL)
            register(OrePlacements.ORE_IRON_MIDDLE, 6, ANOres.IRON)
            register(OrePlacements.ORE_IRON_SMALL, 6, ANOres.IRON)
            register(OrePlacements.ORE_IRON_UPPER, 6, ANOres.IRON)
            register(OrePlacements.ORE_GOLD, 6, ANOres.GOLD)
            register(OrePlacements.ORE_GOLD_LOWER, 6, ANOres.GOLD)
            register(OrePlacements.ORE_GOLD_EXTRA, 6, ANOres.GOLD)
            register(OrePlacements.ORE_GOLD_NETHER, 7, ANOres.GOLD)
            register(OrePlacements.ORE_GOLD_DELTAS, 7, ANOres.GOLD)
            register(OrePlacements.ORE_REDSTONE, 6, ANOres.REDSTONE)
            register(OrePlacements.ORE_REDSTONE_LOWER, 6, ANOres.REDSTONE)
            register(OrePlacements.ORE_DIAMOND, 6, ANOres.DIAMOND)
            register(OrePlacements.ORE_DIAMOND_BURIED, 6, ANOres.DIAMOND)
            register(OrePlacements.ORE_DIAMOND_LARGE, 6, ANOres.DIAMOND)
            register(OrePlacements.ORE_DIAMOND_MEDIUM, 6, ANOres.DIAMOND)
            register(OrePlacements.ORE_LAPIS, 6, ANOres.LAPIS)
            register(OrePlacements.ORE_LAPIS_BURIED, 6, ANOres.LAPIS)
            register(OrePlacements.ORE_COPPER, 6, ANOres.COPPER)
            register(OrePlacements.ORE_COPPER_LARGE, 6, ANOres.COPPER)
            register(OrePlacements.ORE_EMERALD, 6, ANOres.EMERALD)
            register(OrePlacements.ORE_QUARTZ_NETHER, 7, ANOres.QUARTZ)
            register(OrePlacements.ORE_QUARTZ_DELTAS, 7, ANOres.QUARTZ)
            register(OrePlacements.ORE_ANCIENT_DEBRIS_SMALL, 7, ANOres.DEBRIS)
            register(OrePlacements.ORE_ANCIENT_DEBRIS_LARGE, 7, ANOres.DEBRIS)

            val biomeOreMap = mutableMapOf<ResourceKey<Biome>, List<ANOreConfig>>()

            for (biome in biomes) {
                val list = mutableListOf<ANOreConfig>()
                biome.value().generationSettings.features().forEach { holderSet ->
                    holderSet.forEach { holder ->
                        val feat = holder.value()
                        if (featureToOre.containsKey(feat)) {
                            list.add(featureToOre[feat]!!)
                        }
                    }
                }
                biomeOreMap[biome.unwrapKey().get()] = list
            }

            return biomeOreMap
        }
    }
}
