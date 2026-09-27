package com.oriimprovedmobs.elite;

public final class EliteClimbingRules {
    public static final double CLIMB_SPEED = 0.2D;
    public static final double MIN_HEIGHT_ADVANTAGE = 1.0D;
    public static final double BUILD_RANGE = 16.0D;
    public static final long BUILD_INTERVAL_TICKS = 10L;

    private EliteClimbingRules() {}

    public static boolean shouldInstallGoal(boolean hasPathfinder, boolean alreadyInstalled) {
        return hasPathfinder && !alreadyInstalled;
    }

    public static boolean targetIsHigher(double mobY, double targetY) {
        return targetY - mobY >= MIN_HEIGHT_ADVANTAGE;
    }

    public static boolean withinBuildRange(double dx, double dz) {
        return dx * dx + dz * dz <= BUILD_RANGE * BUILD_RANGE;
    }

    public static boolean shouldClimb(
            boolean hasPathfinder,
            boolean hasPlayerTarget,
            boolean targetAlive,
            boolean onClimbable,
            boolean targetHigher) {
        return hasPathfinder && hasPlayerTarget && targetAlive && onClimbable && targetHigher;
    }

    public static boolean shouldPursuePath(
            boolean hasPathfinder,
            boolean hasPlayerTarget,
            boolean targetAlive,
            boolean pathIncomplete,
            boolean targetHigher) {
        return hasPathfinder && hasPlayerTarget && targetAlive && pathIncomplete && targetHigher;
    }

    public static boolean canPlaceBlock(
            boolean hasPathfinder,
            boolean chasingPlayer,
            boolean mobGriefingAllowed,
            boolean positionReplaceable,
            boolean supportOk) {
        return hasPathfinder && chasingPlayer && mobGriefingAllowed && positionReplaceable && supportOk;
    }

    public static boolean cooldownReady(long gameTime, long nextBuildGameTime) {
        return gameTime >= nextBuildGameTime;
    }
}
