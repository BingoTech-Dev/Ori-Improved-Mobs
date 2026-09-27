package com.oriimprovedmobs.events;

import com.oriimprovedmobs.elite.EliteCombatRules;
import com.oriimprovedmobs.elite.EliteCooldowns;
import com.oriimprovedmobs.elite.EliteData;
import com.oriimprovedmobs.elite.EliteHitEffects;
import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EliteCombatHandler {
    private static final int SLOW_DURATION_TICKS = 40;
    private static final int SLOW_AMPLIFIER = 0;
    private static final int FROSTBORN_RETALIATION_DURATION_TICKS = 20;

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        Entity causingEntity = source.getEntity();
        boolean meleeAttack = source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
        applyFrostbornRetaliation(target, causingEntity, meleeAttack);

        if (!(causingEntity instanceof Mob attacker)) {
            return;
        }

        EnumSet<EliteType> types = EliteData.read(attacker);
        if (types.isEmpty()) {
            return;
        }

        float bonus = EliteCombatRules.attackDamageBonus(
                types,
                meleeAttack,
                target.getHealth(),
                target.getMaxHealth(),
                target.isOnFire());
        if (bonus > 0.0F) {
            event.setAmount(event.getAmount() * (1.0F + bonus));
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

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F || event.getEntity().level().isClientSide) {
            return;
        }
        Entity causingEntity = event.getSource().getEntity();
        if (!(causingEntity instanceof Mob attacker) || attacker == event.getEntity()) {
            return;
        }
        EnumSet<EliteType> types = EliteData.read(attacker);
        float healAmount = EliteCombatRules.lifestealHeal(types, event.getAmount());
        if (healAmount > 0.0F) {
            attacker.heal(healAmount);
        }
    }

    private static void applyFrostbornRetaliation(
            LivingEntity target, Entity causingEntity, boolean meleeAttack) {
        if (!(target instanceof Mob defender)
                || !(causingEntity instanceof LivingEntity attacker)
                || attacker == defender) {
            return;
        }

        EnumSet<EliteType> defenderTypes = EliteData.read(defender);
        if (!EliteCombatRules.shouldFrostbornRetaliate(defenderTypes, meleeAttack)) {
            return;
        }

        long gameTime = defender.level().getGameTime();
        if (!EliteCooldowns.isFrostbornRetaliationReady(defender.getPersistentData(), gameTime)) {
            return;
        }

        boolean effectApplied = attacker.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, FROSTBORN_RETALIATION_DURATION_TICKS, SLOW_AMPLIFIER));
        if (effectApplied) {
            EliteCooldowns.markFrostbornRetaliation(defender.getPersistentData(), gameTime);
        }
    }
}
