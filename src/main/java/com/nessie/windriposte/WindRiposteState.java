package com.nessie.windriposte;

import net.minecraft.entity.LivingEntity;

public interface WindRiposteState {

    // ---- attacker + level captured on hit ----
    LivingEntity windriposte$getLastAttacker();
    void windriposte$setLastAttacker(LivingEntity attacker);

    int windriposte$getLastLevel();
    void windriposte$setLastLevel(int level);

    void windriposte$clearLastAttacker();

    // ---- gating (riposte only when armed) ----
    boolean windriposte$isArmed();
    void windriposte$setArmed(boolean armed);

    // ---- sound scheduling + “play once” flags ----
    long windriposte$getLandTick();
    void windriposte$setLandTick(long tick);

    long windriposte$getInhaleTick();
    void windriposte$setInhaleTick(long tick);

    long windriposte$getReadyTick();
    void windriposte$setReadyTick(long tick);

    boolean windriposte$getPlayedLand();
    void windriposte$setPlayedLand(boolean v);

    boolean windriposte$getPlayedInhale();
    void windriposte$setPlayedInhale(boolean v);
}
