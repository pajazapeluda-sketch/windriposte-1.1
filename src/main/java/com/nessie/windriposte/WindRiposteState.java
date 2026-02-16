package com.nessie.windriposte;

public interface WindRiposteState {

    // attacker storage (set elsewhere, used by riposte)
    net.minecraft.entity.LivingEntity windriposte$getLastAttacker();
    void windriposte$setLastAttacker(net.minecraft.entity.LivingEntity attacker);
    void windriposte$clearLastAttacker();

    // enchant "armed" gating
    boolean windriposte$isArmed();
    void windriposte$setArmed(boolean armed);

    // sound + rearm timing (server ticks)
    long windriposte$getLandTick();
    void windriposte$setLandTick(long t);

    long windriposte$getInhaleTick();
    void windriposte$setInhaleTick(long t);

    long windriposte$getReadyTick();
    void windriposte$setReadyTick(long t);

    boolean windriposte$getPlayedLand();
    void windriposte$setPlayedLand(boolean v);

    boolean windriposte$getPlayedInhale();
    void windriposte$setPlayedInhale(boolean v);
}
