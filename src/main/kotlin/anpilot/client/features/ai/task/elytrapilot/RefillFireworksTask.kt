package anpilot.client.features.ai.task.elytrapilot

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.BaritoneHelper
import anpilot.client.features.manager.inventory.Inventory as ANInventory
import anpilot.client.features.utility.ANTimer
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ShulkerBoxMenu
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import net.minecraft.world.entity.item.ItemEntity

class RefillFireworksTask(
    agent: ANAgent,
    private val placePos1: BlockPos,
    private val placePos2: BlockPos
) : AITask(agent) {
    private var phase = Phase.PREPARE
    private var pathing = false
    private val actionTimer = ANTimer()
    private val timeoutTimer = ANTimer()
    private var cooldownMs = 0L
    private var shulkerCountBeforePlace = 0
    private var interacted = false
    private var transferredCount = 0
    private var currentPlacePos: BlockPos? = null
    private var triedAlternative = false

    private fun setCooldown(ms: Long) {
        cooldownMs = ms
        actionTimer.reset()
    }

    override fun start() {
        AgentUtils.sendMessage("执行烟花火箭补给")
        agent.movement.paused = true
        agent.movement.stop()
    }

    override fun tick() {
        if (!actionTimer.passedMs(cooldownMs)) {
            return
        }

        when (phase) {
            Phase.PREPARE -> prepare()
            Phase.PLACE_SHULKER -> placeShulker()
            Phase.OPEN_SHULKER -> openShulker()
            Phase.TRANSFER_FIREWORKS -> transferFireworks()
            Phase.CLOSE_SHULKER -> closeContainer(Phase.BREAK_SHULKER)
            Phase.BREAK_SHULKER -> breakShulker()
            Phase.WAIT_SHULKER_PICKUP -> waitShulkerPickup()
            Phase.FINISH -> finishNext()
        }
    }

    override fun stop() {
        BaritoneHelper.cancel()
        agent.rotation.resume()
        player?.closeContainer()
        agent.render.clear()
        ElytraStorageSupport.resetMining()
        ANAgent.minecraft.options.keyUp.setDown(false)
        agent.movement.paused = false
    }

    private fun prepare() {
        if (AgentUtils.agentNullCheck()) return
        if (ElytraStorageSupport.findFireworksShulkerSlot() == ANInventory.INVALID_SLOT) {
            return fail("没有找到装有烟花的潜影盒")
        }
        agent.render.show(placePos1, placePos2)
        phase = Phase.PLACE_SHULKER
        currentPlacePos = placePos1
        triedAlternative = false
        timeoutTimer.reset()
    }

    private fun placeShulker() {
        val target = currentPlacePos ?: placePos1 ?: return
        val slot = ElytraStorageSupport.findFireworksShulkerSlot()
        if (slot == ANInventory.INVALID_SLOT) return fail("没有找到装有烟花的潜影盒")

        shulkerCountBeforePlace = ElytraStorageSupport.countShulkerBoxes()
        ElytraStorageSupport.lookAt(agent, Vec3.atCenterOf(target))

        val placed = ElytraStorageSupport.placeInventoryBlock(agent, slot, target)
        if (placed && ElytraStorageSupport.isShulkerBlock(target)) {
            setCooldown(500)
            timeoutTimer.reset()
            interacted = false
            phase = Phase.OPEN_SHULKER
            return
        }

        if (timeoutTimer.passedMs(3000L)) {
            if (!triedAlternative && placePos2 != null && placePos2 != target) {
                AgentUtils.sendMessage("§e在位置1放置常规补给潜影盒超时，尝试在备用位置2放置...")
                currentPlacePos = placePos2
                triedAlternative = true
                timeoutTimer.reset()
                setCooldown(500)
            } else {
                return fail("补给潜影盒放置失败，两个位置均超时")
            }
        } else {
            setCooldown(250)
        }
    }

    private fun openShulker() {
        val target = currentPlacePos ?: placePos1
        val menu = player?.containerMenu
        if (menu is ShulkerBoxMenu) {
            transferredCount = 0
            phase = Phase.TRANSFER_FIREWORKS
            setCooldown(0)
            return
        }

        if (!interacted) {
            if (!ElytraStorageSupport.isShulkerBlock(target)) {
                if (timeoutTimer.passedMs(3000L)) return fail("补给潜影盒没有正常放出")
                return
            }
            ElytraStorageSupport.lookAt(agent, Vec3.atCenterOf(target))
            ElytraStorageSupport.interactBlock(agent, target)
            interacted = true
            timeoutTimer.reset()
            setCooldown(500)
        } else {
            if (timeoutTimer.passedMs(3000L)) return fail("补给潜影盒无法打开")
        }
    }

    private fun transferFireworks() {
        val player = player ?: return
        val menu = player.containerMenu as? ShulkerBoxMenu ?: run {
            phase = Phase.OPEN_SHULKER
            return
        }

        if (transferredCount >= 4) {
            phase = Phase.CLOSE_SHULKER
            setCooldown(0)
            return
        }

        var targetSlot = -1
        for (s in 0 until ElytraStorageSupport.SHULKER_SIZE) {
            val stack = menu.slots[s].item
            if (!stack.isEmpty && stack.`is`(Items.FIREWORK_ROCKET)) {
                targetSlot = s
                break
            }
        }

        if (targetSlot == -1) {
            AgentUtils.sendMessage("潜影盒内烟花已取空")
            phase = Phase.CLOSE_SHULKER
            setCooldown(0)
            return
        }

        
        val emptySlots = ElytraStorageSupport.emptyInventorySlots()
        if (emptySlots <= 2) {
            AgentUtils.sendMessage("§e背包空间不足（仅剩 $emptySlots 个空位），为保证容纳潜影盒与鞘翅，提前终止常规补给。")
            phase = Phase.CLOSE_SHULKER
            setCooldown(0)
            return
        }

        ElytraStorageSupport.clickQuickMove(targetSlot)
        transferredCount++
        setCooldown(500)
    }

    private fun closeContainer(next: Phase) {
        player?.closeContainer()
        timeoutTimer.reset()
        setCooldown(250)
        phase = next
    }

    private fun breakShulker() {
        val target = currentPlacePos ?: placePos1
        if (!ElytraStorageSupport.isShulkerBlock(target)) {
            phase = Phase.WAIT_SHULKER_PICKUP
            timeoutTimer.reset()
            setCooldown(0)
            return
        }

        val pickaxeSlot = ElytraStorageSupport.findPickaxeSlot()
        if (pickaxeSlot != ANInventory.INVALID_SLOT) {
            ElytraStorageSupport.switchToHotbar(pickaxeSlot)
        }

        ElytraStorageSupport.lookAt(agent, Vec3.atCenterOf(target))
        ElytraStorageSupport.mineBlock(agent, target)
        setCooldown(50)
    }

    private fun waitShulkerPickup() {
        if (ElytraStorageSupport.countShulkerBoxes() >= shulkerCountBeforePlace) {
            if (pathing) {
                BaritoneHelper.cancel()
                pathing = false
            }
            ANAgent.minecraft.options.keyUp.setDown(false)
            phase = Phase.FINISH
            setCooldown(1000)
            return
        }

        if (timeoutTimer.passedMs(6000L)) {
            ANAgent.minecraft.options.keyUp.setDown(false)
            fail("补给潜影盒未回收,停止以避免丢失")
            return
        }

        val droppedShulker = findDroppedShulker()
        if (droppedShulker != null) {
            if (pathing) {
                BaritoneHelper.cancel()
                pathing = false
            }
            agent.rotation.resume()
            ElytraStorageSupport.lookAt(agent, droppedShulker.position())
            ANAgent.minecraft.options.keyUp.setDown(true)
        } else {
            ANAgent.minecraft.options.keyUp.setDown(false)
            val pathTarget = currentPlacePos ?: placePos1
            if (!pathing) {
                pathTo(pathTarget)
            }
        }
    }

    private fun pathTo(pos: BlockPos) {
        agent.rotation.pause()
        pathing = BaritoneHelper.pathTo(pos)
        if (!pathing) agent.rotation.resume()
    }

    private fun finishNext() {
        if (!finished) {
            AgentUtils.sendMessage("烟花补给完成, 继续任务")
            agent.scheduler.push(FindTakeoffPositionTask(agent))
            finished = true
        }
    }

    private fun fail(message: String) {
        stop()
        if (!finished) {
            ElytraStorageSupport.disablePilot(agent)
            finished = true
            val minecraft = Minecraft.getInstance()
            minecraft.connection?.connection?.disconnect(Component.literal("[RefillFireworks] $message"))
        }
    }

    private fun findDroppedShulker(): ItemEntity? {
        val level = ANAgent.minecraft.level ?: return null
        val player = player ?: return null
        var bestEntity: ItemEntity? = null
        var bestDistance = Double.MAX_VALUE
        for (entity in level.entitiesForRendering()) {
            val itemEntity = entity as? ItemEntity ?: continue
            if (ElytraStorageSupport.isShulkerBox(itemEntity.item)) {
                val dist = itemEntity.distanceToSqr(player)
                if (dist < bestDistance) {
                    bestDistance = dist
                    bestEntity = itemEntity
                }
            }
        }
        return bestEntity
    }

    private enum class Phase {
        PREPARE,
        PLACE_SHULKER,
        OPEN_SHULKER,
        TRANSFER_FIREWORKS,
        CLOSE_SHULKER,
        BREAK_SHULKER,
        WAIT_SHULKER_PICKUP,
        FINISH
    }
}
