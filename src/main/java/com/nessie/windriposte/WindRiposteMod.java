package com.nessie.windriposte;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class WindRiposteMod implements ModInitializer {
    public static final String MODID = "windriposte";

    @Override
    public void onInitialize() {

        // Server tick: play inhale/land sounds & re-arm at the right time
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long tick = server.getTicks();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

                // ✅ NEVER hard-cast. If mixin didn't apply, we just skip.
                if (!(player instanceof WindRiposteState state)) continue;

                ServerWorld world = server.getWorld(player.getWorld().getRegistryKey());
                if (world == null) continue;

                long landTick = state.windriposte$getLandTick();
                if (landTick >= 0 && !state.windriposte$getPlayedLand() && tick >= landTick) {
                    state.windriposte$setPlayedLand(true);

                    // "land" = exact moment shield is back
                    world.playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_BREEZE_LAND,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                    );
                }

                long inhaleTick = state.windriposte$getInhaleTick();
                if (inhaleTick >= 0 && !state.windriposte$getPlayedInhale() && tick >= inhaleTick) {
                    state.windriposte$setPlayedInhale(true);

                    // "inhale" = lead-up sound
                    world.playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_BREEZE_INHALE,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                    );
                }

                long readyTick = state.windriposte$getReadyTick();
                if (readyTick > 0 && tick >= readyTick) {
                    state.windriposte$setArmed(true);

                    // clear schedule
                    state.windriposte$setReadyTick(0);
                    state.windriposte$setInhaleTick(-1);
                    state.windriposte$setLandTick(-1);
                }
            }
        });
    }
}
