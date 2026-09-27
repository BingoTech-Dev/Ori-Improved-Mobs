package com.oriimprovedmobs.elite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteClimbingRulesTest {
    @Test
    void installsOnlyOnceAndOnlyForPathfinders() {
        assertTrue(EliteClimbingRules.shouldInstallGoal(true, false));
        assertFalse(EliteClimbingRules.shouldInstallGoal(true, true));
        assertFalse(EliteClimbingRules.shouldInstallGoal(false, false));
    }

    @Test
    void requiresTheTargetToBeAWholeBlockHigher() {
        assertTrue(EliteClimbingRules.targetIsHigher(64.0D, 65.0D));
        assertFalse(EliteClimbingRules.targetIsHigher(64.0D, 64.99D));
    }

    @Test
    void acceptsTargetsWithinSixteenBlocksAndRejectsFartherOnes() {
        assertTrue(EliteClimbingRules.withinBuildRange(16.0D, 0.0D));
        assertTrue(EliteClimbingRules.withinBuildRange(12.0D, 10.0D));
        assertFalse(EliteClimbingRules.withinBuildRange(16.01D, 0.0D));
    }

    @Test
    void climbsOnlyWhileInsideAClimbableBlockWithAHigherTarget() {
        assertTrue(EliteClimbingRules.shouldClimb(true, true, true, true, true));
        assertFalse(EliteClimbingRules.shouldClimb(true, true, true, false, true));
        assertFalse(EliteClimbingRules.shouldClimb(true, true, true, true, false));
        assertFalse(EliteClimbingRules.shouldClimb(false, true, true, true, true));
    }

    @Test
    void pursuesThePathOnlyWithAnIncompletePathAndAHigherTarget() {
        assertTrue(EliteClimbingRules.shouldPursuePath(true, true, true, true, true));
        assertFalse(EliteClimbingRules.shouldPursuePath(true, true, true, false, true));
        assertFalse(EliteClimbingRules.shouldPursuePath(true, true, true, true, false));
        assertFalse(EliteClimbingRules.shouldPursuePath(false, true, true, true, true));
    }

    @Test
    void placesBlocksOnlyForAGriefingApprovedPursuerOnAValidSpot() {
        assertTrue(EliteClimbingRules.canPlaceBlock(true, true, true, true, true));
        assertFalse(EliteClimbingRules.canPlaceBlock(false, true, true, true, true));
        assertFalse(EliteClimbingRules.canPlaceBlock(true, false, true, true, true));
        assertFalse(EliteClimbingRules.canPlaceBlock(true, true, false, true, true));
        assertFalse(EliteClimbingRules.canPlaceBlock(true, true, true, false, true));
        assertFalse(EliteClimbingRules.canPlaceBlock(true, true, true, true, false));
    }

    @Test
    void allowsAtMostOnePlacementEveryTenTicks() {
        assertFalse(EliteClimbingRules.cooldownReady(9L, 10L));
        assertTrue(EliteClimbingRules.cooldownReady(10L, 10L));
    }
}
