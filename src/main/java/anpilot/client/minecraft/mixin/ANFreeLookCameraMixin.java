package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANFreeLook;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class ANFreeLookCameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void move(float distanceOffset, float verticalOffset, float horizontalOffset);

    @Shadow
    private float getMaxZoom(float desiredCameraDistance) {
        throw new AssertionError();
    }

    @Shadow
    private Entity entity;

    @Shadow
    private boolean detached;

    @Inject(method = "alignWithEntity", at = @At("HEAD"), cancellable = true)
    private void anpilot$freeLookRotation(float partialTicks, CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("FreeLook");
        if (module instanceof ANFreeLook freeLook && freeLook.getEnabled()) {
            Entity cameraEntity = this.entity;
            if (cameraEntity == null) return;

            freeLook.updateCameraRotation();

            double x = Mth.lerp((double) partialTicks, cameraEntity.xo, cameraEntity.getX());
            double y = Mth.lerp((double) partialTicks, cameraEntity.yo, cameraEntity.getY()) + cameraEntity.getEyeHeight();
            double z = Mth.lerp((double) partialTicks, cameraEntity.zo, cameraEntity.getZ());

            setRotation(freeLook.getCameraYaw(), freeLook.getCameraPitch());
            setPosition(x, y, z);
            this.detached = true;
            move(-getMaxZoom(4.0f), 0.0f, 0.0f);
            ci.cancel();
        }
    }
}
