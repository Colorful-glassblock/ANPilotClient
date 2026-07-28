package anpilot.client.features.ai.task.elytrapilot

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.BaritoneHelper
import anpilot.client.features.manager.inventory.Inventory as ANInventory
import anpilot.client.features.utility.ANTimer
import anpilot.client.bootstrap.ANServiceRegistry
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.inventory.ShulkerBoxMenu
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

class StoreElytraTask(agent: ANAgent, private val elytraPos: BlockPos) : AITask(agent) {
    private var phase = Phase.PREPARE
    private var placePos1: BlockPos? = null
    private var placePos2: BlockPos? = null
    private var shulkerTarget: Shulker? = null
    private var pathing = false
    private val actionTimer = ANTimer()
    private val timeoutTimer = ANTimer()
    private var cooldownMs = 0L
    private var shulkerCountBeforePlace = 0
    private var interacted = false
    private var currentPlacePos: BlockPos? = null
    private var triedAlternative = false

    private fun setCooldown(ms: Long) {
        cooldownMs = ms
        actionTimer.reset()
    }

    override fun start() {
        AgentUtils.sendMessage("背包空位不足，开始将鞘翅转入潜影盒")
        agent.movement.paused = true
        agent.movement.stop()
    }

    override fun tick() {
        if (!actionTimer.passedMs(cooldownMs)) {
            return
        }

        when (phase) {
            Phase.PREPARE -> prepare()
            Phase.CLEAR_SHULKER -> clearShulker()
            Phase.PLACE_SHULKER -> placeShulker()
            Phase.OPEN_SHULKER -> openShulker()
            Phase.TRANSFER_ELYTRA -> transferElytra()
            Phase.CLOSE_SHULKER -> closeContainer(Phase.BREAK_SHULKER)
            Phase.BREAK_SHULKER -> breakShulker()
            Phase.WAIT_SHULKER_PICKUP -> waitShulkerPickup()
            Phase.FINISH -> finishAndTakeoff()
        }
    }

    override fun stop() {
        BaritoneHelper.cancel()
        agent.rotation.resume()
        player?.closeContainer()
        agent.render.clear()
        ElytraStorageSupport.resetMining()
        agent.movement.paused = false
    }

    private fun prepare() {
        if (AgentUtils.agentNullCheck()) return
        if (ElytraStorageSupport.findUsableShulkerSlot() == ANInventory.INVALID_SLOT) {
            return fail("没有潜影盒")
        }
        val fronts = ElytraStorageSupport.findChestFrontPositions(elytraPos)

        if (fronts.size == 2) {
            placePos1 = fronts[0]
            placePos2 = fronts[1]
            agent.render.show(fronts[0], fronts[1])
            AgentUtils.sendMessage("确定放置位置: $placePos1，$placePos2")
        }else return

        shulkerTarget = ElytraStorageSupport.findNearestShulker(elytraPos)
        if (shulkerTarget?.isAlive == true) {
            phase = Phase.CLEAR_SHULKER
            return
        }

        phase = Phase.PLACE_SHULKER
        currentPlacePos = placePos1
        triedAlternative = false
        timeoutTimer.reset()
    }

    private fun clearShulker() {
        val target = shulkerTarget?.takeIf { it.isAlive } ?: ElytraStorageSupport.findNearestShulker(elytraPos)?.also {
            shulkerTarget = it
        }

        val player = player ?: return
        if (target == null && player.onGround()) {
            if (pathing) {
                BaritoneHelper.cancel()
                pathing = false
            }
            phase = Phase.PLACE_SHULKER
            setCooldown(500)
            return
        }

        if (pathing) {
            BaritoneHelper.cancel()
            pathing = false
        }

        val killAura = ANServiceRegistry.runtime.moduleManager.get("KillAura")
        if (killAura?.enabled == true) {
            setCooldown(200)
        } else {
            ElytraStorageSupport.lookAt(agent, target!!.eyePosition)

            ANAgent.minecraft.gameMode?.attack(player, target)
            player.swing(InteractionHand.MAIN_HAND)
            setCooldown(400) 
        }
    }


    private fun placeShulker() {
        val target = currentPlacePos ?: placePos1 ?: return
        
        val shulker = ElytraStorageSupport.findNearestShulker(elytraPos)
        if (shulker?.isAlive == true) {
            if (pathing) {
                BaritoneHelper.cancel()
                pathing = false
            }
            shulkerTarget = shulker
            phase = Phase.CLEAR_SHULKER
            setCooldown(0)
            return
        }

        val slot = ElytraStorageSupport.findUsableShulkerSlot()
        if (slot == ANInventory.INVALID_SLOT) return fail("没有潜影盒")

        shulkerCountBeforePlace = ElytraStorageSupport.countShulkerBoxes()

        ElytraStorageSupport.lookAt(agent, Vec3.atCenterOf(target))

        val placed = ElytraStorageSupport.placeInventoryBlock(agent, slot, target)
        if (placed && ElytraStorageSupport.isShulkerBlock(target)) {
            setCooldown(500)
            timeoutTimer.reset()
            interacted = false
            phase = Phase.OPEN_SHULKER
            AgentUtils.sendMessage("校验CHECk")
            return
        }

        if (timeoutTimer.passedMs(3000L)) {
            if (!triedAlternative && placePos2 != null && placePos2 != target) {
                AgentUtils.sendMessage("放置超时，尝试备用位置放置")
                currentPlacePos = placePos2
                triedAlternative = true
                timeoutTimer.reset()
                setCooldown(500)
            } else {
                return fail("潜影盒放置失败，两个位置均超时")
            }
        } else {
            setCooldown(250)
        }
    }

    private fun openShulker() {
        val target = currentPlacePos ?: placePos1 ?: return fail("潜影盒未放置成功")

        val menu = player?.containerMenu
        if (menu is ShulkerBoxMenu) {
            phase = Phase.TRANSFER_ELYTRA
            setCooldown(0)
            return
        }

        if (!interacted) {
            
            if (!ElytraStorageSupport.isShulkerBlock(target)) {
                if (timeoutTimer.passedMs(3000L)) return fail("潜影盒没有正常放出")
                return
            }

            
            ElytraStorageSupport.lookAt(agent, Vec3.atCenterOf(target))

            
            ElytraStorageSupport.interactBlock(agent, target)
            interacted = true
            timeoutTimer.reset()
            setCooldown(500) 
        } else {
            if (timeoutTimer.passedMs(3000L)) return fail("潜影盒无法打开")
        }
    }

    private fun transferElytra() {

        val player = player ?: return
        val menu = player.containerMenu as? ShulkerBoxMenu ?: run {
            phase = Phase.OPEN_SHULKER
            return
        }

        if (ElytraStorageSupport.containerEmptySlots(menu, ElytraStorageSupport.SHULKER_SIZE) <= 0 ||
            ElytraStorageSupport.countInventoryElytra() <= 3
        ) {
            phase = Phase.CLOSE_SHULKER
            AgentUtils.sendMessage("校验CHECk")
            return
        }

        var bestSlot = -1
        var maxDamage = -1

        for (s in ElytraStorageSupport.SHULKER_SIZE until menu.slots.size) {
            val stack = menu.slots[s].item
            if (!stack.isEmpty && stack.`is`(Items.ELYTRA)) {
                val dmg = stack.damageValue
                if (dmg > maxDamage) {
                    maxDamage = dmg
                    bestSlot = s
                }
            }
        }

        val slot = bestSlot
        if (slot == -1) {
            phase = Phase.CLOSE_SHULKER
            AgentUtils.sendMessage("校验CHECk")
            setCooldown(0)
            return
        }

        ElytraStorageSupport.clickQuickMove(slot)
        setCooldown(500) 
    }

    private fun closeContainer(next: Phase) {
        player?.closeContainer()
        timeoutTimer.reset()
        setCooldown(250) 
        phase = next
    }

    private fun breakShulker() {
        val target = currentPlacePos ?: placePos1 ?: return
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
            timeoutTimer.reset()
            if (ElytraStorageSupport.findFullShulkerSlot() != ANInventory.INVALID_SLOT) {
                val enderPlacePos = if (currentPlacePos == placePos2) placePos1!! else placePos2!!
                val altPlacePos = if (currentPlacePos == placePos2) placePos2!! else placePos1!!
                agent.scheduler.push(EnderChestTask(agent, enderPlacePos, altPlacePos))
                finished = true
            } else if (ElytraStorageSupport.countInventoryElytra() > 0) {
                phase = Phase.FINISH
                setCooldown(1000)
                AgentUtils.sendMessage("校验CHECk")

            }
            return
        }

        if (timeoutTimer.passedMs(6000L)) { 
            fail("潜影盒未回收，停止以避免丢失")
            return
        }

        val pathTarget = currentPlacePos ?: placePos1
        if (pathTarget != null && !pathing) {
            pathTo(pathTarget)
        }
    }

    private fun pathTo(pos: BlockPos) {
        agent.rotation.pause()
        pathing = BaritoneHelper.pathTo(pos)
        if (!pathing) agent.rotation.resume()
    }

    private fun finishAndTakeoff() {
        if (!finished) {
            AgentUtils.sendMessage("鞘翅转存完成，继续任务")
            val fireworks = countFireworks()
            if (fireworks < 30 ) {
                AgentUtils.sendMessage("烟花不足，执行烟花补给")
                agent.scheduler.push(RefillFireworksTask(agent, placePos1!!, placePos2!!))
            } else {
                agent.scheduler.push(FindTakeoffPositionTask(agent))
            }
            finished = true
        }
    }

    private fun countFireworks(): Int {
        val player = ANAgent.minecraft.player ?: return 0
        var count = 0
        val inventory = player.inventory
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (stack.item == Items.FIREWORK_ROCKET) count += stack.count
        }
        return count
    }

    private fun fail(message: String) {
        stop()
        if (!finished) {
            ElytraStorageSupport.disablePilot(agent)
            finished = true
            val minecraft = Minecraft.getInstance()
            minecraft.connection?.connection?.disconnect(Component.literal("[ANElytraPilotPlus] $message"))

        }
    }

    private enum class Phase {
        PREPARE,
        CLEAR_SHULKER,
        PLACE_SHULKER,
        OPEN_SHULKER,
        TRANSFER_ELYTRA,
        CLOSE_SHULKER,
        BREAK_SHULKER,
        WAIT_SHULKER_PICKUP,
        FINISH
    }
}
