package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANWeather;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class ANWorldRendererMixin {
    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true)
    private void renderWeatherHook(FrameGraphBuilder frameGraphBuilder, GpuBufferSlice gpuBufferSlice, CallbackInfo ci) {
        if (ANServiceRegistry.INSTANCE.isInitialized()) {
            var weather = (ANWeather) ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Weather");
            if (weather != null && weather.getEnabled() && weather.getWeatherMode().getValue() == ANWeather.Weather.CLEAR) {
                ci.cancel();
            }
        }
    }
}
