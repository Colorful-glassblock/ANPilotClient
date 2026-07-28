package anpilot.client.minecraft.mixin;

import anpilot.client.minecraft.gui.ANHudOffsets;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ContextualBarRenderer.class)
public interface ANContextualBarRendererMixin {
    @ModifyReturnValue(method = "top", at = @At("RETURN"))
    private int moveContextualBarUp(int original) {
        return original - ANHudOffsets.hotBarStatusOffset();
    }

    @ModifyConstant(method = "extractExperienceLevel", constant = @Constant(intValue = 24))
    private static int moveExperienceLevelUp(int original) {
        return original + ANHudOffsets.hotBarStatusOffset();
    }
}
