package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANWeather;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class ANClientWorldTimeWeatherMixin {
    @Shadow
    public boolean isClientSide;

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void onIsRaining(CallbackInfoReturnable<Boolean> cir) {
        if (isClientSide && ANServiceRegistry.INSTANCE.isInitialized()) {
            var weather = (ANWeather) ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Weather");
            if (weather != null && weather.getEnabled()) {
                cir.setReturnValue(weather.getWeatherMode().getValue() != ANWeather.Weather.CLEAR);
            }
        }
    }

    @Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
    private void onIsThundering(CallbackInfoReturnable<Boolean> cir) {
        if (isClientSide && ANServiceRegistry.INSTANCE.isInitialized()) {
            var weather = (ANWeather) ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Weather");
            if (weather != null && weather.getEnabled()) {
                cir.setReturnValue(weather.getWeatherMode().getValue() == ANWeather.Weather.THUNDER);
            }
        }
    }

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void onGetRainLevel(float delta, CallbackInfoReturnable<Float> cir) {
        if (isClientSide && ANServiceRegistry.INSTANCE.isInitialized()) {
            var weather = (ANWeather) ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Weather");
            if (weather != null && weather.getEnabled()) {
                cir.setReturnValue(weather.getWeatherMode().getValue() != ANWeather.Weather.CLEAR ? 1.0f : 0.0f);
            }
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void onGetThunderLevel(float delta, CallbackInfoReturnable<Float> cir) {
        if (isClientSide && ANServiceRegistry.INSTANCE.isInitialized()) {
            var weather = (ANWeather) ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Weather");
            if (weather != null && weather.getEnabled()) {
                cir.setReturnValue(weather.getWeatherMode().getValue() == ANWeather.Weather.THUNDER ? 1.0f : 0.0f);
            }
        }
    }
}
