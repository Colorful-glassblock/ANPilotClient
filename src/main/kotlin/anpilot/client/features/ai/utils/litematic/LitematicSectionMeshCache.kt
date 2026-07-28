package anpilot.client.features.ai.utils.litematic

import anpilot.client.features.ai.utils.LitematicLoader
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.QuadInstance
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.DynamicUniforms
import net.minecraft.client.renderer.SectionBufferBuilderPack
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.client.renderer.block.BlockModelLighter
import net.minecraft.client.renderer.block.BlockQuadOutput
import net.minecraft.client.renderer.block.ModelBlockRenderer
import net.minecraft.client.renderer.chunk.ChunkSectionLayer
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.resources.model.geometry.BakedQuad
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.RenderShape
import java.util.EnumMap
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.function.Supplier







class LitematicSectionMeshCache {
    private val mc = Minecraft.getInstance()
    private val sectionMeshes = HashMap<LitematicLoader.SectionKey, SectionMesh>()
    private val rebuildQueue = ArrayDeque<LitematicLoader.RenderSection>()
    private val queuedSections = HashSet<LitematicLoader.SectionKey>()
    private var activeCacheIdentity = ""

    fun render(
        context: LevelRenderContext,
        cache: LitematicLoader.RenderCache,
        visibleSections: List<LitematicLoader.RenderSection>,
        renderBuilt: Boolean
    ) {
        val level = mc.level ?: return
        if (visibleSections.isEmpty()) return
        RenderSystem.assertOnRenderThread()
        ensureCacheIdentity(cache, renderBuilt)

        for (section in visibleSections) {
            val mesh = sectionMeshes[section.key]
            if (mesh == null || mesh.revision != section.revision || mesh.renderBuilt != renderBuilt) {
                queueRebuild(section)
            }
        }

        repeat(MAX_REBUILDS_PER_FRAME) {
            val section = rebuildQueue.removeFirstOrNull() ?: return@repeat
            queuedSections.remove(section.key)
            val latest = visibleSections.firstOrNull { it.key == section.key } ?: section
            rebuildSection(latest, renderBuilt)
        }

        val blockAtlas = mc.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS)
        val sectionInfos = buildChunkSectionInfos(
            visibleSections,
            blockAtlas.textureView.getWidth(0),
            blockAtlas.textureView.getHeight(0)
        )
        for (layer in DRAW_ORDER) {
            val renderPass = beginPass(layer) ?: continue
            try {
                for (section in visibleSections) {
                    val sectionMesh = sectionMeshes[section.key] ?: continue
                    val layerMesh = sectionMesh.layers[layer] ?: continue
                    val chunkSection = sectionInfos[section.key] ?: continue
                    drawLayer(renderPass, layerMesh, chunkSection)
                }
            } finally {
                renderPass.close()
            }
        }
    }

    fun clear() {
        for (mesh in sectionMeshes.values) {
            mesh.close()
        }
        sectionMeshes.clear()
        rebuildQueue.clear()
        queuedSections.clear()
        activeCacheIdentity = ""
    }

    private fun ensureCacheIdentity(cache: LitematicLoader.RenderCache, renderBuilt: Boolean) {
        val identity = "${cache.projectionName}|${cache.transform}|${cache.blockCount}|$renderBuilt"
        if (activeCacheIdentity == identity) return
        clear()
        activeCacheIdentity = identity
    }

    private fun queueRebuild(section: LitematicLoader.RenderSection) {
        if (queuedSections.add(section.key)) {
            rebuildQueue += section
        }
    }

    private fun rebuildSection(section: LitematicLoader.RenderSection, renderBuilt: Boolean) {
        val level = mc.level ?: return
        val pack = SectionBufferBuilderPack()
        val builders = EnumMap<ChunkSectionLayer, BufferBuilder>(ChunkSectionLayer::class.java)
        val modelRenderer = ModelBlockRenderer(true, false, mc.blockColors)
        val blockGetter: BlockAndTintGetter = level
        val origin = section.origin()
        val quadOutput = BlockQuadOutput { x, y, z, quad, instance ->
            val layer = quad.materialInfo().layer()
            val builder = builders.getOrPut(layer) {
                BufferBuilder(pack.buffer(layer), VertexFormat.Mode.QUADS, layer.vertexFormat())
            }
            putBlockBakedQuad(builder, x, y, z, quad, instance)
        }

        try {
            BlockModelLighter.enableCaching()
            for (block in section.blocks) {
                if (!block.shouldRender(renderBuilt)) continue
                val stateToRender = LitematicLoader.getProxyState(block.state)
                if (stateToRender.renderShape != RenderShape.MODEL) continue

                modelRenderer.tesselateBlock(
                    quadOutput,
                    (block.pos.x - origin.x).toFloat(),
                    (block.pos.y - origin.y).toFloat(),
                    (block.pos.z - origin.z).toFloat(),
                    blockGetter,
                    block.pos,
                    stateToRender,
                    mc.modelManager.blockStateModelSet.get(stateToRender),
                    stateToRender.getSeed(block.pos)
                )
            }

            val layers = EnumMap<ChunkSectionLayer, LayerMesh>(ChunkSectionLayer::class.java)
            for ((layer, builder) in builders) {
                val meshData = builder.build() ?: continue
                meshData.use { mesh ->
                    uploadLayer(section.key, layer, mesh)?.let { layers[layer] = it }
                }
            }

            sectionMeshes.remove(section.key)?.close()
            sectionMeshes[section.key] = SectionMesh(section.revision, renderBuilt, origin, layers)
        } finally {
            BlockModelLighter.clearCache()
            pack.close()
        }
    }

    private fun putBlockBakedQuad(
        builder: BufferBuilder,
        x: Float,
        y: Float,
        z: Float,
        quad: BakedQuad,
        instance: QuadInstance
    ) {
        val normal = quad.direction()
        for (index in 0 until BakedQuad.VERTEX_COUNT) {
            val position = quad.position(index)
            val packedUv = quad.packedUV(index)
            builder.addVertex(
                x + position.x(),
                y + position.y(),
                z + position.z(),
                instance.getColor(index),
                packedUvU(packedUv),
                packedUvV(packedUv),
                instance.overlayCoords(),
                instance.getLightCoords(index),
                normal.stepX.toFloat(),
                normal.stepY.toFloat(),
                normal.stepZ.toFloat()
            )
        }
    }

    private fun uploadLayer(
        sectionKey: LitematicLoader.SectionKey,
        layer: ChunkSectionLayer,
        mesh: MeshData
    ): LayerMesh? {
        val drawState = mesh.drawState()
        if (drawState.indexCount() <= 0 || drawState.vertexCount() <= 0) return null

        val device = RenderSystem.getDevice()
        val vertexBuffer = device.createBuffer(
            Supplier { "anpilot_litematic_${sectionKey}_${layer.label()}_vertices" },
            GpuBuffer.USAGE_VERTEX,
            mesh.vertexBuffer()
        )

        val indexBufferData = mesh.indexBuffer()
        val indexBuffer = if (indexBufferData != null) {
            device.createBuffer(
                Supplier { "anpilot_litematic_${sectionKey}_${layer.label()}_indices" },
                GpuBuffer.USAGE_INDEX,
                indexBufferData
            )
        } else {
            null
        }

        return LayerMesh(vertexBuffer, indexBuffer, drawState.mode(), drawState.indexType(), drawState.indexCount())
    }

    private fun beginPass(layer: ChunkSectionLayer): RenderPass? {
        val renderTarget = mc.mainRenderTarget
        val color = RenderSystem.outputColorTextureOverride ?: renderTarget.colorTextureView ?: return null
        val depth = RenderSystem.outputDepthTextureOverride ?: if (renderTarget.useDepth) renderTarget.depthTextureView else null
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        val renderPass = if (depth != null) {
            encoder.createRenderPass(
                Supplier { "anpilot_litematic/${layer.label()}" },
                color,
                OptionalInt.empty(),
                depth,
                OptionalDouble.empty()
            )
        } else {
            encoder.createRenderPass(
                Supplier { "anpilot_litematic/${layer.label()}" },
                color,
                OptionalInt.empty()
            )
        }

        renderPass.setPipeline(layer.pipeline())
        RenderSystem.bindDefaultUniforms(renderPass)
        val blockAtlas = mc.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS)
        renderPass.bindTexture("Sampler0", blockAtlas.textureView, blockAtlas.sampler)
        renderPass.bindTexture("Sampler2", mc.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
        return renderPass
    }

    private fun buildChunkSectionInfos(
        visibleSections: List<LitematicLoader.RenderSection>,
        textureAtlasWidth: Int,
        textureAtlasHeight: Int
    ): Map<LitematicLoader.SectionKey, GpuBufferSlice> {
        val infos = HashMap<LitematicLoader.SectionKey, GpuBufferSlice>(visibleSections.size)
        val modelView = RenderSystem.getModelViewMatrix()
        for (section in visibleSections) {
            val origin = section.origin()
            infos[section.key] = RenderSystem.getDynamicUniforms().writeChunkSections(
                DynamicUniforms.ChunkSectionInfo(
                    modelView,
                    origin.x,
                    origin.y,
                    origin.z,
                    1.0f,
                    textureAtlasWidth,
                    textureAtlasHeight
                )
            )[0]
        }
        return infos
    }

    private fun drawLayer(renderPass: RenderPass, layerMesh: LayerMesh, chunkSection: GpuBufferSlice) {
        renderPass.setUniform("ChunkSection", chunkSection)
        renderPass.setVertexBuffer(0, layerMesh.vertexBuffer)

        val indexBuffer = layerMesh.indexBuffer
        if (indexBuffer != null) {
            renderPass.setIndexBuffer(indexBuffer, layerMesh.indexType)
        } else {
            val sequential = RenderSystem.getSequentialBuffer(layerMesh.mode)
            renderPass.setIndexBuffer(sequential.getBuffer(layerMesh.indexCount), sequential.type())
        }

        renderPass.drawIndexed(0, 0, layerMesh.indexCount, 1)
    }

    private fun LitematicLoader.RenderSection.origin(): BlockPos {
        return BlockPos(key.x shl 4, key.y shl 4, key.z shl 4)
    }

    private fun packedUvU(packedUv: Long): Float {
        return Float.fromBits((packedUv ushr 32).toInt())
    }

    private fun packedUvV(packedUv: Long): Float {
        return Float.fromBits(packedUv.toInt())
    }

    private data class SectionMesh(
        val revision: Int,
        val renderBuilt: Boolean,
        val origin: BlockPos,
        val layers: EnumMap<ChunkSectionLayer, LayerMesh>
    ) : AutoCloseable {
        override fun close() {
            for (mesh in layers.values) {
                mesh.close()
            }
            layers.clear()
        }
    }

    private data class LayerMesh(
        val vertexBuffer: GpuBuffer,
        val indexBuffer: GpuBuffer?,
        val mode: VertexFormat.Mode,
        val indexType: VertexFormat.IndexType,
        val indexCount: Int
    ) : AutoCloseable {
        override fun close() {
            vertexBuffer.close()
            indexBuffer?.close()
        }
    }

    private companion object {
        val DRAW_ORDER: Array<ChunkSectionLayer> = arrayOf(
            ChunkSectionLayer.SOLID,
            ChunkSectionLayer.CUTOUT,
            ChunkSectionLayer.TRANSLUCENT
        )
        const val MAX_REBUILDS_PER_FRAME = 2
    }
}
