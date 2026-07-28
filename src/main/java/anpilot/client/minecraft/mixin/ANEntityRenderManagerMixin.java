package anpilot.client.minecraft.mixin;

import anpilot.client.renderer.utils.IANEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class ANEntityRenderManagerMixin {
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> void setEntity(E entity, float tickProgress, CallbackInfoReturnable<EntityRenderState> cir) {
        ((IANEntityRenderState) cir.getReturnValue()).an$setEntity(entity);
    }
}
