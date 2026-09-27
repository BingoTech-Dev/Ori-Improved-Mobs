package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteCombatRulesTest {
    @Test
    void noEliteTypesHaveNoOnHitEffects() {
        assertEquals(new EliteHitEffects(false, false), EliteCombatRules.onHit(EnumSet.noneOf(EliteType.class)));
    }

    @Test
    void frostbornSlowsItsTarget() {
        assertEquals(new EliteHitEffects(true, false),
                EliteCombatRules.onHit(EnumSet.of(EliteType.FROSTBORN)));
    }

    @Test
    void infernalIgnitesItsTarget() {
        assertEquals(new EliteHitEffects(false, true),
                EliteCombatRules.onHit(EnumSet.of(EliteType.INFERNAL)));
    }

    @Test
    void combinedTypesApplyBothEffects() {
        assertEquals(new EliteHitEffects(true, true),
                EliteCombatRules.onHit(EnumSet.of(EliteType.FROSTBORN, EliteType.INFERNAL)));
    }

    @Test
    void nightStalkerBonusRequiresMeleeAndTargetBelowHalfHealth() {
        var nightStalker = EnumSet.of(EliteType.NIGHT_STALKER);

        assertEquals(0.15F,
                EliteCombatRules.attackDamageBonus(nightStalker, true, 9.9F, 20.0F, false), 0.0001F);
        assertEquals(0.0F,
                EliteCombatRules.attackDamageBonus(nightStalker, true, 10.0F, 20.0F, false), 0.0001F);
        assertEquals(0.0F,
                EliteCombatRules.attackDamageBonus(nightStalker, false, 9.0F, 20.0F, false), 0.0001F);
    }

    @Test
    void infernalBonusAppliesToBurningTargetsForMeleeAndProjectileDamage() {
        var infernal = EnumSet.of(EliteType.INFERNAL);

        assertEquals(0.15F,
                EliteCombatRules.attackDamageBonus(infernal, true, 15.0F, 20.0F, true), 0.0001F);
        assertEquals(0.15F,
                EliteCombatRules.attackDamageBonus(infernal, false, 15.0F, 20.0F, true), 0.0001F);
        assertEquals(0.0F,
                EliteCombatRules.attackDamageBonus(infernal, false, 15.0F, 20.0F, false), 0.0001F);
    }

    @Test
    void conditionalDamageBonusesAddInsteadOfMultiply() {
        var types = EnumSet.of(EliteType.NIGHT_STALKER, EliteType.INFERNAL);

        assertEquals(0.30F,
                EliteCombatRules.attackDamageBonus(types, true, 9.0F, 20.0F, true), 0.0001F);
    }

    @Test
    void frostbornRetaliationRequiresFrostbornAndMeleeHit() {
        assertTrue(EliteCombatRules.shouldFrostbornRetaliate(
                EnumSet.of(EliteType.FROSTBORN), true));
        assertFalse(EliteCombatRules.shouldFrostbornRetaliate(
                EnumSet.of(EliteType.FROSTBORN), false));
        assertFalse(EliteCombatRules.shouldFrostbornRetaliate(
                EnumSet.noneOf(EliteType.class), true));
    }

    @Test
    void vampiricElitesHealHalfOfTheDamageTheyDeal() {
        assertEquals(2.0F,
                EliteCombatRules.lifestealHeal(EnumSet.of(EliteType.VAMPIRIC), 4.0F), 0.0001F);
    }

    @Test
    void lifestealIsZeroWithoutVampiricOrWithoutDamage() {
        assertEquals(0.0F,
                EliteCombatRules.lifestealHeal(EnumSet.of(EliteType.FROSTBORN), 4.0F), 0.0001F);
        assertEquals(0.0F,
                EliteCombatRules.lifestealHeal(EnumSet.of(EliteType.VAMPIRIC), 0.0F), 0.0001F);
    }
}
