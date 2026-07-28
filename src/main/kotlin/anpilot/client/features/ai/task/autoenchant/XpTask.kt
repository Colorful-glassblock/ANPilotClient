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
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.monster.Silverfish
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

class XpTask(agent: ANAgent) : AITask(agent) {
    private val mc = Minecraft.getInstance()
    private val timer = ANTimer()
    private var cooldownMs = 0L
    private var phase = Phase.CLICK_BUTTON
    private var clickedButton = false

    private enum class Phase {
        CLICK_BUTTON,
        WALK_TO_STAND,
        WAIT_LEVEL,
        FINISH
    }

    override fun start() {
        phase = Phase.CLICK_BUTTON
        clickedButton = false
        setCooldown(0)
    }

    override fun tick() {
        if (!timer.passedMs(cooldownMs)) return
        val module = agent.module as? ANAutoEnchant ?: return finish()
        val player = player ?: return

        if (canLeaveXpMachine()) {
            phase = Phase.FINISH
        }

        when (phase) {
            Phase.CLICK_BUTTON -> {
                val buttonPos = module.xpButtonPos ?: return finishWithMessage(module, "CHECK:没有绑定经验机按钮")
                val distance = player.eyePosition.distanceTo(Vec3.atCenterOf(buttonPos))
                if (distance > 3.5) {
                    BaritoneHelper.pathNear(buttonPos, 1)
                    setCooldown(150)
                    return
                }
                BaritoneHelper.cancel()
                if (!clickedButton && player.experienceLevel <= REQUIRED_LEVEL) {
                    interactBlock(buttonPos)
                    clickedButton = true
                    setCooldown(300)
                    return
                }
                phase = Phase.WALK_TO_STAND
            }
            Phase.WALK_TO_STAND -> {
                val standPos = module.xpStandPos ?: return finishWithMessage(module, "CHECK:没有绑定刷经验站立位置")
                if (player.blockPosition() == standPos || player.distanceToSqr(Vec3.atCenterOf(standPos)) <= 1.0) {
                    BaritoneHelper.cancel()
                    phase = Phase.WAIT_LEVEL
                    return
                }
                BaritoneHelper.pathTo(standPos)
                setCooldown(250)
            }
            Phase.WAIT_LEVEL -> {
                if (canLeaveXpMachine()) {
                    phase = Phase.FINISH
                } else {
                    setCooldown(500)
                }
            }
            Phase.FINISH -> {
                agent.scheduler.push(BootTask(agent))
                finish()
            }
        }
    }

    override fun stop() {
        BaritoneHelper.cancel()
    }

    private fun hasSilverfishNearby(): Boolean {
        val player = player ?: return false
        val level = mc.level ?: return false
        val box = AABB(
            player.x - CHECK_RADIUS,
            player.y - CHECK_RADIUS,
            player.z - CHECK_RADIUS,
            player.x + CHECK_RADIUS,
            player.y + CHECK_RADIUS,
            player.z + CHECK_RADIUS
        )
        return level.getEntities(EntityTypeTest.forClass(Silverfish::class.java), box) { true }.isNotEmpty()
    }

    private fun canLeaveXpMachine(): Boolean {
        val player = player ?: return false
        return player.experienceLevel > REQUIRED_LEVEL && !hasSilverfishNearby()
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
        AgentUtils.sendMessage(message)
        finish()
    }

    private fun setCooldown(ms: Long) {
        cooldownMs = ms
        timer.reset()
    }

    private fun finish() {
        finished = true
    }

    private companion object {
        const val REQUIRED_LEVEL = 30
        const val CHECK_RADIUS = 8.0
    }
}
