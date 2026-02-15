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

    // Tune these by ear / feel:
    private static final long VANILLA_DISABLE_TICKS = 5L * 20L; // 5 seconds
    private static final long INHALE_TICKS = 30L;              // ~1.5 seconds (adjust until it matches the sound)

    @Inject(method = "applyShieldCooldown", at = @At("HEAD"))
    private static void windriposte$onShieldDisabled(ServerWorld world, LivingEntity defender, float amount, ItemStack shield, CallbackInfo ci) {
        doRiposte(world, defender, shield);
    }

    private static void doRiposte(ServerWorld world, LivingEntity defender, ItemStack shield) {
        if (!(defender instanceof PlayerEntity player)) return;
        if (shield == null || shield.isEmpty()) return;

        WindRiposteState state = (WindRiposteState) defender;
        long now = world.getTime();

        // If we’re still in the “not armed yet” window, do nothing.
        if (now < state.windriposte$getNextRiposteTick()) return;

        // Grab attacker stored by LivingEntityStoreAttackerMixin
        LivingEntity attacker = state.windriposte$getLastAttacker();
        state.windriposte$clearLastAttacker();
        if (attacker == null || attacker.isRemoved()) return;

        // Enchantment check
        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Identifier id = Identifier.of(WindRiposteMod.MODID, "wind_riposte");
        RegistryEntry<Enchantment> windRiposteEntry = enchReg.getEntry(id).orElse(null);
        if (windRiposteEntry == null) return;

        int level = EnchantmentHelper.getLevel(windRiposteEntry, shield);
        if (level <= 0) return;

        // Schedule: inhale at shield-return time, then enchant becomes active after inhale duration.
        long inhaleAt = now + VANILLA_DISABLE_TICKS;
        state.windriposte$setInhaleTick(inhaleAt);

        long armedAt = inhaleAt + INHALE_TICKS;
        state.windriposte$setNextRiposteTick(armedAt);

        // AoE launch (same strength all around)
        double radius = 3.0 + 0.75 * level;
        double strength = 1.25 + 0.75 * (level - 1);
        double lift = 0.15 + 0.10 * (level - 1);

        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class,
                defender.getBoundingBox().expand(radius),
                e -> e != null && e.isAlive() && e != defender
        )) {
            Vec3d dir = new Vec3d(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
            if (dir.lengthSquared() < 1.0E-6) continue;
            dir = dir.normalize();

            target.addVelocity(dir.x * strength, lift, dir.z * strength);
            target.velocityDirty = true;
        }

        // Particles + sound for the burst itself
        world.spawnParticles(
                ParticleTypes.GUST,
                player.getX(),
                player.getBodyY(0.5),
                player.getZ(),
                12 + (level * 10),
                0.35, 0.20, 0.35,
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
    }
}
