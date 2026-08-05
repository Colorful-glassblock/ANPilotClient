package anpilot.client.features.module.misc.oreminer

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.api.module.ANModuleState
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.ANTickEvent
import anpilot.client.features.event.impl.PacketEvent
import anpilot.client.features.event.impl.Render3DEvent
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.renderer.ANColor
import anpilot.client.renderer.render.ANRender3DEngine
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.util.Mth
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Blocks
import baritone.api.BaritoneAPI
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.WorldgenRandom
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import net.minecraft.core.Direction

class ANOreMiner : ANBaseModule(
    name = "ANOreMiner",
    description = "使用种子来获取矿物位置，反服务反矿透并进行全自动挖掘",
    category = ANModuleCategory.MISC,
    chineseName = "种子矿透",
) {
    private val chunkRenderers = ConcurrentHashMap<Long, Map<ANOreConfig, MutableSet<Vec3>>>()
    private var worldSeed: Long? = null
    private var oreConfig: Map<ResourceKey<Biome>, List<ANOreConfig>>? = null
    private var lastDimension: ResourceKey<Level>? = null
    private var wasBaritoneActive = false
    private var scanCursor = 0
    val oreGoals = mutableListOf<BlockPos>()

    val seedInput = addSetting(ANSetting("Seed", "-7346913998703726680"))
    val horizontalRadius = addSetting(ANSetting("ChunkRadius", 5, 1, 10))
    val baritone = addSetting(ANSetting("BaritoneMiner", false))
    
    init {
        ANOres.values.forEach { addSetting(it.setting) }
    }

    override fun onEnable() {
        val s = seedInput.value
        try {
            worldSeed = s.toLong()
        } catch (e: Exception) {
            worldSeed = s.hashCode().toLong()
        }
        lastDimension = mc.level?.dimension()
        reload()
    }

    override fun onDisable() {
        chunkRenderers.clear()
        oreConfig = null
        if (wasBaritoneActive) {
            BaritoneAPI.getProvider().primaryBaritone.mineProcess.cancel()
            wasBaritoneActive = false
        }
    }

    @ANEventHandler
    fun onTick(event: ANTickEvent) {
        val level = mc.level ?: return
        if (mc.player == null) return

        if (level.dimension() != lastDimension) {
            lastDimension = level.dimension()
            reload()
        }

        scanVisibleChunksIncremental()

        if (baritone.value && !wasBaritoneActive) {
            BaritoneAPI.getSettings().allowBreak.value = true
            BaritoneAPI.getProvider().primaryBaritone.mineProcess.mine(Blocks.DIAMOND_ORE, Blocks.ANCIENT_DEBRIS, Blocks.GOLD_ORE)
        } else if (!baritone.value && wasBaritoneActive) {
            BaritoneAPI.getProvider().primaryBaritone.mineProcess.cancel()
        }
        wasBaritoneActive = baritone.value

        if (baritone.value && BaritoneAPI.getProvider().primaryBaritone.mineProcess.isActive) {
            oreGoals.clear()
            val chunkPos = mc.player!!.chunkPosition()
            val rangeVal = 4
            for (range in 0..rangeVal) {
                for (x in -range + chunkPos.x..range + chunkPos.x) {
                    oreGoals.addAll(addToBaritone(x, chunkPos.z + range - rangeVal))
                }
                for (x in -range + 1 + chunkPos.x..<range + chunkPos.x) {
                    oreGoals.addAll(addToBaritone(x, chunkPos.z - range + rangeVal + 1))
                }
            }
        }
    }

    private fun scanVisibleChunksIncremental() {
        val player = mc.player ?: return
        val radius = horizontalRadius.value
        val diameter = radius * 2 + 1
        val total = diameter * diameter
        val center = player.chunkPosition()

        repeat(CHUNKS_PER_TICK) {
            val index = scanCursor++ % total
            val offsetX = index % diameter - radius
            val offsetZ = index / diameter - radius
            doMathOnChunk(ChunkPos(center.x + offsetX, center.z + offsetZ))
        }
    }

    private fun addToBaritone(chunkX: Int, chunkZ: Int): List<BlockPos> {
        val baritoneGoals = mutableListOf<BlockPos>()
        val chunkKey = (chunkX.toLong() and 0xFFFFFFFFL) or (chunkZ.toLong() and 0xFFFFFFFFL shl 32)
        val chunkMap = chunkRenderers[chunkKey] ?: return baritoneGoals

        for ((ore, positions) in chunkMap) {
            if (ore.active.value) {
                for (pos in positions) {
                    baritoneGoals.add(BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt()))
                }
            }
        }
        return baritoneGoals
    }

    private fun reload() {
        val seed = worldSeed ?: return
        val currentDim = when (lastDimension) {
            Level.NETHER -> OreDimension.Nether
            Level.END -> OreDimension.End
            else -> OreDimension.Overworld
        }
        oreConfig = ANOreConfig.getRegistry(currentDim)
        chunkRenderers.clear()
        
        if (mc.level != null) {
            loadVisibleChunks()
        }
    }

    private fun loadVisibleChunks() {
        val player = mc.player ?: return
        val chunkX = player.chunkPosition().x
        val chunkZ = player.chunkPosition().z
        val radius = horizontalRadius.value
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                doMathOnChunk(ChunkPos(chunkX + x, chunkZ + z))
            }
        }
    }

    @ANEventHandler
    fun onPacketReceive(event: PacketEvent.Receive) {
        val packet = event.packet
        if (packet is ClientboundBlockUpdatePacket) {
            if (packet.blockState.isSolidRender()) return
            val chunkKey = (packet.pos.x shr 4).toLong() and 0xFFFFFFFFL or ((packet.pos.z shr 4).toLong() and 0xFFFFFFFFL shl 32)
            val chunkMap = chunkRenderers[chunkKey]
            if (chunkMap != null) {
                val vec = Vec3.atLowerCornerOf(packet.pos)
                for (positions in chunkMap.values) {
                    positions.remove(vec)
                }
            }
        }
        if (packet is ClientboundLevelChunkWithLightPacket) {
            doMathOnChunk(ChunkPos(packet.x, packet.z))
        }
    }

    @ANEventHandler
    fun onRender(event: Render3DEvent) {
        if (mc.player == null || oreConfig == null) return
        val chunkX = mc.player!!.chunkPosition().x
        val chunkZ = mc.player!!.chunkPosition().z
        val rangeVal = horizontalRadius.value
        
        for (range in 0..rangeVal) {
            for (x in -range + chunkX..range + chunkX) {
                renderChunk(x, chunkZ + range - rangeVal, event)
            }
            for (x in -range + 1 + chunkX..<range + chunkX) {
                renderChunk(x, chunkZ - range + rangeVal + 1, event)
            }
        }
    }

    private fun renderChunk(x: Int, z: Int, event: Render3DEvent) {
        val chunkKey = (x.toLong() and 0xFFFFFFFFL) or (z.toLong() and 0xFFFFFFFFL shl 32)
        val chunk = chunkRenderers[chunkKey] ?: return
        
        for ((ore, positions) in chunk) {
            if (ore.active.value) {
                val c = ANColor(
                    (ore.color shr 16 and 0xFF),
                    (ore.color shr 8 and 0xFF),
                    (ore.color and 0xFF),
                    (ore.color shr 24 and 0xFF)
                )
                for (pos in positions) {
                    val box = AABB(pos.x, pos.y, pos.z, pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)
                    ANRender3DEngine.box(event.context, box, c, c.withAlpha(40))
                }
            }
        }
    }

    private fun doMathOnChunk(chunkPos: ChunkPos) {
        val world = mc.level ?: return
        val config = oreConfig ?: return
        val seed = worldSeed ?: return
        
        val chunkKey = (chunkPos.x.toLong() and 0xFFFFFFFFL) or (chunkPos.z.toLong() and 0xFFFFFFFFL shl 32)
        if (chunkRenderers.containsKey(chunkKey)) return
        
        val chunk = world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false)
            ?: world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.BIOMES, false)
            ?: return
        
        val biomes = mutableSetOf<ResourceKey<Biome>>()
        for (section in chunk.sections) {
            section.biomes.getAll { entry -> entry.unwrapKey().orElse(null)?.let { biomes.add(it) } }
        }
        
        val oreSet = mutableSetOf<ANOreConfig>()
        for (biome in biomes) {
            oreSet.addAll(getDefaultOres(biome, config))
        }
        if (oreSet.isEmpty()) {
            chunkRenderers[chunkKey] = emptyMap()
            return
        }
        
        val chunkX = chunkPos.x shl 4
        val chunkZ = chunkPos.z shl 4
        val random = WorldgenRandom(WorldgenRandom.Algorithm.XOROSHIRO.newInstance(0))
        val populationSeed = random.setDecorationSeed(seed, chunkX, chunkZ)
        
        val h = mutableMapOf<ANOreConfig, MutableSet<Vec3>>()
        
        for (ore in oreSet) {
            val ores = mutableSetOf<Vec3>()
            random.setFeatureSeed(populationSeed, ore.index, ore.step)
            val repeat = ore.count.sample(random)
            for (i in 0 until repeat) {
                if (ore.rarity != 1f && random.nextFloat() >= 1f / ore.rarity) continue
                val px = random.nextInt(16) + chunkX
                val pz = random.nextInt(16) + chunkZ
                val py = sampleOreY(world, ore, random)
                val origin = BlockPos(px, py, pz)
                
                val biome = chunk.getNoiseBiome(px shr 2, py shr 2, pz shr 2).unwrapKey().orElse(null)
                if (biome != null && !getDefaultOres(biome, config).contains(ore)) continue
                
                if (ore.scattered) {
                    ores.addAll(generateHidden(world, random, origin, ore.size))
                } else {
                    ores.addAll(generateNormal(world, random, origin, ore.size, ore.discardOnAirChance))
                }
            }
            if (ores.isNotEmpty()) h[ore] = ores
        }
        chunkRenderers[chunkKey] = h
    }

    private fun getDefaultOres(biome: ResourceKey<Biome>, config: Map<ResourceKey<Biome>, List<ANOreConfig>>): List<ANOreConfig> {
        return config[biome] ?: config.values.firstOrNull() ?: emptyList()
    }

    private fun generateNormal(world: ClientLevel, random: WorldgenRandom, blockPos: BlockPos, veinSize: Int, discardOnAir: Float): List<Vec3> {
        val poses = mutableListOf<Vec3>()
        val f = random.nextFloat() * 3.1415927f
        val g = veinSize.toDouble() / 8.0
        val i = Mth.ceil((veinSize.toFloat() / 16.0f * 2.0f + 1.0f) / 2.0f)
        val d = blockPos.x.toDouble() + Math.sin(f.toDouble()) * g
        val e = blockPos.x.toDouble() - Math.sin(f.toDouble()) * g
        val h = blockPos.z.toDouble() + Math.cos(f.toDouble()) * g
        val j = blockPos.z.toDouble() - Math.cos(f.toDouble()) * g
        val l = (blockPos.y + random.nextInt(3) - 2).toDouble()
        val m = (blockPos.y + random.nextInt(3) - 2).toDouble()
        val n = blockPos.x - Mth.ceil(g) - i
        val o = blockPos.y - 2 - i
        val p = blockPos.z - Mth.ceil(g) - i
        val q = 2 * (Mth.ceil(g) + i)
        val r = 2 * (2 + i)

        for (s in n..n + q) {
            for (t in p..p + q) {
                if (o <= world.getHeight(Heightmap.Types.MOTION_BLOCKING, s, t)) {
                    return generateVeinPart(world, random, veinSize, d, e, h, j, l, m, n, o, p, q, r, discardOnAir)
                }
            }
        }
        return poses
    }

    private fun generateVeinPart(world: ClientLevel, random: WorldgenRandom, veinSize: Int, startX: Double, endX: Double, startZ: Double, endZ: Double, startY: Double, endY: Double, x: Int, y: Int, z: Int, size: Int, i: Int, discardOnAir: Float): List<Vec3> {
        val bitSet = BitSet(size * i * size)
        val mutable = BlockPos.MutableBlockPos()
        val ds = DoubleArray(veinSize * 4)
        val poses = mutableListOf<Vec3>()

        for (n in 0 until veinSize) {
            val f = n.toFloat() / veinSize.toFloat()
            val p = Mth.lerp(f.toDouble(), startX, endX)
            val q = Mth.lerp(f.toDouble(), startY, endY)
            val r = Mth.lerp(f.toDouble(), startZ, endZ)
            val s = random.nextDouble() * veinSize.toDouble() / 16.0
            val m = (Math.sin((3.1415927f * f).toDouble()) + 1.0) * s / 2.0 + 0.5
            ds[n * 4] = p
            ds[n * 4 + 1] = q
            ds[n * 4 + 2] = r
            ds[n * 4 + 3] = m
        }

        for (n in 0 until veinSize - 1) {
            if (ds[n * 4 + 3] > 0.0) {
                for (o in n + 1 until veinSize) {
                    if (ds[o * 4 + 3] > 0.0) {
                        val p = ds[n * 4] - ds[o * 4]
                        val q = ds[n * 4 + 1] - ds[o * 4 + 1]
                        val r = ds[n * 4 + 2] - ds[o * 4 + 2]
                        val s = ds[n * 4 + 3] - ds[o * 4 + 3]
                        if (s * s > p * p + q * q + r * r) {
                            if (s > 0.0) {
                                ds[o * 4 + 3] = -1.0
                            } else {
                                ds[n * 4 + 3] = -1.0
                            }
                        }
                    }
                }
            }
        }

        for (n in 0 until veinSize) {
            val u = ds[n * 4 + 3]
            if (u >= 0.0) {
                val v = ds[n * 4]
                val w = ds[n * 4 + 1]
                val aa = ds[n * 4 + 2]
                val ab = max(Mth.floor(v - u), x)
                val ac = max(Mth.floor(w - u), y)
                val ad = max(Mth.floor(aa - u), z)
                val ae = max(Mth.floor(v + u), ab)
                val af = max(Mth.floor(w + u), ac)
                val ag = max(Mth.floor(aa + u), ad)

                for (ah in ab..ae) {
                    val ai = (ah.toDouble() + 0.5 - v) / u
                    if (ai * ai < 1.0) {
                        for (aj in ac..af) {
                            val ak = (aj.toDouble() + 0.5 - w) / u
                            if (ai * ai + ak * ak < 1.0) {
                                for (al in ad..ag) {
                                    val am = (al.toDouble() + 0.5 - aa) / u
                                    if (ai * ai + ak * ak + am * am < 1.0) {
                                        val an = ah - x + (aj - y) * size + (al - z) * size * i
                                        if (!bitSet.get(an)) {
                                            bitSet.set(an)
                                            mutable.set(ah, aj, al)
                                            if (isValidY(world, aj) && world.getBlockState(mutable).isSolidRender()) {
                                                if (shouldPlace(world, mutable, discardOnAir, random)) {
                                                    poses.add(Vec3(ah.toDouble(), aj.toDouble(), al.toDouble()))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return poses
    }

    private fun generateHidden(world: ClientLevel, random: WorldgenRandom, blockPos: BlockPos, size: Int): List<Vec3> {
        val poses = mutableListOf<Vec3>()
        val i = random.nextInt(size + 1)
        for (j in 0 until i) {
            val sz = Math.min(j, 7)
            val x = randomCoord(random, sz) + blockPos.x
            val y = randomCoord(random, sz) + blockPos.y
            val z = randomCoord(random, sz) + blockPos.z
            val pos = BlockPos(x, y, z)
            if (isValidY(world, y) && world.getBlockState(pos).isSolidRender()) {
                poses.add(Vec3(x.toDouble(), y.toDouble(), z.toDouble()))
            }
        }
        return poses
    }

    private fun randomCoord(random: WorldgenRandom, size: Int): Int {
        return Math.round((random.nextFloat() - random.nextFloat()) * size.toFloat())
    }

    private fun sampleOreY(world: ClientLevel, ore: ANOreConfig, random: WorldgenRandom): Int {
        val context = ore.heightContext
        if (context != null) {
            ore.heightProvider?.let { provider ->
                runCatching { provider.sample(random, context) }.getOrNull()?.let { return it }
            }
        }

        val minY = world.minY
        val maxY = world.minY + world.dimensionType().logicalHeight() - 1
        val range = when (ore.active.name) {
            "Ancient Debris" -> 8 to 22
            "Quartz" -> 10 to 117
            "Gold" -> if (world.dimension() == Level.NETHER) 10 to 117 else -64 to 32
            "Diamond", "Redstone", "Lapis" -> -64 to 16
            "Iron", "Copper" -> -24 to 80
            "Coal" -> 0 to 160
            "Emerald" -> -16 to 256
            else -> minY to maxY
        }
        val from = range.first.coerceIn(minY, maxY)
        val to = range.second.coerceIn(minY, maxY).coerceAtLeast(from)
        return from + random.nextInt(to - from + 1)
    }

    private fun isValidY(world: ClientLevel, y: Int): Boolean {
        return y >= world.minY && y < world.minY + world.dimensionType().logicalHeight()
    }

    private fun shouldPlace(world: ClientLevel, orePos: BlockPos, discardOnAir: Float, random: WorldgenRandom): Boolean {
        if (discardOnAir == 0f || (discardOnAir != 1f && random.nextFloat() >= discardOnAir)) {
            return true
        }
        for (direction in Direction.values()) {
            if (!world.getBlockState(orePos.relative(direction)).isSolidRender() && discardOnAir != 1f) {
                return false
            }
        }
        return true
    }

    private companion object {
        const val CHUNKS_PER_TICK = 4
    }
}
