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
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

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

        // must have the enchant on THIS shield
        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> windRiposteEntry =
                enchReg.getEntry(Identifier.of(WindRiposteMod.MODID, "wind_riposte")).orElse(null);
        if (windRiposteEntry == null) return;

        int level = EnchantmentHelper.getLevel(windRiposteEntry, shield);
        if (level <= 0) return;

        WindRiposteState state = (WindRiposteState) player;
        long now = world.getTime();

        // Check if enchant is currently armed BEFORE we disarm it
        boolean armed = now >= state.windriposte$getReadyTick();

        // Every disable disarms + schedules rearm behavior
        state.windriposte$setReadyTick(Long.MAX_VALUE);     // disarmed until cooldown ends + inhale finishes
        state.windriposte$setPendingRearm(true);            // tells tick mixin to play land+inhale when cooldown ends

        // Only trigger the actual push if it was armed at the moment of disable
        if (!armed) return;

        LivingEntity attacker = state.windriposte$getLastAttacker();
        state.windriposte$clearLastAttacker();
        if (attacker == null || attacker.isRemoved()) return;

        // Push all around you
        final double strength = 1.25 + 0.75 * (level - 1);
        final double lift     = 0.15 + 0.10 * (level - 1);

        Vec3d center = new Vec3d(player.getX(), player.getY(), player.getZ());

        for (LivingEntity e : world.getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(4.0),
                ent -> ent != player && !ent.isRemoved()
        )) {
            Vec3d ePos = new Vec3d(e.getX(), e.getY(), e.getZ());
            Vec3d dir = ePos.subtract(center);
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
                    10 + (level * 8),
                    0.25, 0.15, 0.25,
                    0.02
            );
        }

        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BREEZE_WIND_BURST,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
        );
    }
}
