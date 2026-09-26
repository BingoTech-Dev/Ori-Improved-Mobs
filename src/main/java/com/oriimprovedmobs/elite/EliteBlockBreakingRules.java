package com.oriimprovedmobs.elite;

public final class EliteBlockBreakingRules {
    public static final float MAX_HARDNESS = 3.0F;
    public static final long BREAK_INTERVAL_TICKS = 10L;

    private EliteBlockBreakingRules() {}

    public static boolean canBreakBlock(
            boolean hasBreacher,
            boolean chasingPlayer,
            boolean mobGriefingAllowed,
            float destroySpeed,
            boolean hasBlockEntity) {
        return hasBreacher
                && chasingPlayer
                && mobGriefingAllowed
                && !hasBlockEntity
                && destroySpeed >= 0.0F
                && destroySpeed <= MAX_HARDNESS;
    }

    public static boolean cooldownReady(long gameTime, long nextBreakGameTime) {
        return gameTime >= nextBreakGameTime;
    }

    public static boolean shouldStartGoal(
            boolean hasBreacher,
            boolean hasPlayerTarget,
            boolean targetAlive,
            boolean pathIncomplete,
            boolean hasBreakableBlockHit,
            long gameTime,
            long nextBreakGameTime) {
        return hasBreacher
                && hasPlayerTarget
                && targetAlive
                && pathIncomplete
                && hasBreakableBlockHit
                && cooldownReady(gameTime, nextBreakGameTime);
    }

    public static boolean shouldInstallGoal(boolean hasBreacher, boolean alreadyInstalled) {
        return hasBreacher && !alreadyInstalled;
    }
}
