package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.DoubleSupplier;

public final class EliteSpawnPolicy {
    private EliteSpawnPolicy() {}

    public static EnumSet<EliteType> select(
            boolean naturalSpawn,
            boolean monsterCategory,
            boolean boss,
            boolean alreadyElite,
            boolean globallyEnabled,
            EliteSpawnContext context,
            Set<EliteType> enabledTypes,
            DoubleSupplier roll,
            double chance) {
        if (!naturalSpawn || !monsterCategory || boss || alreadyElite || !globallyEnabled) {
            return EnumSet.noneOf(EliteType.class);
        }
        EnumSet<EliteType> candidates = EliteTypeResolver.resolve(context, enabledTypes);
        if (candidates.isEmpty() || chance <= 0.0D || roll.getAsDouble() >= chance) {
            return EnumSet.noneOf(EliteType.class);
        }
        return candidates;
    }
}
