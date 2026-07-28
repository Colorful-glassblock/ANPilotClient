package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.movement.ANAntiKnockBack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHook.class)
public abstract class ANFishingBobberEntityMixin {
    @Redirect(method = "handleEntityEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;pullEntity(Lnet/minecraft/world/entity/Entity;)V"))
    private void preventFishingRodPull(FishingHook instance, Entity entity) {
        if (ANServiceRegistry.INSTANCE.isInitialized()
            && ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("AntiKnockBack") instanceof ANAntiKnockBack antiKnockBack
            && antiKnockBack.shouldCancelFishhook()) {
            return;
        }
        anpilot$pullEntity(entity);
    }

    protected abstract void anpilot$pullEntity(Entity entity);
}
