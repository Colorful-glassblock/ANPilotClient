package anpilot.client.minecraft.mixin;

import anpilot.client.features.module.render.ANDisplayTools;
import anpilot.client.minecraft.duck.ANHandledScreenExt;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ANHandledScreenMixin implements ANHandledScreenExt {
    @Shadow
    protected Slot hoveredSlot;

    @Override
    public Slot anpilot_getHoveredSlot() {
        return this.hoveredSlot;
    }

    @Inject(method = "extractTooltip(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V", at = @At("HEAD"), cancellable = true)
    private void onExtractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (ANDisplayTools.onRenderTooltip(graphics, (AbstractContainerScreen<?>)(Object)this, mouseX, mouseY)) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean isDown, CallbackInfoReturnable<Boolean> cir) {
        if (isDown && ANDisplayTools.onMouseClicked((AbstractContainerScreen<?>)(Object)this, event)) {
            cir.setReturnValue(true);
        }
    }
}
