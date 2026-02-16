package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteMod;
import com.nessie.windriposte.WindRiposteState;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

    // Vanilla shield disable is 5s = 100 ticks (this is the "shield disabled" cooldown)
    private static final int SHIELD_DISABLED_TICKS = 100;

    // Breeze inhale length-ish. Adjust this number to match what you hear (try 18–28)
    private static final int INHALE_TICKS = 22;

    // AoE tuning
    private static final double RADIUS = 3.5;

    @Inject(method = "applyShieldCooldown", at = @At("HEAD"))
    private static void windriposte$onShieldDisabled(
            ServerWorld world,
            LivingEntity defender,
            float amount,
            ItemStack shield,
            CallbackInfo ci
    ) {
        if (!(defender instanceof PlayerEntity player)) return;
        if (shield == null || shield.isEmpty()) return;

        // Only run if the shield actually has the enchant
        int level = getWindRiposteLevel(world, shield);
        if (level <= 0) return;

        WindRiposteState state = (WindRiposteState) player;

        // If we're not armed, we still reschedule sounds/timing (fixes your issue #3)
        scheduleSoundsAndArming(world.getServer(), player, state);

        // Only push when armed
        if (!state.windriposte$isArmed()) return;

        // Disarm immediately so it can't chain-trigger while disabled
        state.windriposte$setArmed(false);

        // Push everything around you (including behind you, like you decided to keep 😈)
        doAoEPush(world, player, level);

        // Particles & burst sound at activation moment
        world.spawnParticles(
                ParticleTypes.GUST,
                player.getX(),
                player.getBodyY(0.6),
                player.getZ(),
                18 + (level * 10),
                0.35, 0.18, 0.35,
                0.03
        );

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_BREEZE_WIND_BURST,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
        );
    }

    private static int getWindRiposteLevel(ServerWorld world, ItemStack shield) {
        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> entry = enchReg.getEntry(Identifier.of(WindRiposteMod.MODID, "wind_riposte")).orElse(null);
        if (entry == null) return 0;
        return EnchantmentHelper.getLevel(entry, shield);
    }

    private static void scheduleSoundsAndArming(MinecraftServer server, PlayerEntity player, WindRiposteState state) {
        long now = server.getTicks();

        long shieldBackTick = now + SHIELD_DISABLED_TICKS;      // exact moment shield returns
        long readyTick      = shieldBackTick + INHALE_TICKS;    // enchant becomes active after inhale finishes

        // Reset schedule every time (even if already cooling down)
        state.windriposte$setLandTick(shieldBackTick);
        state.windriposte$setInhaleTick(shieldBackTick);

        state.windriposte$setPlayedLand(false);
        state.windriposte$setPlayedInhale(false);

        state.windriposte$setReadyTick(readyTick);
        // armed stays false until readyTick hits (WindRiposteMod tick handler flips it)
    }

    private static void doAoEPush(ServerWorld world, PlayerEntity player, int level) {
        Vec3d center = new Vec3d(player.getX(), player.getY(), player.getZ());

        Box box = player.getBoundingBox().expand(RADIUS, 1.5, RADIUS);
        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                box,
                e -> e != player && e.isAlive() && !e.isRemoved()
        );

        // Balance by level (tweak freely)
        double strength = 1.1 + 0.55 * (level - 1); // 1.1, 1.65, 2.2
        double lift     = 0.12 + 0.07 * (level - 1); // 0.12, 0.19, 0.26

        for (LivingEntity e : targets) {
            Vec3d ePos = new Vec3d(e.getX(), e.getY(), e.getZ());
            Vec3d dir = ePos.subtract(center);
            dir = new Vec3d(dir.x, 0.0, dir.z);

            if (dir.lengthSquared() < 1.0E-6) continue;
            dir = dir.normalize();

            e.addVelocity(dir.x * strength, lift, dir.z * strength);
            e.velocityDirty = true;
        }
    }
}
