package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.movement.ANAntiKnockBack;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;

@Mixin(FlowingFluid.class)
public abstract class ANFlowableFluidMixin {
    @Redirect(method = "getFlow", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", ordinal = 0), require = 0)
    private boolean getVelocityHasNext(Iterator<Direction> iterator) {
        if (ANServiceRegistry.INSTANCE.isInitialized()
            && ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("AntiKnockBack") instanceof ANAntiKnockBack antiKnockBack
            && antiKnockBack.shouldCancelLiquidPush()) {
            return false;
        }
        return iterator.hasNext();
    }
}
