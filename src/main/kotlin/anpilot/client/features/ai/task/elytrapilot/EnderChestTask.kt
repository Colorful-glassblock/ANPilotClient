package anpilot.client.features.ai.task.elytrapilot

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.BaritoneHelper
import anpilot.client.features.manager.inventory.Inventory as ANInventory
import anpilot.client.features.utility.ANTimer
import anpilot.client.bootstrap.ANServiceRegistry
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.phys.Vec3

class EnderChestTask(
    agent: ANAgent,
    private val placePos: BlockPos,
    private val alternativePlacePos: BlockPos? = null
) : AITask(agent) {
    private var phase = Phase.PLACE_ENDER_CHEST
    private var actualPlacePos: BlockPos = placePos
    private var triedAlternative = false
    private var shulkerTarget: Shulker? = null
    private var pathing = false
    private val actionTimer = ANTimer()
    private val timeoutTimer = ANTimer()
    private var cooldownMs = 0L
    private var enderChestCountBeforePlace = 0
    private var interacted = false

    private fun setCooldown(ms: Long) {
        cooldownMs = ms
        actionTimer.reset()
    }

    override fun start() {
        AgentUtils.sendMessage("潜影盒已满，开始转入末影箱")
        agent.movement.paused = true
        agent.movement.stop()
        setCooldown(1000)
        triedAlternative = false
        timeoutTimer.reset()
    }

    override fun tick() {
        if (!actionTimer.passedMs(cooldownMs)) {
            return
        }

        when (phase) {
            Phase.PLACE_ENDER_CHEST -> placeEnderChest()
            Phase.OPEN_ENDER_CHEST -> openEnderChest()
            Phase.STORE_SHULKER -> storeFullShulker()
            Phase.CLOSE_ENDER_CHEST -> closeContainer(Phase.BREAK_ENDER_CHEST)
            Phase.BREAK_ENDER_CHEST -> breakEnderChest()
            Phase.WAIT_ENDER_CHEST_PICKUP -> waitEnderChestPickup()
            Phase.FINISH -> finishNext()
        }
    }

    override fun stop() {
        player?.closeContainer()
        agent.rotation.resume()
        agent.render.clear()
        ElytraStorageSupport.resetMining()
        agent.movement.paused = false
    }

    private fun placeEnderChest() {
        val player = player ?: return
        if (!player.onGround()) return
        val enderSlot = ElytraStorageSupport.findEnderChestSlot()
        if (enderSlot == ANInventory.INVALID_SLOT) return fail("没有找到末影箱")
        if (ElytraStorageSupport.findSilkTouchPickaxeSlot() == ANInventory.INVALID_SLOT) {
            return fail("没有精准采集镐，无法安全回收末影箱")
        }

        enderChestCountBeforePlace = ElytraStorageSupport.countEnderChests()
        
        ElytraStorageSupport.lookAt(agent, Vec3.atCenterOf(actualPlacePos))

        val placed = ElytraStorageSupport.placeInventoryBlock(agent, enderSlot, actualPlacePos)
        if (placed && ElytraStorageSupport.isEnderChestBlock(actualPlacePos)) {
            setCooldown(1000) 
            timeoutTimer.reset()
            interacted = false
            phase = Phase.OPEN_ENDER_CHEST
            AgentUtils.sendMessage("校验CHECk")
            return
        }

        if (timeoutTimer.passedMs(3000L)) {
            if (!triedAlternative && alternativePlacePos != null && alternativePlacePos != actualPlacePos) {
                AgentUtils.sendMessage("§e在位置1放置末影箱超时，尝试在备用位置2放置...")
                actualPlacePos = alternativePlacePos
                triedAlternative = true
                timeoutTimer.reset()
                setCooldown(500)
            } else {
                return fail("末影箱放置失败，两个位置均超时")
            }
        } else {
            setCooldown(250)
        }
    }

    private fun openEnderChest() {

        val menu = player?.containerMenu
        if (menu is ChestMenu) {
            phase = Phase.STORE_SHULKER
            setCooldown(0)
            return
        }

        if (!interacted) {
            
            if (!ElytraStorageSupport.isEnderChestBlock(actualPlacePos)) {
                if (timeoutTimer.passedMs(3000L)) return fail("末影箱没有正常放出")
                return
            }

            
            ElytraStorageSupport.lookAt(agent, Vec3.atCenterOf(actualPlacePos))

            
            ElytraStorageSupport.interactBlock(agent, actualPlacePos)
            interacted = true
            timeoutTimer.reset()
            setCooldown(1000) 
        } else {
            if (timeoutTimer.passedMs(3000L)) return fail("末影箱无法打开")
        }
    }

    private fun storeFullShulker() {

        val player = player ?: return fail("玩家为空，无法转移潜影盒")
        val menu = player.containerMenu as? ChestMenu ?: run {
            phase = Phase.OPEN_ENDER_CHEST
            setCooldown(0)
            return
        }
        val containerSize = menu.rowCount * 9
        if (ElytraStorageSupport.containerEmptySlots(menu, containerSize) <= 0) return fail("末影箱已满，无法存入潜影盒")

        val slot = ElytraStorageSupport.findPlayerMenuSlot(menu, containerSize) {
            ElytraStorageSupport.isFullShulkerBox(it)
        }
        if (slot == -1) return fail("未找到已满潜影盒，任务保护离开")

        ElytraStorageSupport.clickQuickMove(slot)
        setCooldown(1000) 
        
        if (ElytraStorageSupport.findPlayerMenuSlot(menu, containerSize) {
                ElytraStorageSupport.isFullShulkerBox(it)
            } == -1 ||
            ElytraStorageSupport.countContainerItems(menu, containerSize, ElytraStorageSupport::isFullShulkerBox) > 0
        ) {
            phase = Phase.CLOSE_ENDER_CHEST
            setCooldown(0)
        }
    }

    private fun closeContainer(next: Phase) {
        player?.closeContainer()
        timeoutTimer.reset()
        setCooldown(250) 
        phase = next
    }

    private fun breakEnderChest() {

        if (!ElytraStorageSupport.isEnderChestBlock(actualPlacePos)) {
            phase = Phase.WAIT_ENDER_CHEST_PICKUP
            timeoutTimer.reset()
            setCooldown(0)
            return
        }

        val silkSlot = ElytraStorageSupport.findSilkTouchPickaxeSlot()
        if (silkSlot == ANInventory.INVALID_SLOT || !ElytraStorageSupport.switchToHotbar(silkSlot)) {
            return fail("无法切换精准采集镐，停止回收末影箱")
        }

        
        ElytraStorageSupport.lookAt(agent, Vec3.atCenterOf(actualPlacePos))

        ElytraStorageSupport.mineBlock(agent, actualPlacePos)
        setCooldown(50) 
    }

    private fun waitEnderChestPickup() {
        if (ElytraStorageSupport.countEnderChests() >= enderChestCountBeforePlace) {
            if (pathing) {
                BaritoneHelper.cancel()
                pathing = false
            }
            phase = Phase.FINISH
            timeoutTimer.reset()
            setCooldown(0)
            AgentUtils.sendMessage("校验CHECk")

            return
        }

        if (timeoutTimer.passedMs(6000L)) { 
            fail("末影箱未回收，停止转存")
            return
        }

        val target = actualPlacePos
        if (!pathing) {
            pathTo(target)
        }
    }

    private fun pathTo(pos: BlockPos) {
        agent.rotation.pause()
        pathing = BaritoneHelper.pathTo(pos)
        if (!pathing) agent.rotation.resume()
    }

    private fun finishNext() {
        if (finished) return
        agent.scheduler.push(FindTakeoffPositionTask(agent))
        finished = true
    }

    private fun fail(message: String) {
        if (!finished) {
            ElytraStorageSupport.disablePilot(agent)
            finished = true
        }
    }

    private enum class Phase {
        PLACE_ENDER_CHEST,
        OPEN_ENDER_CHEST,
        STORE_SHULKER,
        CLOSE_ENDER_CHEST,
        BREAK_ENDER_CHEST,
        WAIT_ENDER_CHEST_PICKUP,
        FINISH
    }
}
