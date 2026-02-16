package com.nessie.windriposte;

import net.minecraft.entity.LivingEntity;

public interface WindRiposteState {
    // attacker + enchant level captured when damage happens
    LivingEntity windriposte$getLastAttacker();
    void windriposte$setLastAttacker(LivingEntity attacker);

    int windriposte$getLastLevel();
    void windriposte$setLastLevel(int level);

    void windriposte$clearLastAttacker();

    // shield cooldown tracking + “riposte ready” gating
    boolean windriposte$getWasShieldCoolingDown();
    void windriposte$setWasShieldCoolingDown(boolean v);

    boolean windriposte$getRiposteReady();
    void windriposte$setRiposteReady(boolean v);

    long windriposte$getRiposteReadyAtTick();
    void windriposte$setRiposteReadyAtTick(long tick);

    long windriposte$getLastShieldDisableTick();
    void windriposte$setLastShieldDisableTick(long tick);
}
