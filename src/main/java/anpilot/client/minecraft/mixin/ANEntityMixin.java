package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.movement.ANAntiKnockBack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ANEntityMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPushEntity(Entity entity, CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if ((Object) this != minecraft.player && entity != minecraft.player) return;
        Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("AntiKnockBack");
        if (module instanceof ANAntiKnockBack antiKnockBack && antiKnockBack.shouldCancelEntityPush()) {
            ci.cancel();
        }
    }

    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void onIsPushedByFluid(CallbackInfoReturnable<Boolean> cir) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if ((Object) this != minecraft.player) return;
        Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("AntiKnockBack");
        if (module instanceof ANAntiKnockBack antiKnockBack && antiKnockBack.shouldCancelLiquidPush()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        if ((Object) this instanceof ItemEntity &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().dropsESP() != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().dropsESP().getEnabled()) {
            cir.setReturnValue(true);
            return;
        }
        if ((Object) this instanceof ItemEntity && ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Esp") != null && ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Esp").getEnabled()) {
            cir.setReturnValue(true);
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "getBlockSpeedFactor",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;")
    )
    private net.minecraft.world.level.block.Block modifyBlockSpeedFactor(net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.world.level.block.Block original = state.getBlock();
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return original;

        Minecraft minecraft = Minecraft.getInstance();
        if ((Object) this != minecraft.player) return original;

        Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("NoSlow");
        if (module instanceof anpilot.client.features.module.movement.ANNoSlow && ((anpilot.client.features.module.movement.ANNoSlow) module).getEnabled()) {
            anpilot.client.features.module.movement.ANNoSlow noSlow = (anpilot.client.features.module.movement.ANNoSlow) module;
            if (original == net.minecraft.world.level.block.Blocks.SOUL_SAND && noSlow.getSoulSand().getValue()) {
                return net.minecraft.world.level.block.Blocks.STONE;
            }
            if (original == net.minecraft.world.level.block.Blocks.HONEY_BLOCK && noSlow.getHoney().getValue()) {
                return net.minecraft.world.level.block.Blocks.STONE;
            }
        }
        return original;
    }
}
