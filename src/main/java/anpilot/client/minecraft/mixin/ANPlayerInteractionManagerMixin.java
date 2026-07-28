package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.event.impl.ANAttackBlockEvent;
import anpilot.client.features.event.impl.EventBreakBlock;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ANPlayerInteractionManagerMixin {
    @Shadow
    private void ensureHasSentCarriedItem() {
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        if (ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new ANAttackBlockEvent(blockPos, direction)).isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"), cancellable = true)
    private void breakBlockHook(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        EventBreakBlock event = new EventBreakBlock(pos);
        ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(event);
        if (event.isCancelled()) cir.setReturnValue(false);
    }
}
