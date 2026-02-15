package com.nessie.windriposte;

import net.minecraft.entity.LivingEntity;

public interface WindRiposteState {
    // attacker tracking (for push direction etc.)
    LivingEntity windriposte$getLastAttacker();
    void windriposte$setLastAttacker(LivingEntity attacker);
    void windriposte$clearLastAttacker();

    // cooldown/arming logic
    long windriposte$getReadyTick();          // tick when riposte becomes active again
    void windriposte$setReadyTick(long tick);

    boolean windriposte$getWasShieldCoolingDown();
    void windriposte$setWasShieldCoolingDown(boolean value);

    boolean windriposte$getPendingRearm();    // we only do land+inhale if the last disable involved the enchant
    void windriposte$setPendingRearm(boolean value);
}
