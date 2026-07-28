package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class ANEntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof Player &&
            ANServiceRegistry.INSTANCE.isInitialized() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("NameTags") != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("NameTags").getEnabled()) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void shouldShowName(T entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player &&
            ANServiceRegistry.INSTANCE.isInitialized() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("NameTags") != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("NameTags").getEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onGetOutlineColor(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;

        var dropsESP = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().dropsESP();
        if (dropsESP != null && dropsESP.getEnabled() && dropsESP.getOutline().getValue() && entity instanceof ItemEntity) {
            state.outlineColor = dropsESP.outlineColorInt();
        }

        var chams = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().chams();
        if (chams != null && chams.getEnabled() && chams.shouldRender(entity)) {
            int outlineColor = chams.outlineColorFor(entity);
            if (outlineColor != 0) {
                state.outlineColor = outlineColor;
            }
        }
    }
}
