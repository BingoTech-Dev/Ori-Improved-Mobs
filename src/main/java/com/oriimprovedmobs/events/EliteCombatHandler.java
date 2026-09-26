package com.oriimprovedmobs.events;

import com.oriimprovedmobs.elite.EliteCombatRules;
import com.oriimprovedmobs.elite.EliteData;
import com.oriimprovedmobs.elite.EliteHitEffects;
import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EliteCombatHandler {
    private static final int SLOW_DURATION_TICKS = 40;
    private static final int SLOW_AMPLIFIER = 0;

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Mob attacker)) {
            return;
        }

        EnumSet<EliteType> types = EliteData.read(attacker);
        if (types.isEmpty()) {
            return;
        }

        EliteHitEffects effects = EliteCombatRules.onHit(types);
        if (effects.slowTarget()) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION_TICKS, SLOW_AMPLIFIER));
        }
        if (effects.igniteTarget() && !target.fireImmune()) {
            target.setSecondsOnFire(2);
        }
    }
}
