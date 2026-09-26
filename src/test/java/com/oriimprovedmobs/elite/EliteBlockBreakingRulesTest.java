package com.oriimprovedmobs.elite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteBlockBreakingRulesTest {
    @Test
    void acceptsBothHardnessBoundariesForAnAllowedPursuer() {
        assertTrue(EliteBlockBreakingRules.canBreakBlock(true, true, true, 0.0F, false));
        assertTrue(EliteBlockBreakingRules.canBreakBlock(true, true, true, 3.0F, false));
    }

    @Test
    void rejectsWrongTraitTargetRuleBlockEntityAndHardness() {
        assertFalse(EliteBlockBreakingRules.canBreakBlock(false, true, true, 1.0F, false));
        assertFalse(EliteBlockBreakingRules.canBreakBlock(true, false, true, 1.0F, false));
        assertFalse(EliteBlockBreakingRules.canBreakBlock(true, true, false, 1.0F, false));
        assertFalse(EliteBlockBreakingRules.canBreakBlock(true, true, true, 1.0F, true));
        assertFalse(EliteBlockBreakingRules.canBreakBlock(true, true, true, -1.0F, false));
        assertFalse(EliteBlockBreakingRules.canBreakBlock(true, true, true, 3.01F, false));
    }

    @Test
    void allowsAtMostOneBreakEveryTenTicks() {
        assertFalse(EliteBlockBreakingRules.cooldownReady(9L, 10L));
        assertTrue(EliteBlockBreakingRules.cooldownReady(10L, 10L));
    }
}
