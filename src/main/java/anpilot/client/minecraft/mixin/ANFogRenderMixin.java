package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class ANFogRenderMixin {
    @Shadow
    @Final
    private GpuBuffer emptyBuffer;

    @Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true)
    private void onGetBuffer(FogRenderer.FogMode mode, CallbackInfoReturnable<GpuBufferSlice> cir) {
        if (shouldDisableFog()) {
            cir.setReturnValue(this.emptyBuffer.slice(0L, FogRenderer.FOG_UBO_SIZE));
        }
    }

    private static boolean shouldDisableFog() {
        return ANServiceRegistry.INSTANCE.isInitialized() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender() != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getEnabled() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getFog().getValue();
    }
}
