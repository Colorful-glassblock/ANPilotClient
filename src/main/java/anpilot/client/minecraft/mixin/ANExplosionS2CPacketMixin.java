package anpilot.client.minecraft.mixin;

import anpilot.client.utility.system.IExplosionS2CPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(ClientboundExplodePacket.class)
public abstract class ANExplosionS2CPacketMixin implements IExplosionS2CPacket {
    @Shadow
    @Final
    @Mutable
    private Optional<Vec3> playerKnockback;

    @Override
    public void ANPilotSetVelocityX(float velocity) {
        if (playerKnockback.isPresent()) {
            Vec3 knockback = playerKnockback.get();
            playerKnockback = Optional.of(new Vec3(velocity, knockback.y, knockback.z));
        } else {
            playerKnockback = Optional.of(new Vec3(velocity, 0.0, 0.0));
        }
    }

    @Override
    public void ANPilotSetVelocityY(float velocity) {
        if (playerKnockback.isPresent()) {
            Vec3 knockback = playerKnockback.get();
            playerKnockback = Optional.of(new Vec3(knockback.x, velocity, knockback.z));
        } else {
            playerKnockback = Optional.of(new Vec3(0.0, velocity, 0.0));
        }
    }

    @Override
    public void ANPilotSetVelocityZ(float velocity) {
        if (playerKnockback.isPresent()) {
            Vec3 knockback = playerKnockback.get();
            playerKnockback = Optional.of(new Vec3(knockback.x, knockback.y, velocity));
        } else {
            playerKnockback = Optional.of(new Vec3(0.0, 0.0, velocity));
        }
    }
}
