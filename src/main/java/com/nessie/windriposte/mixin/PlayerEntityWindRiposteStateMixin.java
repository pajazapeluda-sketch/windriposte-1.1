package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityWindRiposteStateMixin implements WindRiposteState {

    private long windriposte$readyTick = 0L;
    private long windriposte$inhaleTick = -1L;
    private long windriposte$landTick = -1L;

    private boolean windriposte$playedInhale = false;
    private boolean windriposte$playedLand = false;

    private boolean windriposte$armed = true; // start armed by default

    @Override
    public long windriposte$getReadyTick() { return windriposte$readyTick; }

    @Override
    public void windriposte$setReadyTick(long tick) { this.windriposte$readyTick = tick; }

    @Override
    public long windriposte$getInhaleTick() { return windriposte$inhaleTick; }

    @Override
    public void windriposte$setInhaleTick(long tick) { this.windriposte$inhaleTick = tick; }

    @Override
    public long windriposte$getLandTick() { return windriposte$landTick; }

    @Override
    public void windriposte$setLandTick(long tick) { this.windriposte$landTick = tick; }

    @Override
    public boolean windriposte$getPlayedInhale() { return windriposte$playedInhale; }

    @Override
    public void windriposte$setPlayedInhale(boolean v) { this.windriposte$playedInhale = v; }

    @Override
    public boolean windriposte$getPlayedLand() { return windriposte$playedLand; }

    @Override
    public void windriposte$setPlayedLand(boolean v) { this.windriposte$playedLand = v; }

    @Override
    public boolean windriposte$isArmed() { return windriposte$armed; }

    @Override
    public void windriposte$setArmed(boolean v) { this.windriposte$armed = v; }
}
