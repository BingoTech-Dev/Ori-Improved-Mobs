package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteConcealmentRulesTest {
    @Test
    void concealsShroudedMobAtAndAboveHalfHealth() {
        var types = EnumSet.of(EliteType.SHROUDED);

        assertTrue(EliteConcealmentRules.shouldConceal(types, 20.0F, 20.0F));
        assertTrue(EliteConcealmentRules.shouldConceal(types, 10.0F, 20.0F));
        assertFalse(EliteConcealmentRules.shouldConceal(types, 9.9F, 20.0F));
    }

    @Test
    void neverConcealsMobsWithoutTheShroudedAffix() {
        assertFalse(EliteConcealmentRules.shouldConceal(
                EnumSet.of(EliteType.NIGHT_STALKER), 20.0F, 20.0F));
    }

    @Test
    void concealedShroudedMobHidesGlowAndName() {
        var types = EnumSet.of(EliteType.SHROUDED);

        assertFalse(EliteConcealmentRules.shouldGlow(types, true));
        assertFalse(EliteConcealmentRules.shouldShowName(types, true));
    }

    @Test
    void revealedShroudedMobShowsGlowAndName() {
        var types = EnumSet.of(EliteType.SHROUDED);

        assertTrue(EliteConcealmentRules.shouldGlow(types, false));
        assertTrue(EliteConcealmentRules.shouldShowName(types, false));
    }

    @Test
    void otherElitesKeepGlowingWithHoverOnlyNames() {
        var types = EnumSet.of(EliteType.INFERNAL);

        assertTrue(EliteConcealmentRules.shouldGlow(types, false));
        assertFalse(EliteConcealmentRules.shouldShowName(types, false));
    }
}
