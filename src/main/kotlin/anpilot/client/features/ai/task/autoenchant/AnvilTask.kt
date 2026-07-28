package anpilot.client.features.ai.task.autoenchant

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.BaritoneHelper
import anpilot.client.features.manager.rotation.RotationUtil
import anpilot.client.features.module.player.ANAutoEnchant
import anpilot.client.features.utility.ANTimer
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

class AnvilTask(agent: ANAgent) : AITask(agent) {
    private val mc = Minecraft.getInstance()
    private val timer = ANTimer()
    private var cooldownMs = 0L
    private var phase = Phase.WALK_TO_ANVIL
    private var interacted = false
    private var activeOperation: Operation? = null
    private var attempts = 0
    private var activeAnvilPos: BlockPos? = null
    private var openAttempts = 0

    private enum class Phase {
        WALK_TO_ANVIL,
        OPEN_ANVIL,
        LOAD_ITEM,
        LOAD_BOOK,
        TAKE_OUTPUT,
        FINISH
    }

    override fun start() {
        phase = Phase.WALK_TO_ANVIL
        interacted = false
        activeOperation = null
        attempts = 0
        activeAnvilPos = null
        openAttempts = 0
        setCooldown(0)
    }

    override fun tick() {
        if (!timer.passedMs(cooldownMs)) return
        val module = agent.module as? ANAutoEnchant ?: return finish()
        val player = player ?: return

        when (phase) {
            Phase.WALK_TO_ANVIL -> {
                val anvilPos = currentAnvil(module) ?: return disableWithMessage(module, "CHECK: 没有可用铁砧，自动附魔暂停")
                if (module.isMissingAnvil(anvilPos)) {
                    return switchToNextAnvil(module, "CHECK:铁砧损坏", true)
                }
                if (player.eyePosition.distanceTo(Vec3.atCenterOf(anvilPos)) <= 3.5) {
                    BaritoneHelper.cancel()
                    phase = Phase.OPEN_ANVIL
                    interacted = false
                    openAttempts = 0
                } else {
                    BaritoneHelper.pathNear(anvilPos, 1)
                    setCooldown(150)
                }
            }
            Phase.OPEN_ANVIL -> {
                val anvilPos = currentAnvil(module) ?: return disableWithMessage(module, "CHECK: 没有可用铁砧，自动附魔已停止")
                if (module.isMissingAnvil(anvilPos)) {
                    return switchToNextAnvil(module, "CHECK:铁砧损坏", true)
                }
                if (player.containerMenu is AnvilMenu) {
                    phase = Phase.LOAD_ITEM
                    return
                }
                if (openAttempts >= MAX_OPEN_ATTEMPTS) {
                    return switchToNextAnvil(module, "CHECK:铁砧损坏", false)
                }
                interactBlock(anvilPos)
                interacted = true
                openAttempts++
                setCooldown(300)
            }
            Phase.LOAD_ITEM -> {
                val menu = player.containerMenu as? AnvilMenu ?: run {
                    phase = Phase.OPEN_ANVIL
                    interacted = false
                    return
                }
                clearInputs(menu)
                val operation = findNextOperation(menu, module)
                if (operation == null) {
                    phase = Phase.FINISH
                    return
                }
                activeOperation = operation
                moveSlotToInput(menu, operation.leftSlot, INPUT_ITEM_SLOT)
                phase = Phase.LOAD_BOOK
                setCooldown(200)
            }
            Phase.LOAD_BOOK -> {
                val menu = player.containerMenu as? AnvilMenu ?: return reopen()
                val operation = activeOperation ?: return reopen()
                moveSlotToInput(menu, operation.rightSlot, INPUT_BOOK_SLOT)
                phase = Phase.TAKE_OUTPUT
                attempts = 0
                setCooldown(250)
            }
            Phase.TAKE_OUTPUT -> {
                val menu = player.containerMenu as? AnvilMenu ?: return reopen()
                val cost = menu.cost
                if (cost > player.experienceLevel) {
                    player.closeContainer()
                    AgentUtils.sendMessage("CHECK:当前附魔需要 $cost 级，玩家等级 ${player.experienceLevel}，开始执行经验机任务")
                    agent.scheduler.push(XpTask(agent))
                    return finish()
                }
                if (menu.slots[OUTPUT_SLOT].item.isEmpty) {
                    attempts++
                    if (attempts >= 8) {
                        clearInputs(menu)
                        activeOperation = null
                        phase = Phase.LOAD_ITEM
                    }
                    setCooldown(250)
                    return
                }
                mc.gameMode?.handleContainerInput(menu.containerId, OUTPUT_SLOT, 0, ContainerInput.QUICK_MOVE, player)
                activeOperation = null
                phase = Phase.LOAD_ITEM
                setCooldown(300)
            }
            Phase.FINISH -> {
                player.closeContainer()
                agent.scheduler.push(StoreTask(agent))
                finish()
            }
        }
    }

    override fun stop() {
        BaritoneHelper.cancel()
        player?.closeContainer()
    }

    private fun findNextOperation(menu: AbstractContainerMenu, module: ANAutoEnchant): Operation? {
        val specs = module.selectedEnchants()
        for (slot in PLAYER_MENU_START until menu.slots.size) {
            val stack = menu.slots[slot].item
            if (!module.isTargetItem(stack)) continue
            for (stage in Order.stagesFor(stack.item, specs)) {
                val missingSpecs = stage.specs(specs).filter { !hasEnchantment(stack, it) }
                if (missingSpecs.isEmpty()) continue

                val combinedBook = findBookWithAll(menu, missingSpecs)
                if (combinedBook != -1) {
                    return Operation(slot, combinedBook)
                }

                if (missingSpecs.size == 1) {
                    val bookSlot = findBookSlot(menu, module, missingSpecs.first())
                    if (bookSlot != -1) return Operation(slot, bookSlot)
                    continue
                }

                val bookMerge = findBookMerge(menu, missingSpecs)
                if (bookMerge != null) {
                    return Operation(bookMerge.first, bookMerge.second)
                }
            }
        }
        return null
    }

    private fun findBookSlot(menu: AbstractContainerMenu, module: ANAutoEnchant, spec: EnchantSpec): Int {
        for (slot in PLAYER_MENU_START until menu.slots.size) {
            val stack = menu.slots[slot].item
            if (module.isMatchingBook(stack, spec)) return slot
        }
        return -1
    }

    private fun hasEnchantment(stack: ItemStack, spec: EnchantSpec): Boolean {
        val enchantments = stack.get(DataComponents.ENCHANTMENTS) ?: return false
        return enchantments.entrySet().any { entry ->
            entry.key.`is`(spec.enchantment) && (spec.level == null || entry.intValue >= spec.level)
        }
    }

    private fun findBookWithAll(menu: AbstractContainerMenu, specs: List<EnchantSpec>): Int {
        for (slot in PLAYER_MENU_START until menu.slots.size) {
            val stack = menu.slots[slot].item
            if (bookSpecs(stack, specs).containsAll(specs)) return slot
        }
        return -1
    }

    private fun findBookMerge(menu: AbstractContainerMenu, specs: List<EnchantSpec>): Pair<Int, Int>? {
        var primarySlot = -1
        var primarySpecs = emptySet<EnchantSpec>()

        for (slot in PLAYER_MENU_START until menu.slots.size) {
            val currentSpecs = bookSpecs(menu.slots[slot].item, specs)
            if (currentSpecs.isNotEmpty() && currentSpecs.size > primarySpecs.size && currentSpecs.size < specs.size) {
                primarySlot = slot
                primarySpecs = currentSpecs
            }
        }

        if (primarySlot == -1) return null
        val missing = specs.filter { it !in primarySpecs }
        for (slot in PLAYER_MENU_START until menu.slots.size) {
            if (slot == primarySlot) continue
            val currentSpecs = bookSpecs(menu.slots[slot].item, missing)
            if (currentSpecs.isNotEmpty()) return primarySlot to slot
        }
        return null
    }

    private fun bookSpecs(stack: ItemStack, specs: List<EnchantSpec>): Set<EnchantSpec> {
        val stored = stack.get(DataComponents.STORED_ENCHANTMENTS) ?: return emptySet()
        val found = LinkedHashSet<EnchantSpec>()
        for (entry in stored.entrySet()) {
            for (spec in specs) {
                if (entry.key.`is`(spec.enchantment) && (spec.level == null || entry.intValue >= spec.level)) {
                    found.add(spec)
                }
            }
        }
        return found
    }

    private fun clearInputs(menu: AbstractContainerMenu) {
        val player = player ?: return
        if (!menu.slots[INPUT_ITEM_SLOT].item.isEmpty) {
            mc.gameMode?.handleContainerInput(menu.containerId, INPUT_ITEM_SLOT, 0, ContainerInput.QUICK_MOVE, player)
        }
        if (!menu.slots[INPUT_BOOK_SLOT].item.isEmpty) {
            mc.gameMode?.handleContainerInput(menu.containerId, INPUT_BOOK_SLOT, 0, ContainerInput.QUICK_MOVE, player)
        }
    }

    private fun moveSlotToInput(menu: AbstractContainerMenu, fromSlot: Int, inputSlot: Int) {
        val player = player ?: return
        mc.gameMode?.handleContainerInput(menu.containerId, fromSlot, 0, ContainerInput.PICKUP, player)
        mc.gameMode?.handleContainerInput(menu.containerId, inputSlot, 0, ContainerInput.PICKUP, player)
        if (!menu.carried.isEmpty) {
            mc.gameMode?.handleContainerInput(menu.containerId, fromSlot, 0, ContainerInput.PICKUP, player)
        }
    }

    private fun reopen() {
        player?.closeContainer()
        phase = Phase.OPEN_ANVIL
        interacted = false
        openAttempts = 0
        setCooldown(250)
    }

    private fun currentAnvil(module: ANAutoEnchant): BlockPos? {
        val anvils = module.availableAnvils()
        if (anvils.isEmpty()) return null
        val active = activeAnvilPos
        if (active != null && active in anvils) return active
        activeAnvilPos = anvils.first()
        return activeAnvilPos
    }

    private fun switchToNextAnvil(module: ANAutoEnchant, message: String, removeCurrent: Boolean) {
        val previous = activeAnvilPos
        if (removeCurrent && previous != null) {
            module.removeAnvil(previous)
        }

        val anvils = module.availableAnvils()
        if (anvils.isEmpty()) {
            return disableWithMessage(module, "没有可用铁砧，自动附魔已停止")
        }

        val next = when {
            previous == null -> anvils.first()
            previous !in anvils -> anvils.first()
            anvils.size > 1 -> anvils[(anvils.indexOf(previous) + 1) % anvils.size]
            else -> return disableWithMessage(module, "$message，且没有其它可用铁砧")
        }

        activeAnvilPos = next
        activeOperation = null
        attempts = 0
        openAttempts = 0
        interacted = false
        player?.closeContainer()
        BaritoneHelper.cancel()
        phase = Phase.WALK_TO_ANVIL
        AgentUtils.sendMessage("$message，切换到铁砧: $next")
        setCooldown(250)
    }

    private fun interactBlock(pos: BlockPos) {
        val p = player ?: return
        val targetVec = Vec3.atCenterOf(pos)
        val rotations = RotationUtil.getRotationsTo(p.eyePosition, targetVec)
        p.yRot = rotations[0]
        p.xRot = rotations[1]
        val hit = BlockHitResult(targetVec, Direction.UP, pos, false)
        mc.gameMode?.useItemOn(p, InteractionHand.MAIN_HAND, hit)
        p.swing(InteractionHand.MAIN_HAND)
    }

    private fun finishWithMessage(module: ANAutoEnchant, message: String) {
        module.sendClientMessage(message)
        finish()
    }

    private fun disableWithMessage(module: ANAutoEnchant, message: String) {
        player?.closeContainer()
        module.disable(message)
        finish()
    }

    private fun setCooldown(ms: Long) {
        cooldownMs = ms
        timer.reset()
    }

    private fun finish() {
        finished = true
    }

    private data class Operation(
        val leftSlot: Int,
        val rightSlot: Int
    )

    private companion object {
        const val INPUT_ITEM_SLOT = 0
        const val INPUT_BOOK_SLOT = 1
        const val OUTPUT_SLOT = 2
        const val PLAYER_MENU_START = 3
        const val MAX_OPEN_ATTEMPTS = 5
    }
}
