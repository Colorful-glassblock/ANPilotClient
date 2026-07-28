package anpilot.client.features.utility

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.sqrt

object CrystalUtils {
    private val mc: Minecraft
        get() = Minecraft.getInstance()

    fun canPlaceOn(pos: BlockPos, protocolPlace: Boolean): Boolean {
        val level = mc.level ?: return false
        val base = level.getBlockState(pos).block
        if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK) return false

        if (!level.getBlockState(pos.above()).isAir) return false
        if (protocolPlace && !level.getBlockState(pos.above(2)).isAir) return false
        return !hasBlockingEntity(crystalBox(pos), ignoreItems = false)
    }

    fun crystalBox(basePos: BlockPos): AABB {
        return AABB(-0.5, 0.0, -0.5, 0.5, 2.0, 0.5).move(Vec3.atBottomCenterOf(basePos.above()))
    }

    fun hasBlockingEntity(box: AABB, ignoreItems: Boolean): Boolean {
        val level = mc.level ?: return false
        val player = mc.player ?: return false
        return level.getEntities(player, box) { entity -> !canIgnoreEntity(entity, ignoreItems) }.isNotEmpty()
    }

    fun canIgnoreEntity(entity: Entity, ignoreItems: Boolean): Boolean {
        return entity is ExperienceOrb || entity is EndCrystal || (ignoreItems && entity is ItemEntity)
    }

    fun hitResult(basePos: BlockPos): BlockHitResult {
        val player = mc.player
        val eye = player?.eyePosition ?: Vec3.atCenterOf(basePos)
        val box = AABB(basePos)
        val cut = Vec3(
            Mth.clamp(eye.x, box.minX, box.maxX),
            Mth.clamp(eye.y, box.minY, box.maxY),
            Mth.clamp(eye.z, box.minZ, box.maxZ)
        )
        val direction = if (eye.y >= box.maxY) Direction.UP else nearestDirection(eye.x - cut.x, eye.y - cut.y, eye.z - cut.z)
        return BlockHitResult(cut, direction, basePos, box.contains(eye))
    }

    private fun nearestDirection(x: Double, y: Double, z: Double): Direction {
        val ax = abs(x)
        val ay = abs(y)
        val az = abs(z)
        return when {
            ay >= ax && ay >= az -> if (y >= 0.0) Direction.UP else Direction.DOWN
            ax >= az -> if (x >= 0.0) Direction.EAST else Direction.WEST
            else -> if (z >= 0.0) Direction.SOUTH else Direction.NORTH
        }
    }

    fun rotationsTo(vec: Vec3): Pair<Float, Float> {
        val player = mc.player ?: return 0f to 0f
        val eyes = player.eyePosition
        val dx = vec.x - eyes.x
        val dy = vec.y - eyes.y
        val dz = vec.z - eyes.z
        val horizontal = sqrt(dx * dx + dz * dz)
        val yaw = (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
        val pitch = -Math.toDegrees(atan2(dy, horizontal)).toFloat()
        return player.yRot + Mth.wrapDegrees(yaw - player.yRot) to player.xRot + Mth.wrapDegrees(pitch - player.xRot)
    }

    fun estimateCrystalDamage(crystalPos: Vec3, target: LivingEntity): Float {
        val distance = sqrt(target.distanceToSqr(crystalPos))
        if (distance > 12.0) return 0f

        val exposure = (1.0 - distance / 12.0).coerceIn(0.0, 1.0)
        val raw = ((exposure * exposure + exposure) / 2.0 * 7.0 * 12.0 + 1.0).toFloat()
        val armorReduction = (1f - target.armorValue.coerceAtMost(20) / 25f).coerceIn(0.2f, 1f)
        return raw * armorReduction
    }
}
