package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.event.impl.EntityVelocityUpdateEvent;
import anpilot.client.features.event.impl.GameJoinedEvent;
import anpilot.client.features.event.impl.GameLeftEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ANClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientLevel level;

    @Unique
    private boolean anpilot$hadLevelBeforeJoin;

    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void onGameJoinHead(ClientboundLoginPacket packet, CallbackInfo info) {
        anpilot$hadLevelBeforeJoin = level != null;
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onGameJoinTail(ClientboundLoginPacket packet, CallbackInfo info) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        if (anpilot$hadLevelBeforeJoin) {
            ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new GameLeftEvent());
        }
        ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new GameJoinedEvent());
    }

    @Redirect(method = "handleSetEntityMotion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpMotion(Lnet/minecraft/world/phys/Vec3;)V"))
    private void velocityHook(Entity entity, Vec3 clientVelocity) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) {
            entity.lerpMotion(clientVelocity);
            return;
        }

        EntityVelocityUpdateEvent event = EntityVelocityUpdateEvent.Companion.get(entity, clientVelocity, false);
        ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(event);
        if (!event.isCancelled()) {
            entity.lerpMotion((Vec3) event.getClientVelocity());
        }
    }



    @Inject(method = "handleExplosion", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void onExplosionVelocity(ClientboundExplodePacket packet, CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized() || packet.playerKnockback().isEmpty()) return;
        if (!Minecraft.getInstance().isSameThread()) return;

        EntityVelocityUpdateEvent event = EntityVelocityUpdateEvent.Companion.get(null, packet.playerKnockback().get(), true);
        ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(event);
    }

    @Inject(method = "handleConfigurationStart", at = @At("HEAD"))
    private void onEnterReconfiguration(ClientboundStartConfigurationPacket packet, CallbackInfo info) {
        if (ANServiceRegistry.INSTANCE.isInitialized()) {
            ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new GameLeftEvent());
        }
    }
}
