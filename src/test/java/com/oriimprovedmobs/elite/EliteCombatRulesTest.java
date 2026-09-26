package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
