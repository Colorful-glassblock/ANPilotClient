package anpilot.client.minecraft.mixin.accessor;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundInteractPacket.class)
public interface ANServerboundInteractPacketAccessor {
    @Accessor("action")
    Object getAction();
}
