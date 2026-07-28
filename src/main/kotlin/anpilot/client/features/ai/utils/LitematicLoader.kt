package anpilot.client.features.ai.utils

import anpilot.client.features.ai.utils.litematic.LitematicSectionMeshCache
import anpilot.client.renderer.ANColor
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.block.BlockModelRenderState
import net.minecraft.client.renderer.block.BlockModelResolver
import net.minecraft.client.renderer.block.model.BlockDisplayContext
import net.minecraft.client.renderer.rendertype.ANPilotRenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.commands.arguments.blocks.BlockStateParser
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.Rotation as BlockRotation
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import org.joml.Vector3f
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.abs
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.properties.BlockStateProperties

object LitematicLoader {
    private val logger = LoggerFactory.getLogger("ANPilotLitematicLoader")
    private val blockDisplayContext: BlockDisplayContext by lazy { BlockDisplayContext.create() }
    private val blockModelStateCache = HashMap<BlockState, BlockModelRenderState>()
    private var blockModelResolver: BlockModelResolver? = null
    private var blockModelManager: ModelManager? = null
    private val textureMeshCache = LitematicSectionMeshCache()

    fun getProxyState(state: BlockState): BlockState {
        val blockName = BuiltInRegistries.BLOCK.getKey(state.block).path
        if (blockName == "chest" || blockName == "trapped_chest" || blockName == "ender_chest") {
            var proxy = Blocks.BARREL.defaultBlockState()
            val horizFacing = BlockStateProperties.HORIZONTAL_FACING
            val facing = BlockStateProperties.FACING
            if (state.hasProperty(horizFacing)) {
                val dir = state.getValue(horizFacing)
                if (proxy.hasProperty(facing)) {
                    proxy = proxy.setValue(facing, dir)
                }
            }
            return proxy
        }
        return state
    }

    fun load(file: File): Projection {
        if (!file.exists() || !file.isFile) return Projection(file.name, emptyList(), Bounds.EMPTY)
        return runCatching {
            if (file.extension.equals("litematic", ignoreCase = true)) {
                loadLitematic(file)
            } else {
                loadTextProjection(file)
            }
        }.getOrElse { exception ->
            logger.warn("Failed to load projection ${file.name}", exception)
            Projection(file.name, emptyList(), Bounds.EMPTY)
        }
    }

    fun render(context: LevelRenderContext, projection: Projection, transform: Transform, options: RenderOptions) {
        render(context, RenderCache.build(projection, transform), options)
    }

    fun render(context: LevelRenderContext, cache: RenderCache, options: RenderOptions) {
        if (cache.isEmpty || (!options.texture && !options.fill && !options.outline)) return
        val player = Minecraft.getInstance().player ?: return
        val level = Minecraft.getInstance().level
        val camera = context.levelState().cameraRenderState.pos
        val frustum = context.levelState().cameraRenderState.cullFrustum
        val pose = context.poseStack().last().copy().apply { setIdentity() }
        val visibleSections = ArrayList<RenderSection>()

        for (section in cache.sections) {
            if (!frustum.isVisible(section.bounds)) continue
            visibleSections += section
        }

        if (visibleSections.isEmpty()) return
        if (level != null) {
            cache.refreshStatuses(level, visibleSections, options.statusChecksPerFrame)
        }

        if (options.texture) {
            textureMeshCache.render(context, cache, visibleSections, options.renderBuilt)
        }

        if (options.fill && isCustomGeometryWithinBudget(visibleSections, options, GeometryKind.FILL)) {
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), ANPilotRenderTypes.XRAY_FILLED_BOX) { _, vertexConsumer ->
                for (section in visibleSections) {
                    for (block in section.blocks) {
                        if (!block.shouldRender(options.renderBuilt)) continue
                        if (isCropOrPlant(block.state.block)) continue
                        val color = statusColor(block.status, options)
                        if (color.alpha <= 0) continue
                        val mask = if (options.onlyTopFace) (1 shl Direction.UP.ordinal) else block.exposedFaceMask
                        fillBox(block.pos, camera, pose, color, mask) { x, y, z, c ->
                            vertexConsumer.addVertex(pose, x, y, z).setColor(c.red, c.green, c.blue, c.alpha)
                        }
                    }
                }
            }
        }

        if (options.outline && isCustomGeometryWithinBudget(visibleSections, options, GeometryKind.OUTLINE)) {
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), ANPilotRenderTypes.XRAY_LINES) { _, vertexConsumer ->
                for (section in visibleSections) {
                    for (block in section.blocks) {
                        if (!block.shouldRender(options.renderBuilt)) continue
                        if (isCropOrPlant(block.state.block)) continue
                        val color = statusColor(block.status, options)
                        if (color.alpha <= 0) continue
                        val mask = if (options.onlyTopFace) (1 shl Direction.UP.ordinal) else block.exposedFaceMask
                        lineBox(block.pos, camera, mask) { x1, y1, z1, x2, y2, z2 ->
                            val normal = lineNormal(x1, y1, z1, x2, y2, z2)
                            vertexConsumer.addVertex(pose, x1, y1, z1)
                                .setColor(color.red, color.green, color.blue, color.alpha)
                                .setNormal(pose, normal.x, normal.y, normal.z)
                                .setLineWidth(1.5f)
                            vertexConsumer.addVertex(pose, x2, y2, z2)
                                .setColor(color.red, color.green, color.blue, color.alpha)
                                .setNormal(pose, normal.x, normal.y, normal.z)
                                .setLineWidth(1.5f)
                        }
                    }
                }
            }
        }
    }

    private fun isCustomGeometryWithinBudget(
        visibleSections: List<RenderSection>,
        options: RenderOptions,
        kind: GeometryKind
    ): Boolean {
        var vertices = 0L
        for (section in visibleSections) {
            for (block in section.blocks) {
                if (!block.shouldRender(options.renderBuilt)) continue
                if (isCropOrPlant(block.state.block)) continue
                if (statusColor(block.status, options).alpha <= 0) continue
                val mask = if (options.onlyTopFace) (1 shl Direction.UP.ordinal) else block.exposedFaceMask
                vertices += when (kind) {
                    GeometryKind.FILL -> mask.countOneBits() * 4L
                    GeometryKind.OUTLINE -> outlineVertexCount(mask).toLong()
                }
                if (vertices > MAX_CUSTOM_GEOMETRY_VERTICES) return false
            }
        }
        return true
    }

    fun clearTextureMeshes() {
        textureMeshCache.clear()
    }

    private fun loadLitematic(file: File): Projection {
        val root = NbtIo.readCompressed(file.toPath(), NbtAccounter.create(256L * 1024L * 1024L))
        val metadata = root.getCompound("Metadata").orElse(null)
        val projectionName = metadata?.getString("Name")?.orElse(null)?.takeIf { it.isNotBlank() } ?: file.name
        val author = metadata?.getString("Author")?.orElse(null)?.takeIf { it.isNotBlank() } ?: UNKNOWN_AUTHOR
        val regions = root.getCompoundOrEmpty("Regions")
        val blocks = ArrayList<ProjectedBlock>()

        for (regionName in regions.keySet()) {
            val region = regions.getCompound(regionName).orElse(null) ?: continue
            val regionPos = region.getBlockPos("Position")
            val size = region.getBlockPos("Size")
            val min = BlockPos(
                if (size.x < 0) regionPos.x + size.x + 1 else regionPos.x,
                if (size.y < 0) regionPos.y + size.y + 1 else regionPos.y,
                if (size.z < 0) regionPos.z + size.z + 1 else regionPos.z
            )
            val width = abs(size.x)
            val height = abs(size.y)
            val length = abs(size.z)
            if (width <= 0 || height <= 0 || length <= 0) continue

            val palette = ArrayList<BlockState>()
            region.getListOrEmpty("BlockStatePalette").compoundStream().forEach { tag ->
                palette += parsePaletteState(tag) ?: Blocks.AIR.defaultBlockState()
            }
            if (palette.isEmpty()) continue

            val packed = region.get("BlockStates")?.asLongArray()?.orElse(null) ?: continue
            val bits = ceilLog2(palette.size).coerceAtLeast(2)
            val mask = (1L shl bits) - 1L
            val total = width * height * length

            for (index in 0 until total) {
                val paletteIndex = unpack(packed, index, bits, mask)
                val state = palette.getOrNull(paletteIndex) ?: continue
                if (state.isAir) continue
                val x = index % width
                val y = (index / (width * length))
                val z = (index / width) % length
                blocks += ProjectedBlock(BlockPos(min.x + x, min.y + y, min.z + z), state)
            }
        }

        return Projection(projectionName, blocks, Bounds.of(blocks), author)
    }

    private fun loadTextProjection(file: File): Projection {
        val blocks = file.readLines().mapNotNull(::parseTextLine)
        return Projection(file.name, blocks, Bounds.of(blocks), UNKNOWN_AUTHOR)
    }

    private fun parseTextLine(line: String): ProjectedBlock? {
        val clean = line.substringBefore('#').trim()
        if (clean.isEmpty()) return null
        val parts = clean.split(',', ' ', '\t', ';').filter { it.isNotBlank() }
        if (parts.size < 4) return null

        val xyzFirst = parts[0].toIntOrNull() != null
        val x = (if (xyzFirst) parts[0] else parts[1]).toIntOrNull() ?: return null
        val y = (if (xyzFirst) parts[1] else parts[2]).toIntOrNull() ?: return null
        val z = (if (xyzFirst) parts[2] else parts[3]).toIntOrNull() ?: return null
        val blockId = if (xyzFirst) parts[3] else parts[0]
        val state = blockStateById(blockId) ?: return null
        if (state.isAir) return null
        return ProjectedBlock(BlockPos(x, y, z), state)
    }

    private fun parsePaletteState(tag: CompoundTag): BlockState? {
        val name = tag.getString("Name").orElse(null) ?: return null
        val properties = tag.getCompound("Properties").orElse(null)
        val stateString = if (properties == null || properties.isEmpty) {
            name
        } else {
            val props = properties.keySet().sorted().mapNotNull { key ->
                val value = properties.getString(key).orElse(null) ?: return@mapNotNull null
                "$key=$value"
            }.joinToString(",")
            "$name[$props]"
        }
        return blockStateById(stateString)
    }

    private fun blockStateById(id: String): BlockState? {
        val normalizedBlockId = id.substringBefore('[').let { if (it.contains(':')) it else "minecraft:$it" }
        val identifier = Identifier.tryParse(normalizedBlockId) ?: return null
        if (!BuiltInRegistries.BLOCK.containsKey(identifier)) return null
        val propertySuffix = id.indexOf('[').takeIf { it >= 0 }?.let { id.substring(it) }.orEmpty()
        val normalized = normalizedBlockId + propertySuffix
        return runCatching {
            BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, normalized, false).blockState()
        }.getOrNull()
    }

    private fun CompoundTag.getBlockPos(name: String): BlockPos {
        val tag = getCompoundOrEmpty(name)
        return BlockPos(
            tag.getIntOr("x", tag.getIntOr("X", 0)),
            tag.getIntOr("y", tag.getIntOr("Y", 0)),
            tag.getIntOr("z", tag.getIntOr("Z", 0))
        )
    }

    private fun unpack(data: LongArray, index: Int, bits: Int, mask: Long): Int {
        val bitIndex = index * bits
        val startLong = bitIndex ushr 6
        val startOffset = bitIndex and 63
        if (startLong >= data.size) return 0
        var value = data[startLong] ushr startOffset
        val endOffset = startOffset + bits
        if (endOffset > 64 && startLong + 1 < data.size) {
            value = value or (data[startLong + 1] shl (64 - startOffset))
        }
        return (value and mask).toInt()
    }

    private fun ceilLog2(value: Int): Int {
        if (value <= 1) return 1
        return 32 - Integer.numberOfLeadingZeros(value - 1)
    }

    private fun statusColor(actual: BlockState?, expected: BlockState, options: RenderOptions): ANColor {
        if (actual == null || actual.isAir) return options.missingColor
        if (actual.block == expected.block) return options.builtColor
        return options.wrongColor
    }

    private fun statusColor(status: BlockStatus, options: RenderOptions): ANColor = when (status) {
        BlockStatus.UNKNOWN -> options.missingColor
        BlockStatus.MISSING -> options.missingColor
        BlockStatus.WRONG -> options.wrongColor
        BlockStatus.BUILT -> options.builtColor
    }

    private fun renderTexturedBlock(context: LevelRenderContext, block: RenderBlock, camera: Vec3, poseStack: PoseStack) {
        val stateToRender = getProxyState(block.state)
        if (stateToRender.renderShape != RenderShape.MODEL) return
        val level = Minecraft.getInstance().level ?: return
        val modelState = blockModelRenderState(stateToRender)
        if (modelState.isEmpty) return
        val light = LevelRenderer.getLightCoords(level, block.pos)

        poseStack.pushPose()
        poseStack.translate(block.pos.x - camera.x, block.pos.y - camera.y, block.pos.z - camera.z)
        modelState.submit(poseStack, context.submitNodeCollector(), light, OverlayTexture.NO_OVERLAY, -1)
        poseStack.popPose()
    }

    private fun blockModelRenderState(state: BlockState): BlockModelRenderState {
        val manager = Minecraft.getInstance().modelManager
        if (blockModelManager !== manager) {
            blockModelManager = manager
            blockModelResolver = BlockModelResolver(manager)
            blockModelStateCache.clear()
        }

        return blockModelStateCache.getOrPut(state) {
            BlockModelRenderState().also { renderState ->
                blockModelResolver?.update(renderState, state, blockDisplayContext)
            }
        }
    }

    private fun fillBox(
        pos: BlockPos,
        camera: Vec3,
        pose: PoseStack.Pose,
        color: ANColor,
        faceMask: Int = ALL_FACES_MASK,
        vertex: (Float, Float, Float, ANColor) -> Unit
    ) {
        val x1 = (pos.x - camera.x).toFloat()
        val y1 = (pos.y - camera.y).toFloat()
        val z1 = (pos.z - camera.z).toFloat()
        val x2 = x1 + 1f
        val y2 = y1 + 1f
        val z2 = z1 + 1f

        fun quad(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float, cx: Float, cy: Float, cz: Float, dx: Float, dy: Float, dz: Float) {
            vertex(ax, ay, az, color)
            vertex(bx, by, bz, color)
            vertex(cx, cy, cz, color)
            vertex(dx, dy, dz, color)
        }

        if (faceMask.hasFace(Direction.WEST)) quad(x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1)
        if (faceMask.hasFace(Direction.EAST)) quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2)
        if (faceMask.hasFace(Direction.DOWN)) quad(x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2)
        if (faceMask.hasFace(Direction.UP)) quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1)
        if (faceMask.hasFace(Direction.NORTH)) quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1)
        if (faceMask.hasFace(Direction.SOUTH)) quad(x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2)
    }

    private fun lineBox(pos: BlockPos, camera: Vec3, faceMask: Int = ALL_FACES_MASK, line: (Float, Float, Float, Float, Float, Float) -> Unit) {
        val x1 = (pos.x - camera.x).toFloat()
        val y1 = (pos.y - camera.y).toFloat()
        val z1 = (pos.z - camera.z).toFloat()
        val x2 = x1 + 1f
        val y2 = y1 + 1f
        val z2 = z1 + 1f

        fun exposed(a: Direction, b: Direction): Boolean = faceMask.hasFace(a) || faceMask.hasFace(b)

        if (exposed(Direction.DOWN, Direction.NORTH)) line(x1, y1, z1, x2, y1, z1)
        if (exposed(Direction.DOWN, Direction.SOUTH)) line(x1, y1, z2, x2, y1, z2)
        if (exposed(Direction.DOWN, Direction.WEST)) line(x1, y1, z1, x1, y1, z2)
        if (exposed(Direction.DOWN, Direction.EAST)) line(x2, y1, z1, x2, y1, z2)

        if (exposed(Direction.UP, Direction.NORTH)) line(x1, y2, z1, x2, y2, z1)
        if (exposed(Direction.UP, Direction.SOUTH)) line(x1, y2, z2, x2, y2, z2)
        if (exposed(Direction.UP, Direction.WEST)) line(x1, y2, z1, x1, y2, z2)
        if (exposed(Direction.UP, Direction.EAST)) line(x2, y2, z1, x2, y2, z2)

        if (exposed(Direction.NORTH, Direction.WEST)) line(x1, y1, z1, x1, y2, z1)
        if (exposed(Direction.NORTH, Direction.EAST)) line(x2, y1, z1, x2, y2, z1)
        if (exposed(Direction.SOUTH, Direction.WEST)) line(x1, y1, z2, x1, y2, z2)
        if (exposed(Direction.SOUTH, Direction.EAST)) line(x2, y1, z2, x2, y2, z2)
    }

    private fun outlineVertexCount(faceMask: Int): Int {
        fun exposed(a: Direction, b: Direction): Boolean = faceMask.hasFace(a) || faceMask.hasFace(b)
        var lines = 0
        if (exposed(Direction.DOWN, Direction.NORTH)) lines++
        if (exposed(Direction.DOWN, Direction.SOUTH)) lines++
        if (exposed(Direction.DOWN, Direction.WEST)) lines++
        if (exposed(Direction.DOWN, Direction.EAST)) lines++
        if (exposed(Direction.UP, Direction.NORTH)) lines++
        if (exposed(Direction.UP, Direction.SOUTH)) lines++
        if (exposed(Direction.UP, Direction.WEST)) lines++
        if (exposed(Direction.UP, Direction.EAST)) lines++
        if (exposed(Direction.NORTH, Direction.WEST)) lines++
        if (exposed(Direction.NORTH, Direction.EAST)) lines++
        if (exposed(Direction.SOUTH, Direction.WEST)) lines++
        if (exposed(Direction.SOUTH, Direction.EAST)) lines++
        return lines * 2
    }

    private fun lineNormal(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Vector3f {
        val normal = Vector3f(x2 - x1, y2 - y1, z2 - z1)
        return if (normal.lengthSquared() > 0f) normal.normalize() else Vector3f(0f, 1f, 0f)
    }

    data class Projection(
        val name: String,
        val blocks: List<ProjectedBlock>,
        val bounds: Bounds,
        val author: String = UNKNOWN_AUTHOR
    )

    data class ProjectedBlock(
        val relativePos: BlockPos,
        val state: BlockState
    )

    data class Transform(
        val origin: BlockPos,
        val offsetX: Int = 0,
        val offsetY: Int = 0,
        val offsetZ: Int = 0,
        val rotation: Rotation = Rotation.NONE
    ) {
        fun apply(relative: BlockPos): BlockPos {
            val rotated = when (rotation) {
                Rotation.NONE -> relative
                Rotation.R_90 -> BlockPos(-relative.z, relative.y, relative.x)
                Rotation.R_180 -> BlockPos(-relative.x, relative.y, -relative.z)
                Rotation.R_270 -> BlockPos(relative.z, relative.y, -relative.x)
            }
            return origin.offset(rotated.x + offsetX, rotated.y + offsetY, rotated.z + offsetZ)
        }

        fun apply(state: BlockState): BlockState {
            return when (rotation) {
                Rotation.NONE -> state
                Rotation.R_90 -> state.rotate(BlockRotation.CLOCKWISE_90)
                Rotation.R_180 -> state.rotate(BlockRotation.CLOCKWISE_180)
                Rotation.R_270 -> state.rotate(BlockRotation.COUNTERCLOCKWISE_90)
            }
        }
    }

    data class RenderOptions(
        val texture: Boolean = true,
        val fill: Boolean = true,
        val outline: Boolean = true,
        val renderBuilt: Boolean = true,
        val onlyTopFace: Boolean = false,
        val statusChecksPerFrame: Int = 2048,
        val missingColor: ANColor = ANColor.rgba(70, 170, 255, 120),
        val wrongColor: ANColor = ANColor.rgba(255, 80, 80, 140),
        val builtColor: ANColor = ANColor.rgba(80, 255, 120, 85)
    )

    enum class Rotation {
        NONE,
        R_90,
        R_180,
        R_270
    }

    class RenderCache private constructor(
        val projectionName: String,
        val transform: Transform,
        val sections: List<RenderSection>,
        val blockCount: Int
    ) {
        private var statusSectionCursor = 0
        private val sectionIndex = sections.associateBy { it.key }

        val isEmpty: Boolean
            get() = blockCount == 0 || sections.isEmpty()

        fun refreshStatuses(level: ClientLevel, visibleSections: List<RenderSection>, budget: Int) {
            if (budget <= 0 || visibleSections.isEmpty()) return
            var remaining = budget
            var sectionOffset = 0
            while (remaining > 0 && sectionOffset < visibleSections.size) {
                val section = visibleSections[(statusSectionCursor + sectionOffset) % visibleSections.size]
                remaining -= section.refreshStatuses(level, remaining)
                sectionOffset++
            }
            statusSectionCursor = (statusSectionCursor + sectionOffset).floorMod(visibleSections.size)
        }

        fun markDirty(pos: BlockPos) {
            sectionIndex[SectionKey.of(pos)]?.markDirty(pos)
        }

        fun updateStatus(pos: BlockPos, actual: BlockState) {
            sectionIndex[SectionKey.of(pos)]?.updateStatus(pos, actual)
        }

        companion object {
            val EMPTY = RenderCache("", Transform(BlockPos.ZERO), emptyList(), 0)

            fun build(projection: Projection, transform: Transform): RenderCache {
                val builder = Builder(projection, transform)
                builder.step(Int.MAX_VALUE)
                return builder.snapshot()
            }

            private fun fromBuckets(
                projectionName: String,
                transform: Transform,
                buckets: LinkedHashMap<SectionKey, MutableList<RenderBlock>>,
                blockCount: Int
            ): RenderCache {
                if (blockCount <= 0 || buckets.isEmpty()) return EMPTY
                val sections = buckets.mapNotNull { (key, blocks) ->
                    val renderable = blocks.filterNot { it.interior }
                    if (renderable.isEmpty()) null else RenderSection.create(key, renderable)
                }
                    .sortedWith(compareBy({ it.key.x }, { it.key.y }, { it.key.z }))
                return RenderCache(projectionName, transform, sections, blockCount)
            }
        }

        class Builder(
            private val projection: Projection,
            val transform: Transform
        ) {
            private val buckets = LinkedHashMap<SectionKey, MutableList<RenderBlock>>()
            private val positions = LongOpenHashSet(projection.blocks.size)
            private val renderBlocks = ArrayList<RenderBlock>(projection.blocks.size)
            private var cursor = 0
            private var cullCursor = 0
            private var phase = if (projection.blocks.isEmpty()) BuildPhase.DONE else BuildPhase.TRANSFORM

            val projectionName: String
                get() = projection.name

            val totalBlocks: Int
                get() = projection.blocks.size

            val processedBlocks: Int
                get() = cursor

            val isDone: Boolean
                get() = phase == BuildPhase.DONE

            fun matches(projection: Projection, transform: Transform): Boolean {
                return projectionName == projection.name && totalBlocks == projection.blocks.size && this.transform == transform
            }

            fun step(budget: Int): Boolean {
                if (budget <= 0 || isDone) return isDone
                var remaining = budget
                while (remaining > 0 && !isDone) {
                    remaining = when (phase) {
                        BuildPhase.TRANSFORM -> stepTransform(remaining)
                        BuildPhase.CULL_INTERIORS -> stepCullInteriors(remaining)
                        BuildPhase.DONE -> 0
                    }
                }
                return isDone
            }

            fun snapshot(): RenderCache {
                return fromBuckets(projection.name, transform, buckets, cursor)
            }

            private fun stepTransform(budget: Int): Int {
                val start = cursor
                val end = (cursor + budget).coerceAtMost(projection.blocks.size)
                while (cursor < end) {
                    val projected = projection.blocks[cursor++]
                    val worldPos = transform.apply(projected.relativePos)
                    val block = RenderBlock(worldPos, transform.apply(projected.state))
                    positions.add(worldPos.asLong())
                    renderBlocks += block
                    buckets.getOrPut(SectionKey.of(worldPos)) { ArrayList() } += block
                }
                if (cursor >= projection.blocks.size) {
                    phase = BuildPhase.CULL_INTERIORS
                }
                return budget - (end - start)
            }

            private fun stepCullInteriors(budget: Int): Int {
                val start = cullCursor
                val end = (cullCursor + budget).coerceAtMost(renderBlocks.size)
                while (cullCursor < end) {
                    val block = renderBlocks[cullCursor++]
                    block.updateExposedFaces(positions)
                }
                if (cullCursor >= renderBlocks.size) {
                    phase = BuildPhase.DONE
                }
                return budget - (end - start)
            }
        }
    }

    data class RenderSection(
        val key: SectionKey,
        val blocks: List<RenderBlock>,
        val bounds: AABB
    ) {
        private var verifyCursor = 0
        private val blockIndex = blocks.associateBy { it.pos.asLong() }
        var revision: Int = 0
            private set

        fun refreshStatuses(level: ClientLevel, budget: Int): Int {
            if (budget <= 0 || blocks.isEmpty()) return 0
            var used = 0
            while (used < budget && used < blocks.size) {
                val block = blocks[(verifyCursor + used) % blocks.size]
                if (!block.interior) {
                    if (block.refreshStatus(level.getBlockState(block.pos))) {
                        revision++
                    }
                }
                used++
            }
            verifyCursor = (verifyCursor + used).floorMod(blocks.size)
            return used
        }

        fun markDirty(pos: BlockPos) {
            blockIndex[pos.asLong()]?.let { block ->
                if (block.status != BlockStatus.UNKNOWN) {
                    block.status = BlockStatus.UNKNOWN
                    revision++
                }
            }
        }

        fun updateStatus(pos: BlockPos, actual: BlockState) {
            blockIndex[pos.asLong()]?.let { block ->
                if (block.refreshStatus(actual)) {
                    revision++
                }
            }
        }

        companion object {
            fun create(key: SectionKey, blocks: List<RenderBlock>): RenderSection {
                var minX = Int.MAX_VALUE
                var minY = Int.MAX_VALUE
                var minZ = Int.MAX_VALUE
                var maxX = Int.MIN_VALUE
                var maxY = Int.MIN_VALUE
                var maxZ = Int.MIN_VALUE
                for (block in blocks) {
                    val pos = block.pos
                    minX = minOf(minX, pos.x)
                    minY = minOf(minY, pos.y)
                    minZ = minOf(minZ, pos.z)
                    maxX = maxOf(maxX, pos.x)
                    maxY = maxOf(maxY, pos.y)
                    maxZ = maxOf(maxZ, pos.z)
                }
                return RenderSection(key, blocks, AABB(minX.toDouble(), minY.toDouble(), minZ.toDouble(), maxX + 1.0, maxY + 1.0, maxZ + 1.0))
            }
        }
    }

    data class SectionKey(
        val x: Int,
        val y: Int,
        val z: Int
    ) {
        companion object {
            fun of(pos: BlockPos): SectionKey = SectionKey(pos.x shr 4, pos.y shr 4, pos.z shr 4)
        }
    }

    data class Bounds(
        val minX: Int,
        val minY: Int,
        val minZ: Int,
        val maxX: Int,
        val maxY: Int,
        val maxZ: Int
    ) {
        companion object {
            val EMPTY = Bounds(0, 0, 0, 0, 0, 0)

            fun of(blocks: List<ProjectedBlock>): Bounds {
                if (blocks.isEmpty()) return EMPTY
                var minX = Int.MAX_VALUE
                var minY = Int.MAX_VALUE
                var minZ = Int.MAX_VALUE
                var maxX = Int.MIN_VALUE
                var maxY = Int.MIN_VALUE
                var maxZ = Int.MIN_VALUE
                for (block in blocks) {
                    val pos = block.relativePos
                    minX = minOf(minX, pos.x)
                    minY = minOf(minY, pos.y)
                    minZ = minOf(minZ, pos.z)
                    maxX = maxOf(maxX, pos.x)
                    maxY = maxOf(maxY, pos.y)
                    maxZ = maxOf(maxZ, pos.z)
                }
                return Bounds(minX, minY, minZ, maxX, maxY, maxZ)
            }
        }
    }

    data class RenderBlock(
        val pos: BlockPos,
        val state: BlockState
    ) {
        var status: BlockStatus = BlockStatus.UNKNOWN
        var interior: Boolean = false
        var exposedFaceMask: Int = ALL_FACES_MASK

        fun refreshStatus(actual: BlockState?): Boolean {
            val isSlabMismatch = actual != null && actual.block == state.block &&
                    state.properties.any { it.name == "type" && state.getValue(it).toString() == "double" } &&
                    actual.properties.any { it.name == "type" && actual.getValue(it).toString() != "double" }

            val next = when {
                actual == null || actual.isAir -> BlockStatus.MISSING
                isSlabMismatch -> BlockStatus.MISSING
                actual.block == state.block -> BlockStatus.BUILT
                else -> BlockStatus.WRONG
            }
            if (status == next) return false
            status = next
            return true
        }

        fun shouldRender(renderBuilt: Boolean): Boolean = !interior && (renderBuilt || status != BlockStatus.BUILT)

        fun updateExposedFaces(positions: LongOpenHashSet) {
            if (state.renderShape != RenderShape.MODEL || !state.canOcclude() || !state.isSolidRender) {
                exposedFaceMask = ALL_FACES_MASK
                interior = false
                return
            }

            val packed = pos.asLong()
            var mask = 0
            for (direction in Direction.entries) {
                if (!positions.contains(BlockPos.offset(packed, direction))) {
                    mask = mask or direction.faceBit()
                }
            }
            exposedFaceMask = mask
            interior = mask == 0
        }
    }

    enum class BlockStatus {
        UNKNOWN,
        MISSING,
        WRONG,
        BUILT
    }

    private enum class GeometryKind {
        FILL,
        OUTLINE
    }

    private enum class BuildPhase {
        TRANSFORM,
        CULL_INTERIORS,
        DONE
    }

    private fun Int.floorMod(mod: Int): Int = if (mod == 0) 0 else Math.floorMod(this, mod)

    private fun Direction.faceBit(): Int = 1 shl ordinal

    private fun Int.hasFace(direction: Direction): Boolean = (this and direction.faceBit()) != 0

    fun isCropOrPlant(block: Block): Boolean {
        val path = BuiltInRegistries.BLOCK.getKey(block).path.lowercase()
        return path == "wheat"
                || path == "carrots"
                || path == "potatoes"
                || path == "beetroots"
                || path == "sweet_berry_bush"
                || path == "cocoa"
                || path == "cactus"
                || path == "sugar_cane"
                || path == "nether_wart"
                || path == "seagrass"
                || path == "tall_seagrass"
                || path == "sea_pickle"
                || path == "kelp"
                || path == "kelp_plant"
                || path == "bamboo"
                || path == "bamboo_sapling"
                || path == "dead_bush"
                || path.contains("stem")
                || path.contains("sapling")
                || path.contains("flower")
                || path.contains("grass")
                || path.contains("fern")
                || path.contains("mushroom")
                || path.contains("vines")
                || path.contains("crop")
                || path.contains("tulip")
                || path == "dandelion"
                || path == "poppy"
                || path == "blue_orchid"
                || path == "allium"
                || path == "azure_bluet"
                || path == "oxeye_daisy"
                || path == "cornflower"
                || path == "lily_of_the_valley"
                || path == "wither_rose"
                || path == "lilac"
                || path == "rose_bush"
                || path == "peony"
                || path == "sunflower"
    }

    private const val UNKNOWN_AUTHOR = "Unknown"
    private const val MAX_CUSTOM_GEOMETRY_VERTICES = 12_000_000L
    private val ALL_FACES_MASK = Direction.entries.fold(0) { mask, direction -> mask or direction.faceBit() }
}
