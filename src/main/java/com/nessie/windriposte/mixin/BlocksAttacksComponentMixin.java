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
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

    // In 1.21.11 this method is static, so the injector MUST be static.
    @Inject(method = "applyShieldCooldown", at = @At("HEAD"))
    private static void windriposte$onShieldDisabled(
            ServerWorld world,
            LivingEntity defender,
            float amount,
            ItemStack shield,
            CallbackInfo ci
    ) {
        doRiposte(world, defender, shield);
    }

    private static void doRiposte(ServerWorld world, LivingEntity defender, ItemStack shield) {
        if (!(defender instanceof PlayerEntity player)) return;
        if (shield == null || shield.isEmpty()) return;

        // Grab attacker stored by your other hook (WindRiposteState)
        LivingEntity attacker = ((WindRiposteState) defender).windriposte$getLastAttacker();
        ((WindRiposteState) defender).windriposte$clearLastAttacker();
        if (attacker == null) return;
        if (attacker.isRemoved()) return;

        // --- Enchantment check ---
        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        Identifier id = Identifier.of(WindRiposteMod.MODID, "wind_riposte");
        RegistryEntry<Enchantment> windRiposteEntry = enchReg.getEntry(id).orElse(null);
        if (windRiposteEntry == null) return;

        int level = EnchantmentHelper.getLevel(windRiposteEntry, shield);
        if (level <= 0) return;

        // --- Direction: push attacker away from player (horizontal) ---
        Vec3d attackerPos = new Vec3d(attacker.getX(), attacker.getY(), attacker.getZ());
        Vec3d playerPos   = new Vec3d(player.getX(), player.getY(), player.getZ());

        Vec3d dir = attackerPos.subtract(playerPos);
        dir = new Vec3d(dir.x, 0.0, dir.z);

        if (dir.lengthSquared() < 1.0E-6) return;
        dir = dir.normalize();

        // --- Balance by level ---
        double strength = 1.25 + 0.75 * (level - 1); // 1.25, 2.0, 2.75
        double lift     = 0.15 + 0.10 * (level - 1); // 0.15, 0.25, 0.35

        attacker.addVelocity(dir.x * strength, lift, dir.z * strength);
        attacker.velocityDirty = true;

        // --- Extra durability loss ---
        int extraDamage = level * 5;
        // If the shield is ALWAYS in offhand for your use-case:
        shield.damage(extraDamage, player, Hand.OFF_HAND);

        // --- Wind-y particles + sound ---
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
    }
}
