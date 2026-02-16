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
        // Runs every server tick (also in singleplayer because it uses an integrated server)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long tick = server.getTicks();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                WindRiposteState state = (WindRiposteState) player;

                // Play LAND exactly when shield comes back
                long landTick = state.windriposte$getLandTick();
                if (landTick >= 0 && !state.windriposte$getPlayedLand() && tick >= landTick) {
                    state.windriposte$setPlayedLand(true);

                    player.getServerWorld().playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_BREEZE_LAND,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                    );
                }

                // Play INHALE as lead-up to enchant being active
                long inhaleTick = state.windriposte$getInhaleTick();
                if (inhaleTick >= 0 && !state.windriposte$getPlayedInhale() && tick >= inhaleTick) {
                    state.windriposte$setPlayedInhale(true);

                    player.getServerWorld().playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_BREEZE_INHALE,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                    );
                }

                // Re-arm when readyTick hits
                long readyTick = state.windriposte$getReadyTick();
                if (readyTick > 0 && tick >= readyTick) {
                    state.windriposte$setArmed(true);

                    // Optional: clear schedule so it doesn't retrigger
                    state.windriposte$setReadyTick(0);
                    state.windriposte$setInhaleTick(-1);
                    state.windriposte$setLandTick(-1);
                }
            }
        });
    }
}
