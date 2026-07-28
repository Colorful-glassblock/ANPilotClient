package anpilot.client.features.utility

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.ceil
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.item.ItemStack





object ExplosionUtils {

    


    fun crystalDamageToEntity(
        level: BlockGetter,
        entity: LivingEntity,
        explosion: Vec3,
        ignoreTerrain: Boolean = false,
        ignoreBlocks: Set<BlockPos> = emptySet()
    ): Float {
        return damageToEntity(level, entity, explosion, 12.0f, ignoreTerrain, ignoreBlocks)
    }

    


    fun damageToEntity(
        level: BlockGetter,
        entity: LivingEntity,
        explosion: Vec3,
        power: Float,
        ignoreTerrain: Boolean,
        ignoreBlocks: Set<BlockPos>
    ): Float {
        return damageToEntity(level, entity, entity.position(), entity.boundingBox, explosion, power, ignoreTerrain, ignoreBlocks)
    }

    fun damageToEntity(
        level: BlockGetter,
        entity: LivingEntity,
        entityPosition: Vec3,
        entityBox: AABB,
        explosion: Vec3,
        power: Float,
        ignoreTerrain: Boolean,
        ignoreBlocks: Set<BlockPos>
    ): Float {
        val rawDamage = getRawExplosionDamage(
            level, explosion, entityPosition, entityBox,
            power, ignoreTerrain, ignoreBlocks
        )
        return getAppliedDamageToEntity(entity, rawDamage)
    }

    


    private fun getRawExplosionDamage(
        level: BlockGetter,
        source: Vec3,
        pos: Vec3,
        box: AABB,
        power: Float,
        ignoreTerrain: Boolean,
        ignoreBlocks: Set<BlockPos>
    ): Float {
        val distance = sqrt(pos.distanceToSqr(source))
        val exposure = getExposure(source, box, level, ignoreTerrain, ignoreBlocks)
        val w = distance / power
        val ac = (1.0 - w) * exposure
        return ((ac * ac + ac) / 2.0 * 7.0 * 12.0 + 1.0).toFloat()
    }

    


    private fun getExposure(
        source: Vec3,
        box: AABB,
        level: BlockGetter,
        ignoreTerrain: Boolean,
        ignoreBlocks: Set<BlockPos>
    ): Float {
        val xDiff = box.maxX - box.minX
        val yDiff = box.maxY - box.minY
        val zDiff = box.maxZ - box.minZ

        val xStepBase = 1.0 / (xDiff * 2 + 1)
        val yStepBase = 1.0 / (yDiff * 2 + 1)
        val zStepBase = 1.0 / (zDiff * 2 + 1)

        if (xStepBase <= 0 || yStepBase <= 0 || zStepBase <= 0) return 0f

        var misses = 0
        var hits = 0

        val xOffset = (1 - floor(1 / xStepBase) * xStepBase) * 0.5
        val zOffset = (1 - floor(1 / zStepBase) * zStepBase) * 0.5

        val xStep = xStepBase * xDiff
        val yStep = yStepBase * yDiff
        val zStep = zStepBase * zDiff

        val startX = box.minX + xOffset
        val startY = box.minY
        val startZ = box.minZ + zOffset
        val endX = box.maxX + xOffset
        val endY = box.maxY
        val endZ = box.maxZ + zOffset

        var x = startX
        while (x <= endX) {
            var y = startY
            while (y <= endY) {
                var z = startZ
                while (z <= endZ) {
                    val position = Vec3(x, y, z)
                    if (raycastExplosion(position, source, level, ignoreTerrain, ignoreBlocks) == null) {
                        misses++
                    }
                    hits++
                    z += zStep
                }
                y += yStep
            }
            x += xStep
        }

        return if (hits > 0) misses.toFloat() / hits else 0f
    }

    


    private fun raycastExplosion(
        from: Vec3,
        to: Vec3,
        level: BlockGetter,
        ignoreTerrain: Boolean,
        ignoreBlocks: Set<BlockPos>
    ): HitResult? {
        val result = level.clip(
            ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
            )
        )

        if (result.type == HitResult.Type.MISS) return null

        val blockPos = result.blockPos
        if (blockPos in ignoreBlocks) return null

        val blockState = level.getBlockState(blockPos)
        if (ignoreTerrain && blockState.block.explosionResistance < 600f) return null

        return result
    }

    


    fun getAppliedDamageToEntity(entity: LivingEntity, damage: Float): Float {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return max(0f, damage)
        val damageSource = level.damageSources().explosion(null)
        return max(0f, getReduction(entity, damageSource, damage))
    }

    private fun getReduction(entity: LivingEntity, damageSource: DamageSource, damageIn: Float): Float {
        var damage = damageIn

        
        val mc = Minecraft.getInstance()
        val level = mc.level
        if (level != null && damageSource.scalesWithDifficulty()) {
            when (level.difficulty) {
                Difficulty.EASY -> damage = (damage / 2 + 1).coerceAtMost(damage)
                Difficulty.HARD -> damage *= 1.5f
                else -> {}
            }
        }

        
        val armorValue = entity.armorValue.toFloat()
        val armorToughness = getAttributeValue(entity, Attributes.ARMOR_TOUGHNESS).toFloat()
        damage = getDamageAfterArmor(damage, armorValue, armorToughness)

        
        damage = getResistanceReduction(entity, damage)

        
        damage = getProtectionReduction(entity, damage)

        return damage
    }

    


    private fun getDamageAfterArmor(damage: Float, armor: Float, toughness: Float): Float {
        val f = 2.0f + toughness / 4.0f
        val g = (armor - damage / f).coerceAtLeast(armor * 0.2f)
        val h = (g.coerceAtMost(20.0f)) / 25.0f
        return damage * (1.0f - h)
    }

    private fun getAttributeValue(
        entity: LivingEntity,
        attribute: Holder<Attribute>
    ): Double {
        return try {
            entity.getAttributeValue(attribute)
        } catch (_: NullPointerException) {
            0.0
        }
    }

    private fun getResistanceReduction(entity: LivingEntity, damageIn: Float): Float {
        var damage = damageIn
        val resistance = entity.getEffect(MobEffects.RESISTANCE)
        if (resistance != null) {
            val lvl = resistance.amplifier + 1
            damage *= (1.0f - lvl * 0.2f)
        }
        return max(damage, 0f)
    }

    private fun getProtectionReduction(entity: LivingEntity, damageIn: Float): Float {
        val protLevel = getProtectionAmount(entity)
        if (protLevel == 0) return damageIn
        val f = (protLevel * 0.04f).coerceAtMost(0.8f)
        return damageIn * (1.0f - f)
    }

    



    fun getArmorDurabilityDamage(armorStack: ItemStack, rawDamage: Float): Int {
        val armorValue = armorStack.damageValue
        val maxDurability = armorStack.maxDamage
        if (maxDurability <= 0) return 0
        
        return ceil(rawDamage / 4.0).toInt().coerceAtLeast(0)
    }

    


    private fun getProtectionAmount(entity: LivingEntity): Int {
        var total = 0
        val armorSlots = listOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD)
        for (slot in armorSlots) {
            val stack = entity.getItemBySlot(slot)
            if (stack.isEmpty) continue
            val enchantments = stack.enchantments
            for (holder in enchantments.keySet()) {
                val key = holder.unwrapKey().orElse(null) ?: continue
                val id = key.identifier().toString()
                val lvl = enchantments.getLevel(holder)
                if (id.contains("protection")) {
                    total += lvl
                }
                if (id.contains("blast_protection")) {
                    total += lvl * 2
                }
            }
        }
        return total
    }
}
