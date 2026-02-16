package com.nessie.windriposte;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
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

                // LAND exactly when shield returns
                long landTick = state.windriposte$getLandTick();
                if (landTick >= 0 && !state.windriposte$getPlayedLand() && tick >= landTick) {
                    state.windriposte$setPlayedLand(true);

                    // plays at the player's position (no world lookup needed)
                    player.playSound(
                            SoundEvents.ENTITY_BREEZE_LAND,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                    );
                }

                // INHALE as the lead-up to enchant re-arm
                long inhaleTick = state.windriposte$getInhaleTick();
                if (inhaleTick >= 0 && !state.windriposte$getPlayedInhale() && tick >= inhaleTick) {
                    state.windriposte$setPlayedInhale(true);

                    player.playSound(
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
