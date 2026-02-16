package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteMod;
import com.nessie.windriposte.WindRiposteState;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

    // The "extra enchant cooldown" length in ticks (20 ticks = 1 second).
    // Make this match how long you want the inhale lead-up to be.
    private static final int EXTRA_ARM_DELAY_TICKS = 20;  // 1.0s
    private static final int INHALE_LEAD_TICKS     = 20;  // start inhale 1.0s before re-arm

    // Crowd settings
    private static final double CROWD_MAX_RANGE = 5.5; // blocks
    private static final double CROWD_PARTICLE_CHANCE = 0.45; // not every mob needs particles

    @Inject(method = "applyShieldCooldown", at = @At("HEAD"))
    private static void windriposte$onShieldDisabled(
            ServerWorld world,
            LivingEntity defender,
            float amount,
            ItemStack shield,
            CallbackInfo ci
    ) {
        doRiposteAndSchedule(world, defender, shield, amount);
    }

    private static void doRiposteAndSchedule(ServerWorld world, LivingEntity defender, ItemStack shield, float amount) {
        if (!(defender instanceof PlayerEntity player)) return;
        if (shield == null || shield.isEmpty()) return;

        WindRiposteState state = (WindRiposteState) defender;

        // --- Enchantment check ---
        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Identifier id = Identifier.of(WindRiposteMod.MODID, "wind_riposte");
        RegistryEntry<Enchantment> entry = enchReg.getEntry(id).orElse(null);
        if (entry == null) return;

        int level = EnchantmentHelper.getLevel(entry, shield);
        if (level <= 0) return;

        // If we are not armed, do not riposte.
        // But we STILL reschedule the audio + re-arm timing every time shield gets disabled.
        boolean armed = state.windriposte$isArmed();
        state.windriposte$setArmed(false);

        // --- Schedule sounds and re-arm ticks ---
        long now = world.getTime();

        // Vanilla shield disable is 5 seconds = 100 ticks.
        long shieldBackTick = now + 100;

        // Enchant becomes active AFTER the extra delay, not when shield returns.
        long readyTick = shieldBackTick + EXTRA_ARM_DELAY_TICKS;

        // Inhale is a lead-up before readyTick.
        long inhaleTick = readyTick - INHALE_LEAD_TICKS;
        if (inhaleTick < now) inhaleTick = now;

        // Land is the exact moment the enchantment becomes active again.
        long landTick = readyTick;

        state.windriposte$setReadyTick(readyTick);
        state.windriposte$setInhaleTick(inhaleTick);
        state.windriposte$setLandTick(landTick);

        // Reset “played” flags every time the shield gets disabled so it works on repeated disables.
        state.windriposte$setPlayedInhale(false);
        state.windriposte$setPlayedLand(false);

        // --- Only apply the knockback if we were armed at the moment of disable ---
        if (!armed) return;

        // Identify the TRUE attacker (the one who disabled the shield)
        LivingEntity attacker = state.windriposte$getLastAttacker();
        state.windriposte$clearLastAttacker();
        if (attacker == null) return;
        if (attacker.isRemoved()) return;

        // Level-based base strength/lift (full power for attacker)
        double baseStrength = 1.25 + 0.75 * (level - 1);
        double baseLift     = 0.15 + 0.10 * (level - 1);

        // 1) FULL POWER to attacker (always)
        pushEntityAwayFromPlayer(attacker, player, baseStrength, baseLift);

        // Burst particles + sound centered on attacker (feels responsive)
        world.spawnParticles(
                ParticleTypes.GUST,
                attacker.getX(),
                attacker.getBodyY(0.5),
                attacker.getZ(),
                10 + (level * 8),
                0.25, 0.15, 0.25,
                0.02
        );

        world.playSound(
                null,
                attacker.getX(),
                attacker.getY(),
                attacker.getZ(),
                SoundEvents.ENTITY_BREEZE_WIND_BURST,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
        );

        // 2) CROWD AoE with DISTANCE FALLOFF (excluding attacker)
        Box box = player.getBoundingBox().expand(CROWD_MAX_RANGE, 4.0, CROWD_MAX_RANGE);

        List<Entity> entities = world.getOtherEntities(
                player,
                box,
                e -> e instanceof LivingEntity le && le != attacker && !le.isRemoved()
        );

        for (Entity e : entities) {
            LivingEntity le = (LivingEntity) e;

            // Horizontal distance from player (don’t punish Y differences)
            double dx = le.getX() - player.getX();
            double dz = le.getZ() - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist <= 1.0E-6) continue;
            if (dist > CROWD_MAX_RANGE) continue;

            // Linear falloff: 1 at dist=0 → 0 at dist=maxRange
            double t = 1.0 - (dist / CROWD_MAX_RANGE);
            t = MathHelper.clamp(t, 0.0, 1.0);

            // Scale strength/lift by falloff
            double strength = baseStrength * t;
            double lift = baseLift * (0.35 + 0.65 * t); // crowd lift is softer so it doesn’t look goofy at edge

            if (strength <= 0.01) continue;

            pushEntityAwayFromPlayer(le, player, strength, lift);

            // Optional: occasional gust particles for crowd, not all of them (keeps it readable)
            if (world.random.nextDouble() < CROWD_PARTICLE_CHANCE) {
                world.spawnParticles(
                        ParticleTypes.GUST,
                        le.getX(),
                        le.getBodyY(0.5),
                        le.getZ(),
                        4 + (int)(6 * t),
                        0.18, 0.10, 0.18,
                        0.01
                );
            }
        }
    }

    private static void pushEntityAwayFromPlayer(LivingEntity target, PlayerEntity player, double strength, double lift) {
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());

        Vec3d dir = targetPos.subtract(playerPos);
        dir = new Vec3d(dir.x, 0.0, dir.z);

        if (dir.lengthSquared() < 1.0E-6) return;
        dir = dir.normalize();

        target.addVelocity(dir.x * strength, lift, dir.z * strength);
        target.velocityDirty = true;
    }
}
