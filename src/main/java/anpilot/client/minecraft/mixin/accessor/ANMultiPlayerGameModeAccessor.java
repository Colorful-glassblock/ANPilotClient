package anpilot.client.minecraft.mixin.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface ANMultiPlayerGameModeAccessor {
    @Invoker("ensureHasSentCarriedItem")
    void anpilot$ensureHasSentCarriedItem();

    @Invoker("startPrediction")
    void anpilot$startPrediction(ClientLevel level, PredictiveAction action);
}
