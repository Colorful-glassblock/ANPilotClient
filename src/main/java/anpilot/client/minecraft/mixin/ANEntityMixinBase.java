package anpilot.client.minecraft.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

abstract class ANEntityMixinBase extends Entity {
    protected ANEntityMixinBase(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
}
