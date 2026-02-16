package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityWindRiposteStateMixin implements WindRiposteState {

    @Unique private LivingEntity windriposte$lastAttacker;

    @Unique private boolean windriposte$armed = true;

    @Unique private long windriposte$landTick = -1;
    @Unique private long windriposte$inhaleTick = -1;
    @Unique private long windriposte$readyTick = -1;

    @Unique private boolean windriposte$playedLand = false;
    @Unique private boolean windriposte$playedInhale = false;

    @Override
    public LivingEntity windriposte$getLastAttacker() {
        return windriposte$lastAttacker;
    }

    @Override
    public void windriposte$setLastAttacker(LivingEntity attacker) {
        windriposte$lastAttacker = attacker;
    }

    @Override
    public void windriposte$clearLastAttacker() {
        windriposte$lastAttacker = null;
    }

    @Override
    public boolean windriposte$isArmed() {
        return windriposte$armed;
    }

    @Override
    public void windriposte$setArmed(boolean armed) {
        windriposte$armed = armed;
    }

    @Override
    public long windriposte$getLandTick() {
        return windriposte$landTick;
    }

    @Override
    public void windriposte$setLandTick(long t) {
        windriposte$landTick = t;
    }

    @Override
    public long windriposte$getInhaleTick() {
        return windriposte$inhaleTick;
    }

    @Override
    public void windriposte$setInhaleTick(long t) {
        windriposte$inhaleTick = t;
    }

    @Override
    public long windriposte$getReadyTick() {
        return windriposte$readyTick;
    }

    @Override
    public void windriposte$setReadyTick(long t) {
        windriposte$readyTick = t;
    }

    @Override
    public boolean windriposte$getPlayedLand() {
        return windriposte$playedLand;
    }

    @Override
    public void windriposte$setPlayedLand(boolean v) {
        windriposte$playedLand = v;
    }

    @Override
    public boolean windriposte$getPlayedInhale() {
        return windriposte$playedInhale;
    }

    @Override
    public void windriposte$setPlayedInhale(boolean v) {
        windriposte$playedInhale = v;
    }
}
