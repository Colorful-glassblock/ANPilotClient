package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.player.ANAutoSign;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;

@Mixin(AbstractSignEditScreen.class)
public abstract class ANAbstractSignEditScreenMixin {
    @Shadow
    @Final
    private String[] messages;

    @Shadow
    private void onDone() {
    }

    @Inject(at = @At("HEAD"), method = "init()V")
    private void onInit(CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("AutoSign");
        if (module instanceof ANAutoSign && ((ANAutoSign) module).getEnabled()) {
            ANAutoSign autoSign = (ANAutoSign) module;
            messages[0] = autoSign.getLine1().getValue();
            messages[1] = autoSign.getLine2().getValue();
            messages[2] = autoSign.getLine3().getValue();
            messages[3] = autoSign.getLine4().getValue();
            onDone();
        }
    }
}
