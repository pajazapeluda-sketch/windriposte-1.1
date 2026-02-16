package com.nessie.windriposte;

import net.minecraft.entity.LivingEntity;

public interface WindRiposteState {
    // attacker + level captured when the hit happens
    LivingEntity windriposte$getLastAttacker();
    void windriposte$setLastAttacker(LivingEntity attacker);

    int windriposte$getLastLevel();
    void windriposte$setLastLevel(int level);

    void windriposte$clearLastAttacker();

    // --- cooldown / arming state ---
    boolean windriposte$getWasShieldCoolingDown();
    void windriposte$setWasShieldCoolingDown(boolean v);

    boolean windriposte$getRiposteReady();
    void windriposte$setRiposteReady(boolean v);

    long windriposte$getRiposteReadyAtTick();
    void windriposte$setRiposteReadyAtTick(long tick);

    long windriposte$getLastShieldDisableTick();
    void windriposte$setLastShieldDisableTick(long tick);
}
