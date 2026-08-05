package anpilot.client.features.module.combat

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.bootstrap.ANServiceRegistry
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.GameLeftEvent
import anpilot.client.features.manager.ANFriendManager
import anpilot.client.features.manager.rotation.RotateMode
import anpilot.client.features.manager.rotation.Rotation
import anpilot.client.features.manager.rotation.RotationPriority
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

class ANBowAim : ANBaseModule(
    name = "BowAim",
    description = "自动蓄力并静默预瞄玩家后释放弓箭",
    category = ANModuleCategory.COMBAT,
    chineseName = "弓箭预瞄"
) {
    val range = addSetting(ANSetting("Range", 80.0f, 8.0f, 160.0f))
    val fov = addSetting(ANSetting("Fov", 120.0f, 5.0f, 180.0f))
    val rotate = addSetting(ANSetting("Rotate", RotateMode.SILENT))
    val autoCharge = addSetting(ANSetting("AutoCharge", true))
    val releasePower = addSetting(ANSetting("ReleasePower", 0.98f, 0.1f, 1.0f))
    val aimHeight = addSetting(ANSetting("AimHeight", 0.62f, 0.1f, 1.0f))
    val maxPredictTicks = addSetting(ANSetting("PredictTicks", 8.0f, 0.0f, 20.0f))

    private var autoUsingBow = false
    private var releaseCooldownTicks = 0

    override fun onDisable() {
        stopAutoUse()
        releaseCooldownTicks = 0
    }

    @ANEventHandler
    fun onGameLeft(event: GameLeftEvent) {
        disable()
    }

    override fun onTick() {
        val player = mc.player ?: return
        val level = mc.level ?: return
        if (releaseCooldownTicks > 0) {
            releaseCooldownTicks--
            stopAutoUse()
            return
        }
        if (player.isSpectator || player.isCreative || shouldPauseAim()) {
            stopAutoUse()
            return
        }
        if (!hasArrow(player)) {
            stopAutoUse()
            disable("没有箭矢")
            return
        }

        val target = findTarget(player) ?: run {
            stopAutoUse()
            return
        }
        if (!isUsingBow(player) && !tryStartUsingBow(player)) return
        val rotation = calculateBowRotation(player, target)

        if (rotate.value == RotateMode.SILENT) {
            ANServiceRegistry.runtime.rotationManager.requestRotation(
                rotation = rotation,
                priority = RotationPriority.COMBAT,
                owner = "BowAim"
            )
        }

        if (!target.isAlive) return
        if (BowItem.getPowerForTime(player.getTicksUsingItem()) < releasePower.value) return

        when (rotate.value) {
            RotateMode.OFF -> if (!isVisuallyAimed(player, rotation)) return
            RotateMode.SILENT -> if (!ANServiceRegistry.runtime.rotationManager.isRotationReached(rotation)) {
                ANServiceRegistry.runtime.rotationManager.sendInstantRotation(rotation)
            }
            RotateMode.GRIM -> {
                ANServiceRegistry.runtime.rotationManager.sendInstantRotation(rotation)
            }
        }

        releaseBow(player)

        if (rotate.value == RotateMode.GRIM) {
            ANServiceRegistry.runtime.rotationManager.sendInstantRotation(
                Rotation(player.yRot, player.xRot),
                mouseSensitivityFix = false
            )
        }
    }

    private fun isUsingBow(player: Player): Boolean {
        return player.isUsingItem && player.getUseItem().item == Items.BOW
    }

    private fun tryStartUsingBow(player: Player): Boolean {
        if (!autoCharge.value || player.mainHandItem.item != Items.BOW) return false
        mc.options.keyUse.isDown = true
        autoUsingBow = true
        mc.gameMode?.useItem(player, InteractionHand.MAIN_HAND)
        player.startUsingItem(InteractionHand.MAIN_HAND)
        return isUsingBow(player)
    }

    private fun stopAutoUse() {
        if (!autoUsingBow) return
        mc.options.keyUse.isDown = false
        autoUsingBow = false
    }

    private fun shouldPauseAim(): Boolean {
        return mc.options.keySprint.isDown
    }

    private fun hasArrow(player: Player): Boolean {
        if (isArrow(player.offhandItem)) return true
        val inventory = player.inventory
        for (slot in 0 until inventory.containerSize) {
            if (isArrow(inventory.getItem(slot))) return true
        }
        return false
    }

    private fun isArrow(stack: net.minecraft.world.item.ItemStack): Boolean {
        return stack.`is`(Items.ARROW) ||
            stack.`is`(Items.TIPPED_ARROW) ||
            stack.`is`(Items.SPECTRAL_ARROW)
    }

    private fun findTarget(player: Player): Player? {
        val level = mc.level ?: return null
        val maxDistance = range.value.toDouble()
        val maxDistanceSq = maxDistance * maxDistance
        val maxFov = fov.value
        val eye = player.eyePosition

        return level.players()
            .asSequence()
            .filter { it !== player && it.isAlive && !it.isSpectator && !it.isCreative }
            .filter { !ANFriendManager.isFriend(it.name.string) }
            .filter { it.boundingBox.distanceToSqr(eye) <= maxDistanceSq }
            .mapNotNull { target ->
                val rotation = calculateBowRotation(player, target)
                val fovToTarget = Rotation(player).fov(rotation)
                if (fovToTarget > maxFov) return@mapNotNull null
                val point = predictedAimPoint(player, target, BowItem.getPowerForTime(player.getTicksUsingItem()).coerceAtLeast(0.1f))
                if (!canSeePoint(player, point)) return@mapNotNull null
                TargetScore(target, player.distanceToSqr(target) + fovToTarget * 0.15)
            }
            .minByOrNull { it.score }
            ?.player
    }

    private fun calculateBowRotation(player: Player, target: Player): Rotation {
        val power = BowItem.getPowerForTime(player.getTicksUsingItem()).coerceAtLeast(0.1f)
        val speed = ARROW_SPEED * power
        val eye = player.eyePosition
        val targetPoint = predictedAimPoint(player, target, power)
        val diff = targetPoint.subtract(eye)
        val horizontal = hypot(diff.x, diff.z)
        val yaw = Mth.wrapDegrees(Math.toDegrees(atan2(diff.z, diff.x)).toFloat() - 90.0f)
        val pitch = ballisticPitch(horizontal, diff.y, speed) ?: directPitch(horizontal, diff.y)
        return Rotation(yaw, pitch).wrap()
    }

    private fun predictedAimPoint(player: Player, target: Player, power: Float): Vec3 {
        val box = target.boundingBox
        val base = Vec3(target.x, target.y + box.ysize * aimHeight.value, target.z)
        val speed = (ARROW_SPEED * power).coerceAtLeast(0.1)
        var predicted = base

        repeat(PREDICTION_ITERATIONS) {
            val diff = predicted.subtract(player.eyePosition)
            val horizontal = hypot(diff.x, diff.z)
            val flightTicks = (horizontal / speed).coerceIn(0.0, maxPredictTicks.value.toDouble())
            predicted = base.add(target.deltaMovement.scale(flightTicks))
        }

        return predicted
    }

    private fun ballisticPitch(horizontal: Double, vertical: Double, speed: Double): Float? {
        if (horizontal < 0.001 || speed <= 0.0) return directPitch(horizontal, vertical)

        val speedSq = speed * speed
        val discriminant = speedSq * speedSq - ARROW_GRAVITY * (ARROW_GRAVITY * horizontal * horizontal + 2.0 * vertical * speedSq)
        if (discriminant < 0.0) return null

        val lowArc = atan((speedSq - sqrt(discriminant)) / (ARROW_GRAVITY * horizontal))
        return -Math.toDegrees(lowArc).toFloat()
    }

    private fun directPitch(horizontal: Double, vertical: Double): Float {
        return -Math.toDegrees(atan2(vertical, horizontal)).toFloat()
    }

    private fun canSeePoint(player: Player, point: Vec3): Boolean {
        val level = mc.level ?: return false
        val hit = level.clip(
            ClipContext(
                player.eyePosition,
                point,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
            )
        )
        return hit.type == HitResult.Type.MISS || hit.location.distanceToSqr(point) < 0.09
    }

    private fun isVisuallyAimed(player: Player, rotation: Rotation): Boolean {
        return Rotation(player).fov(rotation) <= AIM_EPSILON_DEGREES
    }

    private fun releaseBow(player: Player) {
        stopAutoUse()
        mc.gameMode?.releaseUsingItem(player)
        if (player.isUsingItem) player.stopUsingItem()
        releaseCooldownTicks = RELEASE_COOLDOWN_TICKS
    }

    private data class TargetScore(val player: Player, val score: Double)

    private companion object {
        private const val ARROW_SPEED = 3.0
        private const val ARROW_GRAVITY = 0.05
        private const val PREDICTION_ITERATIONS = 3
        private const val AIM_EPSILON_DEGREES = 2.0f
        private const val RELEASE_COOLDOWN_TICKS = 3
    }
}
