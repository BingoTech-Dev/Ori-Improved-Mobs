package com.oriimprovedmobs.events;

import com.oriimprovedmobs.elite.EliteConcealmentRules;
import com.oriimprovedmobs.elite.EliteData;
import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EliteConcealmentHandler {
    private static final int CONCEALED_EFFECT_DURATION = Integer.MAX_VALUE;

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        EnumSet<EliteType> types = EliteData.read(mob);
        if (!types.contains(EliteType.SHROUDED)) {
            return;
        }
        refresh(mob, types, Math.max(0.0F, mob.getHealth() - event.getAmount()));
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        EnumSet<EliteType> types = EliteData.read(mob);
        if (!types.contains(EliteType.SHROUDED)) {
            return;
        }
        refresh(mob, types, Math.min(mob.getMaxHealth(), mob.getHealth() + event.getAmount()));
    }

    static void refresh(Mob mob, EnumSet<EliteType> types, float effectiveHealth) {
        boolean concealed = EliteConcealmentRules.shouldConceal(types, effectiveHealth, mob.getMaxHealth());
        mob.setGlowingTag(EliteConcealmentRules.shouldGlow(types, concealed));
        if (!types.contains(EliteType.SHROUDED)) {
            return;
        }
        mob.setCustomNameVisible(EliteConcealmentRules.shouldShowName(types, concealed));
        if (concealed) {
            if (!mob.hasEffect(MobEffects.INVISIBILITY)) {
                mob.addEffect(new MobEffectInstance(
                        MobEffects.INVISIBILITY, CONCEALED_EFFECT_DURATION, 0, false, false));
            }
        } else {
            mob.removeEffect(MobEffects.INVISIBILITY);
        }
    }
}
