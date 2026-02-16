package com.nessie.windriposte;

import com.nessie.windriposte.mixin.EntityWorldAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class WindRiposteMod implements ModInitializer {

    public static final String MODID = "windriposte";

    // global “server tick counter”
    public static long TICK = 0;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TICK++;

            for (PlayerEntity player : server.getPlayerManager().getPlayerList()) {
                WindRiposteState state = (WindRiposteState) player;

                // get ServerWorld safely using accessor (since your mappings hide getWorld())
                if (!(((EntityWorldAccessor) player).windriposte$getWorld() instanceof ServerWorld world)) continue;

                // LAND (exact moment shield cooldown ends)
                long landTick = state.windriposte$getLandTick();
                if (landTick >= 0 && !state.windriposte$getPlayedLand() && TICK >= landTick) {
                    state.windriposte$setPlayedLand(true);
                    world.playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_BREEZE_LAND,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                    );
                }

                // INHALE (lead-up)
                long inhaleTick = state.windriposte$getInhaleTick();
                if (inhaleTick >= 0 && !state.windriposte$getPlayedInhale() && TICK >= inhaleTick) {
                    state.windriposte$setPlayedInhale(true);
                    world.playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_BREEZE_INHALE,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                    );
                }

                // ready again
                long readyTick = state.windriposte$getReadyTick();
                if (readyTick >= 0 && TICK >= readyTick) {
                    state.windriposte$setArmed(true);

                    // clear timers so it doesn’t loop forever
                    state.windriposte$setReadyTick(-1);
                    state.windriposte$setInhaleTick(-1);
                    state.windriposte$setLandTick(-1);
                    state.windriposte$setPlayedInhale(false);
                    state.windriposte$setPlayedLand(false);
                }
            }
        });
    }
}
