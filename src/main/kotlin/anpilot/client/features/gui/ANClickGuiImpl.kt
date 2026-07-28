package anpilot.client.features.gui

import anpilot.client.api.gui.ANClickGui
import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.api.module.ANModuleCategory
import anpilot.client.api.module.ANModuleRegistry
import anpilot.client.features.gui.component.ANCategoryPanel
import anpilot.client.features.manager.ANConfigManager
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.module.anpilot.ANPilotHud
import anpilot.client.features.module.anpilot.ANTheme
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ANClickGuiImpl(private val moduleRegistry: ANModuleRegistry) : ANClickGui {
    private var panels: MutableList<ANCategoryPanel>? = null
    private var hudOnly = false

    private companion object {
        private val FALLBACK_BACKGROUND_IMAGE = Identifier.fromNamespaceAndPath("anpilotclient", "textures/customimage/anpilotclient.png")
        private val CUSTOM_BACKGROUND_IMAGE = Identifier.fromNamespaceAndPath("anpilotclient", "custom_background/click_gui")
        private val logger = LoggerFactory.getLogger("ANClickGui")
        private const val BACKGROUND_FILE_NAME = "anpilotclient.png"
        private const val PANEL_GAP = 6f
        private const val OUTER_PADDING = 12f
        private const val MIN_CATEGORY_WIDTH = 96f
        private const val MAX_CATEGORY_WIDTH = 126f
        private const val BASE_PANEL_WIDTH = 660f
        private const val HEADER_SAFE_HEIGHT = 40f
        private var loadedBackgroundFile: File? = null
        private var loadedBackgroundModified = -1L
    }

    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        val categories = visibleCategories()
        val panelWidth = adaptivePanelWidth(context)
        val panelHeight = adaptivePanelHeight(context)
        val layoutScale = (panelWidth / BASE_PANEL_WIDTH).coerceIn(0.62f, 1.6f)
        val normalCount = if (hudOnly) {
            val priority = listOf(
                ANModuleCategory.COMBAT,
                ANModuleCategory.MOVEMENT,
                ANModuleCategory.PLAYER,
                ANModuleCategory.RENDER,
                ANModuleCategory.MISC,
                ANModuleCategory.CLIENT
            )
            priority.count { moduleRegistry.modules(it).isNotEmpty() }
        } else {
            categories.size
        }
        val grid = calculateGrid(panelWidth, panelHeight, normalCount, layoutScale)
        val outerPadding = OUTER_PADDING * layoutScale
        val panelX = (context.width - panelWidth) / 2f
        val panelY = (context.height - panelHeight) / 2f

        if (!hudOnly) {
            context.imageRect(backgroundImage(), panelX, panelY, panelWidth, panelHeight, ANTheme.BgTint)
        }

        val contentX = if (hudOnly) (context.width - outerPadding - grid.categoryWidth).coerceAtLeast(outerPadding) else panelX + outerPadding
        val contentY = panelY + outerPadding
        val guiPanels = panels ?: createPanels(categories).also { panels = it }
        layoutPanels(guiPanels, contentX, contentY, grid, layoutScale)
        guiPanels.forEach { it.render(context, mouseX, mouseY, deltaTicks) }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        panels?.forEach { if (it.mouseClicked(mouseX, mouseY, button)) return true }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        panels?.forEach { if (it.mouseReleased(mouseX, mouseY, button)) return true }
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        panels?.forEach { if (it.mouseScrolled(mouseX, mouseY, amount)) return true }
        return false
    }

    override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        panels?.forEach { if (it.keyPressed(key, scanCode, modifiers)) return true }
        return false
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        panels?.forEach { if (it.charTyped(chr, modifiers)) return true }
        return false
    }

    override fun resetView() {
        hudOnly = false
        panels = null
    }

    private fun createPanels(categories: List<ANModuleCategory>): MutableList<ANCategoryPanel> {
        return categories.map { category ->
            val modules = moduleRegistry.modules(category).filterIsInstance<ANBaseModule>()
            ANCategoryPanel(category, modules, 0f, 0f, MIN_CATEGORY_WIDTH, ::onModulePrimaryClick)
        }.toMutableList()
    }

    private fun layoutPanels(panels: List<ANCategoryPanel>, startX: Float, startY: Float, grid: GridLayout, layoutScale: Float) {
        val panelGap = PANEL_GAP * layoutScale
        panels.forEachIndexed { index, panel ->
            val column = index % grid.columns
            val row = index / grid.columns
            panel.x = startX + column * (grid.categoryWidth + panelGap)
            panel.y = startY + row * (grid.categoryHeight + panelGap)
            panel.width = grid.categoryWidth
            panel.maxHeight = grid.categoryHeight
        }
    }

    private fun visibleCategories(): List<ANModuleCategory> {
        if (hudOnly) return listOf(ANModuleCategory.HUD)

        val priority = listOf(
            ANModuleCategory.COMBAT,
            ANModuleCategory.MOVEMENT,
            ANModuleCategory.PLAYER,
            ANModuleCategory.RENDER,
            ANModuleCategory.MISC,
            ANModuleCategory.CLIENT,
        )
        return priority.filter { moduleRegistry.modules(it).isNotEmpty() }
    }

    private fun onModulePrimaryClick(module: ANBaseModule): Boolean {
        if (module !is ANPilotHud) return false
        hudOnly = true
        panels = null
        return true
    }

    private fun backgroundImage(): Identifier {
        val file = File(ANConfigManager.customBackgroundFolder(), BACKGROUND_FILE_NAME)
        if (!file.isFile) return FALLBACK_BACKGROUND_IMAGE

        val modified = file.lastModified()
        if (loadedBackgroundFile?.absolutePath == file.absolutePath && loadedBackgroundModified == modified) {
            return CUSTOM_BACKGROUND_IMAGE
        }

        return runCatching {
            val image = readBackgroundImage(file)
            Minecraft.getInstance().textureManager.register(
                CUSTOM_BACKGROUND_IMAGE,
                DynamicTexture({ "ANPilot Custom ClickGUI Background" }, image)
            )
            loadedBackgroundFile = file
            loadedBackgroundModified = modified
            logger.info("Loaded custom ClickGUI background: {}", file.absolutePath)
            CUSTOM_BACKGROUND_IMAGE
        }.getOrElse {
            logger.warn("Failed to load custom ClickGUI background: {}", file.absolutePath, it)
            FALLBACK_BACKGROUND_IMAGE
        }
    }

    private fun readBackgroundImage(file: File): NativeImage {
        val buffered = ImageIO.read(file)
            ?: throw IllegalArgumentException("Unsupported image format: ${file.absolutePath}")
        val argb = if (buffered.type == BufferedImage.TYPE_INT_ARGB) {
            buffered
        } else {
            BufferedImage(buffered.width, buffered.height, BufferedImage.TYPE_INT_ARGB).also { converted ->
                val graphics = converted.createGraphics()
                graphics.drawImage(buffered, 0, 0, null)
                graphics.dispose()
            }
        }
        val image = NativeImage(argb.width, argb.height, false)
        for (y in 0 until argb.height) {
            for (x in 0 until argb.width) {
                image.setPixel(x, y, argb.getRGB(x, y))
            }
        }
        return image
    }

    private fun adaptivePanelWidth(context: ANGuiRenderContext): Float {
        val screenWidth = context.width.toFloat()
        val ratio = when {
            screenWidth < 520f -> 0.94f
            screenWidth < 760f -> 0.9f
            else -> 0.7f
        }
        return (screenWidth * ratio).coerceAtMost(screenWidth - OUTER_PADDING * 2f).coerceAtLeast(screenWidth * 0.5f)
    }

    private fun adaptivePanelHeight(context: ANGuiRenderContext): Float {
        val screenHeight = context.height.toFloat()
        val ratio = when {
            screenHeight < 320f -> 0.9f
            screenHeight < 520f -> 0.82f
            else -> 0.6f
        }
        return (screenHeight * ratio).coerceAtMost(screenHeight - OUTER_PADDING * 2f).coerceAtLeast(screenHeight * 0.5f)
    }

    private fun calculateGrid(panelWidth: Float, panelHeight: Float, count: Int, layoutScale: Float): GridLayout {
        val outerPadding = OUTER_PADDING * layoutScale
        val panelGap = PANEL_GAP * layoutScale
        val minCategoryWidth = MIN_CATEGORY_WIDTH * layoutScale
        val maxCategoryWidth = MAX_CATEGORY_WIDTH * layoutScale
        val minCategoryHeight = 118f * layoutScale
        val availableWidth = panelWidth - outerPadding * 2f
        val availableHeight = panelHeight - outerPadding * 2f
        val safeCount = count.coerceAtLeast(1)
        val columns = (safeCount downTo 1).firstOrNull { candidate ->
            val rows = ((safeCount + candidate - 1) / candidate).coerceAtLeast(1)
            val width = (availableWidth - panelGap * (candidate - 1)) / candidate
            val height = (availableHeight - panelGap * (rows - 1)) / rows
            width >= minCategoryWidth && height >= minCategoryHeight
        } ?: (safeCount downTo 1).firstOrNull { candidate ->
            val width = (availableWidth - panelGap * (candidate - 1)) / candidate
            width >= minCategoryWidth
        } ?: 1
        val rows = ((safeCount + columns - 1) / columns).coerceAtLeast(1)
        val categoryWidth = ((availableWidth - panelGap * (columns - 1)) / columns).coerceIn(minCategoryWidth, maxCategoryWidth)
        val categoryHeight = ((availableHeight - panelGap * (rows - 1)) / rows).coerceAtLeast(HEADER_SAFE_HEIGHT * layoutScale)
        return GridLayout(columns, rows, categoryWidth, categoryHeight)
    }

    private data class GridLayout(val columns: Int, val rows: Int, val categoryWidth: Float, val categoryHeight: Float)

}

