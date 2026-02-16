package com.nessie.windriposte;

public interface WindRiposteState {
    // When the enchant is allowed to trigger again
    long windriposte$getReadyTick();
    void windriposte$setReadyTick(long tick);

    // One-shot sound scheduling
    long windriposte$getInhaleTick();
    void windriposte$setInhaleTick(long tick);

    long windriposte$getLandTick();
    void windriposte$setLandTick(long tick);

    boolean windriposte$getPlayedInhale();
    void windriposte$setPlayedInhale(boolean v);

    boolean windriposte$getPlayedLand();
    void windriposte$setPlayedLand(boolean v);

    boolean windriposte$isArmed();
    void windriposte$setArmed(boolean v);
}
