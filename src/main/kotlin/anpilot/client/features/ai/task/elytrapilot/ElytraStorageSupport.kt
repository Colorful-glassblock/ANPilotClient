package anpilot.client.features.ai.task.elytrapilot

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.BaritoneHelper
import anpilot.client.features.manager.inventory.Inventory as ANInventory
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.ItemTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ShulkerBoxBlock
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

internal object ElytraStorageSupport {
    const val SHULKER_SIZE = 27
    const val SHULKER_SEARCH_RADIUS = 5.0
    const val VERIFY_TIMEOUT = 60
    const val PICKUP_TIMEOUT = 120
    const val MAX_ROUNDS = 4

    fun disablePilot(agent: ANAgent) {
        ANAgent.minecraft.player?.closeContainer()
        BaritoneHelper.cancel()
        agent.rotation.resume()
        agent.module.disable()
    }

    fun placeInventoryBlock(agent: ANAgent, inventorySlot: Int, pos: BlockPos): Boolean {
        ANAgent.minecraft.player?.closeContainer()
        if (!switchToHotbar(inventorySlot)) return false
        val player = ANAgent.minecraft.player ?: return false
        val hit = supportHitResult(pos) ?: return false
        lookAt(agent, Vec3.atCenterOf(pos))
        ANAgent.minecraft.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, hit)
        player.swing(InteractionHand.MAIN_HAND)
        return true
    }

    fun switchToHotbar(inventorySlot: Int): Boolean {
        val player = ANAgent.minecraft.player ?: return false
        val hotbarSlot = when (inventorySlot) {
            in 0 until ANInventory.HOTBAR_SIZE -> inventorySlot
            else -> {
                val selected = player.inventory.selectedSlot
                if (player.containerMenu !is InventoryMenu) player.closeContainer()
                if (!ANInventory.swapInventorySlot(inventorySlot, selected)) return false
                selected
            }
        }
        return ANInventory.switchTo(hotbarSlot)
    }

    fun interactBlock(agent: ANAgent, pos: BlockPos) {
        val player = ANAgent.minecraft.player ?: return
        lookAt(agent, Vec3.atCenterOf(pos))
        ANAgent.minecraft.gameMode?.useItemOn(
            player,
            InteractionHand.MAIN_HAND,
            BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
        )
        player.swing(InteractionHand.MAIN_HAND)
    }

    private var miningPos: BlockPos? = null

    fun mineBlock(agent: ANAgent, pos: BlockPos) {
        val player = ANAgent.minecraft.player ?: return
        val level = ANAgent.minecraft.level ?: return
        if (miningPos != null && level.getBlockState(miningPos!!).isAir) {
            miningPos = null
        }
        lookAt(agent, Vec3.atCenterOf(pos))
        val gameMode = ANAgent.minecraft.gameMode ?: return
        if (miningPos != pos) {
            gameMode.startDestroyBlock(pos, Direction.UP)
            miningPos = pos
        } else {
            gameMode.continueDestroyBlock(pos, Direction.UP)
        }
        player.swing(InteractionHand.MAIN_HAND)
    }

    fun mineBlock(pos: BlockPos) {
        val player = ANAgent.minecraft.player ?: return
        val level = ANAgent.minecraft.level ?: return
        if (miningPos != null && level.getBlockState(miningPos!!).isAir) {
            miningPos = null
        }
        val gameMode = ANAgent.minecraft.gameMode ?: return
        if (miningPos != pos) {
            gameMode.startDestroyBlock(pos, Direction.UP)
            miningPos = pos
        } else {
            gameMode.continueDestroyBlock(pos, Direction.UP)
        }
        player.swing(InteractionHand.MAIN_HAND)
    }

    fun resetMining() {
        miningPos = null
    }

    fun supportHitResult(pos: BlockPos): BlockHitResult? {
        val level = ANAgent.minecraft.level ?: return null
        if (!level.getBlockState(pos).canBeReplaced()) return null

        val below = pos.below()
        if (!level.getBlockState(below).isAir && !level.getBlockState(below).canBeReplaced()) {
            return BlockHitResult(Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5), Direction.UP, below, false)
        }

        for (direction in Direction.entries) {
            val neighbor = pos.relative(direction.opposite)
            val state = level.getBlockState(neighbor)
            if (state.isAir || state.canBeReplaced()) continue
            return BlockHitResult(Vec3.atCenterOf(neighbor), direction, neighbor, false)
        }
        return null
    }

    fun findTargetFrame(elytraPos: BlockPos): ItemFrame? {
        val level = ANAgent.minecraft.level ?: return null
        for (entity in level.entitiesForRendering()) {
            val frame = entity as? ItemFrame ?: continue
            if (frame.blockPosition() == elytraPos && frame.item.`is`(Items.ELYTRA)) return frame
        }
        return null
    }

    fun findNearestShulker(elytraPos: BlockPos): Shulker? {
        val level = ANAgent.minecraft.level ?: return null
        var best: Shulker? = null
        var bestDistance = Double.MAX_VALUE
        val center = Vec3.atCenterOf(elytraPos)
        for (entity in level.entitiesForRendering()) {
            val shulker = entity as? Shulker ?: continue
            if (!shulker.isAlive) continue
            val distance = shulker.position().distanceToSqr(center)
            if (distance <= SHULKER_SEARCH_RADIUS * SHULKER_SEARCH_RADIUS && distance < bestDistance) {
                best = shulker
                bestDistance = distance
            }
        }
        return best
    }

    fun fallbackPlacePos(elytraPos: BlockPos): BlockPos? {
        val frame = findTargetFrame(elytraPos)
        val preferred = frame?.blockPosition()?.relative(frame.direction.opposite)
        if (preferred != null && supportHitResult(preferred) != null) return preferred

        for (direction in Direction.Plane.HORIZONTAL) {
            val pos = elytraPos.relative(direction)
            if (supportHitResult(pos) != null) return pos
        }
        return null
    }

    fun findShulkerPlacePos(playerPos: BlockPos, elytraPos: BlockPos): BlockPos {
        val fronts = findChestFrontPositions(elytraPos)
        if (fronts.isNotEmpty()) {
            return fronts[0]
        }
        return findFallbackPlacePos(playerPos, elytraPos)
    }

    fun findEnderChestPlacePos(playerPos: BlockPos, elytraPos: BlockPos, shulkerPlacePos: BlockPos?): BlockPos {
        val fronts = findChestFrontPositions(elytraPos)
        if (fronts.size >= 2) {
            if (shulkerPlacePos != null && fronts[0] == shulkerPlacePos) {
                return fronts[1]
            }
            return fronts[0]
        } else if (fronts.size == 1) {
            if (shulkerPlacePos != null && fronts[0] == shulkerPlacePos) {
                return findFallbackPlacePosForEnder(playerPos, elytraPos, shulkerPlacePos)
            }
            return fronts[0]
        }
        return findFallbackPlacePosForEnder(playerPos, elytraPos, shulkerPlacePos ?: BlockPos.ZERO)
    }

    private fun findEndShipChestPositions(center: BlockPos): List<BlockPos> {
        val level = ANAgent.minecraft.level ?: return emptyList()
        val list = mutableListOf<BlockPos>()
        for (y in -2..2) {
            for (x in -4..4) {
                for (z in -4..4) {
                    val pos = center.offset(x, y, z)
                    val state = level.getBlockState(pos)
                    if (state.`is`(Blocks.CHEST)) {
                        list.add(pos)
                    }
                }
            }
        }
        return list.sortedBy { it.distSqr(center) }
    }

    internal fun findChestFrontPositions(elytraPos: BlockPos): List<BlockPos> {
        val chests = findEndShipChestPositions(elytraPos)
        val level = ANAgent.minecraft.level ?: return emptyList()
        val fronts = mutableListOf<BlockPos>()
        for (chestPos in chests) {
            val state = level.getBlockState(chestPos)
            val facing = state.getOptionalValue(ChestBlock.FACING).orElse(null) ?: Direction.SOUTH
            val front = chestPos.relative(facing)
            if (level.getBlockState(front).isAir) {
                fronts.add(front)
            }
        }
        return fronts
    }

    internal fun findFallbackPlacePos(playerPos: BlockPos, elytraPos: BlockPos): BlockPos {
        val level = ANAgent.minecraft.level ?: return playerPos.relative(Direction.NORTH)
        val frame = findTargetFrame(elytraPos)
        val facing = frame?.direction ?: Direction.SOUTH
        val candidates = listOf(
            playerPos.relative(facing.clockWise),
            playerPos.relative(facing.counterClockWise),
            playerPos.relative(facing)
        )
        for (pos in candidates) {
            if (level.getBlockState(pos).isAir && supportHitResult(pos) != null) {
                return pos
            }
        }
        for (direction in Direction.Plane.HORIZONTAL) {
            val pos = playerPos.relative(direction)
            if (level.getBlockState(pos).isAir && supportHitResult(pos) != null) {
                return pos
            }
        }
        return playerPos.relative(facing)
    }

    internal fun findFallbackPlacePosForEnder(playerPos: BlockPos, elytraPos: BlockPos, exclude: BlockPos): BlockPos {
        val level = ANAgent.minecraft.level ?: return playerPos.relative(Direction.SOUTH)
        val frame = findTargetFrame(elytraPos)
        val facing = frame?.direction ?: Direction.SOUTH
        val candidates = listOf(
            playerPos.relative(facing.clockWise),
            playerPos.relative(facing.counterClockWise),
            playerPos.relative(facing)
        )
        for (pos in candidates) {
            if (pos == exclude) continue
            if (level.getBlockState(pos).isAir && supportHitResult(pos) != null) {
                return pos
            }
        }
        for (direction in Direction.Plane.HORIZONTAL) {
            val pos = playerPos.relative(direction)
            if (pos == exclude) continue
            if (level.getBlockState(pos).isAir && supportHitResult(pos) != null) {
                return pos
            }
        }
        return playerPos.relative(facing)
    }

    fun findStandPos(pos: BlockPos): BlockPos {
        if (canStandAt(pos)) return pos
        for (direction in Direction.Plane.HORIZONTAL) {
            val candidate = pos.relative(direction)
            if (canStandAt(candidate)) return candidate
        }
        return ANAgent.minecraft.player?.blockPosition() ?: pos
    }

    private fun canStandAt(pos: BlockPos): Boolean {
        val level = ANAgent.minecraft.level ?: return false
        return !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty &&
                level.getBlockState(pos).getCollisionShape(level, pos).isEmpty &&
                level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty
    }

    fun clickQuickMove(slot: Int): Boolean {
        val player = ANAgent.minecraft.player ?: return false
        val gameMode = ANAgent.minecraft.gameMode ?: return false
        gameMode.handleContainerInput(player.containerMenu.containerId, slot, 0, ContainerInput.QUICK_MOVE, player)
        return true
    }

    fun findPlayerMenuSlot(menu: AbstractContainerMenu, firstPlayerSlot: Int, predicate: (ItemStack) -> Boolean): Int {
        for (slot in firstPlayerSlot until menu.slots.size) {
            val stack = menu.slots[slot].item
            if (!stack.isEmpty && predicate(stack)) return slot
        }
        return -1
    }

    fun containerEmptySlots(menu: AbstractContainerMenu, containerSize: Int): Int {
        var count = 0
        for (slot in 0 until containerSize) {
            if (menu.slots[slot].item.isEmpty) count++
        }
        return count
    }

    fun countContainerItems(menu: AbstractContainerMenu, containerSize: Int, predicate: (ItemStack) -> Boolean): Int {
        var count = 0
        for (slot in 0 until containerSize) {
            val stack = menu.slots[slot].item
            if (!stack.isEmpty && predicate(stack)) count += stack.count
        }
        return count
    }

    fun countInventoryElytra(): Int {
        var count = 0
        forInventoryStack { stack ->
            if (stack.`is`(Items.ELYTRA)) count += stack.count
        }
        return count
    }

    fun emptyInventorySlots(): Int {
        var count = 0
        forInventoryStack { stack ->
            if (stack.isEmpty) count++
        }
        return count
    }

    fun findUsableShulkerSlot(): Int = findInventorySlot { isShulkerBox(it) && shulkerFreeSlots(it) > 0 && !shulkerHasFireworks(it) }

    fun findFullShulkerSlot(): Int = findInventorySlot(::isFullShulkerBox)

    fun shulkerHasFireworks(stack: ItemStack): Boolean {
        if (!isShulkerBox(stack)) return false
        val contents = stack.get(DataComponents.CONTAINER) ?: ItemContainerContents.EMPTY
        return contents.nonEmptyItemCopyStream().anyMatch { it.`is`(Items.FIREWORK_ROCKET) }
    }

    fun findFireworksShulkerSlot(): Int = findInventorySlot { shulkerHasFireworks(it) }

    fun findEnderChestSlot(): Int = findInventorySlot { it.`is`(Items.ENDER_CHEST) }

    fun findSilkTouchPickaxeSlot(): Int =
        findInventorySlot { it.`is`(ItemTags.PICKAXES) && ANInventory.hasEnchantment(it, Enchantments.SILK_TOUCH) }

    fun findPickaxeSlot(): Int =
        findInventorySlot { it.`is`(ItemTags.PICKAXES) }

    fun countShulkerBoxes(): Int = countInventoryItems(::isShulkerBox)

    fun countEnderChests(): Int = countInventoryItems { it.`is`(Items.ENDER_CHEST) }

    fun isShulkerBlock(pos: BlockPos): Boolean =
        ANAgent.minecraft.level?.getBlockState(pos)?.block is ShulkerBoxBlock

    fun isEnderChestBlock(pos: BlockPos): Boolean =
        ANAgent.minecraft.level?.getBlockState(pos)?.`is`(Blocks.ENDER_CHEST) == true

    fun isShulkerBox(stack: ItemStack): Boolean {
        val item = stack.item as? BlockItem ?: return false
        return item.block is ShulkerBoxBlock
    }

    fun isFullShulkerBox(stack: ItemStack): Boolean =
        isShulkerBox(stack) && shulkerFreeSlots(stack) <= 0

    fun shulkerFreeSlots(stack: ItemStack): Int =
        SHULKER_SIZE - shulkerUsedSlots(stack)

    private fun shulkerUsedSlots(stack: ItemStack): Int {
        val contents = stack.get(DataComponents.CONTAINER) ?: ItemContainerContents.EMPTY
        return contents.nonEmptyItemCopyStream().count().toInt()
    }

    fun lookAt(agent: ANAgent, pos: Vec3) {
        val player = ANAgent.minecraft.player ?: return
        val dx = pos.x - player.x
        val dz = pos.z - player.z
        val dy = pos.y - player.eyeY
        val horizontal = sqrt(dx * dx + dz * dz)
        val yaw = (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
        val pitch = (-Math.toDegrees(atan2(dy, horizontal))).toFloat()
        agent.rotation.request(yaw, pitch)
    }

    private fun findInventorySlot(predicate: (ItemStack) -> Boolean): Int {
        val player = ANAgent.minecraft.player ?: return ANInventory.INVALID_SLOT
        for (slot in 0 until ANInventory.MAIN_SIZE) {
            val stack = player.inventory.getItem(slot)
            if (!stack.isEmpty && predicate(stack)) return slot
        }
        return ANInventory.INVALID_SLOT
    }

    private fun countInventoryItems(predicate: (ItemStack) -> Boolean): Int {
        var count = 0
        forInventoryStack { stack ->
            if (!stack.isEmpty && predicate(stack)) count += stack.count
        }
        return count
    }

    private inline fun forInventoryStack(action: (ItemStack) -> Unit) {
        val player = ANAgent.minecraft.player ?: return
        for (slot in 0 until ANInventory.MAIN_SIZE) {
            action(player.inventory.getItem(slot))
        }
    }
}
