package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.event.impl.PacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ANClientConnectionMixin {
    @Inject(at = @At("HEAD"), method = "genericsFtw", cancellable = true)
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> {
                if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
                ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new PacketEvent.Receive(packet));
            });
            return;
        }

        PacketEvent.Receive event = new PacketEvent.Receive(packet);
        ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacketPre(Packet<?> packet, CallbackInfo info) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        PacketEvent.Outbound outboundEvent = new PacketEvent.Outbound(packet);
        ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(outboundEvent);
        if (outboundEvent.isCancelled()) {
            info.cancel();
            return;
        }

        PacketEvent.Send event = new PacketEvent.Send(packet);
        ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(event);
        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("RETURN"))
    private void onSendPacketPost(Packet<?> packet, CallbackInfo info) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new PacketEvent.OutboundPost(packet));
        ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new PacketEvent.Sent(packet));
    }
}
