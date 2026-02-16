package com.nessie.windriposte;

import com.nessie.windriposte.mixin.EntityWorldAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class WindRiposteMod implements ModInitializer {
    public static final String MODID = "windriposte";

    // ✅ BlocksAttacksComponentMixin uses this
    public static volatile long TICK = 0;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WindRiposteMod::onEndServerTick);
    }

    private static void onEndServerTick(MinecraftServer server) {
        TICK = server.getTicks();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // ✅ Never hard-cast. If mixin didn’t apply, skip (no crash).
            if (!(player instanceof WindRiposteState state)) continue;

            // ✅ In your mappings, player.getWorld() doesn’t exist.
            // Use the accessor you already have.
            ServerWorld world = (ServerWorld) ((EntityWorldAccessor) player).windriposte$getWorld();
            if (world == null) continue;

            long tick = TICK;

            // "land" = exact moment shield is back
            long landTick = state.windriposte$getLandTick();
            if (landTick >= 0 && !state.windriposte$getPlayedLand() && tick >= landTick) {
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

            // "inhale" = lead-up sound
            long inhaleTick = state.windriposte$getInhaleTick();
            if (inhaleTick >= 0 && !state.windriposte$getPlayedInhale() && tick >= inhaleTick) {
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

            // re-arm exactly at readyTick
            long readyTick = state.windriposte$getReadyTick();
            if (readyTick > 0 && tick >= readyTick) {
                state.windriposte$setArmed(true);

                // CONFIRMATION (actionbar)
                player.sendMessage(net.minecraft.text.Text.literal("§a[WindRiposte] READY"), true);
                
                // clear schedule
                state.windriposte$setReadyTick(0);
                state.windriposte$setInhaleTick(-1);
                state.windriposte$setLandTick(-1);
            }
        }
    }
}
