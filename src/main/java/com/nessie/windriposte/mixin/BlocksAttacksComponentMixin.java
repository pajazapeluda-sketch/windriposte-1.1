package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteMod;
import com.nessie.windriposte.WindRiposteState;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
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

        // Attacker stored by your other hook (WindRiposteState)
        LivingEntity attacker = ((WindRiposteState) defender).windriposte$getLastAttacker();
        ((WindRiposteState) defender).windriposte$clearLastAttacker();
        if (attacker == null || attacker.isRemoved()) return;

        // --- Enchantment check ---
        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Identifier id = Identifier.of(WindRiposteMod.MODID, "wind_riposte");
        RegistryEntry<Enchantment> windRiposteEntry = enchReg.getEntry(id).orElse(null);
        if (windRiposteEntry == null) return;

        int level = EnchantmentHelper.getLevel(windRiposteEntry, shield);
        if (level <= 0) return;

        // --- Balance by level ---
        double strength = 1.25 + 0.75 * (level - 1); // 1.25, 2.0, 2.75...
        double lift     = 0.15 + 0.10 * (level - 1); // 0.15, 0.25, 0.35...

        // --- Reliable knockback (works better for players) ---
        double dx = attacker.getX() - player.getX();
        double dz = attacker.getZ() - player.getZ();
        attacker.takeKnockback(strength, dx, dz);
        attacker.addVelocity(0.0, lift, 0.0);
        attacker.velocityDirty = true;

        // Force sync for other clients (THIS is the big fix for PvP testing)
        if (attacker instanceof ServerPlayerEntity sp) {
            sp.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(sp));
        }

        // --- Durability cost (keep this) ---
        int extraDamage = level * 5;
        Hand hand = (player.getOffHandStack() == shield) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        shield.damage(extraDamage, player, hand);

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
