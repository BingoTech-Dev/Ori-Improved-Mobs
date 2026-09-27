package com.oriimprovedmobs.events;

import com.oriimprovedmobs.elite.EliteBlockBreakingRules;
import com.oriimprovedmobs.elite.EliteData;
import com.oriimprovedmobs.elite.ElitePresentation;
import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EliteJoinHandler {
    private static final String NAME_APPLIED_KEY = "ori_improved_mobs:elite_name_applied";
    private static final int ELITE_EFFECT_DURATION = Integer.MAX_VALUE;

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        EnumSet<EliteType> types = EliteData.read(mob);
        if (types.isEmpty()) {
            return;
        }

        mob.setCustomNameVisible(false);

        CompoundTag persistentData = mob.getPersistentData();
        if (!persistentData.getBoolean(NAME_APPLIED_KEY)) {
            mob.setCustomName(ElitePresentation.name(mob.getDisplayName(), types));
            persistentData.putBoolean(NAME_APPLIED_KEY, true);
        }

        EliteConcealmentHandler.refresh(mob, types, mob.getHealth());

        if (types.contains(EliteType.NIGHT_STALKER)) {
            addEffectIfMissing(mob, MobEffects.MOVEMENT_SPEED);
        }
        if (types.contains(EliteType.INFERNAL)) {
            addEffectIfMissing(mob, MobEffects.FIRE_RESISTANCE);
        }

        boolean blockBreakingGoalInstalled = mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrappedGoal -> wrappedGoal.getGoal() instanceof EliteBlockBreakingGoal);
        if (EliteBlockBreakingRules.shouldInstallGoal(
                types.contains(EliteType.BREACHER), blockBreakingGoalInstalled)) {
            mob.goalSelector.addGoal(1, new EliteBlockBreakingGoal(mob));
        }
    }

    private static void addEffectIfMissing(Mob mob, MobEffect effect) {
        if (!mob.hasEffect(effect)) {
            mob.addEffect(new MobEffectInstance(effect, ELITE_EFFECT_DURATION, 0, false, true));
        }
    }
}
