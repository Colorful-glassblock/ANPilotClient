package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANXRay;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public abstract class ANSodiumBlockOcclusionCacheMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, remap = false)
    public void shouldDrawSideHook(BlockState selfBlockState, BlockGetter view, BlockPos selfPos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;

        Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("XRay");
        if (module instanceof ANXRay && ((ANXRay) module).getEnabled()) {
            ANXRay xray = (ANXRay) module;
            Block block = selfBlockState.getBlock();
            
            boolean blockVisible = xray.isVisible(block, selfPos);
            if (blockVisible) {
                BlockPos neighborPos = selfPos.relative(facing);
                BlockState neighborState = view.getBlockState(neighborPos);
                Block neighborBlock = neighborState.getBlock();
                if (neighborBlock != block || !xray.isVisible(neighborBlock, neighborPos)) {
                    cir.setReturnValue(true);
                }
            } else {
                if (xray.getOpacity().getValue() == 0.0f) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
