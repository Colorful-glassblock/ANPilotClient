package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANNoRender;
import anpilot.client.features.event.impl.Render2DEvent;
import anpilot.client.features.event.impl.ANMinecraftEvents;
import anpilot.client.minecraft.gui.ANHudOffsets;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class ANInGameHudMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderHook(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (ANServiceRegistry.INSTANCE.isInitialized()) {
            float delta = tickCounter.getGameTimeDeltaPartialTick(true);
            ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new Render2DEvent(context, delta));
            ANMinecraftEvents.INSTANCE.renderHud(context, delta);
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        var hotBar = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("HotBar");
        if (hotBar != null && hotBar.getEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlayHook(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        var potions = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Potions");
        if (potions != null && potions.getEnabled()) {
            ci.cancel();
        }
    }

    @ModifyConstant(method = "extractPlayerHealth", constant = @Constant(intValue = 39))
    private int movePlayerStatusBarsUp(int original) {
        return original + ANHudOffsets.hotBarStatusOffset();
    }

    @ModifyConstant(method = "extractVehicleHealth", constant = @Constant(intValue = 39))
    private int moveVehicleStatusBarsUp(int original) {
        return original + ANHudOffsets.hotBarStatusOffset();
    }

    @ModifyArg(method = "extractCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V", ordinal = 0), index = 2)
    private float onRenderPumpkinOverlay(float alpha) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoPumpkinOverlay().getValue()) {
            return 0.0f;
        }
        return alpha;
    }

    @ModifyArg(method = "extractCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V", ordinal = 1), index = 2, require = 0)
    private float onRenderPowderSnowOverlay(float alpha) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoPowderSnowOverlay().getValue()) {
            return 0.0f;
        }
        return alpha;
    }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void onExtractVignette(GuiGraphicsExtractor graphics, Entity camera, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoVignette().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void onExtractSpyglassOverlay(GuiGraphicsExtractor graphics, float scopeScale, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoSpyglassOverlay().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void onExtractPortalOverlay(GuiGraphicsExtractor graphics, float alpha, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoPortalOverlay().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void onExtractConfusionOverlay(GuiGraphicsExtractor graphics, float alpha, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoNausea().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
    private void onExtractBossOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoBossBar().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onExtractScoreboardSidebar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoScoreboard().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void onExtractTitle(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoTitle().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onExtractOverlayMessage(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoOverlayMessage().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void onExtractEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoPotionIcons().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void onExtractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoCrosshair().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void onExtractSelectedItemName(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoItemName().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSleepOverlay", at = @At("HEAD"), cancellable = true)
    private void onExtractSleepOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ANNoRender noRender = noRender();
        if (noRender != null && noRender.getNoSleepOverlay().getValue()) {
            ci.cancel();
        }
    }

    @Unique
    private static ANNoRender noRender() {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return null;
        ANNoRender noRender = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender();
        if (noRender == null || !noRender.getEnabled()) return null;
        return noRender;
    }
}
