package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStoreAttackerMixin implements WindRiposteState {

    @Unique private LivingEntity windriposte$lastAttacker;

    @Unique private long windriposte$readyTick = 0L; // 0 = ready immediately on first ever disable
    @Unique private boolean windriposte$wasShieldCoolingDown = false;
    @Unique private boolean windriposte$pendingRearm = false;

    @Override
    public LivingEntity windriposte$getLastAttacker() {
        return windriposte$lastAttacker;
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
    public long windriposte$getReadyTick() {
        return windriposte$readyTick;
    }

    @Override
    public void windriposte$setReadyTick(long tick) {
        this.windriposte$readyTick = tick;
    }

    @Override
    public boolean windriposte$getWasShieldCoolingDown() {
        return windriposte$wasShieldCoolingDown;
    }

    @Override
    public void windriposte$setWasShieldCoolingDown(boolean value) {
        this.windriposte$wasShieldCoolingDown = value;
    }

    @Override
    public boolean windriposte$getPendingRearm() {
        return windriposte$pendingRearm;
    }

    @Override
    public void windriposte$setPendingRearm(boolean value) {
        this.windriposte$pendingRearm = value;
    }
}
