package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.event.impl.TravelEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class ANLivingEntityMixin {
    
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 modifyTravelInput(Vec3 movementInput) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return movementInput;
        if (((Object) this) == net.minecraft.client.Minecraft.getInstance().player) {
            TravelEvent.Pre event = new TravelEvent.Pre(movementInput);
            ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(event);
            return event.getMovementInput();
        }
        return movementInput;
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void onTravelPost(Vec3 movementInput, CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        if (((Object) this) == net.minecraft.client.Minecraft.getInstance().player) {
            ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new TravelEvent.Post(movementInput));
        }
    }
}
