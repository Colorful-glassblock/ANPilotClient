package anpilot.client.features.module.render

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.api.module.ANModuleState
import anpilot.client.bootstrap.ANServiceRegistry
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.renderer.render.ANRender2DEngine
import anpilot.client.minecraft.duck.ANHandledScreenExt
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ShulkerBoxMenu
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.block.ShulkerBoxBlock
import org.joml.Matrix3x2f
import net.minecraft.client.renderer.state.MapRenderState

class ANDisplayTools : ANBaseModule(
    name = "DisplayTools",
    description = "悬停物品时提供潜影盒容器预览、地图画预览与中键直接打开潜影盒",
    category = ANModuleCategory.RENDER,
    chineseName = "悬浮提示增强",
    defaultState = ANModuleState.ENABLED
) {
    val middleClickOpen = addSetting(ANSetting("MiddleClickOpen", true))
    val storage = addSetting(ANSetting("Storage", true))
    val maps = addSetting(ANSetting("Maps", true))

    companion object {
        @JvmStatic
        fun hasItems(itemStack: ItemStack): Boolean {
            val containerComponent = itemStack.get(DataComponents.CONTAINER)
            return containerComponent != null && containerComponent.nonEmptyItems().iterator().hasNext()
        }

        @JvmStatic
        fun onRenderTooltip(graphics: GuiGraphicsExtractor, screen: AbstractContainerScreen<*>, mouseX: Int, mouseY: Int): Boolean {
            if (!ANServiceRegistry.isInitialized) return false
            val module = ANServiceRegistry.runtime.moduleManager.get("DisplayTools") as? ANDisplayTools ?: return false
            if (!module.enabled) return false

            val hovered = (screen as? ANHandledScreenExt)?.anpilot_getHoveredSlot() ?: return false
            val stack = hovered.getItem()
            if (stack.isEmpty) return false

            val mc = Minecraft.getInstance()

            if (hasItems(stack) && module.storage.value) {
                val container = stack.get(DataComponents.CONTAINER) ?: return false
                val list = container.nonEmptyItems().iterator()
                val items = mutableListOf<ItemStack>()
                while (list.hasNext()) {
                    items.add(list.next().create())
                }

                val width = 162f + 8f
                val height = 54f + 8f
                val windowWidth = mc.window.guiScaledWidth.toFloat()
                val windowHeight = mc.window.guiScaledHeight.toFloat()

                var x = mouseX + 12f
                var y = mouseY - height - 12f
                if (x + width > windowWidth) {
                    x = windowWidth - width - 5f
                }
                if (y < 0f) {
                    y = mouseY + 15f
                }

                val block = (stack.item as? BlockItem)?.block as? ShulkerBoxBlock
                val rawColor = block?.color?.textureDiffuseColor ?: 0xFF905EC6.toInt() 
                val finalColor = (rawColor and 0x00FFFFFF) or 0xFF000000.toInt() 

                ANRender2DEngine.borderedRoundedRect(
                    graphics,
                    x, y,
                    width, height,
                    8f,
                    1.5f,
                    finalColor,
                    0xFF942BC7.toInt()
                )

                val slotColor = 0xFFD6EAE3.toInt()
                for (row in 0 until 3) {
                    for (col in 0 until 9) {
                        val slotX = x + 4f + col * 18f
                        val slotY = y + 4f + row * 18f
                        ANRender2DEngine.roundedRect(
                            graphics,
                            slotX, slotY,
                            16f, 16f,
                            3f,
                            slotColor
                        )
                    }
                }


                val itemScale = 0.8f
                val offset = (16f * (1f - itemScale)) / 2f
                var idx = 0
                for (itemStack in items) {
                    if (idx >= 27) break
                    val row = idx / 9
                    val col = idx % 9
                    val slotX = x + 4f + col * 18f
                    val slotY = y + 4f + row * 18f

                    val oldMatrix = Matrix3x2f(graphics.pose())
                    graphics.pose().translate(slotX + offset, slotY + offset)
                    graphics.pose().scale(itemScale)

                    graphics.item(itemStack, 0, 0)
                    graphics.itemDecorations(mc.font, itemStack, 0, 0)

                    graphics.pose().set(oldMatrix)
                    idx++
                }

                return true
            } else if (stack.item === Items.FILLED_MAP && module.maps.value) {
                val mapId = stack.get(DataComponents.MAP_ID) ?: return false
                val level = mc.level ?: return false
                val mapState = MapItem.getSavedData(mapId, level) ?: return false

                val width = 128f
                val height = 128f
                val windowWidth = mc.window.guiScaledWidth.toFloat()
                val windowHeight = mc.window.guiScaledHeight.toFloat()

                var x = mouseX + 12f
                var y = mouseY - height - 12f
                if (x + width > windowWidth) {
                    x = windowWidth - width - 5f
                }
                if (y < 0f) {
                    y = mouseY + 15f
                }

                ANRender2DEngine.roundedRectWithGlow(
                    graphics,
                    x, y,
                    width, height,
                    4f,
                    1.2f,
                    0xFF000000.toInt(),
                    0xFF585C7A.toInt(),
                    6f,
                    0x4D585C7A.toInt()
                )

                val mapRenderState = MapRenderState()
                mc.mapRenderer.extractRenderState(mapId, mapState, mapRenderState)

                val oldMatrix = Matrix3x2f(graphics.pose())
                graphics.pose().translate(x, y)
                
                graphics.map(mapRenderState)
                
                graphics.pose().set(oldMatrix)

                return true
            }

            return false
        }

        @JvmStatic
        fun onMouseClicked(screen: AbstractContainerScreen<*>, event: MouseButtonEvent): Boolean {
            if (!ANServiceRegistry.isInitialized) return false
            val module = ANServiceRegistry.runtime.moduleManager.get("DisplayTools") as? ANDisplayTools ?: return false
            if (!module.enabled) return false

            if (event.button() == 2) {
                val hovered = (screen as? ANHandledScreenExt)?.anpilot_getHoveredSlot() ?: return false
                val stack = hovered.getItem()
                if (stack.isEmpty) return false

                if (hasItems(stack) && module.middleClickOpen.value) {
                    val mc = Minecraft.getInstance()
                    val player = mc.player ?: return false
                    if (player.containerMenu.carried.isEmpty) {
                        val items = Array(27) { ItemStack.EMPTY }
                        val container = stack.get(DataComponents.CONTAINER)
                        if (container != null) {
                            val list = container.nonEmptyItems().iterator()
                            var idx = 0
                            while (list.hasNext() && idx < 27) {
                                items[idx++] = list.next().create()
                            }
                        }

                        val simpleContainer = SimpleContainer(*items)
                        val shulkerMenu = ShulkerBoxMenu(0, player.inventory, simpleContainer)

                        mc.execute {
                            mc.setScreen(ANPeekScreen(shulkerMenu, player.inventory, stack.hoverName))
                        }
                        return true
                    }
                }
            }
            return false
        }
    }
}

class ANPeekScreen(menu: ShulkerBoxMenu, inventory: Inventory, title: Component) : ShulkerBoxScreen(menu, inventory, title) {
    override fun mouseClicked(event: MouseButtonEvent, isDown: Boolean): Boolean {
        if (isDown && event.button() == 2 ) {
            val hovered = hoveredSlot
            if (hovered != null && !hovered.getItem().isEmpty && Minecraft.getInstance().player?.containerMenu?.carried?.isEmpty == true) {
                val stack = hovered.getItem()
                if (ANServiceRegistry.isInitialized) {
                    val module = ANServiceRegistry.runtime.moduleManager.get("DisplayTools") as? ANDisplayTools
                    if (module != null && module.enabled && module.middleClickOpen.value && ANDisplayTools.hasItems(stack)) {
                        val items = Array(27) { ItemStack.EMPTY }
                        val container = stack.get(DataComponents.CONTAINER)
                        if (container != null) {
                            val list = container.nonEmptyItems().iterator()
                            var idx = 0
                            while (list.hasNext() && idx < 27) {
                                items[idx++] = list.next().create()
                            }
                        }
                        val simpleContainer = SimpleContainer(*items)
                        val menu = ShulkerBoxMenu(0, Minecraft.getInstance().player!!.inventory, simpleContainer)
                        Minecraft.getInstance().setScreen(ANPeekScreen(menu, Minecraft.getInstance().player!!.inventory, stack.hoverName))
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        return false
    }
}
