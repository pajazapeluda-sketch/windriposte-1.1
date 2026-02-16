package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFlagMixin implements WindRiposteState {

    @Unique private LivingEntity windriposte$lastAttacker;
    @Unique private int windriposte$lastLevel;

    @Unique private boolean windriposte$wasShieldCoolingDown = false;
    @Unique private boolean windriposte$riposteReady = true;
    @Unique private long windriposte$riposteReadyAtTick = 0L;
    @Unique private long windriposte$lastShieldDisableTick = -1L;

    @Override
    public LivingEntity windriposte$getLastAttacker() {
        return windriposte$lastAttacker;
    }

    @Override
    public void windriposte$setLastAttacker(LivingEntity attacker) {
        windriposte$lastAttacker = attacker;
    }

    @Override
    public int windriposte$getLastLevel() {
        return windriposte$lastLevel;
    }

    @Override
    public void windriposte$setLastLevel(int level) {
        windriposte$lastLevel = level;
    }

    @Override
    public void windriposte$clearLastAttacker() {
        windriposte$lastAttacker = null;
        windriposte$lastLevel = 0;
    }

    @Override
    public boolean windriposte$getWasShieldCoolingDown() {
        return windriposte$wasShieldCoolingDown;
    }

    @Override
    public void windriposte$setWasShieldCoolingDown(boolean v) {
        windriposte$wasShieldCoolingDown = v;
    }

    @Override
    public boolean windriposte$getRiposteReady() {
        return windriposte$riposteReady;
    }

    @Override
    public void windriposte$setRiposteReady(boolean v) {
        windriposte$riposteReady = v;
    }

    @Override
    public long windriposte$getRiposteReadyAtTick() {
        return windriposte$riposteReadyAtTick;
    }

    @Override
    public void windriposte$setRiposteReadyAtTick(long tick) {
        windriposte$riposteReadyAtTick = tick;
    }

    @Override
    public long windriposte$getLastShieldDisableTick() {
        return windriposte$lastShieldDisableTick;
    }

    @Override
    public void windriposte$setLastShieldDisableTick(long tick) {
        windriposte$lastShieldDisableTick = tick;
    }
}
