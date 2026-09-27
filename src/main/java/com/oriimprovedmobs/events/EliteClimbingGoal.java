package com.oriimprovedmobs.events;

import com.oriimprovedmobs.elite.EliteClimbingRules;
import com.oriimprovedmobs.elite.EliteData;
import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

public final class EliteClimbingGoal extends Goal {
    private static final String NEXT_BUILD_GAME_TIME_KEY = "ori_improved_mobs:pathfinder_next_build_game_time";
    private static final double MOVE_SPEED = 1.0D;

    private final Mob mob;

    public EliteClimbingGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return plan() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return plan() != null;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Plan plan = plan();
        if (plan == null) {
            return;
        }
        switch (plan.action()) {
            case CLIMB -> climbUp();
            case PLACE_LADDER -> placeLadder(serverLevel, plan.pos());
            case PLACE_SCAFFOLD -> placeScaffolding(serverLevel, plan.pos());
            case APPROACH -> approachTarget();
        }
    }

    private enum Action {
        CLIMB, PLACE_LADDER, PLACE_SCAFFOLD, APPROACH
    }

    private record Plan(Action action, BlockPos pos) {}

    private Plan plan() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        EnumSet<EliteType> types = EliteData.read(mob);
        boolean hasPathfinder = types.contains(EliteType.PATHFINDER);
        LivingEntity target = mob.getTarget();
        boolean hasPlayerTarget = target instanceof Player;
        boolean targetAlive = target != null && target.isAlive();
        if (!hasPathfinder || !hasPlayerTarget || !targetAlive) {
            return null;
        }

        boolean targetHigher = EliteClimbingRules.targetIsHigher(mob.getY(), target.getY());
        boolean onClimbable = serverLevel.getBlockState(mob.blockPosition()).is(BlockTags.CLIMBABLE);
        if (EliteClimbingRules.shouldClimb(
                hasPathfinder, hasPlayerTarget, targetAlive, onClimbable, targetHigher)) {
            return new Plan(Action.CLIMB, mob.blockPosition());
        }

        Path path = mob.getNavigation().getPath();
        boolean pathIncomplete = path != null && !path.canReach();
        if (!EliteClimbingRules.shouldPursuePath(
                hasPathfinder, hasPlayerTarget, targetAlive, pathIncomplete, targetHigher)) {
            return null;
        }
        if (!EliteClimbingRules.withinBuildRange(target.getX() - mob.getX(), target.getZ() - mob.getZ())) {
            return new Plan(Action.APPROACH, mob.blockPosition());
        }

        boolean mobGriefingAllowed = serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        long gameTime = serverLevel.getGameTime();
        long nextBuildGameTime = mob.getPersistentData().getLong(NEXT_BUILD_GAME_TIME_KEY);
        if (!mobGriefingAllowed || !EliteClimbingRules.cooldownReady(gameTime, nextBuildGameTime)) {
            return new Plan(Action.APPROACH, mob.blockPosition());
        }

        boolean chasingPlayer = hasPlayerTarget && targetAlive;
        Direction towardsTarget = Direction.getNearest(
                target.getX() - mob.getX(), 0.0D, target.getZ() - mob.getZ());
        BlockPos ladderPos = findLadderPosition(
                serverLevel, towardsTarget, hasPathfinder, chasingPlayer, mobGriefingAllowed);
        if (ladderPos != null) {
            return new Plan(Action.PLACE_LADDER, ladderPos);
        }
        BlockPos scaffoldPos = findScaffoldingPosition(
                serverLevel, hasPathfinder, chasingPlayer, mobGriefingAllowed);
        if (scaffoldPos != null) {
            return new Plan(Action.PLACE_SCAFFOLD, scaffoldPos);
        }
        return new Plan(Action.APPROACH, mob.blockPosition());
    }

    private void climbUp() {
        LivingEntity target = mob.getTarget();
        mob.getJumpControl().jump();
        mob.setDeltaMovement(0.0D, EliteClimbingRules.CLIMB_SPEED, 0.0D);
        if (target != null) {
            mob.getMoveControl().setWantedPosition(target.getX(), mob.getY(), target.getZ(), MOVE_SPEED);
        }
    }

    private void approachTarget() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.getMoveControl().setWantedPosition(target.getX(), mob.getY(), target.getZ(), MOVE_SPEED);
    }

    private BlockPos findLadderPosition(
            ServerLevel level,
            Direction towardsTarget,
            boolean hasPathfinder,
            boolean chasingPlayer,
            boolean mobGriefingAllowed) {
        BlockPos mobPos = mob.blockPosition();
        for (BlockPos candidate : List.of(mobPos.relative(towardsTarget), mobPos, mobPos.above())) {
            if (isLadderPlaceable(
                    level, candidate, towardsTarget, hasPathfinder, chasingPlayer, mobGriefingAllowed)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isLadderPlaceable(
            ServerLevel level,
            BlockPos ladderPos,
            Direction towardsTarget,
            boolean hasPathfinder,
            boolean chasingPlayer,
            boolean mobGriefingAllowed) {
        BlockState ladderSpace = level.getBlockState(ladderPos);
        boolean positionReplaceable = ladderSpace.canBeReplaced() && level.getFluidState(ladderPos).isEmpty();
        BlockPos supportPos = ladderPos.relative(towardsTarget);
        BlockState support = level.getBlockState(supportPos);
        boolean supportOk = support.isFaceSturdy(level, supportPos, towardsTarget.getOpposite());
        return EliteClimbingRules.canPlaceBlock(
                hasPathfinder, chasingPlayer, mobGriefingAllowed, positionReplaceable, supportOk);
    }

    private BlockPos findScaffoldingPosition(
            ServerLevel level,
            boolean hasPathfinder,
            boolean chasingPlayer,
            boolean mobGriefingAllowed) {
        if (!mob.onGround()) {
            return null;
        }
        BlockPos scaffoldPos = mob.blockPosition();
        BlockState scaffoldSpace = level.getBlockState(scaffoldPos);
        boolean positionReplaceable = scaffoldSpace.canBeReplaced() && level.getFluidState(scaffoldPos).isEmpty();
        BlockPos supportPos = scaffoldPos.below();
        BlockState support = level.getBlockState(supportPos);
        boolean supportOk = support.isFaceSturdy(level, supportPos, Direction.UP)
                || support.is(Blocks.SCAFFOLDING);
        boolean columnSupported =
                ScaffoldingBlock.getDistance(level, scaffoldPos) < ScaffoldingBlock.STABILITY_MAX_DISTANCE;
        if (EliteClimbingRules.canPlaceBlock(
                hasPathfinder, chasingPlayer, mobGriefingAllowed, positionReplaceable, supportOk)
                && columnSupported) {
            return scaffoldPos;
        }
        return null;
    }

    private void placeLadder(ServerLevel level, BlockPos ladderPos) {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        Direction towardsTarget = Direction.getNearest(
                target.getX() - mob.getX(), 0.0D, target.getZ() - mob.getZ());
        Direction ladderFacing = towardsTarget.getOpposite();
        BlockState ladderState = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, ladderFacing)
                .setValue(LadderBlock.WATERLOGGED, false);
        placeBlock(level, ladderPos, ladderState, ladderFacing);
    }

    private void placeScaffolding(ServerLevel level, BlockPos scaffoldPos) {
        int distance = ScaffoldingBlock.getDistance(level, scaffoldPos);
        BlockState support = level.getBlockState(scaffoldPos.below());
        BlockState scaffoldState = Blocks.SCAFFOLDING.defaultBlockState()
                .setValue(ScaffoldingBlock.DISTANCE, distance)
                .setValue(ScaffoldingBlock.WATERLOGGED, false)
                .setValue(ScaffoldingBlock.BOTTOM, distance > 0 && !support.is(Blocks.SCAFFOLDING));
        placeBlock(level, scaffoldPos, scaffoldState, Direction.UP);
    }

    private void placeBlock(ServerLevel level, BlockPos pos, BlockState state, Direction placedAgainst) {
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (ForgeEventFactory.onBlockPlace(mob, snapshot, placedAgainst)) {
            return;
        }
        level.setBlockAndUpdate(pos, state);
        mob.getPersistentData().putLong(
                NEXT_BUILD_GAME_TIME_KEY,
                level.getGameTime() + EliteClimbingRules.BUILD_INTERVAL_TICKS);
    }
}
