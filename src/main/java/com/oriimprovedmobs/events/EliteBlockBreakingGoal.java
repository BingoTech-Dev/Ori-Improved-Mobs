package com.oriimprovedmobs.events;

import com.oriimprovedmobs.elite.EliteBlockBreakingRules;
import com.oriimprovedmobs.elite.EliteData;
import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeHooks;

public final class EliteBlockBreakingGoal extends Goal {
    private static final String NEXT_BREAK_GAME_TIME_KEY = "ori_improved_mobs:breacher_next_break_game_time";
    private static final int BLOCK_UPDATE_LIMIT = 512;

    private final Mob mob;

    public EliteBlockBreakingGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return findBreakableBlock() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return findBreakableBlock() != null;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos blockPos = findBreakableBlock();
        if (blockPos == null) {
            return;
        }

        if (ForgeHooks.canEntityDestroy(serverLevel, blockPos, mob)) {
            serverLevel.destroyBlock(blockPos, false, mob, BLOCK_UPDATE_LIMIT);
        }
        mob.getPersistentData().putLong(
                NEXT_BREAK_GAME_TIME_KEY,
                serverLevel.getGameTime() + EliteBlockBreakingRules.BREAK_INTERVAL_TICKS);
    }

    private BlockPos findBreakableBlock() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        boolean hasBreacher = EliteData.read(mob).contains(EliteType.BREACHER);
        LivingEntity target = mob.getTarget();
        boolean hasPlayerTarget = target instanceof Player;
        boolean targetAlive = target != null && target.isAlive();
        boolean mobGriefingAllowed = serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        Path path = mob.getNavigation().getPath();
        boolean pathIncomplete = path != null && !path.canReach();
        if (!hasBreacher || !hasPlayerTarget || !targetAlive || !mobGriefingAllowed || !pathIncomplete) {
            return null;
        }

        long gameTime = serverLevel.getGameTime();
        long nextBreakGameTime = mob.getPersistentData().getLong(NEXT_BREAK_GAME_TIME_KEY);
        if (!EliteBlockBreakingRules.cooldownReady(gameTime, nextBreakGameTime)) {
            return null;
        }

        BlockHitResult hit = serverLevel.clip(new ClipContext(
                mob.getBoundingBox().getCenter(),
                target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos blockPos = hit.getBlockPos();
        if (!serverLevel.isLoaded(blockPos)) {
            return null;
        }

        BlockState blockState = serverLevel.getBlockState(blockPos);
        boolean hasBlockEntity = blockState.hasBlockEntity() || serverLevel.getBlockEntity(blockPos) != null;
        boolean hasBreakableBlockHit = !blockState.isAir() && EliteBlockBreakingRules.canBreakBlock(
                hasBreacher,
                hasPlayerTarget && targetAlive,
                mobGriefingAllowed,
                blockState.getDestroySpeed(serverLevel, blockPos),
                hasBlockEntity);
        if (!EliteBlockBreakingRules.shouldStartGoal(
                hasBreacher,
                hasPlayerTarget,
                targetAlive,
                pathIncomplete,
                hasBreakableBlockHit,
                gameTime,
                nextBreakGameTime)) {
            return null;
        }

        return blockPos;
    }
}
