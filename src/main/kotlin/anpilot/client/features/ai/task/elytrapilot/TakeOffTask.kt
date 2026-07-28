package anpilot.client.features.ai.task.elytrapilot

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import anpilot.client.features.ai.utils.AgentUtils
import anpilot.client.features.ai.utils.FireworkUtils
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items

class TakeOffTask(agent: ANAgent) : AITask(agent) {
    private var stage = Stage.PREPARE
    private var ticks = 0
    private var boosted = false

    override fun tick() {
        val player = player ?: run {
            stop()
            return
        }
        if (ANAgent.minecraft.level == null) {
            stop()
            return
        }

        ticks++
        when (stage) {
            Stage.PREPARE -> prepare()
            Stage.ROTATE -> rotate()
            Stage.JUMP -> jump()
            Stage.ACTIVATE -> activate()
            Stage.BOOST -> boost()
            Stage.STABILIZE -> stabilize()
        }
    }

    private fun prepare() {
        val player = player ?: return
        if (player.getItemBySlot(EquipmentSlot.CHEST).item != Items.ELYTRA) {
            AgentUtils.sendMessage("No Elytra equipped")
            finished = true
            return
        }
        if (!AgentUtils.hasFirework()) {
            AgentUtils.sendMessage("No Fireworks")
            finished = true
            return
        }
        if (player.isFallFlying) {
            stage = Stage.STABILIZE
            return
        }
        stage = Stage.ROTATE
    }

    private fun rotate() {
        val player = player ?: return
        agent.rotation.request(player.yRot, -12f)
        stage = Stage.JUMP
    }

    private fun jump() {
        val player = player ?: return
        agent.movement.jump()
        if (!player.onGround()) stage = Stage.ACTIVATE
    }

    private fun activate() {
        val player = player ?: return
        if (player.deltaMovement.y < 0.0) {
            ANAgent.minecraft.connection?.send(ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING))
            stage = Stage.BOOST
        }
        if (ticks > 60) {
            AgentUtils.sendMessage("Takeoff Timeout")
            finished = true
        }
    }

    private fun boost() {
        val player = player ?: return
        if (boosted || !player.isFallFlying) return
        
        boosted = true
        stage = Stage.STABILIZE
    }

    private fun stabilize() {
        val player = player ?: return
        if (player.isFallFlying) {
            
            agent.scheduler.push(EndGlideTask(agent))
            finished = true
            return
        }
        if (player.onGround() || (!player.onGround() && player.deltaMovement.y > -1.0)) {
            finished = true
        }
    }

    override fun stop() {
        agent.movement.stop()
    }

    private enum class Stage { PREPARE, ROTATE, JUMP, ACTIVATE, BOOST, STABILIZE }
}
