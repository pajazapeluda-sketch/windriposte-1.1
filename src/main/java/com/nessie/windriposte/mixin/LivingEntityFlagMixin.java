package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFlagMixin implements WindRiposteState {

    // attacker + level
    @Unique private LivingEntity windriposte$lastAttacker;
    @Unique private int windriposte$lastLevel = 0;

    // armed gate
    @Unique private boolean windriposte$armed = true;

    // sound schedule ticks
    @Unique private long windriposte$landTick = -1L;
    @Unique private long windriposte$inhaleTick = -1L;
    @Unique private long windriposte$readyTick = -1L;

    // sound “play once”
    @Unique private boolean windriposte$playedLand = false;
    @Unique private boolean windriposte$playedInhale = false;

    // ---------------- attacker + level ----------------
    @Override
    public LivingEntity windriposte$getLastAttacker() {
        return windriposte$lastAttacker;
    }

    @Override
    public void windriposte$setLastAttacker(LivingEntity attacker) {
        this.windriposte$lastAttacker = attacker;
    }

    @Override
    public int windriposte$getLastLevel() {
        return windriposte$lastLevel;
    }

    @Override
    public void windriposte$setLastLevel(int level) {
        this.windriposte$lastLevel = level;
    }

    @Override
    public void windriposte$clearLastAttacker() {
        this.windriposte$lastAttacker = null;
        this.windriposte$lastLevel = 0;
    }

    // ---------------- armed ----------------
    @Override
    public boolean windriposte$isArmed() {
        return windriposte$armed;
    }

    @Override
    public void windriposte$setArmed(boolean armed) {
        this.windriposte$armed = armed;
    }

    // ---------------- ticks ----------------
    @Override
    public long windriposte$getLandTick() {
        return windriposte$landTick;
    }

    @Override
    public void windriposte$setLandTick(long tick) {
        this.windriposte$landTick = tick;
    }

    @Override
    public long windriposte$getInhaleTick() {
        return windriposte$inhaleTick;
    }

    @Override
    public void windriposte$setInhaleTick(long tick) {
        this.windriposte$inhaleTick = tick;
    }

    @Override
    public long windriposte$getReadyTick() {
        return windriposte$readyTick;
    }

    @Override
    public void windriposte$setReadyTick(long tick) {
        this.windriposte$readyTick = tick;
    }

    // ---------------- played flags ----------------
    @Override
    public boolean windriposte$getPlayedLand() {
        return windriposte$playedLand;
    }

    @Override
    public void windriposte$setPlayedLand(boolean v) {
        this.windriposte$playedLand = v;
    }

    @Override
    public boolean windriposte$getPlayedInhale() {
        return windriposte$playedInhale;
    }

    @Override
    public void windriposte$setPlayedInhale(boolean v) {
        this.windriposte$playedInhale = v;
    }
}
