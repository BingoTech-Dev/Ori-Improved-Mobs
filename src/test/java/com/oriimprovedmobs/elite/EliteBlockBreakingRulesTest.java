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

    @Test
    void startsOnlyForBreacherWithLivePlayerBlockedPathAndReadyCooldown() {
        assertTrue(EliteBlockBreakingRules.shouldStartGoal(true, true, true, true, true, 10L, 10L));
        assertFalse(EliteBlockBreakingRules.shouldStartGoal(false, true, true, true, true, 10L, 10L));
        assertFalse(EliteBlockBreakingRules.shouldStartGoal(true, false, true, true, true, 10L, 10L));
        assertFalse(EliteBlockBreakingRules.shouldStartGoal(true, true, false, true, true, 10L, 10L));
        assertFalse(EliteBlockBreakingRules.shouldStartGoal(true, true, true, false, true, 10L, 10L));
        assertFalse(EliteBlockBreakingRules.shouldStartGoal(true, true, true, true, false, 10L, 10L));
        assertFalse(EliteBlockBreakingRules.shouldStartGoal(true, true, true, true, true, 9L, 10L));
    }

    @Test
    void installsGoalOnlyOnceAndOnlyForBreacher() {
        assertTrue(EliteBlockBreakingRules.shouldInstallGoal(true, false));
        assertFalse(EliteBlockBreakingRules.shouldInstallGoal(true, true));
        assertFalse(EliteBlockBreakingRules.shouldInstallGoal(false, false));
    }
}
