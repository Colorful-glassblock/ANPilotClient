package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANXRay;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public abstract class ANSodiumBlockRenderContextMixin {
    @Shadow
    protected BlockAndTintGetter level;

    @Shadow
    protected BlockState state;

    @Shadow
    protected BlockPos pos;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, remap = false)
    private void shouldDrawSideHook(Direction face, CallbackInfoReturnable<Boolean> cir) {
        ANXRay xray = getXRay();
        if (xray == null) return;

        if (xray.isVisible(state.getBlock(), pos)) {
            BlockPos neighborPos = pos.relative(face);
            Block neighborBlock = level.getBlockState(neighborPos).getBlock();
            cir.setReturnValue(!xray.isVisible(neighborBlock, neighborPos));
        } else if (!xray.isOpacityMode()) {
            cir.setReturnValue(false);
        }
    }

    private static ANXRay getXRay() {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return null;

        Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("XRay");
        if (module instanceof ANXRay xray && xray.getEnabled()) {
            return xray;
        }

        return null;
    }
}
