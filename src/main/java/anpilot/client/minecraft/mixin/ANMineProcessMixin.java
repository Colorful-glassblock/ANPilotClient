package anpilot.client.minecraft.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import anpilot.client.features.module.misc.oreminer.ANOreMiner;
import anpilot.client.api.module.ANModule;
import anpilot.client.bootstrap.ANServiceRegistry;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.pathing.movement.CalculationContext;
import baritone.process.MineProcess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(MineProcess.class)
public class ANMineProcessMixin {
    
    @Shadow(remap = false)
    private List<BlockPos> a; // knownOreLocations

    @Inject(method = "a(Ljava/util/List;Lbaritone/pathing/movement/CalculationContext;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRescan(List<BlockPos> already, CalculationContext context, CallbackInfo ci) {
        ANModule module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("ANOreMiner");
        if (!(module instanceof ANOreMiner)) {
            return;
        }
        ANOreMiner oreMiner = (ANOreMiner) module;
        if (!oreMiner.getEnabled() || !oreMiner.getBaritone().getValue()) {
            return;
        }
        this.a = oreMiner.getOreGoals();
        ci.cancel();
    }

    @Redirect(method = "a(Lbaritone/pathing/movement/CalculationContext;Lbaritone/api/utils/BlockOptionalMetaLookup;Ljava/util/List;Lnet/minecraft/core/BlockPos;)Z",
            at = @At(value = "INVOKE", target = "Lbaritone/api/utils/BlockOptionalMetaLookup;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false)
    private static boolean onPruneStream(BlockOptionalMetaLookup instance, BlockState blockState) {
        ANModule module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("ANOreMiner");
        if (!(module instanceof ANOreMiner)) {
            return instance.has(blockState);
        }
        ANOreMiner oreMiner = (ANOreMiner) module;
        if (!oreMiner.getEnabled() || !oreMiner.getBaritone().getValue()) {
            return instance.has(blockState);
        }
        return !blockState.isAir();
    }
}
