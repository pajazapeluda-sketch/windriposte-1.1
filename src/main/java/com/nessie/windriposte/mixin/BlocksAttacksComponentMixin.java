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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

    // Vanilla shield disable is 5s = 100 ticks
    private static final int SHIELD_DISABLE_TICKS = 100;

    // “Lead-up inhale” timing
    private static final int INHALE_LEAD_TICKS = 25; // ~1.25s before land

    // Extra “rearm time” AFTER the shield returns (option #1 behavior)
    private static final int EXTRA_REARM_TICKS = 25; // match inhale-ish length

    // AOE radius
    private static final double RADIUS = 3.75;

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

        WindRiposteState state = (WindRiposteState) player;

        // if still rearming, we *still* want this disable to reset the cue chain
        state.windriposte$setArmed(false);

        // --- enchant level check ---
        // If you’re using your own enchant id:
        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> entry = enchReg.getEntry(Identifier.of("windriposte", "wind_riposte")).orElse(null);
        if (entry == null) return;

        int level = EnchantmentHelper.getLevel(entry, shield);
        if (level <= 0) return;

        // --- schedule sound cues + rearm ---
        long now = WindRiposteMod.TICK;

        long shieldBackTick = now + SHIELD_DISABLE_TICKS;
        long inhaleTick = Math.max(now + 1, shieldBackTick - INHALE_LEAD_TICKS);
        long readyTick = shieldBackTick + EXTRA_REARM_TICKS;

        state.windriposte$setLandTick(shieldBackTick);
        state.windriposte$setInhaleTick(inhaleTick);
        state.windriposte$setReadyTick(readyTick);

        // reset play flags so they can play again even if spam-disabled
        state.windriposte$setPlayedLand(false);
        state.windriposte$setPlayedInhale(false);

        // --- AOE push (same strength all around) ---
        double strength = 0.9 + 0.35 * (level - 1); // L1 0.9, L2 1.25, L3 1.6
        double lift = 0.18 + 0.07 * (level - 1);    // L1 0.18, L2 0.25, L3 0.32

        Vec3d center = new Vec3d(player.getX(), player.getY(), player.getZ());

        Box box = new Box(
                player.getX() - RADIUS, player.getY() - 2.0, player.getZ() - RADIUS,
                player.getX() + RADIUS, player.getY() + 2.0, player.getZ() + RADIUS
        );

        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, ent -> ent != player && ent.isAlive())) {
            Vec3d pos = new Vec3d(e.getX(), e.getY(), e.getZ());
            Vec3d dir = pos.subtract(center);
            dir = new Vec3d(dir.x, 0.0, dir.z);

            if (dir.lengthSquared() < 1.0E-6) continue;
            dir = dir.normalize();

            // Better than raw addVelocity for players
            e.takeKnockback(strength, -dir.x, -dir.z); // NOTE: takeKnockback expects “toward attacker” style signs
            e.addVelocity(0.0, lift, 0.0);

            e.velocityDirty = true;

            world.spawnParticles(
                    ParticleTypes.GUST,
                    e.getX(), e.getBodyY(0.5), e.getZ(),
                    6 + level * 6,
                    0.25, 0.15, 0.25,
                    0.02
            );
        }

        // --- durability cost (keep it) ---
        int extraDamage = level * 5;
        // active hand is the hand that was blocking
        shield.damage(extraDamage, player, player.getActiveHand());
    }
}
