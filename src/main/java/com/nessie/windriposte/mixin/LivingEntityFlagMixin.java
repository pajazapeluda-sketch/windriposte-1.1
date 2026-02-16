package com.nessie.windriposte.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFlagMixin {

    // store last attacker on the *player* by casting to WindRiposteState (player mixin provides it)
    @Inject(method = "damage", at = @At("HEAD"))
    private void windriposte$storeLastAttacker(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(source.getAttacker() instanceof LivingEntity attacker)) return;

        // only store on players (players have WindRiposteState via PlayerEntityWindRiposteStateMixin)
        if (self instanceof net.minecraft.entity.player.PlayerEntity player) {
            ((com.nessie.windriposte.WindRiposteState) player).windriposte$setLastAttacker(attacker);
        }
    }
}
