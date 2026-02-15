package com.nessie.windriposte;

import net.minecraft.entity.LivingEntity;

public interface WindRiposteState {
    LivingEntity windriposte$getLastAttacker();
    void windriposte$setLastAttacker(LivingEntity attacker);
    void windriposte$clearLastAttacker();

    // When the enchantment becomes active again (server ticks)
    long windriposte$getNextRiposteTick();
    void windriposte$setNextRiposteTick(long tick);

    // When we should play the inhale sound (server ticks)
    long windriposte$getInhaleTick();
    void windriposte$setInhaleTick(long tick);
}
