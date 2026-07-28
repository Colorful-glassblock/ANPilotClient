package anpilot.client.features.utility

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.Vec3
import net.minecraft.client.Minecraft
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents






class DamageableFakePlayer(player: Player, name: String) : FakePlayerEntity(player, name) {

    init {
        absorptionAmount = player.absorptionAmount
    }

    override fun baseTick() {
        super.baseTick()
        
        for (effectInstance in activeEffects) {
            val effect = effectInstance.effect
            if (effect.value().shouldApplyEffectTickThisTick(effectInstance.duration, effectInstance.amplifier)) {
                try {
                    effect.value().applyEffectTick(level() as? ServerLevel ?: continue, this, effectInstance.amplifier)
                } catch (_: Exception) {
                    
                }
            }
        }
    }

    



    fun simulateAttackFrom(attacker: Player) {
        var f = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat()
        val itemStack = attacker.mainHandItem
        val damageSource: DamageSource = attacker.damageSources().playerAttack(attacker)

        
        val sharpnessHolder = itemStack.enchantments.keySet().firstOrNull { it.`is`(Enchantments.SHARPNESS) }
        val sharpnessLevel = if (sharpnessHolder != null) itemStack.enchantments.getLevel(sharpnessHolder) else 0
        f += 1.0f + 0.5f * sharpnessLevel

        
        attacker.getEffect(MobEffects.STRENGTH)?.let {
            f += 3.0f * (it.amplifier + 1)
        }

        
        attacker.getEffect(MobEffects.WEAKNESS)?.let {
            f -= 4.0f * (it.amplifier + 1)
        }

        
        val h = attacker.getAttackStrengthScale(0.5f)
        f *= 0.2f + h * h * 0.8f

        
        val critAttack = h > 0.9f && attacker.deltaMovement.y < 0.0
                && !attacker.onGround() && !attacker.onClimbable()
                && !attacker.isInWater && !attacker.hasEffect(MobEffects.BLINDNESS)
                && !attacker.isPassenger && !attacker.isSprinting

        if (critAttack) {
            f *= 1.5f
        }

        damage(damageSource, f)
    }

    


    fun simulateExplosionFrom(explosionCenter: Vec3) {
        val damage = ExplosionUtils.crystalDamageToEntity(
            level(), this, explosionCenter, false
        )
        val applied = ExplosionUtils.getAppliedDamageToEntity(this, damage)
        damage(damageSources().explosion(null), applied)
    }

    



    private fun damage(source: DamageSource, amount: Float) {
        var dmg = amount

        if (source.scalesWithDifficulty()) {
            dmg = dmg * 3.0f / 2.0f
        }

        if (dmg == 0.0f) return

        
        if (hurtTime > 0 && dmg <= lastHurt) return

        
        if (!source.`is`(DamageTypeTags.BYPASSES_ARMOR)) {
            dmg = getDamageAfterArmorAbsorb(source, dmg)
        }

        
        dmg = getDamageAfterMagicAbsorb(source, dmg)

        
        val abs = absorptionAmount
        val afterAbs = (dmg - abs).coerceAtLeast(0.0f)
        val absorbed = dmg - afterAbs
        absorptionAmount = (abs - absorbed).coerceAtLeast(0.0f)

        if (afterAbs == 0.0f) return

        val newHealth = health - afterAbs
        if (newHealth <= 0.0f) {
            simulateTotemPop()
            return
        }

        health = newHealth
        lastHurt = dmg
        hurtTime = 10
        hurtDuration = 10
        playHurtSound(source)
    }

    override fun playHurtSound(source: DamageSource) {
        val soundEvent = when {
            source.`is`(DamageTypeTags.IS_DROWNING) -> SoundEvents.PLAYER_HURT_DROWN
            source.`is`(DamageTypeTags.IS_FIRE) -> SoundEvents.PLAYER_HURT_ON_FIRE
            else -> SoundEvents.PLAYER_HURT
        }
        level().playLocalSound(x, y, z, soundEvent, soundSource, 1.0f, 1.0f, false)
    }

    


    fun simulateGappleEat() {
        addEffect(MobEffectInstance(MobEffects.REGENERATION, 400, 1))
        addEffect(MobEffectInstance(MobEffects.RESISTANCE, 6000, 0))
        addEffect(MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0))
        addEffect(MobEffectInstance(MobEffects.ABSORPTION, 2400, 3))
        absorptionAmount = 16.0f
    }

    


    fun simulateTotemPop() {
        health = 1.0f
        removeAllEffects()

        addEffect(MobEffectInstance(MobEffects.REGENERATION, 900, 1))
        addEffect(MobEffectInstance(MobEffects.ABSORPTION, 100, 1))
        absorptionAmount = 8.0f

        
        val connection = Minecraft.getInstance().connection ?: return
        connection.handleEntityEvent(ClientboundEntityEventPacket(this, 35.toByte()))
    }

    override fun isInvulnerableTo(world: ServerLevel, source: DamageSource): Boolean = false

    override fun setDeltaMovement(x: Double, y: Double, z: Double) {
        
    }

}
