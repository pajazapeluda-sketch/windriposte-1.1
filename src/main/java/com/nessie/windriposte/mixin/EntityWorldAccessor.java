package com.nessie.windriposte.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.entity.Entity.class)
public interface EntityWorldAccessor {
    @Accessor("world")
    World windriposte$getWorld();
}
