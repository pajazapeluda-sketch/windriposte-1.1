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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

    // 1.21.11: applyShieldCooldown is static
    @Inject(method = "applyShieldCooldown", at = @At("HEAD"))
    private static void windriposte$onShieldDisabled(
            ServerWorld world,
            LivingEntity defender,
            float amount,
            ItemStack shield,
            CallbackInfo ci
    ) {
        doRiposteAoE(world, defender, shield);
    }

    private static void doRiposteAoE(ServerWorld world, LivingEntity defender, ItemStack shield) {
        if (!(defender instanceof PlayerEntity player)) return;
        if (shield == null || shield.isEmpty()) return;

        // Stored attacker (used as "proof" that a real attacker caused this cooldown)
        LivingEntity attacker = ((WindRiposteState) defender).windriposte$getLastAttacker();
        ((WindRiposteState) defender).windriposte$clearLastAttacker();
        if (attacker == null || attacker.isRemoved()) return;

        // Enchantment check
        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Identifier id = Identifier.of(WindRiposteMod.MODID, "wind_riposte");
        RegistryEntry<Enchantment> windRiposteEntry = enchReg.getEntry(id).orElse(null);
        if (windRiposteEntry == null) return;

        int level = EnchantmentHelper.getLevel(windRiposteEntry, shield);
        if (level <= 0) return;

        // Balance knobs by level
        double radius   = 2.75 + 0.75 * (level - 1);   // L1 2.75, L2 3.5, L3 4.25
        double strength = 0.90 + 0.45 * (level - 1);   // L1 0.90, L2 1.35, L3 1.80
        double lift     = 0.10 + 0.08 * (level - 1);   // L1 0.10, L2 0.18, L3 0.26

        // Find nearby living entities (AoE)
        Box box = player.getBoundingBox().expand(radius, 1.25, radius);

        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                box,
                e -> e != null
                        && e.isAlive()
                        && e != player
                        && !e.isRemoved()
                        // don’t fling spectators/creative players
                        && (!(e instanceof PlayerEntity pe) || (!pe.isSpectator() && !pe.getAbilities().creativeMode))
        );

        if (targets.isEmpty()) return;

        // Apply knockback away from the defender
        for (LivingEntity t : targets) {
            double dx = player.getX() - t.getX(); // correct direction for takeKnockback
            double dz = player.getZ() - t.getZ();

            // If somehow perfectly aligned (rare), skip
            if ((dx * dx + dz * dz) < 1.0E-6) continue;

            t.takeKnockback(strength, dx, dz);
            t.addVelocity(0.0, lift, 0.0);
            t.velocityDirty = true;
        }

        // Particles + sound (centered on defender)
        world.spawnParticles(
                ParticleTypes.GUST,
                player.getX(),
                player.getBodyY(0.5),
                player.getZ(),
                16 + (level * 10),
                0.6, 0.2, 0.6,
                0.02
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

        // Extra durability cost (keep your tradeoff)
        // NOTE: this stacks on top of normal blocking damage
        int extraDamage = level * 5;

        // IMPORTANT: ItemStack.damage in 1.21.11 needs a slot/hand, not a lambda
        // We’ll just damage it directly (no break animation callback needed)
        // This works fine server-side.
        shield.damage(extraDamage, player, player.getActiveHand());
    }
}
