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

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

    // The "extra enchant cooldown" length in ticks (20 ticks = 1 second).
    // Make this match how long you want the inhale lead-up to be.
    private static final int EXTRA_ARM_DELAY_TICKS = 20;  // 1.0s
    private static final int INHALE_LEAD_TICKS     = 20;  // start inhale 1.0s before re-arm

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
        // Some versions pass "amount" that is NOT reliable as ticks, so we hardcode the known vanilla value.
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

        // Clear stored attacker so it doesn't linger (even though we're doing AoE now)
        state.windriposte$clearLastAttacker();

        // --------- CROWD VERSION (AoE) ---------
        // Radius scales slightly with level (tweak freely)
        double radius = 3.25 + 0.75 * (level - 1); // L1 3.25, L2 4.0, L3 4.75
        Box box = player.getBoundingBox().expand(radius, 1.5, radius);

        // Balance by level (same for everyone hit)
        double strength = 1.25 + 0.75 * (level - 1);
        double lift     = 0.15 + 0.10 * (level - 1);

        boolean hitSomeone = false;

        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, e -> e != null && e.isAlive() && e != player)) {
            if (e.isRemoved()) continue;

            Vec3d ePos = new Vec3d(e.getX(), e.getY(), e.getZ());
            Vec3d pPos = new Vec3d(player.getX(), player.getY(), player.getZ());

            Vec3d dir = ePos.subtract(pPos);
            dir = new Vec3d(dir.x, 0.0, dir.z);

            if (dir.lengthSquared() < 1.0E-6) continue;
            dir = dir.normalize();

            e.addVelocity(dir.x * strength, lift, dir.z * strength);
            e.velocityDirty = true;

            world.spawnParticles(
                    ParticleTypes.GUST,
                    e.getX(),
                    e.getBodyY(0.5),
                    e.getZ(),
                    8 + (level * 6),
                    0.25, 0.15, 0.25,
                    0.02
            );

            hitSomeone = true;
        }

        // One burst sound for the whole crowd pop (not per entity)
        if (hitSomeone) {
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
    }
}
