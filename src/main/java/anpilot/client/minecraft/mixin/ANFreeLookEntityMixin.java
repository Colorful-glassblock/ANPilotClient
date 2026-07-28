package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANFreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ANFreeLookEntityMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void anpilot$freeLookTurn(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if ((Object) this != minecraft.player) return;

        Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("FreeLook");
        if (module instanceof ANFreeLook freeLook && freeLook.cameraMode()) {
            freeLook.onCameraMouseMove(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
