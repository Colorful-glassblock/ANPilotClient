package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANXRay;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VisGraph.class)
public abstract class ANVisGraphMixin {
    @Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true)
    private void onSetOpaque(BlockPos pos, CallbackInfo ci) {
        if (ANServiceRegistry.INSTANCE.isInitialized()) {
            Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("XRay");
            if (module instanceof ANXRay && ((ANXRay) module).getEnabled()) {
                ci.cancel();
            }
        }
    }
}
