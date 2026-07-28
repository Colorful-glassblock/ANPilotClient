package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ANParticleManagerMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void onAddParticle(ParticleOptions effect, double x, double y, double z, double velX, double velY, double velZ, CallbackInfoReturnable<Particle> cir) {
        if (!ANServiceRegistry.INSTANCE.isInitialized() ||
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender() == null ||
            !ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getEnabled()) {
            return;
        }

        var noRender = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender();
        if ((noRender.getNoAmbientParticle().getValue() && isAmbientParticle(effect)) ||
            (noRender.getNoEffectParticle().getValue() && isStatusEffectParticle(effect)) ||
            (noRender.getNoTotemParticle().getValue() && effect.getType() == ParticleTypes.TOTEM_OF_UNDYING)) {
            cir.setReturnValue(null);
        }
    }

    private boolean isStatusEffectParticle(ParticleOptions effect) {
        return effect.getType() == ParticleTypes.EFFECT
            || effect.getType() == ParticleTypes.INSTANT_EFFECT
            || effect.getType() == ParticleTypes.ENTITY_EFFECT;
    }

    private boolean isAmbientParticle(ParticleOptions effect) {
        return effect.getType() == ParticleTypes.MYCELIUM
            || effect.getType() == ParticleTypes.ASH
            || effect.getType() == ParticleTypes.CRIMSON_SPORE
            || effect.getType() == ParticleTypes.WARPED_SPORE
            || effect.getType() == ParticleTypes.WHITE_ASH
            || effect.getType() == ParticleTypes.UNDERWATER
            || effect.getType() == ParticleTypes.SOUL
            || effect.getType() == ParticleTypes.SOUL_FIRE_FLAME
            || effect.getType() == ParticleTypes.END_ROD
            || effect.getType() == ParticleTypes.CHERRY_LEAVES
            || effect.getType() == ParticleTypes.ENCHANTED_HIT
            || effect.getType() == ParticleTypes.EXPLOSION
            || effect.getType() == ParticleTypes.EXPLOSION_EMITTER
            || effect.getType() == ParticleTypes.POOF
            || effect.getType() == ParticleTypes.RAIN
            || effect.getType() == ParticleTypes.SMOKE;
    }
}
