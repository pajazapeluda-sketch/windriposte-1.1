package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStoreAttackerMixin implements WindRiposteState {

    @Unique private LivingEntity windriposte$lastAttacker;

    @Unique private long windriposte$nextRiposteTick = 0L;
    @Unique private long windriposte$inhaleTick = 0L;

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD")
    )
    private void windriposte$storeAttacker(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source == null) return;
        LivingEntity attacker = (LivingEntity) source.getAttacker();
        if (attacker != null) {
            this.windriposte$lastAttacker = attacker;
        }
    }

    @Override
    public LivingEntity windriposte$getLastAttacker() {
        return this.windriposte$lastAttacker;
    }

    @Override
    public void windriposte$setLastAttacker(LivingEntity attacker) {
        this.windriposte$lastAttacker = attacker;
    }

    @Override
    public void windriposte$clearLastAttacker() {
        this.windriposte$lastAttacker = null;
    }

    @Override
    public long windriposte$getNextRiposteTick() {
        return this.windriposte$nextRiposteTick;
    }

    @Override
    public void windriposte$setNextRiposteTick(long tick) {
        this.windriposte$nextRiposteTick = tick;
    }

    @Override
    public long windriposte$getInhaleTick() {
        return this.windriposte$inhaleTick;
    }

    @Override
    public void windriposte$setInhaleTick(long tick) {
        this.windriposte$inhaleTick = tick;
    }
}
