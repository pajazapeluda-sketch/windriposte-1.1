package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityRiposteInhaleMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void windriposte$playInhaleWhenShieldReturns(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player.getWorld() instanceof ServerWorld world)) return;

        WindRiposteState state = (WindRiposteState) player;

        long inhaleTick = state.windriposte$getInhaleTick();
        if (inhaleTick <= 0) return;

        long now = world.getTime();
        if (now < inhaleTick) return;

        // Play inhale exactly once
        state.windriposte$setInhaleTick(0L);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_BREEZE_INHALE,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
        );
    }
}
