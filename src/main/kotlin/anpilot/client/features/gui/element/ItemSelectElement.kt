package anpilot.client.features.gui.element

import anpilot.client.api.gui.ANGuiRenderContext
import anpilot.client.features.gui.component.ANElement
import anpilot.client.features.module.anpilot.ANPilotGuiEditor
import anpilot.client.features.module.anpilot.ANTheme
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.ItemSelectSetting
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.ClientLanguage
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor

class ItemSelectElement(
    private val setting: ANSetting<ItemSelectSetting>
) : ANElement(height = CLOSED_HEIGHT) {
    private var isOpenHover = false
    private var isCloseHover = false
    private var isItemsHover = false
    private var isMenuHover = false
    private var isMenuOpen = false
    private var isHoverDown = false
    private var isHoverUp = false

    private var index = 1
    private var countIndex = 1
    private var menuIndex = 1
    private var menuCountIndex = 1

    private var hoveredMenuIndex = -1
    private var deleteHoveredIndex = -1
    private var selectedItems: MutableList<String> = mutableListOf()

    override fun render(context: ANGuiRenderContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        selectedItems = setting.value.getItemsById()
        val open = moduleHasItemSelectOpen(setting.module)

        countIndex = pageCount(selectedItems.size, SELECTED_PAGE_SIZE)
        menuCountIndex = if (open && isMenuOpen) pageCount(menuItems().size, menuPageSize()) else 1

        index = index.coerceIn(1, countIndex)
        menuIndex = menuIndex.coerceIn(1, menuCountIndex)

        height = if (open) OPEN_HEIGHT else CLOSED_HEIGHT
        isOpenHover = hover(openButtonX(), y + 5f, OPEN_BUTTON_WIDTH, CLOSED_BUTTON_HEIGHT, mouseX, mouseY)

        if (open) {
            renderOpen(context, mouseX, mouseY)
        } else {
            renderClosed(context)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) return false

        if (isOpenHover) setModuleOpen(true)
        if (isCloseHover) {
            if (!isMenuOpen) setModuleOpen(false) else isMenuOpen = false
        }
        if (isItemsHover) isMenuOpen = false
        if (isMenuHover) isMenuOpen = true

        if (isHoverDown) {
            if (!isMenuOpen) {
                index = (index - 1).coerceAtLeast(1)
            } else {
                menuIndex = (menuIndex - 1).coerceAtLeast(1)
            }
        }

        if (isHoverUp) {
            if (!isMenuOpen) {
                index = (index + 1).coerceAtMost(countIndex)
            } else {
                menuIndex = (menuIndex + 1).coerceAtMost(menuCountIndex)
            }
        }

        val items = if (hoveredMenuIndex != -1) menuItems() else emptyList()
        if (hoveredMenuIndex in items.indices) {
            val name = BuiltInRegistries.ITEM.getKey(items[hoveredMenuIndex].item).path
            if (!selectedItems.contains(name)) {
                selectedItems.add(name)
                setting.setValue(setting.value)
            }
        }

        if (deleteHoveredIndex in selectedItems.indices) {
            selectedItems.removeAt(deleteHoveredIndex)
            setting.setValue(setting.value)
        }

        return isOpenHover || isCloseHover || isItemsHover || isMenuHover || isHoverDown || isHoverUp || hoveredMenuIndex != -1 || deleteHoveredIndex != -1
    }

    private fun renderOpen(context: ANGuiRenderContext, mouseX: Int, mouseY: Int) {
        isCloseHover = hover(closeButtonX(), y + 5f, SIDE_BUTTON_WIDTH, HEADER_BUTTON_HEIGHT, mouseX, mouseY)
        val halfWidth = menuButtonWidth() / 2f
        isItemsHover = hover(menuButtonX(), y + 5f, halfWidth, HEADER_BUTTON_HEIGHT, mouseX, mouseY)
        isMenuHover = hover(menuButtonX() + halfWidth, y + 5f, halfWidth, HEADER_BUTTON_HEIGHT, mouseX, mouseY)
        isHoverDown = hover(pageLeftX(), pageY(), PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT, mouseX, mouseY)
        isHoverUp = hover(pageRightX(), pageY(), PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT, mouseX, mouseY)

        context.borderedRoundedRect(x, y, width, height, 8f, 1f, ANTheme.BtnFill, ANTheme.BtnBorder)
        context.borderedRoundedRect(x + 5f, y + 5f, width-10f, 15f, 6f, 1f, ANTheme.SetCtrlFill, ANTheme.Magenta)
        renderHeaderText(context)
        if (!isMenuOpen) renderSelectedItems(context, mouseX, mouseY) else renderItemMenu(context, mouseX, mouseY)
        
    }

    private fun renderClosed(context: ANGuiRenderContext) {
        isCloseHover = false
        isItemsHover = false
        isMenuHover = false
        isHoverDown = false
        isHoverUp = false
        hoveredMenuIndex = -1
        deleteHoveredIndex = -1

        context.borderedRoundedRect(x, y, width, height, 8f, 1f, ANTheme.BtnFill, ANTheme.BtnBorder)
        context.borderedRoundedRect(openButtonX(), y + 5f, OPEN_BUTTON_WIDTH, 10f, 4f, 1f, ANTheme.Green, ANTheme.Magenta)
        context.text(fitText(context, setting.name, openButtonX() - x - 16f), x + 6f, y + 5f, ANTheme.BtnText.rgb, TEXT_SCALE)

        val size = selectedItems.size.toString()
        val sizeWidth = context.textWidth(size, TEXT_SCALE).toFloat()
        context.text(size, openButtonX() + (OPEN_BUTTON_WIDTH - sizeWidth) / 2f, y + 6f, ANTheme.SetCtrlBorder.rgb, TEXT_SCALE)
    }

    private fun renderHeaderText(context: ANGuiRenderContext) {
        val size = selectedItems.size.toString()
        val sizeWidth = context.textWidth(size, TEXT_SCALE).toFloat()
        context.text(size, x + 5f + (SIDE_BUTTON_WIDTH - sizeWidth) / 2f, y + 8f, ANTheme.SetCtrlBorder.rgb, TEXT_SCALE)

        context.text("-", closeButtonX() + (SIDE_BUTTON_WIDTH - 10) / 2f, y + 8f, ANTheme.SetCtrlBorder.rgb, TEXT_SCALE)

        val halfWidth = menuButtonWidth() / 2f

        
        val iText = "I"
        val iWidth = context.textWidth(iText, TEXT_SCALE).toFloat()
        val iColor = when {
            !isMenuOpen -> ANTheme.Yellow.rgb
            isItemsHover -> ANTheme.White.rgb
            else -> ANTheme.SetMutedText.rgb
        }
        val iX = menuButtonX() + (halfWidth - iWidth) / 2f
        context.text(iText, iX, y + 8f, iColor, TEXT_SCALE)

        
        val sepText = "|"
        val sepWidth = context.textWidth(sepText, TEXT_SCALE).toFloat()
        val sepX = menuButtonX() + halfWidth - sepWidth / 2f
        context.text(sepText, sepX, y + 8f, ANTheme.SetCtrlBorder.rgb, TEXT_SCALE)

        
        val mText = "M"
        val mWidth = context.textWidth(mText, TEXT_SCALE).toFloat()
        val mColor = when {
            isMenuOpen -> ANTheme.Yellow.rgb
            isMenuHover -> ANTheme.White.rgb
            else -> ANTheme.SetMutedText.rgb
        }
        val mX = menuButtonX() + halfWidth + (halfWidth - mWidth) / 2f
        context.text(mText, mX, y + 8f, mColor, TEXT_SCALE)
    }

    private fun renderSelectedItems(context: ANGuiRenderContext, mouseX: Int, mouseY: Int) {
        deleteHoveredIndex = -1
        hoveredMenuIndex = -1
        var rowY = y + CONTENT_TOP
        val listX = x + 5f
        val listWidth = (width - 10f).coerceAtLeast(1f)

        val deleteWidth = 10f
        val deleteHeight = 9f
        val deleteX = x + width - 5f - deleteWidth - 3f
        val textX = x + 23f
        val textMaxWidth = (deleteX - textX - 2f).coerceAtLeast(0f)

        for (i in (index - 1) * SELECTED_PAGE_SIZE until (index - 1) * SELECTED_PAGE_SIZE + SELECTED_PAGE_SIZE) {
            if (i >= selectedItems.size) continue
            val selected = selectedItems[i]
            val entry = entryByName(selected) ?: continue

            val rowDeleteHover = hover(deleteX, rowY + 2f, deleteWidth, deleteHeight, mouseX, mouseY)
            context.borderedRoundedRect(listX, rowY, listWidth, SELECTED_ROW_HEIGHT, 6f, 0.6f, ANTheme.SetCtrlFill, ANTheme.Magenta)
            context.borderedRoundedRect(deleteX, rowY + 3f, deleteWidth, deleteHeight, 4f, 0.6f, ANTheme.SetCtrlFill, if (rowDeleteHover) DELETE_HOVER else DELETE)

            if (rowDeleteHover) deleteHoveredIndex = i

            drawItem(context, entry.stack, x + 8f, rowY + 2f, SELECTED_ITEM_SCALE)
            context.text(fitText(context, displayName(entry), textMaxWidth, COMPACT_TEXT_SCALE), textX, rowY + 5f, ANTheme.Yellow.rgb, COMPACT_TEXT_SCALE)
            context.text("-", deleteX + (deleteWidth - context.textWidth("-", COMPACT_TEXT_SCALE)) / 2f, rowY + 5f, ANTheme.Red.rgb, COMPACT_TEXT_SCALE)

            rowY += SELECTED_ROW_HEIGHT + SELECTED_ROW_GAP
        }

        renderIndexText(context, index, countIndex)
    }

    private fun renderItemMenu(context: ANGuiRenderContext, mouseX: Int, mouseY: Int) {
        hoveredMenuIndex = -1
        deleteHoveredIndex = -1
        val items = menuItems()
        val frameX = x + 5f
        val frameY = y + 25f
        val frameWidth = (width - 10f).coerceAtLeast(1f)
        context.borderedRoundedRect(frameX, frameY, frameWidth, MENU_HEIGHT, 8f, 1f, ANTheme.SetCtrlFill, ANTheme.Magenta)

        val cols = menuCols()
        val pageSize = menuPageSize()
        val start = (menuIndex - 1) * pageSize
        val end = start + pageSize

        val gridX = frameX + 2f
        val gridY = frameY + MENU_PADDING

        for (i in start until end) {
            if (i >= items.size) continue
            val gridPos = i - start
            val col = gridPos % cols
            val row = gridPos / cols

            val itemX = gridX + col * CELL_WIDTH
            val itemY = gridY + row * CELL_HEIGHT

            val cellHover = hover(itemX, itemY, CELL_BOX_W, CELL_BOX_H, mouseX, mouseY)
            if (cellHover) {
                context.borderedRoundedRect(itemX, itemY, CELL_BOX_W, CELL_BOX_H, 5f, 1f, ANTheme.SetCtrlFill, ANTheme.Green)
                hoveredMenuIndex = i
            }
            drawItem(context, items[i], itemX + ITEM_INSET, itemY + ITEM_INSET, ITEM_SCALE)
        }

        renderIndexText(context, menuIndex, menuCountIndex)
    }

    private fun renderPageControls(context: ANGuiRenderContext) {
        context.borderedRoundedRect(pageLeftX(), pageY(), PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT, 8f, 0.4f, ANTheme.Transparent, ANTheme.Purple)
        context.borderedRoundedRect(pageRightX(), pageY(), PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT, 8f, 0.4f, ANTheme.Transparent, ANTheme.Purple)
        context.borderedRoundedRect(pageControlX(), pageY(), PAGE_CONTROL_WIDTH, PAGE_BUTTON_HEIGHT, 2f, 0.4f, ANTheme.Transparent, ANTheme.Cyan)
    }

    private fun renderIndexText(context: ANGuiRenderContext, current: Int, count: Int) {
        val textY = pageY()
        context.text("<", pageLeftX() + (PAGE_BUTTON_WIDTH - context.textWidth("<", TEXT_SCALE)) / 2f, textY, ANTheme.Cyan.rgb, TEXT_SCALE)
        context.text(">", pageRightX() + (PAGE_BUTTON_WIDTH - context.textWidth(">", TEXT_SCALE)) / 2f, textY, ANTheme.Cyan.rgb, TEXT_SCALE)

        val currentText = current.toString()
        val countText = count.toString()
        val centerX = pageControlX() + PAGE_CONTROL_WIDTH / 2f

        context.text(currentText, centerX - 4f - context.textWidth(currentText, TEXT_SCALE), textY, ANTheme.Red.rgb, TEXT_SCALE)
        context.text("|", centerX - context.textWidth("|", TEXT_SCALE) / 2f, textY, ANTheme.Yellow.rgb, TEXT_SCALE)
        context.text(countText, centerX + 4f, textY, ANTheme.Red.rgb, TEXT_SCALE)
    }

    private fun drawItem(context: ANGuiRenderContext, stack: ItemStack, itemX: Float, itemY: Float, scale: Float) {
        context.item(stack, itemX, itemY, scale, true)
    }

    private fun entryByName(name: String): RegistryEntry? {
        return blockEntries().firstOrNull { it.id == name } ?: itemEntries().firstOrNull { it.id == name }
    }

    private fun setModuleOpen(value: Boolean) {
        val module = setting.module ?: return
        val method = module.javaClass.methods.firstOrNull { it.name == "Set_itemSelect_open" && it.parameterTypes.size == 1 }
        method?.invoke(module, value)
    }

    private fun moduleHasItemSelectOpen(module: Any?): Boolean {
        if (module == null) return false
        val method = module.javaClass.methods.firstOrNull { it.name == "Is_itemSelect_open" && it.parameterTypes.isEmpty() }
        return method?.invoke(module) as? Boolean ?: false
    }

    private fun hover(left: Float, top: Float, areaWidth: Float, areaHeight: Float, mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= left && mouseX <= left + areaWidth && mouseY >= top && mouseY <= top + areaHeight
    }

    private fun openButtonX(): Float = x + width - OPEN_BUTTON_WIDTH - 5f

    private fun closeButtonX(): Float = x + width - SIDE_BUTTON_WIDTH - 5f

    private fun menuButtonX(): Float = x + 5f + SIDE_BUTTON_WIDTH

    private fun menuButtonWidth(): Float = (closeButtonX() - menuButtonX()).coerceAtLeast(24f)

    private fun pageControlX(): Float = x + (width - PAGE_CONTROL_WIDTH) / 2f

    private fun pageY(): Float = y + PAGE_OFFSET_Y

    private fun pageLeftX(): Float = pageControlX() - PAGE_BUTTON_WIDTH - 5f

    private fun pageRightX(): Float = pageControlX() + PAGE_CONTROL_WIDTH + 5f


    private fun menuCols(): Int {
        val frameWidth = width.coerceAtLeast(1f)
        val availableGridWidth = frameWidth - MENU_PADDING * 2f
        return floor(availableGridWidth / CELL_WIDTH).toInt().coerceAtLeast(1)
    }

    private fun menuRows(): Int {
        val available = (MENU_HEIGHT - MENU_PADDING * 2f).coerceAtLeast(CELL_HEIGHT)
        return floor(available / CELL_HEIGHT).toInt().coerceAtLeast(1)
    }

    private fun menuPageSize(): Int = (menuCols() * menuRows()).coerceAtLeast(1)

    private fun fitText(context: ANGuiRenderContext, text: String, maxWidth: Float, scale: Float = TEXT_SCALE): String {
        if (maxWidth <= 0f) return ""
        if (context.textWidth(text, scale) <= maxWidth) return text
        val suffix = "..."
        val suffixWidth = context.textWidth(suffix, scale)
        if (suffixWidth > maxWidth) return ""
        var end = text.length
        while (end > 0) {
            val candidate = text.take(end) + suffix
            if (context.textWidth(candidate, scale) <= maxWidth) return candidate
            end--
        }
        return suffix
    }

    private data class RegistryEntry(val id: String, val stack: ItemStack, val translationKey: String)

    private companion object {
        const val CLOSED_HEIGHT = 20f
        const val OPEN_HEIGHT = 140f
        const val SELECTED_PAGE_SIZE = 6
        const val TEXT_SCALE = 0.78f
        const val COMPACT_TEXT_SCALE = 0.58f

        const val CLOSED_BUTTON_HEIGHT = 10f
        const val HEADER_BUTTON_HEIGHT = 15f
        const val CONTENT_TOP = 25f
        const val OPEN_BUTTON_WIDTH = 15f
        const val SIDE_BUTTON_WIDTH = 15f
        const val SELECTED_ROW_HEIGHT = 15f
        const val SELECTED_ROW_GAP = 2f
        const val SELECTED_ITEM_SCALE = 0.7f
        const val MENU_HEIGHT = 100f
        const val MENU_PADDING = 3f

        const val CELL_WIDTH = 18f
        const val CELL_HEIGHT = 18f
        const val CELL_BOX_W = 17f
        const val CELL_BOX_H = 17f
        const val ITEM_SCALE = 0.82f
        const val ITEM_INSET = 1.8f


        const val PAGE_OFFSET_Y = 128f
        const val PAGE_CONTROL_WIDTH = 10f
        const val PAGE_BUTTON_WIDTH = 10f
        const val PAGE_BUTTON_HEIGHT = 13f

        val DELETE: Color = Color(50, 250, 30, 255)
        val DELETE_HOVER: Color = Color(245, 10, 10, 255)

        @Volatile
        private var cachedMenuItems: List<ItemStack>? = null

        @Volatile
        private var cachedItemEntries: List<RegistryEntry>? = null

        @Volatile
        private var cachedBlockEntries: List<RegistryEntry>? = null

        @Volatile
        private var cachedChineseLanguage: ClientLanguage? = null

        @Volatile
        private var cachedEnglishLanguage: ClientLanguage? = null

        fun pageCount(size: Int, pageSize: Int): Int = ceil(size / pageSize.toDouble()).toInt().coerceAtLeast(1)

        private fun menuItems(): List<ItemStack> {
            return cachedMenuItems ?: BuiltInRegistries.ITEM.stream()
                .toList()
                .mapNotNull { item -> safeStack(item) }
                .also { cachedMenuItems = it }
        }

        private fun itemEntries(): List<RegistryEntry> {
            return cachedItemEntries ?: BuiltInRegistries.ITEM.stream()
                .toList()
                .mapNotNull { item: Item ->
                    safeStack(item)?.let { stack ->
                        RegistryEntry(BuiltInRegistries.ITEM.getKey(item).path, stack, item.descriptionId)
                    }
                }
                .also { cachedItemEntries = it }
        }

        private fun blockEntries(): List<RegistryEntry> {
            return cachedBlockEntries ?: BuiltInRegistries.BLOCK.stream()
                .toList()
                .mapNotNull { block: Block ->
                    safeStack(block.asItem())?.let { stack ->
                        RegistryEntry(BuiltInRegistries.BLOCK.getKey(block).path, stack, stack.item.descriptionId)
                    }
                }
                .also { cachedBlockEntries = it }
        }

        private fun safeStack(item: Item): ItemStack? {
            if (item == Items.AIR) return null
            return runCatching { ItemStack(item) }
                .getOrNull()
                ?.takeUnless { it.isEmpty }
        }

        private fun displayName(entry: RegistryEntry): String {
            return if (ANPilotGuiEditor.useChineseNames()) {
                translatedName("zh_cn", entry.translationKey, entry.id)
            } else {
                translatedName("en_us", entry.translationKey, entry.id)
            }
        }

        private fun translatedName(languageCode: String, translationKey: String, fallback: String): String {
            val language = when (languageCode) {
                "zh_cn" -> cachedChineseLanguage ?: loadLanguage(languageCode).also { cachedChineseLanguage = it }
                "en_us" -> cachedEnglishLanguage ?: loadLanguage(languageCode).also { cachedEnglishLanguage = it }
                else -> null
            }
            language?.getOrDefault(translationKey, fallback)?.let { return it }
            if (I18n.exists(translationKey)) return I18n.get(translationKey)
            return fallback
        }

        private fun loadLanguage(languageCode: String): ClientLanguage? {
            return runCatching {
                ClientLanguage.loadFrom(Minecraft.getInstance().resourceManager, listOf(languageCode), false)
            }.getOrNull()
        }
    }
}

