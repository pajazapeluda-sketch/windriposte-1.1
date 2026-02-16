package com.nessie.windriposte;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class WindRiposteMod implements ModInitializer {

    public static final String MODID = "windriposte";

    public static final RegistryKey<Enchantment> WIND_RIPOSTE =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "wind_riposte"));

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long tick = server.getTicks();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                WindRiposteState state = (WindRiposteState) player;

                // Works in your mappings:
                ServerWorld world = (ServerWorld) player.getWorld();

                // LAND exactly when shield returns
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

                // INHALE lead-up to re-arm
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

                // Re-arm when ready tick hits
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
