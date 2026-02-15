package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityRiposteInhaleMixin {

    // tweak this if you want inhale window longer/shorter
    private static final int INHALE_TICKS = 22; // ~1.1s at 20 TPS

    @Inject(method = "tick", at = @At("TAIL"))
    private void windriposte$tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;

        if (!(player instanceof WindRiposteState state)) return;
        if (!(player instanceof EntityWorldAccessor worldAcc)) return;
        if (!(worldAcc.windriposte$getWorld() instanceof ServerWorld world)) return;

        // Check if ANY shield stack the player holds is currently cooling down
        boolean coolingDownNow = isShieldCoolingDown(player);

        boolean wasCoolingDown = state.windriposte$getWasShieldCoolingDown();

        // Transition: cooldown ended this tick
        if (wasCoolingDown && !coolingDownNow) {
            if (state.windriposte$getPendingRearm()) {
                long now = world.getTime();

                // 1) exact moment shield becomes usable
                world.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_BREEZE_LAND,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.0f
                );

                // 2) lead-up sound for enchantment becoming active
                world.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_BREEZE_INHALE,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.0f
                );

                // Enchant becomes active after inhale finishes
                state.windriposte$setReadyTick(now + INHALE_TICKS);
                state.windriposte$setPendingRearm(false);
            }
        }

        state.windriposte$setWasShieldCoolingDown(coolingDownNow);
    }

    private static boolean isShieldCoolingDown(PlayerEntity player) {
        // You can hold shield in main hand or offhand
        ItemStack main = player.getMainHandStack();
        ItemStack off  = player.getOffHandStack();

        boolean mainShield = main != null && !main.isEmpty() && main.isOf(Items.SHIELD);
        boolean offShield  = off  != null && !off.isEmpty()  && off.isOf(Items.SHIELD);

        if (mainShield && player.getItemCooldownManager().isCoolingDown(main)) return true;
        if (offShield  && player.getItemCooldownManager().isCoolingDown(off))  return true;

        return false;
    }
}
